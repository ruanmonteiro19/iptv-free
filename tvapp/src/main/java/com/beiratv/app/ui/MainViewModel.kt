package com.beiratv.app.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.beiratv.app.data.local.BeiraTVDatabase
import com.beiratv.app.data.local.ChannelEntity
import com.beiratv.app.data.local.ChannelStreamEntity
import com.beiratv.app.data.local.EpgProgramEntity
import com.beiratv.app.data.local.HistoryEntity
import com.beiratv.app.data.normalization.CategoryClassifier
import com.beiratv.app.data.normalization.TextNormalizer
import com.beiratv.app.data.preferences.PlaybackPreferences
import com.beiratv.app.data.preferences.ThemeMode
import com.beiratv.app.data.preferences.TvPreferencesRepository
import com.beiratv.app.data.repository.ChannelRepository
import com.beiratv.app.data.repository.EpgRepository
import com.beiratv.app.data.repository.HistoryRepository
import com.beiratv.app.data.repository.SourceRepository
import com.beiratv.app.data.source.BuiltInSources
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = BeiraTVDatabase.getInstance(application)
    private val channelRepository = ChannelRepository(db)
    private val sourceRepository = SourceRepository(db)
    private val epgRepository = EpgRepository(db)
    private val historyRepository = HistoryRepository(db)
    private val preferences = TvPreferencesRepository(application)
    private val metadataDao = db.metadataDao()

    private val categoryPreferences = application.getSharedPreferences(
        "beiratv_category_preferences", Context.MODE_PRIVATE
    )

    val allChannels: StateFlow<List<ChannelEntity>> = channelRepository.allChannels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val favoriteChannels: StateFlow<List<ChannelEntity>> = channelRepository.favoriteChannels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val history: StateFlow<List<HistoryEntity>> = historyRepository.history
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val regionalStates: StateFlow<List<String>> = metadataDao.getRegionalStates()
        .map { states -> states.sortedWith(compareBy<String> { if (it.equals("PA", true)) 0 else 1 }.thenBy { it }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val lastSuccessfulSync: StateFlow<Long?> = sourceRepository.lastSuccessfulSync
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val themeMode: StateFlow<ThemeMode> = preferences.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.DARK)

    val playbackPreferences: StateFlow<PlaybackPreferences> = preferences.playback
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlaybackPreferences())

    private val _favoriteCategories = MutableStateFlow(
        categoryPreferences.getStringSet("favorite_categories", emptySet()).orEmpty()
    )
    val favoriteCategories: StateFlow<Set<String>> = _favoriteCategories.asStateFlow()

    val categories: StateFlow<List<String>> = combine(
        channelRepository.categories,
        _favoriteCategories
    ) { categoryList, favoriteSet ->
        categoryList
            .filter { it.isNotBlank() }
            .distinct()
            .sortedWith(
                compareBy<String> { if (it in favoriteSet) 0 else 1 }
                    .thenBy { CategoryClassifier.order(it) }
                    .thenBy { TextNormalizer.fold(it) }
            )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _selectedRegionState = MutableStateFlow<String?>(null)
    val selectedRegionState: StateFlow<String?> = _selectedRegionState.asStateFlow()
    private val _regionalChannelIds = MutableStateFlow<Set<String>>(emptySet())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedChannel = MutableStateFlow<ChannelEntity?>(null)
    val selectedChannel: StateFlow<ChannelEntity?> = _selectedChannel.asStateFlow()

    private val _selectedStreams = MutableStateFlow<List<ChannelStreamEntity>>(emptyList())
    val selectedStreams: StateFlow<List<ChannelStreamEntity>> = _selectedStreams.asStateFlow()

    private val _currentEpg = MutableStateFlow<EpgProgramEntity?>(null)
    val currentEpg: StateFlow<EpgProgramEntity?> = _currentEpg.asStateFlow()

    private val _nextEpg = MutableStateFlow<EpgProgramEntity?>(null)
    val nextEpg: StateFlow<EpgProgramEntity?> = _nextEpg.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _syncStage = MutableStateFlow<String?>(null)
    val syncStage: StateFlow<String?> = _syncStage.asStateFlow()

    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage: StateFlow<String?> = _uiMessage.asStateFlow()

    val filteredChannels: StateFlow<List<ChannelEntity>> = combine(
        allChannels,
        _selectedCategory,
        _selectedRegionState,
        _regionalChannelIds
    ) { channels, category, region, regionalIds ->
        channels.filter { channel ->
            val categoryMatches = category.isNullOrBlank() || channel.category.equals(category, ignoreCase = true)
            val regionMatches = region.isNullOrBlank() || channel.id in regionalIds
            categoryMatches && regionMatches
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Search is deliberately independent from category/region filters. */
    val searchResults: StateFlow<List<ChannelEntity>> = combine(allChannels, _searchQuery) { channels, query ->
        val needle = TextNormalizer.fold(query)
        if (needle.isBlank()) channels else channels.filter { channel ->
            TextNormalizer.fold(channel.name).contains(needle) ||
                TextNormalizer.fold(channel.category).contains(needle) ||
                TextNormalizer.fold(channel.tvgId.orEmpty()).contains(needle)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch { channelRepository.removeLegacySampleContent() }
        viewModelScope.launch {
            val hasCache = sourceRepository.hasCachedChannels()
            if (!hasCache) _syncStage.value = "Preparando sua TV..."
            refreshChannels(force = !hasCache, silent = hasCache)
        }
        viewModelScope.launch {
            while (true) {
                delay(12L * 60L * 60L * 1000L)
                refreshChannels(force = true, silent = true)
            }
        }
    }

    fun refreshChannels(force: Boolean = true, silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) {
                _isLoading.value = true
                _syncStage.value = "Baixando canais..."
            }
            when (val result = sourceRepository.syncIfNeeded(force)) {
                is SourceRepository.SyncResult.Success -> {
                    _syncStage.value = "Organizando sua grade..."
                    _uiMessage.value = "${result.channels} canais atualizados • ${result.duplicatesGrouped} duplicatas agrupadas."
                }
                is SourceRepository.SyncResult.Failure -> _uiMessage.value = result.message
                is SourceRepository.SyncResult.Skipped -> Unit
            }
            _syncStage.value = null
            _isLoading.value = false
        }
    }

    fun refreshEpg() {
        viewModelScope.launch {
            _isLoading.value = true
            _syncStage.value = "Atualizando guia..."
            val result = epgRepository.importXmltvFromUrl(BuiltInSources.EPG_URL)
            _isLoading.value = false
            _syncStage.value = null
            result.onSuccess { _uiMessage.value = "Guia atualizado: $it programas." }
                .onFailure { _uiMessage.value = "Não foi possível atualizar o guia agora." }
        }
    }

    fun selectCategory(category: String?) {
        _selectedCategory.value = category
        if (category != "Regionais") selectRegion(null)
    }

    fun selectRegion(state: String?) {
        _selectedRegionState.value = state
        viewModelScope.launch {
            _regionalChannelIds.value = if (state == null) emptySet() else metadataDao.channelIdsForState(state).toSet()
        }
    }

    fun clearFiltersForSearch() {
        _selectedCategory.value = null
        _selectedRegionState.value = null
        _regionalChannelIds.value = emptySet()
    }

    fun toggleFavoriteCategory(category: String) {
        val updated = _favoriteCategories.value.toMutableSet().apply {
            if (!add(category)) remove(category)
        }.toSet()
        _favoriteCategories.value = updated
        categoryPreferences.edit().putStringSet("favorite_categories", updated).apply()
    }

    fun setSearchQuery(query: String) { _searchQuery.value = query }

    fun playChannel(channel: ChannelEntity) {
        _selectedChannel.value = channel
        viewModelScope.launch {
            _selectedStreams.value = channelRepository.getStreams(channel.id).ifEmpty {
                listOf(ChannelStreamEntity("legacy-${channel.id}", channel.id, "legacy", channel.streamUrl, 0))
            }
            historyRepository.recordWatch(channel)
            val epgKey = channel.tvgId ?: channel.name
            _currentEpg.value = epgRepository.getCurrentProgram(epgKey)
            _nextEpg.value = epgRepository.getNextProgram(epgKey)
        }
    }

    fun closePlayer() {
        _selectedChannel.value = null
        _selectedStreams.value = emptyList()
        _currentEpg.value = null
        _nextEpg.value = null
    }

    fun playNextChannel() = playRelative(1)
    fun playPreviousChannel() = playRelative(-1)

    private fun playRelative(offset: Int) {
        val current = _selectedChannel.value ?: return
        val list = filteredChannels.value.ifEmpty { allChannels.value }
        if (list.isEmpty()) return
        val index = list.indexOfFirst { it.id == current.id }.coerceAtLeast(0)
        val target = (index + offset + list.size) % list.size
        playChannel(list[target])
    }

    fun toggleFavorite(channel: ChannelEntity) {
        viewModelScope.launch {
            val newStatus = !channel.isFavorite
            channelRepository.toggleFavorite(channel.id, newStatus)
            if (_selectedChannel.value?.id == channel.id) {
                _selectedChannel.value = channel.copy(isFavorite = newStatus)
            }
        }
    }

    fun markStreamSuccess(streamId: String) {
        viewModelScope.launch { channelRepository.markStreamSuccess(streamId) }
    }

    fun markStreamFailure(streamId: String) {
        viewModelScope.launch { channelRepository.markStreamFailure(streamId) }
    }

    fun setTheme(mode: ThemeMode) { viewModelScope.launch { preferences.setTheme(mode) } }
    fun setAutomaticQuality(enabled: Boolean) { viewModelScope.launch { preferences.setAutomaticQuality(enabled) } }
    fun setFallbackStreams(enabled: Boolean) { viewModelScope.launch { preferences.setFallbackStreams(enabled) } }

    fun clearCache() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                getApplication<Application>().cacheDir.listFiles()?.forEach { file ->
                    runCatching { file.deleteRecursively() }
                }
            }
            _uiMessage.value = "Cache temporário limpo."
        }
    }

    fun clearUiMessage() { _uiMessage.value = null }
}

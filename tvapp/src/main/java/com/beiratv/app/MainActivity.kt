package com.beiratv.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.beiratv.app.ui.MainViewModel
import com.beiratv.app.ui.navigation.BeiraTVMainLayout
import com.beiratv.app.ui.player.Media3PlayerContainer
import com.beiratv.app.ui.theme.BeiraTVTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            BeiraTVTheme(themeMode = themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val allChannels by viewModel.allChannels.collectAsStateWithLifecycle()
                    val filteredChannels by viewModel.filteredChannels.collectAsStateWithLifecycle()
                    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
                    val favoriteChannels by viewModel.favoriteChannels.collectAsStateWithLifecycle()
                    val history by viewModel.history.collectAsStateWithLifecycle()
                    val categories by viewModel.categories.collectAsStateWithLifecycle()
                    val favoriteCategories by viewModel.favoriteCategories.collectAsStateWithLifecycle()
                    val regionalStates by viewModel.regionalStates.collectAsStateWithLifecycle()
                    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
                    val selectedRegion by viewModel.selectedRegionState.collectAsStateWithLifecycle()
                    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
                    val selectedChannel by viewModel.selectedChannel.collectAsStateWithLifecycle()
                    val selectedStreams by viewModel.selectedStreams.collectAsStateWithLifecycle()
                    val currentEpg by viewModel.currentEpg.collectAsStateWithLifecycle()
                    val nextEpg by viewModel.nextEpg.collectAsStateWithLifecycle()
                    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
                    val syncStage by viewModel.syncStage.collectAsStateWithLifecycle()
                    val uiMessage by viewModel.uiMessage.collectAsStateWithLifecycle()
                    val lastSync by viewModel.lastSuccessfulSync.collectAsStateWithLifecycle()
                    val playbackPreferences by viewModel.playbackPreferences.collectAsStateWithLifecycle()

                    Box(modifier = Modifier.fillMaxSize()) {
                        BeiraTVMainLayout(
                            channels = allChannels,
                            filteredChannels = filteredChannels,
                            searchResults = searchResults,
                            favorites = favoriteChannels,
                            history = history,
                            categories = categories,
                            favoriteCategories = favoriteCategories,
                            regionalStates = regionalStates,
                            selectedCategory = selectedCategory,
                            selectedRegion = selectedRegion,
                            searchQuery = searchQuery,
                            isLoading = isLoading,
                            syncStage = syncStage,
                            uiMessage = uiMessage,
                            themeMode = themeMode,
                            playbackPreferences = playbackPreferences,
                            lastSync = lastSync,
                            onSelectCategory = viewModel::selectCategory,
                            onSelectRegion = viewModel::selectRegion,
                            onSearchQueryChange = viewModel::setSearchQuery,
                            onOpenSearch = viewModel::clearFiltersForSearch,
                            onChannelClick = viewModel::playChannel,
                            onToggleFavorite = viewModel::toggleFavorite,
                            onToggleCategoryFavorite = viewModel::toggleFavoriteCategory,
                            onRefreshChannels = { viewModel.refreshChannels(force = true) },
                            onRefreshEpg = viewModel::refreshEpg,
                            onThemeChange = viewModel::setTheme,
                            onAutomaticQualityChange = viewModel::setAutomaticQuality,
                            onFallbackStreamsChange = viewModel::setFallbackStreams,
                            onClearCache = viewModel::clearCache,
                            onClearUiMessage = viewModel::clearUiMessage
                        )

                        selectedChannel?.let { channel ->
                            Media3PlayerContainer(
                                channel = channel,
                                streams = selectedStreams,
                                epgProgram = currentEpg,
                                nextEpgProgram = nextEpg,
                                fallbackEnabled = playbackPreferences.fallbackStreams,
                                automaticQuality = playbackPreferences.automaticQuality,
                                onClose = viewModel::closePlayer,
                                onNext = viewModel::playNextChannel,
                                onPrevious = viewModel::playPreviousChannel,
                                onToggleFavorite = viewModel::toggleFavorite,
                                onStreamSuccess = viewModel::markStreamSuccess,
                                onStreamFailure = viewModel::markStreamFailure,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
}

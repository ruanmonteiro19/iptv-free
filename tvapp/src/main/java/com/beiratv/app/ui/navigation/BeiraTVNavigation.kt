package com.beiratv.app.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.beiratv.app.data.local.ChannelEntity
import com.beiratv.app.data.local.HistoryEntity
import com.beiratv.app.data.preferences.PlaybackPreferences
import com.beiratv.app.data.preferences.ThemeMode
import com.beiratv.app.ui.screens.FavoritesScreen
import com.beiratv.app.ui.screens.HomeScreen
import com.beiratv.app.ui.screens.LiveTvScreen
import com.beiratv.app.ui.screens.SearchScreen
import com.beiratv.app.ui.screens.SettingsScreen
import com.beiratv.app.ui.tv.TvFocusableBox

enum class NavigationDestination(val title: String, val icon: ImageVector) {
    HOME("Início", Icons.Default.Home),
    LIVE_TV("Ao vivo", Icons.Default.Tv),
    FAVORITES("Favoritos", Icons.Default.Star),
    SEARCH("Buscar", Icons.Default.Search),
    SETTINGS("Ajustes", Icons.Default.Settings)
}

@Composable
fun BeiraTVMainLayout(
    channels: List<ChannelEntity>,
    filteredChannels: List<ChannelEntity>,
    searchResults: List<ChannelEntity>,
    favorites: List<ChannelEntity>,
    history: List<HistoryEntity>,
    categories: List<String>,
    favoriteCategories: Set<String>,
    regionalStates: List<String>,
    selectedCategory: String?,
    selectedRegion: String?,
    searchQuery: String,
    isLoading: Boolean,
    syncStage: String?,
    uiMessage: String?,
    themeMode: ThemeMode,
    playbackPreferences: PlaybackPreferences,
    lastSync: Long?,
    onSelectCategory: (String?) -> Unit,
    onSelectRegion: (String?) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onOpenSearch: () -> Unit,
    onChannelClick: (ChannelEntity) -> Unit,
    onToggleFavorite: (ChannelEntity) -> Unit,
    onToggleCategoryFavorite: (String) -> Unit,
    onRefreshChannels: () -> Unit,
    onRefreshEpg: () -> Unit,
    onThemeChange: (ThemeMode) -> Unit,
    onAutomaticQualityChange: (Boolean) -> Unit,
    onFallbackStreamsChange: (Boolean) -> Unit,
    onClearCache: () -> Unit,
    onClearUiMessage: () -> Unit
) {
    var currentName by rememberSaveable { mutableStateOf(NavigationDestination.HOME.name) }
    var historyStack by rememberSaveable { mutableStateOf(emptyList<String>()) }
    val current = NavigationDestination.valueOf(currentName)
    val snackbar = remember { SnackbarHostState() }

    fun navigate(destination: NavigationDestination) {
        if (destination == current) return
        if (destination == NavigationDestination.SEARCH) onOpenSearch()
        historyStack = historyStack + current.name
        currentName = destination.name
    }

    BackHandler(enabled = true) {
        when {
            current == NavigationDestination.LIVE_TV && selectedRegion != null -> onSelectRegion(null)
            current == NavigationDestination.LIVE_TV && selectedCategory != null -> onSelectCategory(null)
            current == NavigationDestination.SEARCH && searchQuery.isNotBlank() -> onSearchQueryChange("")
            historyStack.isNotEmpty() -> {
                currentName = historyStack.last()
                historyStack = historyStack.dropLast(1)
            }
            current != NavigationDestination.HOME -> currentName = NavigationDestination.HOME.name
            else -> Unit
        }
    }

    LaunchedEffect(uiMessage) {
        if (!uiMessage.isNullOrBlank()) {
            snackbar.showSnackbar(uiMessage, duration = SnackbarDuration.Short)
            onClearUiMessage()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TvTopNavigation(current = current, onNavigate = ::navigate)

        Box(modifier = Modifier.weight(1f)) {
            if (channels.isEmpty() && syncStage != null) {
                StartupLoading(stage = syncStage)
            } else if (channels.isEmpty()) {
                EmptyStartup(onRetry = onRefreshChannels)
            } else {
                when (current) {
                    NavigationDestination.HOME -> HomeScreen(
                        channels = channels,
                        favorites = favorites,
                        history = history,
                        categories = categories,
                        favoriteCategories = favoriteCategories,
                        onChannelClick = onChannelClick,
                        onCategoryClick = { category ->
                            onSelectCategory(category)
                            navigate(NavigationDestination.LIVE_TV)
                        },
                        onToggleFavorite = onToggleFavorite,
                        onToggleCategoryFavorite = onToggleCategoryFavorite
                    )
                    NavigationDestination.LIVE_TV -> LiveTvScreen(
                        channels = filteredChannels,
                        categories = categories,
                        favoriteCategories = favoriteCategories,
                        regionalStates = regionalStates,
                        selectedCategory = selectedCategory,
                        selectedRegion = selectedRegion,
                        onSelectCategory = onSelectCategory,
                        onSelectRegion = onSelectRegion,
                        onChannelClick = onChannelClick,
                        onToggleFavorite = onToggleFavorite,
                        onToggleCategoryFavorite = onToggleCategoryFavorite
                    )
                    NavigationDestination.FAVORITES -> FavoritesScreen(
                        favorites = favorites,
                        onChannelClick = onChannelClick,
                        onToggleFavorite = onToggleFavorite
                    )
                    NavigationDestination.SEARCH -> SearchScreen(
                        query = searchQuery,
                        onQueryChange = onSearchQueryChange,
                        filteredChannels = searchResults,
                        onChannelClick = onChannelClick,
                        onToggleFavorite = onToggleFavorite
                    )
                    NavigationDestination.SETTINGS -> SettingsScreen(
                        isLoading = isLoading,
                        themeMode = themeMode,
                        playbackPreferences = playbackPreferences,
                        lastSync = lastSync,
                        onRefreshChannels = onRefreshChannels,
                        onRefreshEpg = onRefreshEpg,
                        onThemeChange = onThemeChange,
                        onAutomaticQualityChange = onAutomaticQualityChange,
                        onFallbackStreamsChange = onFallbackStreamsChange,
                        onClearCache = onClearCache
                    )
                }
            }

            SnackbarHost(
                hostState = snackbar,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp)
            )
        }
    }
}

@Composable
private fun EmptyStartup(onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Não foi possível preparar os canais.", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Verifique a internet e tente novamente.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            TvFocusableBox(onClick = onRetry, cornerRadius = 10.dp, unfocusedBorder = MaterialTheme.colorScheme.primary) { focused ->
                Text(
                    "Tentar novamente",
                    color = if (focused) Color.Black else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.background(if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 18.dp, vertical = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun StartupLoading(stage: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("Tekas", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold)
                Text("TV", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold)
            }
            Text("Sua TV, do seu jeito", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f))
            Text(stage, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TvTopNavigation(
    current: NavigationDestination,
    onNavigate: (NavigationDestination) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)
    ) {
        val compact = maxWidth < 900.dp
        val horizontalPadding = if (compact) 24.dp else 36.dp
        val itemSpacing = if (compact) 2.dp else 6.dp
        val itemHorizontalPadding = if (compact) 9.dp else 13.dp
        val itemVerticalPadding = if (compact) 7.dp else 8.dp

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = horizontalPadding, vertical = if (compact) 8.dp else 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("Tekas", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                Text("TV", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.width(if (compact) 18.dp else 30.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(itemSpacing)) {
                NavigationDestination.entries.forEach { destination ->
                    TvFocusableBox(
                        onClick = { onNavigate(destination) },
                        cornerRadius = 10.dp,
                        scaleOnFocus = 1.025f,
                        unfocusedBorder = if (destination == current) MaterialTheme.colorScheme.primary else Color.Transparent
                    ) { focused ->
                        Row(
                            modifier = Modifier
                                .background(
                                    when {
                                        focused -> MaterialTheme.colorScheme.primary
                                        destination == current -> MaterialTheme.colorScheme.surfaceVariant
                                        else -> Color.Transparent
                                    }
                                )
                                .padding(horizontal = itemHorizontalPadding, vertical = itemVerticalPadding),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(destination.icon, null, tint = if (focused) Color.Black else MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                destination.title,
                                color = if (focused) Color.Black else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        }
    }
}

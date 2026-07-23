package com.beiratv.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.beiratv.app.R
import com.beiratv.app.data.local.ChannelEntity
import com.beiratv.app.data.local.HistoryEntity
import com.beiratv.app.ui.components.ChannelItemCard
import com.beiratv.app.ui.components.ChannelPlaceholderLogo
import com.beiratv.app.ui.tv.TvFocusableBox

@Composable
fun HomeScreen(
    channels: List<ChannelEntity>,
    favorites: List<ChannelEntity>,
    history: List<HistoryEntity>,
    categories: List<String>,
    favoriteCategories: Set<String>,
    onChannelClick: (ChannelEntity) -> Unit,
    onCategoryClick: (String) -> Unit,
    onToggleFavorite: (ChannelEntity) -> Unit,
    onToggleCategoryFavorite: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val side = when {
            maxWidth >= 1400.dp -> 40.dp
            maxWidth >= 880.dp -> 34.dp
            else -> 24.dp
        }
        val cardsPerScreen = when {
            maxWidth >= 1400.dp -> 8
            maxWidth >= 1100.dp -> 7
            maxWidth >= 880.dp -> 6
            maxWidth >= 680.dp -> 5
            else -> 4
        }
        val gap = if (maxWidth >= 880.dp) 12.dp else 9.dp
        val cardWidth = (maxWidth - side * 2 - gap * (cardsPerScreen - 1)) / cardsPerScreen
        val heroHeight = when {
            maxHeight >= 800.dp -> 220.dp
            maxHeight >= 620.dp -> 190.dp
            else -> 165.dp
        }
        val grouped = channels.groupBy { it.category }
        val homeOrder = listOf("Esportes", "Abertos", "Notícias", "Filmes e Séries", "Regionais", "Infantil", "Documentários", "Variedades")

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = side, end = side, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { TekasHero(heroHeight) }

            if (history.isNotEmpty()) {
                item {
                    SectionHeader("Continuar assistindo", Icons.Default.History)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        items(history.take(12), key = { it.channelId }) { item ->
                            val channel = channels.firstOrNull { it.id == item.channelId } ?: return@items
                            ContinueCard(channel, cardWidth) { onChannelClick(channel) }
                        }
                    }
                }
            }

            if (favorites.isNotEmpty()) {
                item {
                    SectionHeader("Favoritos", Icons.Default.Star)
                    ChannelRail(favorites.take(20), cardWidth, gap, onChannelClick, onToggleFavorite)
                }
            }

            if (categories.isNotEmpty()) {
                item {
                    SectionHeader("Categorias", Icons.Default.Tv)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(9.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
                        items(categories.take(12), key = { it }) { category ->
                            CategoryChip(
                                title = category,
                                favorite = category in favoriteCategories,
                                onClick = { onCategoryClick(category) },
                                onFavorite = { onToggleCategoryFavorite(category) }
                            )
                        }
                    }
                }
            }

            homeOrder.forEach { category ->
                val items = grouped[category].orEmpty()
                if (items.isNotEmpty()) {
                    item {
                        SectionHeader(category, null)
                        ChannelRail(items.take(24), cardWidth, gap, onChannelClick, onToggleFavorite)
                    }
                }
            }
        }
    }
}

@Composable
private fun TekasHero(height: Dp) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
    ) {
        Image(
            painter = painterResource(R.drawable.tekastv_logo),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .fillMaxWidth(0.42f)
                .alpha(0.48f)
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(
                    listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.25f)
                    )
                )
            )
        )
        Column(
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 28.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("Tekas", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
                Text("TV", color = MaterialTheme.colorScheme.primary, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
            }
            Text("Sua TV, do seu jeito", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text("Uma grade limpa, rápida e feita para o controle remoto.", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector?) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 5.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(7.dp))
        }
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CategoryChip(title: String, favorite: Boolean, onClick: () -> Unit, onFavorite: () -> Unit) {
    TvFocusableBox(onClick = onClick, cornerRadius = 20.dp, unfocusedBorder = MaterialTheme.colorScheme.surfaceVariant) { focused ->
        Row(
            modifier = Modifier
                .background(if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, color = if (focused) Color.Black else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            TvFocusableBox(onClick = onFavorite, modifier = Modifier.width(27.dp), cornerRadius = 14.dp, scaleOnFocus = 1.08f) {
                Text(if (favorite) "★" else "☆", color = if (favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun ContinueCard(channel: ChannelEntity, width: Dp, onClick: () -> Unit) {
    TvFocusableBox(onClick = onClick, modifier = Modifier.width(width), unfocusedBorder = MaterialTheme.colorScheme.surfaceVariant) { focused ->
        Column(
            modifier = Modifier.background(if (focused) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface).padding(7.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().height(width * 0.50f).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (channel.logo.isNullOrBlank()) {
                    ChannelPlaceholderLogo(channel.name, Modifier.fillMaxSize())
                } else {
                    SubcomposeAsyncImage(
                        model = channel.logo,
                        contentDescription = channel.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                        error = { ChannelPlaceholderLogo(channel.name, Modifier.fillMaxSize()) }
                    )
                }
                Text("▶", color = MaterialTheme.colorScheme.primary, fontSize = 26.sp)
            }
            Text(channel.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 5.dp))
            Text(channel.category, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ChannelRail(
    channels: List<ChannelEntity>,
    cardWidth: Dp,
    gap: Dp,
    onChannelClick: (ChannelEntity) -> Unit,
    onToggleFavorite: (ChannelEntity) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(gap)) {
        items(channels, key = { it.id }) { channel ->
            ChannelItemCard(
                channel = channel,
                onClick = { onChannelClick(channel) },
                onFavoriteToggle = { onToggleFavorite(channel) },
                modifier = Modifier.width(cardWidth)
            )
        }
    }
}

package com.beiratv.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beiratv.app.data.local.ChannelEntity
import com.beiratv.app.ui.components.ChannelItemCard

@Composable
fun FavoritesScreen(
    favorites: List<ChannelEntity>,
    onChannelClick: (ChannelEntity) -> Unit,
    onToggleFavorite: (ChannelEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val columns = when {
            maxWidth >= 1400.dp -> 8
            maxWidth >= 1100.dp -> 7
            maxWidth >= 880.dp -> 6
            maxWidth >= 680.dp -> 5
            else -> 4
        }
        val side = if (maxWidth >= 880.dp) 34.dp else 24.dp
        Column(Modifier.fillMaxSize()) {
            Text(
                "Favoritos",
                fontSize = 27.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(start = side, top = 18.dp, bottom = 10.dp)
            )
            if (favorites.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Você ainda não favoritou nenhum canal.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f))
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    contentPadding = PaddingValues(horizontal = side, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(favorites, key = { it.id }) { channel ->
                        ChannelItemCard(channel, { onChannelClick(channel) }, { onToggleFavorite(channel) })
                    }
                }
            }
        }
    }
}

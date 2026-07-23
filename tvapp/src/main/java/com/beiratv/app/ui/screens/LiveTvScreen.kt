package com.beiratv.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beiratv.app.data.local.ChannelEntity
import com.beiratv.app.ui.components.ChannelItemCard
import com.beiratv.app.ui.tv.TvFocusableBox

@Composable
fun LiveTvScreen(
    channels: List<ChannelEntity>,
    categories: List<String>,
    favoriteCategories: Set<String>,
    regionalStates: List<String>,
    selectedCategory: String?,
    selectedRegion: String?,
    onSelectCategory: (String?) -> Unit,
    onSelectRegion: (String?) -> Unit,
    onChannelClick: (ChannelEntity) -> Unit,
    onToggleFavorite: (ChannelEntity) -> Unit,
    onToggleCategoryFavorite: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAllCategories by remember { mutableStateOf(false) }
    BackHandler(enabled = selectedRegion != null) { onSelectRegion(null) }
    BackHandler(enabled = selectedRegion == null && selectedCategory != null) { onSelectCategory(null) }

    BoxWithConstraints(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val columns = when {
            maxWidth >= 1400.dp -> 8
            maxWidth >= 1100.dp -> 7
            maxWidth >= 880.dp -> 6
            maxWidth >= 680.dp -> 5
            else -> 4
        }
        val side = if (maxWidth >= 880.dp) 34.dp else 24.dp
        val gap = if (maxWidth >= 880.dp) 12.dp else 9.dp
        val visibleCategories = if (showAllCategories) categories else categories.take(10)

        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = side, end = side, top = 12.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Column(Modifier.weight(1f)) {
                    Text("TV ao Vivo", fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
                    Text(
                        when {
                            selectedRegion != null -> "Regionais • ${stateDisplayName(selectedRegion)}"
                            selectedCategory != null -> selectedCategory
                            else -> "Toda a grade TekasTV"
                        },
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f)
                    )
                }
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = side, vertical = 9.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { CategoryFilter("Todos", selectedCategory == null, false, { onSelectCategory(null) }, {}) }
                items(visibleCategories, key = { it }) { category ->
                    CategoryFilter(
                        label = category,
                        selected = selectedCategory == category,
                        favorite = category in favoriteCategories,
                        onClick = { onSelectCategory(category) },
                        onFavorite = { onToggleCategoryFavorite(category) }
                    )
                }
                if (categories.size > 10) {
                    item {
                        CategoryFilter(if (showAllCategories) "Menos" else "Mais", false, false, { showAllCategories = !showAllCategories }, {})
                    }
                }
            }

            if (selectedCategory == "Regionais" && regionalStates.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = side, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    item { RegionFilter("Todos", selectedRegion == null) { onSelectRegion(null) } }
                    items(regionalStates, key = { it }) { state ->
                        RegionFilter(stateDisplayName(state), selectedRegion == state) { onSelectRegion(state) }
                    }
                }
                Spacer(Modifier.height(5.dp))
            }

            if (channels.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nenhum canal nesta seleção", style = MaterialTheme.typography.titleLarge)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    contentPadding = PaddingValues(start = side, end = side, top = 6.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(gap),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(channels, key = { it.id }) { channel ->
                        ChannelItemCard(
                            channel = channel,
                            onClick = { onChannelClick(channel) },
                            onFavoriteToggle = { onToggleFavorite(channel) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryFilter(
    label: String,
    selected: Boolean,
    favorite: Boolean,
    onClick: () -> Unit,
    onFavorite: () -> Unit
) {
    TvFocusableBox(onClick = onClick, cornerRadius = 18.dp, unfocusedBorder = MaterialTheme.colorScheme.surfaceVariant) { focused ->
        Row(
            modifier = Modifier
                .background(if (selected || focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 13.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = if (selected || focused) Color.Black else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, maxLines = 1)
            if (label != "Todos" && label != "Mais" && label != "Menos") {
                Spacer(Modifier.width(7.dp))
                TvFocusableBox(onClick = onFavorite, modifier = Modifier.width(25.dp), cornerRadius = 13.dp) {
                    Text(if (favorite) "★" else "☆", color = if (selected || focused) Color.Black else MaterialTheme.colorScheme.primary, fontSize = 17.sp)
                }
            }
        }
    }
}

@Composable
private fun RegionFilter(label: String, selected: Boolean, onClick: () -> Unit) {
    TvFocusableBox(onClick = onClick, cornerRadius = 16.dp) { focused ->
        Text(
            label,
            color = if (selected || focused) Color.Black else MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .background(if (selected || focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

private fun stateDisplayName(code: String): String = when (code.uppercase()) {
    "PA" -> "Pará"
    "SP" -> "São Paulo"
    "RJ" -> "Rio de Janeiro"
    "MG" -> "Minas Gerais"
    "BA" -> "Bahia"
    "PR" -> "Paraná"
    "RS" -> "Rio Grande do Sul"
    "SC" -> "Santa Catarina"
    "CE" -> "Ceará"
    "PE" -> "Pernambuco"
    "GO" -> "Goiás"
    "MT" -> "Mato Grosso"
    "MS" -> "Mato Grosso do Sul"
    "AM" -> "Amazonas"
    "MA" -> "Maranhão"
    else -> code
}

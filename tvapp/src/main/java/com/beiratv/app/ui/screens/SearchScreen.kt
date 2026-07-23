package com.beiratv.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beiratv.app.data.local.ChannelEntity
import com.beiratv.app.ui.components.ChannelItemCard

@Composable
fun SearchScreen(
    query: String,
    onQueryChange: (String) -> Unit,
    filteredChannels: List<ChannelEntity>,
    onChannelClick: (ChannelEntity) -> Unit,
    onToggleFavorite: (ChannelEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = query.isNotBlank()) { onQueryChange("") }
    BoxWithConstraints(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val columns = when {
            maxWidth >= 1400.dp -> 7
            maxWidth >= 1100.dp -> 6
            maxWidth >= 880.dp -> 5
            maxWidth >= 680.dp -> 4
            else -> 3
        }
        val horizontalPadding = if (maxWidth >= 880.dp) 36.dp else 24.dp
        Column(Modifier.fillMaxSize()) {
            Text(
                "Buscar em todos os canais",
                fontSize = 27.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(start = horizontalPadding, top = 18.dp, bottom = 10.dp)
            )
            TvSearchField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(0.58f).padding(horizontal = horizontalPadding, vertical = 4.dp)
            )
            if (filteredChannels.isEmpty() && query.isNotBlank()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nenhum canal encontrado.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f))
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    contentPadding = PaddingValues(horizontal = horizontalPadding, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredChannels, key = { it.id }) { channel ->
                        ChannelItemCard(channel, { onChannelClick(channel) }, { onToggleFavorite(channel) })
                    }
                }
            }
        }
    }
}

@Composable
private fun TvSearchField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    var editing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    BasicTextField(
        value = value,
        onValueChange = { if (editing) onValueChange(it) },
        readOnly = !editing,
        singleLine = true,
        textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 20.sp),
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
            .focusRequester(focusRequester)
            .focusable()
            .clickable {
                editing = true
                keyboard?.show()
            }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.Enter, Key.DirectionCenter -> {
                        editing = true
                        keyboard?.show()
                        true
                    }
                    Key.Back -> if (editing) {
                        editing = false
                        keyboard?.hide()
                        true
                    } else false
                    else -> false
                }
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        decorationBox = { inner ->
            if (value.isBlank()) Text("Digite o nome do canal", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
            inner()
        }
    )
}

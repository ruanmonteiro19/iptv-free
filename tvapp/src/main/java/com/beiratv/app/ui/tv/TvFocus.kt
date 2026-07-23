package com.beiratv.app.ui.tv

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun TvFocusableBox(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 14.dp,
    scaleOnFocus: Float = 1.035f,
    focusedBorder: Color = Color(0xFFFF8A00),
    unfocusedBorder: Color = Color.Transparent,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onFocusStateChanged: (Boolean) -> Unit = {},
    content: @Composable (Boolean) -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) scaleOnFocus else 1f,
        animationSpec = tween(durationMillis = 90),
        label = "tvFocusScale"
    )
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .border(
                border = BorderStroke(if (focused) 2.dp else 1.dp, if (focused) focusedBorder else unfocusedBorder),
                shape = shape
            )
            .background(Color.Transparent, shape)
            .onFocusChanged {
                focused = it.isFocused
                onFocusStateChanged(focused)
            }
            .clickable(onClick = onClick)
            .focusable()
            .padding(contentPadding)
    ) {
        content(focused)
    }
}

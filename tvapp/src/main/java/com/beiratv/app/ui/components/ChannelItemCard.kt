package com.beiratv.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.beiratv.app.data.local.ChannelEntity
import com.beiratv.app.ui.tv.TvFocusableBox

@Composable
fun ChannelItemCard(
    channel: ChannelEntity,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    TvFocusableBox(
        onClick = onClick,
        modifier = modifier,
        unfocusedBorder = MaterialTheme.colorScheme.surfaceVariant,
        scaleOnFocus = 1.035f
    ) { focused ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (focused) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface)
                .padding(7.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (channel.logo.isNullOrBlank()) {
                    ChannelPlaceholderLogo(channel.name, Modifier.fillMaxSize())
                } else {
                    SubcomposeAsyncImage(
                        model = channel.logo,
                        contentDescription = channel.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                        loading = { ChannelPlaceholderLogo(channel.name, Modifier.fillMaxSize()) },
                        error = { ChannelPlaceholderLogo(channel.name, Modifier.fillMaxSize()) }
                    )
                }

                TvFocusableBox(
                    onClick = onFavoriteToggle,
                    modifier = Modifier.align(Alignment.TopEnd).padding(5.dp).size(30.dp),
                    cornerRadius = 15.dp,
                    scaleOnFocus = 1.10f,
                    unfocusedBorder = Color.Transparent
                ) {
                    Box(
                        modifier = Modifier.size(30.dp).background(Color.Black.copy(alpha = 0.60f), RoundedCornerShape(15.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (channel.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Favorito",
                            tint = if (channel.isFavorite) MaterialTheme.colorScheme.primary else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Text(
                text = channel.name,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 6.dp)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = channel.category,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "AO VIVO",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
fun ChannelPlaceholderLogo(name: String, modifier: Modifier = Modifier) {
    val initials = name.split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.take(1).uppercase() }
        .ifBlank { "TV" }

    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(initials, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.headlineMedium)
            Text("TEKASTV", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), style = MaterialTheme.typography.labelSmall)
        }
    }
}

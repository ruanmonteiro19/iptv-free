package com.beiratv.app.ui.screens

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beiratv.app.data.preferences.PlaybackPreferences
import com.beiratv.app.data.preferences.ThemeMode
import com.beiratv.app.ui.tv.TvFocusableBox
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    isLoading: Boolean,
    themeMode: ThemeMode,
    playbackPreferences: PlaybackPreferences,
    lastSync: Long?,
    onRefreshChannels: () -> Unit,
    onRefreshEpg: () -> Unit,
    onThemeChange: (ThemeMode) -> Unit,
    onAutomaticQualityChange: (Boolean) -> Unit,
    onFallbackStreamsChange: (Boolean) -> Unit,
    onClearCache: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
        val twoColumns = maxWidth >= 900.dp
        val pagePadding = if (maxWidth >= 1100.dp) 40.dp else 28.dp
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = pagePadding, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Configurações", fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
            Text("Ajuste o TekasTV sem precisar lidar com playlists ou URLs.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))

            SettingsPanel("Aparência", Modifier.fillMaxWidth()) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        ChoiceButton(
                            text = when (mode) {
                                ThemeMode.LIGHT -> "Claro"
                                ThemeMode.DARK -> "Escuro"
                                ThemeMode.AMOLED -> "Dark / AMOLED"
                            },
                            selected = themeMode == mode,
                            onClick = { onThemeChange(mode) }
                        )
                    }
                }
            }

            if (twoColumns) {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    SettingsPanel("Reprodução", Modifier.weight(1f)) {
                        ToggleButton("Qualidade automática", playbackPreferences.automaticQuality) {
                            onAutomaticQualityChange(!playbackPreferences.automaticQuality)
                        }
                        ToggleButton("Tentar fontes alternativas", playbackPreferences.fallbackStreams) {
                            onFallbackStreamsChange(!playbackPreferences.fallbackStreams)
                        }
                    }
                    SettingsPanel("Canais", Modifier.weight(1f)) {
                        Text(
                            "Última atualização: ${formatSync(lastSync)}",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                        )
                        ActionButton(if (isLoading) "Atualizando..." else "Atualizar canais", enabled = !isLoading, onClick = onRefreshChannels)
                        ActionButton("Atualizar guia (EPG)", enabled = !isLoading, onClick = onRefreshEpg)
                    }
                }
            } else {
                SettingsPanel("Reprodução", Modifier.fillMaxWidth()) {
                    ToggleButton("Qualidade automática", playbackPreferences.automaticQuality) {
                        onAutomaticQualityChange(!playbackPreferences.automaticQuality)
                    }
                    ToggleButton("Tentar fontes alternativas", playbackPreferences.fallbackStreams) {
                        onFallbackStreamsChange(!playbackPreferences.fallbackStreams)
                    }
                }
                SettingsPanel("Canais", Modifier.fillMaxWidth()) {
                    Text("Última atualização: ${formatSync(lastSync)}")
                    ActionButton(if (isLoading) "Atualizando..." else "Atualizar canais", !isLoading, onRefreshChannels)
                    ActionButton("Atualizar guia (EPG)", !isLoading, onRefreshEpg)
                }
            }

            SettingsPanel("Sistema", Modifier.fillMaxWidth()) {
                ActionButton("Limpar cache", true, onClearCache)
                Text("TekasTV", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                Text("Sua TV, do seu jeito", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f))
                Text("Versão 0.3.6", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f))
                Text(
                    "Android ${Build.VERSION.RELEASE} • API ${Build.VERSION.SDK_INT}",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                )
            }
        }
    }
}

@Composable
private fun SettingsPanel(title: String, modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface, androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(title, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        content()
    }
}

@Composable
private fun ChoiceButton(text: String, selected: Boolean, onClick: () -> Unit) {
    TvFocusableBox(
        onClick = onClick,
        cornerRadius = 10.dp,
        unfocusedBorder = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    ) { focused ->
        Text(
            text,
            color = if (focused || selected) Color.Black else MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .background(if (focused || selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun ToggleButton(text: String, checked: Boolean, onClick: () -> Unit) {
    TvFocusableBox(onClick = onClick, cornerRadius = 10.dp) { focused ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (focused) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(text, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(12.dp))
            Text(if (checked) "ATIVADO" else "DESATIVADO", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun ActionButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    TvFocusableBox(onClick = { if (enabled) onClick() }, cornerRadius = 10.dp) { focused ->
        Text(
            text,
            color = if (focused && enabled) Color.Black else MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .background(if (focused && enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        )
    }
}

private fun formatSync(timestamp: Long?): String {
    if (timestamp == null) return "ainda não concluída"
    return SimpleDateFormat("dd/MM 'às' HH:mm", Locale("pt", "BR")).format(Date(timestamp))
}

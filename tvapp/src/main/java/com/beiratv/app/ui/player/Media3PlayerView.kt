package com.beiratv.app.ui.player

import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackGroup
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.beiratv.app.data.local.ChannelEntity
import com.beiratv.app.data.local.ChannelStreamEntity
import com.beiratv.app.data.local.EpgProgramEntity
import com.beiratv.app.ui.tv.TvFocusableBox
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class VideoQualityOption(
    val label: String,
    val trackGroup: TrackGroup,
    val trackIndex: Int,
    val height: Int,
    val bitrate: Int
)

@OptIn(UnstableApi::class)
@Composable
fun Media3PlayerContainer(
    channel: ChannelEntity,
    streams: List<ChannelStreamEntity>,
    epgProgram: EpgProgramEntity?,
    nextEpgProgram: EpgProgramEntity?,
    fallbackEnabled: Boolean,
    automaticQuality: Boolean,
    onClose: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleFavorite: (ChannelEntity) -> Unit,
    onStreamSuccess: (String) -> Unit,
    onStreamFailure: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val actualStreams = remember(channel.id, streams) {
        streams.ifEmpty {
            listOf(ChannelStreamEntity("legacy-${channel.id}", channel.id, "legacy", channel.streamUrl, 0))
        }
    }

    var streamIndex by remember(channel.id) { mutableIntStateOf(0) }
    var isPlaying by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var fallbackMessage by remember { mutableStateOf<String?>(null) }
    var qualityOptions by remember(channel.id) { mutableStateOf<List<VideoQualityOption>>(emptyList()) }
    var selectedQualityIndex by remember(channel.id) { mutableIntStateOf(-1) }
    var controlsVisible by remember(channel.id) { mutableStateOf(true) }
    var interactionToken by remember { mutableIntStateOf(0) }

    val rootFocus = remember { FocusRequester() }
    val playFocus = remember { FocusRequester() }
    val retryFocus = remember { FocusRequester() }

    val httpFactory = remember {
        DefaultHttpDataSource.Factory()
            .setUserAgent("TekasTV-TV/0.3.6 AndroidTV")
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(8_000)
            .setReadTimeoutMs(15_000)
    }
    val mediaSourceFactory = remember {
        DefaultMediaSourceFactory(DefaultDataSource.Factory(context, httpFactory))
    }
    val player = remember {
        ExoPlayer.Builder(context).setMediaSourceFactory(mediaSourceFactory).build().apply { playWhenReady = true }
    }

    fun currentStream(): ChannelStreamEntity = actualStreams[streamIndex.coerceIn(0, actualStreams.lastIndex)]

    fun prepareCurrentStream() {
        val stream = currentStream()
        isLoading = true
        errorMessage = null
        val item = MediaItem.Builder().setUri(stream.streamUrl).apply {
            if (stream.streamUrl.contains(".m3u8", true) || stream.streamUrl.contains("/hls", true)) {
                setMimeType(MimeTypes.APPLICATION_M3U8)
            }
        }.build()
        player.setMediaItem(item)
        player.prepare()
        player.playWhenReady = true
    }

    fun showControls() {
        controlsVisible = true
        interactionToken += 1
        scope.launch {
            delay(60)
            runCatching { playFocus.requestFocus() }
        }
    }

    fun cycleQuality() {
        if (qualityOptions.isEmpty()) return
        selectedQualityIndex = if (selectedQualityIndex + 1 >= qualityOptions.size) -1 else selectedQualityIndex + 1
        player.trackSelectionParameters = if (selectedQualityIndex == -1) {
            player.trackSelectionParameters.buildUpon().clearOverridesOfType(C.TRACK_TYPE_VIDEO).build()
        } else {
            val option = qualityOptions[selectedQualityIndex]
            player.trackSelectionParameters.buildUpon()
                .setOverrideForType(TrackSelectionOverride(option.trackGroup, option.trackIndex))
                .build()
        }
    }

    BackHandler(true) {
        if (controlsVisible) controlsVisible = false else onClose()
    }

    DisposableEffect(Unit) { onDispose { player.release() } }

    LaunchedEffect(channel.id, actualStreams) {
        streamIndex = 0
        selectedQualityIndex = -1
        qualityOptions = emptyList()
        fallbackMessage = null
        controlsVisible = true
        prepareCurrentStream()
    }

    LaunchedEffect(controlsVisible, interactionToken, isPlaying, errorMessage) {
        if (controlsVisible && isPlaying && errorMessage == null) {
            delay(5_000)
            controlsVisible = false
            runCatching { rootFocus.requestFocus() }
        }
    }

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            delay(80)
            runCatching { retryFocus.requestFocus() }
        }
    }

    DisposableEffect(player, channel.id, streamIndex, fallbackEnabled) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_BUFFERING -> isLoading = true
                    Player.STATE_READY -> {
                        isLoading = false
                        fallbackMessage = null
                        errorMessage = null
                        isPlaying = player.isPlaying
                        onStreamSuccess(currentStream().id)
                    }
                    Player.STATE_ENDED -> isLoading = false
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                if (!playing) showControls()
            }

            override fun onTracksChanged(tracks: Tracks) {
                qualityOptions = buildVideoQualityOptions(tracks)
                if (automaticQuality) selectedQualityIndex = -1
            }

            override fun onPlayerError(error: PlaybackException) {
                isLoading = false
                onStreamFailure(currentStream().id)
                val nextIndex = streamIndex + 1
                if (fallbackEnabled && isFatalSourceError(error) && nextIndex < actualStreams.size) {
                    fallbackMessage = "Tentando fonte alternativa..."
                    streamIndex = nextIndex
                    scope.launch {
                        delay(450)
                        prepareCurrentStream()
                    }
                } else {
                    errorMessage = friendlyPlaybackError(error)
                    controlsVisible = false
                }
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(rootFocus)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                val isNavigation = event.key in setOf(Key.DirectionUp, Key.DirectionDown, Key.DirectionLeft, Key.DirectionRight, Key.DirectionCenter, Key.Enter)
                if (!controlsVisible && isNavigation) {
                    showControls()
                    true
                } else {
                    interactionToken += 1
                    false
                }
            }
    ) {
        val compact = maxWidth < 900.dp
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    keepScreenOn = true
                }
            },
            update = { it.player = player },
            modifier = Modifier.fillMaxSize()
        )

        AnimatedVisibility(visible = controlsVisible, enter = fadeIn(), exit = fadeOut()) {
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier.fillMaxWidth().background(
                        Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.82f), Color.Transparent))
                    ).padding(horizontal = if (compact) 24.dp else 36.dp, vertical = 18.dp)
                ) {
                    Column {
                        Text(channel.name, color = Color.White, fontSize = if (compact) 22.sp else 27.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            epgProgram?.title ?: channel.category,
                            color = Color.White.copy(alpha = 0.78f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (nextEpgProgram != null) {
                            Text(
                                "A seguir: ${nextEpgProgram.title}",
                                color = Color.White.copy(alpha = 0.62f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        if (isLoading) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.Center))
        }

        fallbackMessage?.let { message ->
            Text(
                message,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center).background(Color.Black.copy(alpha = 0.72f), RoundedCornerShape(12.dp)).padding(16.dp)
            )
        }

        if (errorMessage != null) {
            Column(
                modifier = Modifier.align(Alignment.Center).background(Color.Black.copy(alpha = 0.90f), RoundedCornerShape(16.dp)).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Não foi possível conectar ao canal", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.size(8.dp))
                Text(errorMessage.orEmpty(), color = Color.White.copy(alpha = 0.75f))
                Spacer(Modifier.size(14.dp))
                TvFocusableBox(
                    onClick = {
                        streamIndex = 0
                        errorMessage = null
                        fallbackMessage = null
                        prepareCurrentStream()
                    },
                    modifier = Modifier.focusRequester(retryFocus),
                    unfocusedBorder = MaterialTheme.colorScheme.primary
                ) { focused ->
                    Row(
                        modifier = Modifier.background(if (focused) MaterialTheme.colorScheme.primary else Color(0xFF2B2B2B)).padding(horizontal = 15.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Refresh, null, tint = if (focused) Color.Black else Color.White)
                        Spacer(Modifier.width(6.dp))
                        Text("Tentar novamente", color = if (focused) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = controlsVisible && errorMessage == null,
            enter = fadeIn(), exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.95f))))
                    .padding(horizontal = if (compact) 22.dp else 34.dp, vertical = if (compact) 16.dp else 22.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlayerButton("Anterior", Icons.Default.SkipPrevious, { showControls(); onPrevious() }, compact = compact)
                Spacer(Modifier.width(if (compact) 8.dp else 12.dp))
                PlayerButton(
                    if (isPlaying) "Pausar" else "Reproduzir",
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    { showControls(); if (player.isPlaying) player.pause() else player.play() },
                    modifier = Modifier.focusRequester(playFocus),
                    compact = compact
                )
                Spacer(Modifier.width(if (compact) 8.dp else 12.dp))
                PlayerButton("Próximo", Icons.Default.SkipNext, { showControls(); onNext() }, compact = compact)
                Spacer(Modifier.width(if (compact) 8.dp else 12.dp))
                PlayerButton(
                    if (selectedQualityIndex == -1) "Qualidade: Auto" else "Qualidade: ${qualityOptions.getOrNull(selectedQualityIndex)?.label ?: "Auto"}",
                    Icons.Default.Refresh,
                    { showControls(); cycleQuality() },
                    compact = compact
                )
                Spacer(Modifier.width(if (compact) 8.dp else 12.dp))
                PlayerButton(
                    if (channel.isFavorite) "Favorito" else "Favoritar",
                    if (channel.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    { showControls(); onToggleFavorite(channel) },
                    compact = compact
                )
            }
        }
    }
}

@Composable
private fun PlayerButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean
) {
    TvFocusableBox(
        onClick = onClick,
        modifier = modifier,
        unfocusedBorder = Color(0xFF5A3512),
        cornerRadius = 12.dp,
        scaleOnFocus = 1.04f
    ) { focused ->
        Row(
            modifier = Modifier.background(if (focused) MaterialTheme.colorScheme.primary else Color(0xDD1D1D1D))
                .padding(horizontal = if (compact) 10.dp else 13.dp, vertical = if (compact) 8.dp else 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = if (focused) Color.Black else Color.White, modifier = Modifier.size(if (compact) 18.dp else 20.dp))
            Spacer(Modifier.width(5.dp))
            Text(text, color = if (focused) Color.Black else Color.White, fontWeight = FontWeight.Bold, maxLines = 1, fontSize = if (compact) 12.sp else 14.sp)
        }
    }
}

private fun buildVideoQualityOptions(tracks: Tracks): List<VideoQualityOption> {
    val options = mutableListOf<VideoQualityOption>()
    tracks.groups.filter { it.type == C.TRACK_TYPE_VIDEO }.forEach { group ->
        for (index in 0 until group.length) {
            if (!group.isTrackSupported(index)) continue
            val format = group.getTrackFormat(index)
            options += VideoQualityOption(qualityLabel(format.height, format.bitrate), group.mediaTrackGroup, index, format.height, format.bitrate)
        }
    }
    return options.sortedWith(compareByDescending<VideoQualityOption> { it.height }.thenByDescending { it.bitrate }).distinctBy { it.label }
}

private fun qualityLabel(height: Int, bitrate: Int): String = when {
    height >= 2160 -> "4K"
    height >= 1080 -> "1080p"
    height >= 720 -> "720p"
    height >= 480 -> "480p"
    height >= 360 -> "360p"
    height > 0 -> "${height}p"
    bitrate > 0 -> "${bitrate / 1000} kbps"
    else -> "Padrão"
}

private fun isFatalSourceError(error: PlaybackException): Boolean = error.errorCode in setOf(
    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
    PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
    PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
    PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
    PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED
)

private fun friendlyPlaybackError(error: PlaybackException): String = when (error.errorCode) {
    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> "A fonte não respondeu."
    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> "A transmissão demorou demais para responder."
    PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> "O servidor recusou a transmissão."
    PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
    PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED -> "A transmissão recebida está inválida."
    PlaybackException.ERROR_CODE_DECODING_FAILED,
    PlaybackException.ERROR_CODE_DECODER_INIT_FAILED -> "Esta TV não conseguiu decodificar o vídeo."
    else -> "Não foi possível reproduzir este canal agora."
}

package com.swipefoto.app

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.Canvas
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random

object Palette {
    val Keep = Color(0xFF29C78F)
    val Trash = Color(0xFFFF6175)
    val Accent = Color(0xFF7361F5)
    val Sun = Color(0xFFFFC747)
    val Ink = Color(0xFF242438)
    val Confetti = listOf(Keep, Trash, Accent, Sun, Color(0xFFFF77C8), Color(0xFF3CC8F0))
}

val AppBackground: Brush = Brush.verticalGradient(listOf(Color(0xFFF7F2FF), Color(0xFFE3F5FF)))

private var cachedLoader: ImageLoader? = null

fun appImageLoader(context: Context): ImageLoader {
    cachedLoader?.let { return it }
    val loader = ImageLoader.Builder(context.applicationContext)
        .components { add(VideoFrameDecoder.Factory()) }
        .build()
    cachedLoader = loader
    return loader
}

fun formatDuration(ms: Long): String {
    val total = (ms / 1000.0).roundToInt()
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

@Composable
fun Badge(text: String) {
    Text(
        text = text,
        color = Color.White,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Composable
fun Chip(text: String, highlighted: Boolean = false, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .shadow(2.dp, RoundedCornerShape(50))
            .clip(RoundedCornerShape(50))
            .background(if (highlighted) Palette.Accent else Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$text ▾",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (highlighted) Color.White else Palette.Ink
        )
    }
}

@Composable
fun RoundIcon(symbol: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .shadow(3.dp, CircleShape)
            .clip(CircleShape)
            .background(Color.White)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(symbol, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Palette.Ink)
    }
}

@Composable
fun ActionButton(
    label: String,
    symbol: String,
    background: Color,
    foreground: Color,
    size: Dp,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.alpha(if (enabled) 1f else 0.35f)
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .shadow(8.dp, CircleShape)
                .clip(CircleShape)
                .background(background)
                .clickable(enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = symbol,
                fontSize = (size.value * 0.4f).sp,
                fontWeight = FontWeight.ExtraBold,
                color = foreground
            )
        }
        Spacer(Modifier.size(4.dp))
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
    }
}

@Composable
fun Thumb(item: MediaFile, sizeText: String, onClick: () -> Unit) {
    val ctx = LocalContext.current
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = item.uri,
            contentDescription = null,
            imageLoader = appImageLoader(ctx),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(Modifier.align(Alignment.TopEnd).padding(5.dp)) {
            Text("✕", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        }
        Box(Modifier.align(Alignment.BottomStart).padding(4.dp)) {
            Badge(if (item.isVideo) "${formatDuration(item.durationMs)} • $sizeText" else sizeText)
        }
    }
}

/** Memutar video otomatis tanpa suara. Ketuk untuk jeda, tombol speaker untuk suara. */
@Composable
fun VideoPlayer(uri: Uri, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    var muted by remember { mutableStateOf(true) }
    var paused by remember { mutableStateOf(false) }

    val player = remember(uri) {
        ExoPlayer.Builder(ctx).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            repeatMode = Player.REPEAT_MODE_ONE
            volume = 0f
            playWhenReady = true
            prepare()
        }
    }
    DisposableEffect(player) {
        onDispose { player.release() }
    }

    Box(modifier) {
        AndroidView(
            factory = { context ->
                val view = LayoutInflater.from(context).inflate(R.layout.video_surface, null) as PlayerView
                view.player = player
                view
            },
            modifier = Modifier.fillMaxSize()
        )
        Box(
            Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    paused = !paused
                    player.playWhenReady = !paused
                }
        )
        if (paused) {
            Text(
                "▶",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 56.sp,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable {
                    muted = !muted
                    player.volume = if (muted) 0f else 1f
                },
            contentAlignment = Alignment.Center
        ) {
            Text(if (muted) "🔇" else "🔊", fontSize = 16.sp)
        }
    }
}

/** Kartu yang bisa digeser. Kanan = simpan, kiri = masuk antrean hapus. */
@Composable
fun SwipeCard(item: MediaFile, sizeText: String, isTop: Boolean, onSwipe: (Boolean) -> Unit) {
    val ctx = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val currentOnSwipe by rememberUpdatedState(onSwipe)

    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    val screenWidthPx = with(density) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    val thresholdPx = with(density) { 110.dp.toPx() }
    val progress = (abs(offsetX.value) / thresholdPx).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationX = offsetX.value
                translationY = offsetY.value
                rotationZ = offsetX.value / 40f
                val s = if (isTop) 1f else 0.94f
                scaleX = s
                scaleY = s
            }
            .shadow(14.dp, RoundedCornerShape(28.dp))
            .clip(RoundedCornerShape(28.dp))
            .background(Color.White)
            .pointerInput(isTop) {
                if (isTop) {
                    detectDragGestures(
                        onDragEnd = {
                            scope.launch {
                                when {
                                    offsetX.value > thresholdPx -> {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        offsetX.animateTo(screenWidthPx * 1.5f, tween(220))
                                        currentOnSwipe(false)
                                    }
                                    offsetX.value < -thresholdPx -> {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        offsetX.animateTo(-screenWidthPx * 1.5f, tween(220))
                                        currentOnSwipe(true)
                                    }
                                    else -> {
                                        launch { offsetX.animateTo(0f, spring()) }
                                        launch { offsetY.animateTo(0f, spring()) }
                                    }
                                }
                            }
                        },
                        onDragCancel = {
                            scope.launch {
                                launch { offsetX.animateTo(0f, spring()) }
                                launch { offsetY.animateTo(0f, spring()) }
                            }
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        scope.launch {
                            offsetX.snapTo(offsetX.value + dragAmount.x)
                            offsetY.snapTo(offsetY.value + dragAmount.y)
                        }
                    }
                }
            }
    ) {
        AsyncImage(
            model = item.uri,
            contentDescription = null,
            imageLoader = appImageLoader(ctx),
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
        if (item.isVideo && isTop) {
            VideoPlayer(item.uri, Modifier.fillMaxSize())
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
        ) {
            if (item.isVideo) {
                Badge("🎞 ${formatDuration(item.durationMs)}")
                Spacer(Modifier.width(6.dp))
            }
            Badge(sizeText)
        }

        Stamp(
            text = "SIMPAN 💚",
            color = Palette.Keep,
            alpha = if (offsetX.value > 0) progress else 0f,
            angle = -10f,
            modifier = Modifier.align(Alignment.TopStart)
        )
        Stamp(
            text = "HAPUS 🗑",
            color = Palette.Trash,
            alpha = if (offsetX.value < 0) progress else 0f,
            angle = 10f,
            modifier = Modifier.align(Alignment.TopEnd)
        )
    }
}

@Composable
private fun Stamp(text: String, color: Color, alpha: Float, angle: Float, modifier: Modifier) {
    Box(
        modifier = modifier
            .padding(20.dp)
            .alpha(alpha)
            .rotate(angle)
            .background(Color.White.copy(alpha = 0.85f), RoundedCornerShape(12.dp))
            .border(4.dp, color, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(text, color = color, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
    }
}

/** Konfeti sederhana untuk perayaan saat semua media selesai dicek. */
@Composable
fun Confetti(modifier: Modifier = Modifier) {
    class Piece(
        val x: Float,
        val size: Float,
        val delay: Float,
        val duration: Float,
        val color: Color,
        val spin: Float
    )

    val pieces = remember {
        List(36) {
            Piece(
                x = Random.nextFloat(),
                size = 8f + Random.nextFloat() * 8f,
                delay = Random.nextFloat() * 1.2f,
                duration = 2f + Random.nextFloat() * 1.5f,
                color = Palette.Confetti.random(),
                spin = 180f + Random.nextFloat() * 360f
            )
        }
    }
    val clock = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        clock.animateTo(5f, tween(5000, easing = LinearEasing))
    }

    Canvas(modifier) {
        pieces.forEach { p ->
            val local = ((clock.value - p.delay) / p.duration).coerceIn(0f, 1f)
            if (local > 0f && local < 1f) {
                val cx = p.x * size.width
                val cy = -30f + (size.height + 60f) * local
                rotate(degrees = p.spin * local, pivot = Offset(cx, cy)) {
                    drawRect(
                        color = p.color,
                        topLeft = Offset(cx - p.size / 2f, cy - p.size * 0.8f),
                        size = Size(p.size, p.size * 1.6f)
                    )
                }
            }
        }
    }
}

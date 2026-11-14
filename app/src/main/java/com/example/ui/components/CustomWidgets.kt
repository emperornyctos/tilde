package com.example.ui.components

import android.graphics.ImageDecoder
import android.graphics.Movie
import android.graphics.drawable.AnimatedImageDrawable
import android.os.Build
import android.os.SystemClock
import android.widget.ImageView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.data.MediaPlaybackState
import com.example.ui.theme.LocalLauncherTokens
import kotlinx.coroutines.delay
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun PhotoFrameWidget(
    widgetConfig: String?,
    isInteractive: Boolean,
    onPickPhoto: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalLauncherTokens.current
    val parsed = remember(widgetConfig) {
        val json = runCatching { JSONObject(widgetConfig ?: "{}") }.getOrNull()
        val path = json?.optString("photoPath", "")?.takeIf { it.isNotBlank() }
        val showBorder = json?.optBoolean("showBorder", true) ?: true
        path to showBorder
    }
    val photoFile = remember(parsed.first) {
        parsed.first?.let { File(it) }?.takeIf { it.exists() }
    }
    val showBorder = parsed.second
    val shape = RoundedCornerShape(tokens.cardCornerRadius)

    if (photoFile == null) {
        TuiCard(
            modifier = modifier
                .fillMaxSize()
                .then(if (isInteractive) Modifier.clickable(onClick = onPickPhoto) else Modifier),
            contentPadding = PaddingValues(10.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .border(1.dp, tokens.border, shape)
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AddPhotoAlternate,
                    contentDescription = "Add photo or GIF widget media",
                    tint = tokens.accent,
                    modifier = Modifier.size(30.dp)
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Photo / GIF Frame",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = tokens.textPrimary
                )
                Text(
                    text = "tap to pick photo or GIF",
                    style = MaterialTheme.typography.labelSmall,
                    color = tokens.textMuted,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        val isGif = remember(photoFile.absolutePath) {
            photoFile.extension.equals("gif", ignoreCase = true)
        }
        val frameModifier = if (showBorder) {
            Modifier
                .background(tokens.surface, shape)
                .border(tokens.borderWidth.coerceAtLeast(1.dp), tokens.border, shape)
                .padding(3.dp)
        } else {
            Modifier
        }
        Box(
            modifier = modifier
                .fillMaxSize()
                .clip(shape)
                .then(frameModifier)
        ) {
            if (isGif) {
                AnimatedGifView(
                    file = photoFile,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(shape)
                )
            } else {
                AsyncImage(
                    model = photoFile,
                    contentDescription = "Custom photo widget",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(shape)
                )
            }
        }
    }
}

@Composable
private fun AnimatedGifView(
    file: File,
    modifier: Modifier = Modifier
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val drawableState = androidx.compose.runtime.produceState<android.graphics.drawable.Drawable?>(
            initialValue = null,
            key1 = file.absolutePath
        ) {
            value = withContext(Dispatchers.IO) {
                runCatching {
                    val source = ImageDecoder.createSource(file)
                    ImageDecoder.decodeDrawable(source)
                }.getOrNull()
            }
        }
        val drawable = drawableState.value
        if (drawable != null) {
            AndroidView(
                factory = { ctx ->
                    ImageView(ctx).apply {
                        scaleType = ImageView.ScaleType.CENTER_CROP
                    }
                },
                update = { iv ->
                    iv.setImageDrawable(drawable)
                    if (drawable is AnimatedImageDrawable) {
                        drawable.repeatCount = AnimatedImageDrawable.REPEAT_INFINITE
                        drawable.start()
                    }
                },
                modifier = modifier
            )
        } else {
            AsyncImage(
                model = file,
                contentDescription = "Animated GIF widget",
                contentScale = ContentScale.Crop,
                modifier = modifier
            )
        }
    } else {
        @Suppress("DEPRECATION")
        val movie = remember(file.absolutePath) {
            runCatching { Movie.decodeFile(file.absolutePath) }.getOrNull()
        }
        if (movie != null && movie.duration() > 0) {
            var frameTimeMs by remember(file.absolutePath) { mutableLongStateOf(0L) }
            LaunchedEffect(file.absolutePath) {
                val start = SystemClock.uptimeMillis()
                while (true) {
                    val duration = movie.duration().coerceAtLeast(1)
                    frameTimeMs = ((SystemClock.uptimeMillis() - start) % duration)
                    delay(33L)
                }
            }
            Canvas(modifier = modifier) {
                val mw = movie.width().toFloat().coerceAtLeast(1f)
                val mh = movie.height().toFloat().coerceAtLeast(1f)
                val scale = kotlin.math.max(size.width / mw, size.height / mh)
                val dx = (size.width - mw * scale) / 2f
                val dy = (size.height - mh * scale) / 2f
                movie.setTime(frameTimeMs.toInt())
                drawContext.canvas.nativeCanvas.apply {
                    save()
                    translate(dx, dy)
                    scale(scale, scale)
                    movie.draw(this, 0f, 0f)
                    restore()
                }
            }
        } else {
            AsyncImage(
                model = file,
                contentDescription = "GIF widget",
                contentScale = ContentScale.Crop,
                modifier = modifier
            )
        }
    }
}

@Composable
fun AnalogClockWidget(
    modifier: Modifier = Modifier
) {
    val tokens = LocalLauncherTokens.current
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            val current = System.currentTimeMillis()
            nowMs = current
            val remainder = current % 1000L
            delay((1000L - remainder).coerceAtLeast(16L))
        }
    }

    val cal = remember(nowMs) {
        Calendar.getInstance().apply { timeInMillis = nowMs }
    }
    val hour = cal.get(Calendar.HOUR)
    val minute = cal.get(Calendar.MINUTE)
    val second = cal.get(Calendar.SECOND)
    val dateLabel = remember(nowMs / 60_000L) {
        SimpleDateFormat("EEE dd", Locale.getDefault()).format(Date(nowMs)).lowercase()
    }

    TuiCard(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp)
    ) {
        BoxWithConstraints(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            val boxW = maxWidth
            val boxH = maxHeight
            val showSubDate = boxW >= 110.dp && boxH >= 110.dp

            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = (min(size.width, size.height) / 2f) * 0.90f

                // Outer dial bezel
                drawCircle(
                    color = tokens.background.copy(alpha = 0.65f),
                    radius = radius,
                    center = center
                )
                drawCircle(
                    color = tokens.border,
                    radius = radius,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx())
                )
                drawCircle(
                    color = tokens.border.copy(alpha = 0.35f),
                    radius = radius * 0.78f,
                    center = center,
                    style = Stroke(width = 0.75.dp.toPx())
                )

                // 60 tick marks around the dial
                for (i in 0 until 60) {
                    val angleRad = Math.toRadians((i * 6.0) - 90.0)
                    val isHourTick = i % 5 == 0
                    val isCardinal = i % 15 == 0
                    val innerR = when {
                        isCardinal -> radius * 0.79f
                        isHourTick -> radius * 0.83f
                        else -> radius * 0.90f
                    }
                    val outerR = radius * 0.95f
                    val start = Offset(
                        x = center.x + (cos(angleRad) * innerR).toFloat(),
                        y = center.y + (sin(angleRad) * innerR).toFloat()
                    )
                    val end = Offset(
                        x = center.x + (cos(angleRad) * outerR).toFloat(),
                        y = center.y + (sin(angleRad) * outerR).toFloat()
                    )
                    drawLine(
                        color = when {
                            isCardinal -> tokens.accent
                            isHourTick -> tokens.textPrimary
                            else -> tokens.textMuted.copy(alpha = 0.55f)
                        },
                        start = start,
                        end = end,
                        strokeWidth = when {
                            isCardinal -> 2.4.dp.toPx()
                            isHourTick -> 1.6.dp.toPx()
                            else -> 0.85.dp.toPx()
                        },
                        cap = StrokeCap.Round
                    )
                }

                // Hour hand
                val hourAngleRad = Math.toRadians(((hour + minute / 60.0) * 30.0) - 90.0)
                drawLine(
                    color = tokens.textPrimary,
                    start = center,
                    end = Offset(
                        x = center.x + (cos(hourAngleRad) * radius * 0.50f).toFloat(),
                        y = center.y + (sin(hourAngleRad) * radius * 0.50f).toFloat()
                    ),
                    strokeWidth = 3.4.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Minute hand
                val minAngleRad = Math.toRadians(((minute + second / 60.0) * 6.0) - 90.0)
                drawLine(
                    color = tokens.textPrimary,
                    start = center,
                    end = Offset(
                        x = center.x + (cos(minAngleRad) * radius * 0.72f).toFloat(),
                        y = center.y + (sin(minAngleRad) * radius * 0.72f).toFloat()
                    ),
                    strokeWidth = 2.2.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Ticking second hand with counterweight tail
                val secAngleRad = Math.toRadians((second * 6.0) - 90.0)
                val tailStart = Offset(
                    x = center.x - (cos(secAngleRad) * radius * 0.20f).toFloat(),
                    y = center.y - (sin(secAngleRad) * radius * 0.20f).toFloat()
                )
                val secTip = Offset(
                    x = center.x + (cos(secAngleRad) * radius * 0.82f).toFloat(),
                    y = center.y + (sin(secAngleRad) * radius * 0.82f).toFloat()
                )
                drawLine(
                    color = tokens.accent,
                    start = tailStart,
                    end = secTip,
                    strokeWidth = 1.3.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Center jewel pin
                drawCircle(
                    color = tokens.accent,
                    radius = 3.5.dp.toPx(),
                    center = center
                )
                drawCircle(
                    color = tokens.background,
                    radius = 1.4.dp.toPx(),
                    center = center
                )
            }

            if (showSubDate) {
                Text(
                    text = dateLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = tokens.textMuted,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = boxH * 0.19f)
                )
            }
        }
    }
}

@Composable
fun MiniCalendarWidget(
    modifier: Modifier = Modifier
) {
    val tokens = LocalLauncherTokens.current
    val cal = remember { Calendar.getInstance() }
    val today = cal.get(Calendar.DAY_OF_MONTH)
    val monthHeader = remember {
        SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(cal.time).lowercase()
    }
    val daysGrid = remember {
        val c = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }
        val firstDayOfWeek = c.get(Calendar.DAY_OF_WEEK) // 1=Sun..7=Sat
        val mondayOffset = (firstDayOfWeek + 5) % 7
        val maxDays = c.getActualMaximum(Calendar.DAY_OF_MONTH)
        val cells = MutableList<Int?>(mondayOffset) { null }
        for (d in 1..maxDays) cells += d
        while (cells.size % 7 != 0) cells += null
        cells.chunked(7)
    }

    TuiCard(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(10.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                TuiSectionHeader(text = monthHeader)
                Text(
                    text = today.toString(),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = tokens.accent
                )
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("M", "T", "W", "T", "F", "S", "S").forEach { wd ->
                    Text(
                        text = wd,
                        style = MaterialTheme.typography.labelSmall,
                        color = tokens.textMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            daysGrid.forEach { week ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    week.forEach { day ->
                        val isToday = day == today
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(tokens.cardCornerRadius))
                                .background(if (isToday) tokens.pillBg else tokens.surface.copy(alpha = 0f))
                                .padding(vertical = 1.dp)
                        ) {
                            Text(
                                text = day?.toString() ?: "",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (isToday) tokens.pillText else tokens.textPrimary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NowPlayingWidget(
    mediaState: MediaPlaybackState,
    isInteractive: Boolean,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onOpenPlayer: () -> Unit,
    onGrantMediaAccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalLauncherTokens.current
    var livePositionMs by remember(
        mediaState.positionMs,
        mediaState.isPlaying,
        mediaState.lastPositionUpdateElapsedMs
    ) {
        mutableLongStateOf(mediaState.positionMs)
    }

    LaunchedEffect(
        mediaState.isPlaying,
        mediaState.positionMs,
        mediaState.playbackSpeed,
        mediaState.lastPositionUpdateElapsedMs,
        mediaState.durationMs
    ) {
        while (true) {
            if (mediaState.isPlaying && mediaState.lastPositionUpdateElapsedMs > 0L) {
                val delta = ((SystemClock.elapsedRealtime() - mediaState.lastPositionUpdateElapsedMs) *
                    mediaState.playbackSpeed.coerceAtLeast(0.5f)).toLong()
                val computed = (mediaState.positionMs + delta).coerceAtLeast(0L)
                livePositionMs = if (mediaState.durationMs > 0L) {
                    computed.coerceAtMost(mediaState.durationMs)
                } else {
                    computed
                }
            } else {
                livePositionMs = mediaState.positionMs
            }
            if (!mediaState.isPlaying) break
            delay(1000L)
        }
    }

    val shape = RoundedCornerShape(tokens.cardCornerRadius)
    TuiCard(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(10.dp)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val boxW = maxWidth
            val boxH = maxHeight
            val artSize = if (boxH >= 130.dp) 86.dp else 64.dp
            val showArt = boxW >= 180.dp

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxSize()
            ) {
                if (showArt) {
                    val artBitmap = remember(mediaState.albumArt) {
                        mediaState.albumArt?.asImageBitmap()
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(artSize)
                            .clip(shape)
                            .background(tokens.background.copy(alpha = 0.8f))
                            .border(1.dp, tokens.border, shape)
                            .then(
                                if (isInteractive && mediaState.hasSession) {
                                    Modifier.clickable(onClick = onOpenPlayer)
                                } else {
                                    Modifier
                                }
                            )
                    ) {
                        if (artBitmap != null) {
                            Image(
                                bitmap = artBitmap,
                                contentDescription = mediaState.album.ifBlank { mediaState.title },
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = if (mediaState.isPlaying) Icons.Default.Album else Icons.Default.MusicNote,
                                contentDescription = "Now playing art placeholder",
                                tint = tokens.accent,
                                modifier = Modifier.size(artSize * 0.45f)
                            )
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                }

                Column(
                    verticalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    // Top status + player badge or grant access button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (mediaState.isPlaying) tokens.accent else tokens.textMuted)
                            )
                            Spacer(Modifier.width(6.dp))
                            val statusLabel = when {
                                mediaState.hasSession -> {
                                    val stateWord = if (mediaState.isPlaying) "playing" else "paused"
                                    "${mediaState.playerAppName ?: "media"} · $stateWord"
                                }
                                mediaState.isAudioActiveFallback -> "audio active"
                                !mediaState.hasNotificationAccess -> "media access needed"
                                else -> "idle · no track"
                            }
                            Text(
                                text = statusLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = tokens.accent,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (!mediaState.hasNotificationAccess && isInteractive) {
                            TuiCliLink(
                                label = "enable",
                                onClick = onGrantMediaAccess
                            )
                        }
                    }

                    // Track title + artist
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (isInteractive && mediaState.hasSession) {
                                    Modifier.clickable(onClick = onOpenPlayer)
                                } else {
                                    Modifier
                                }
                            )
                    ) {
                        val headline = when {
                            mediaState.hasSession -> mediaState.title
                            !mediaState.hasNotificationAccess -> "Tap [enable] for track info & art"
                            mediaState.isAudioActiveFallback -> "Playback active"
                            else -> "Nothing playing right now"
                        }
                        val subline = when {
                            mediaState.hasSession -> listOf(mediaState.artist, mediaState.album)
                                .filter { it.isNotBlank() }
                                .joinToString(" · ")
                                .ifBlank { mediaState.playerAppName ?: "" }
                            !mediaState.hasNotificationAccess -> "Grant Notification Access to sync any player"
                            else -> "Play music in Spotify, YT Music, or any player"
                        }
                        Text(
                            text = headline,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = tokens.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (subline.isNotBlank()) {
                            Text(
                                text = subline,
                                style = MaterialTheme.typography.bodySmall,
                                color = tokens.textMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Live Progress bar when duration > 0
                    if (mediaState.hasSession && mediaState.durationMs > 0L) {
                        val fraction = (livePositionMs.toFloat() / mediaState.durationMs.toFloat())
                            .coerceIn(0f, 1f)
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(tokens.cardCornerRadius))
                                    .background(tokens.background)
                                    .border(0.5.dp, tokens.border, RoundedCornerShape(tokens.cardCornerRadius))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fraction.coerceAtLeast(0.02f))
                                        .fillMaxHeight()
                                        .background(tokens.accent)
                                )
                            }
                            Spacer(Modifier.height(2.dp))
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = formatDurationMs(livePositionMs),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = tokens.textMuted
                                )
                                Text(
                                    text = formatDurationMs(mediaState.durationMs),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = tokens.textMuted
                                )
                            }
                        }
                    }

                    // Transport Controls
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(
                            onClick = { if (isInteractive) onSkipPrevious() },
                            enabled = isInteractive && mediaState.canSkipPrevious,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(shape)
                                .background(tokens.background.copy(alpha = 0.7f))
                                .border(0.75.dp, tokens.border, shape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "Previous track",
                                tint = tokens.textPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(shape)
                                .background(tokens.pillBg)
                                .border(1.dp, tokens.accent.copy(alpha = 0.75f), shape)
                                .then(if (isInteractive) Modifier.clickable(onClick = onTogglePlayPause) else Modifier)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = if (mediaState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (mediaState.isPlaying) "Pause" else "Play",
                                    tint = tokens.pillText,
                                    modifier = Modifier.size(20.dp)
                                )
                                if (boxW >= 240.dp) {
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = if (mediaState.isPlaying) "pause" else "play",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = tokens.pillText
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = { if (isInteractive) onSkipNext() },
                            enabled = isInteractive && mediaState.canSkipNext,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(shape)
                                .background(tokens.background.copy(alpha = 0.7f))
                                .border(0.75.dp, tokens.border, shape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Next track",
                                tint = tokens.textPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatDurationMs(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val mins = totalSec / 60L
    val secs = totalSec % 60L
    return String.format(Locale.US, "%d:%02d", mins, secs)
}

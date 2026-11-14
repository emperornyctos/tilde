package com.example.ui.components

import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.model.LauncherSettings
import com.example.model.WallpaperType
import java.io.File

@Composable
fun WallpaperBackground(
    settings: LauncherSettings,
    pageOffsetFraction: Float = 0f,
    modifier: Modifier = Modifier
) {
    val bgColor = Color(settings.backgroundColor)
    val accent = Color(settings.accentColor)
    val border = Color(settings.borderColor)
    val parallaxShift = if (settings.wallpaperParallax) pageOffsetFraction * 42f else 0f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        val blurModifier = if (
            settings.wallpaperBlurDp > 0.5f &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        ) {
            Modifier.blur(settings.wallpaperBlurDp.dp)
        } else {
            Modifier
        }

        when (settings.wallpaperType) {
            WallpaperType.CUSTOM_IMAGE -> {
                val file = settings.customWallpaperPath.takeIf { it.isNotBlank() }?.let { File(it) }
                if (file != null && file.exists()) {
                    AsyncImage(
                        model = file,
                        contentDescription = "Launcher Wallpaper",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .then(blurModifier)
                    )
                } else {
                    ProceduralTerminalCanvas(
                        type = WallpaperType.DOT_MATRIX,
                        bgColor = bgColor,
                        accent = accent,
                        border = border,
                        parallaxShift = parallaxShift,
                        modifier = Modifier.fillMaxSize().then(blurModifier)
                    )
                }
            }

            else -> {
                ProceduralTerminalCanvas(
                    type = settings.wallpaperType,
                    bgColor = bgColor,
                    accent = accent,
                    border = border,
                    parallaxShift = parallaxShift,
                    modifier = Modifier.fillMaxSize().then(blurModifier)
                )
            }
        }

        if (settings.wallpaperDim > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgColor.copy(alpha = settings.wallpaperDim.coerceIn(0f, 0.92f)))
            )
        }
    }
}

@Composable
private fun ProceduralTerminalCanvas(
    type: WallpaperType,
    bgColor: Color,
    accent: Color,
    border: Color,
    parallaxShift: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRect(color = bgColor)

        when (type) {
            WallpaperType.TERMINAL_GRID -> {
                val step = 64f
                val gridColor = border.copy(alpha = 0.45f)
                var x = -step + (parallaxShift % step)
                while (x < w + step) {
                    drawLine(
                        color = gridColor,
                        start = Offset(x, 0f),
                        end = Offset(x, h),
                        strokeWidth = 1f
                    )
                    x += step
                }
                var y = 0f
                while (y < h) {
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1f
                    )
                    y += step
                }
            }

            WallpaperType.DOT_MATRIX -> {
                val step = 48f
                val dotColor = accent.copy(alpha = 0.22f)
                var x = -step + (parallaxShift % step)
                while (x < w + step) {
                    var y = step / 2f
                    while (y < h) {
                        drawCircle(
                            color = dotColor,
                            radius = 2.2f,
                            center = Offset(x, y)
                        )
                        y += step
                    }
                    x += step
                }
            }

            WallpaperType.CRT_SCANLINES -> {
                val lineColor = accent.copy(alpha = 0.09f)
                var y = 0f
                while (y < h) {
                    drawLine(
                        color = lineColor,
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 2f
                    )
                    y += 10f
                }
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(accent.copy(alpha = 0.14f), Color.Transparent),
                        center = Offset(w * 0.5f - parallaxShift, h * 0.35f),
                        radius = w * 0.85f
                    )
                )
            }

            WallpaperType.RADIAL_GLOW -> {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.28f),
                            accent.copy(alpha = 0.08f),
                            bgColor
                        ),
                        center = Offset(w * 0.5f - parallaxShift * 1.5f, h * 0.28f),
                        radius = w * 0.95f
                    )
                )
            }

            WallpaperType.SOLID, WallpaperType.CUSTOM_IMAGE -> Unit
        }
    }
}

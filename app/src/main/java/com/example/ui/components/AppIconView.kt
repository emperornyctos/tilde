package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.IconPackManager
import com.example.model.AppInfo
import com.example.model.BuiltInIconStyle
import com.example.model.IconShape
import com.example.model.LauncherSettings
import com.example.ui.theme.LocalLauncherTokens
import java.io.File

fun IconShape.toComposeShape(cornerDp: Float): Shape = when (this) {
    IconShape.SHARP -> RoundedCornerShape(0.dp)
    IconShape.ROUNDED -> RoundedCornerShape(cornerDp.dp)
    IconShape.SQUIRCLE -> RoundedCornerShape(percent = 30)
    IconShape.CIRCLE -> CircleShape
    IconShape.TEARDROP -> RoundedCornerShape(
        topStartPercent = 50,
        topEndPercent = 50,
        bottomStartPercent = 50,
        bottomEndPercent = 12
    )
}

@Composable
fun AppIconView(
    app: AppInfo,
    settings: LauncherSettings,
    iconPackManager: IconPackManager,
    modifier: Modifier = Modifier,
    sizeOverride: Dp? = null
) {
    val tokens = LocalLauncherTokens.current
    val iconSize = sizeOverride ?: settings.iconSizeDp.dp
    val shape = settings.iconShape.toComposeShape(settings.iconCornerRadiusDp)
    val padding = settings.iconPaddingDp.dp

    // 1. Check per-app custom image path
    val customImageFile = app.customIconUri?.takeIf { it.isNotBlank() }?.let { File(it) }

    // 2. Check per-app pack drawable or global icon pack
    val packDrawable by produceState<Drawable?>(
        initialValue = null,
        key1 = app.componentKey,
        key2 = app.customPackPackage,
        key3 = settings.selectedIconPack
    ) {
        value = when {
            !app.customPackPackage.isNullOrBlank() && !app.customPackDrawable.isNullOrBlank() -> {
                iconPackManager.loadNamedDrawableFromPack(
                    app.customPackPackage,
                    app.customPackDrawable
                )
            }
            settings.selectedIconPack.startsWith("pack:") -> {
                val pkg = settings.selectedIconPack.removePrefix("pack:")
                iconPackManager.loadDrawableFromPack(pkg, app.componentKey)
            }
            else -> null
        }
    }

    val activeBuiltInStyle = BuiltInIconStyle.entries.firstOrNull {
        it.id == settings.selectedIconPack
    } ?: BuiltInIconStyle.ORIGINAL

    val colorFilter = remember(activeBuiltInStyle, tokens.accent, tokens.background) {
        when (activeBuiltInStyle) {
            BuiltInIconStyle.ORIGINAL, BuiltInIconStyle.TUI_FRAMED -> null
            BuiltInIconStyle.MONOCHROME_TINT -> {
                val accent = tokens.accent
                val saturation = ColorMatrix().apply { setToSaturation(0f) }
                val tint = ColorMatrix(
                    floatArrayOf(
                        accent.red * 1.15f, 0f, 0f, 0f, 12f,
                        0f, accent.green * 1.15f, 0f, 0f, 12f,
                        0f, 0f, accent.blue * 1.15f, 0f, 12f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                tint.timesAssign(saturation)
                ColorFilter.colorMatrix(tint)
            }
            BuiltInIconStyle.GLYPH_INVERT -> {
                ColorFilter.colorMatrix(
                    ColorMatrix(
                        floatArrayOf(
                            -1f, 0f, 0f, 0f, 255f,
                            0f, -1f, 0f, 0f, 255f,
                            0f, 0f, -1f, 0f, 255f,
                            0f, 0f, 0f, 1f, 0f
                        )
                    )
                )
            }
            BuiltInIconStyle.DUOTONE_PHOSPHOR -> {
                val accent = tokens.accent
                val sat = ColorMatrix().apply { setToSaturation(0f) }
                val duotone = ColorMatrix(
                    floatArrayOf(
                        accent.red * 1.4f, 0f, 0f, 0f, -20f,
                        0f, accent.green * 1.4f, 0f, 0f, -20f,
                        0f, 0f, accent.blue * 1.4f, 0f, -20f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                duotone.timesAssign(sat)
                ColorFilter.colorMatrix(duotone)
            }
        }
    }

    val containerBg = when {
        activeBuiltInStyle == BuiltInIconStyle.TUI_FRAMED -> tokens.surface
        activeBuiltInStyle == BuiltInIconStyle.DUOTONE_PHOSPHOR -> tokens.background
        else -> Color.Transparent
    }

    val frameModifier = if (activeBuiltInStyle == BuiltInIconStyle.TUI_FRAMED) {
        Modifier
            .background(containerBg, shape)
            .border(1.dp, tokens.border, shape)
    } else {
        Modifier.background(containerBg, shape)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(iconSize)
            .clip(shape)
            .then(frameModifier)
            .padding(padding)
    ) {
        when {
            customImageFile != null && customImageFile.exists() -> {
                AsyncImage(
                    model = customImageFile,
                    contentDescription = app.displayLabel,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(shape)
                )
            }
            packDrawable != null -> {
                val bitmap = remember(packDrawable) { packDrawable?.toImageBitmap() }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = app.displayLabel,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            app.systemIcon != null -> {
                val bitmap = remember(app.componentKey, app.systemIcon) {
                    app.systemIcon.toImageBitmap()
                }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = app.displayLabel,
                        contentScale = ContentScale.Crop,
                        colorFilter = if (packDrawable == null) colorFilter else null,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    FallbackTerminalIcon(app.displayLabel, tokens.accent)
                }
            }
            else -> {
                FallbackTerminalIcon(app.displayLabel, tokens.accent)
            }
        }
    }
}

@Composable
private fun FallbackTerminalIcon(label: String, tint: Color) {
    Icon(
        imageVector = Icons.Default.Terminal,
        contentDescription = label,
        tint = tint,
        modifier = Modifier.fillMaxSize(0.72f)
    )
}

fun Drawable.toImageBitmap(): ImageBitmap? {
    return runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && this is AdaptiveIconDrawable) {
            val size = 192
            val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            val bg = background
            val fg = foreground
            if (bg != null || fg != null) {
                val inset = (size * 0.22f).toInt()
                bg?.apply {
                    setBounds(-inset, -inset, size + inset, size + inset)
                    draw(canvas)
                }
                fg?.apply {
                    setBounds(-inset, -inset, size + inset, size + inset)
                    draw(canvas)
                }
            } else {
                setBounds(0, 0, size, size)
                draw(canvas)
            }
            bmp.asImageBitmap()
        } else if (this is BitmapDrawable && bitmap != null) {
            bitmap.asImageBitmap()
        } else {
            val w = intrinsicWidth.coerceAtLeast(1)
            val h = intrinsicHeight.coerceAtLeast(1)
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            setBounds(0, 0, canvas.width, canvas.height)
            draw(canvas)
            bmp.asImageBitmap()
        }
    }.getOrNull()
}

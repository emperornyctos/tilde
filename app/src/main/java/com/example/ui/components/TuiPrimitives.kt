package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalLauncherTokens

@Composable
fun TuiCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color? = null,
    borderColor: Color? = null,
    contentPadding: PaddingValues = PaddingValues(12.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val tokens = LocalLauncherTokens.current
    val shape = RoundedCornerShape(tokens.cardCornerRadius)
    val bg = backgroundColor ?: tokens.surface
    val stroke = borderColor ?: tokens.border

    Box(
        modifier = modifier
            .clip(shape)
            .background(bg)
            .then(
                if (tokens.borderWidth > 0.dp) {
                    Modifier.border(tokens.borderWidth, stroke, shape)
                } else {
                    Modifier
                }
            )
            .padding(contentPadding),
        content = content
    )
}

@Composable
fun TuiSectionHeader(
    text: String,
    modifier: Modifier = Modifier,
    color: Color? = null
) {
    val tokens = LocalLauncherTokens.current
    val display = if (tokens.uppercaseHeaders) text.uppercase() else text
    Text(
        text = display,
        style = MaterialTheme.typography.labelMedium.copy(
            letterSpacing = 1.6.sp,
            fontWeight = FontWeight.Medium,
            fontStyle = if (tokens.italicizeHeaders) FontStyle.Italic else FontStyle.Normal
        ),
        color = color ?: tokens.textMuted,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

@Composable
fun TuiCliLink(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color? = null,
    enforceTouchTarget: Boolean = false
) {
    val tokens = LocalLauncherTokens.current
    val shape = RoundedCornerShape(tokens.cardCornerRadius)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .then(
                if (enforceTouchTarget) Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                else Modifier.defaultMinSize(minHeight = 32.dp)
            )
            .clip(shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Text(
            text = "[$label]",
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Medium
            ),
            color = color ?: tokens.accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

enum class NerdGlyph {
    HOME,       // nf-oct-home / nf-md-home_variant
    APPS,       // nf-fa-th (3x3 terminal grid)
    WIDGETS,    // nf-cod-extensions (blocks/widgets)
    SETTINGS,   // nf-cod-settings_gear
    SEARCH,     // nf-oct-search
    PROMPT      // nf-oct-chevron_right / prompt
}

@Composable
fun NerdGlyphIcon(
    glyph: NerdGlyph,
    tint: Color,
    size: androidx.compose.ui.unit.Dp = 15.dp,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = (w * 0.11f).coerceAtLeast(1.6f)
        when (glyph) {
            NerdGlyph.HOME -> {
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(w * 0.5f, h * 0.12f)
                    lineTo(w * 0.9f, h * 0.48f)
                    lineTo(w * 0.78f, h * 0.48f)
                    lineTo(w * 0.78f, h * 0.88f)
                    lineTo(w * 0.58f, h * 0.88f)
                    lineTo(w * 0.58f, h * 0.62f)
                    lineTo(w * 0.42f, h * 0.62f)
                    lineTo(w * 0.42f, h * 0.88f)
                    lineTo(w * 0.22f, h * 0.88f)
                    lineTo(w * 0.22f, h * 0.48f)
                    lineTo(w * 0.1f, h * 0.48f)
                    close()
                }
                drawPath(
                    path = path,
                    color = tint,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = stroke,
                        join = androidx.compose.ui.graphics.StrokeJoin.Miter
                    )
                )
            }

            NerdGlyph.APPS -> {
                val cell = w * 0.22f
                val gap = w * 0.12f
                val start = (w - (cell * 3 + gap * 2)) / 2f
                for (r in 0..2) {
                    for (c in 0..2) {
                        drawRect(
                            color = tint,
                            topLeft = androidx.compose.ui.geometry.Offset(
                                start + c * (cell + gap),
                                start + r * (cell + gap)
                            ),
                            size = androidx.compose.ui.geometry.Size(cell, cell)
                        )
                    }
                }
            }

            NerdGlyph.WIDGETS -> {
                val box = w * 0.34f
                val pad = w * 0.12f
                val style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke)
                // Top-left, bottom-left, bottom-right squares + detached top-right module (nf-cod-extensions)
                drawRect(tint, androidx.compose.ui.geometry.Offset(pad, pad), androidx.compose.ui.geometry.Size(box, box), style = style)
                drawRect(tint, androidx.compose.ui.geometry.Offset(pad, h - pad - box), androidx.compose.ui.geometry.Size(box, box), style = style)
                drawRect(tint, androidx.compose.ui.geometry.Offset(w - pad - box, h - pad - box), androidx.compose.ui.geometry.Size(box, box), style = style)
                drawRect(tint, androidx.compose.ui.geometry.Offset(w - pad - box + w * 0.04f, pad - w * 0.04f), androidx.compose.ui.geometry.Size(box, box))
            }

            NerdGlyph.SETTINGS -> {
                val cx = w / 2f
                val cy = h / 2f
                val rOuter = w * 0.34f
                val rInner = w * 0.15f
                drawCircle(
                    color = tint,
                    radius = rOuter * 0.78f,
                    center = androidx.compose.ui.geometry.Offset(cx, cy),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke * 1.15f)
                )
                drawCircle(
                    color = tint,
                    radius = rInner,
                    center = androidx.compose.ui.geometry.Offset(cx, cy)
                )
                for (i in 0 until 8) {
                    val angle = i * (Math.PI / 4.0)
                    val sx = cx + kotlin.math.cos(angle).toFloat() * (rOuter * 0.72f)
                    val sy = cy + kotlin.math.sin(angle).toFloat() * (rOuter * 0.72f)
                    val ex = cx + kotlin.math.cos(angle).toFloat() * (w * 0.46f)
                    val ey = cy + kotlin.math.sin(angle).toFloat() * (w * 0.46f)
                    drawLine(
                        color = tint,
                        start = androidx.compose.ui.geometry.Offset(sx, sy),
                        end = androidx.compose.ui.geometry.Offset(ex, ey),
                        strokeWidth = stroke * 1.25f,
                        cap = androidx.compose.ui.graphics.StrokeCap.Butt
                    )
                }
            }

            NerdGlyph.SEARCH -> {
                val r = w * 0.28f
                val cx = w * 0.42f
                val cy = h * 0.42f
                drawCircle(
                    color = tint,
                    radius = r,
                    center = androidx.compose.ui.geometry.Offset(cx, cy),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke)
                )
                drawLine(
                    color = tint,
                    start = androidx.compose.ui.geometry.Offset(cx + r * 0.7f, cy + r * 0.7f),
                    end = androidx.compose.ui.geometry.Offset(w * 0.86f, h * 0.86f),
                    strokeWidth = stroke * 1.2f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Square
                )
            }

            NerdGlyph.PROMPT -> {
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(w * 0.22f, h * 0.2f)
                    lineTo(w * 0.62f, h * 0.5f)
                    lineTo(w * 0.22f, h * 0.8f)
                }
                drawPath(
                    path = path,
                    color = tint,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke * 1.2f)
                )
                drawLine(
                    color = tint,
                    start = androidx.compose.ui.geometry.Offset(w * 0.58f, h * 0.8f),
                    end = androidx.compose.ui.geometry.Offset(w * 0.86f, h * 0.8f),
                    strokeWidth = stroke * 1.2f
                )
            }
        }
    }
}

@Composable
fun TuiPillTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    glyph: NerdGlyph? = null
) {
    val tokens = LocalLauncherTokens.current
    val shape = RoundedCornerShape(tokens.cardCornerRadius)
    val contentColor = if (selected) tokens.pillText else tokens.textMuted
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clip(shape)
            .background(if (selected) tokens.pillBg else Color.Transparent)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        androidx.compose.foundation.layout.Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center
        ) {
            if (glyph != null) {
                NerdGlyphIcon(
                    glyph = glyph,
                    tint = contentColor,
                    size = 14.dp
                )
                androidx.compose.foundation.layout.Spacer(Modifier.width(6.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                ),
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.LauncherSettings
import com.example.model.ThemePreset
import com.example.ui.components.TuiCard
import com.example.ui.components.TuiCliLink
import com.example.ui.components.TuiSectionHeader
import com.example.ui.theme.LocalLauncherTokens
import kotlin.math.roundToInt
import android.graphics.Color as AndroidColor

private enum class ThemeColorSlot(val label: String) {
    ACCENT("Accent"),
    BACKGROUND("Background"),
    SURFACE("Card Surface"),
    BORDER("Border"),
    TEXT("Primary Text"),
    MUTED("Muted Text"),
    PILL_BG("Pill Fill"),
    PILL_TEXT("Pill Text")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomThemeBuilderDialog(
    settings: LauncherSettings,
    onApplyCustomTheme: (LauncherSettings) -> Unit,
    onDismiss: () -> Unit
) {
    val tokens = LocalLauncherTokens.current

    var accentHex by remember { mutableLongStateOf(settings.accentColor) }
    var bgHex by remember { mutableLongStateOf(settings.backgroundColor) }
    var surfaceHex by remember { mutableLongStateOf(settings.surfaceColor) }
    var borderHex by remember { mutableLongStateOf(settings.borderColor) }
    var textHex by remember { mutableLongStateOf(settings.textColor) }
    var mutedHex by remember { mutableLongStateOf(settings.mutedTextColor) }
    var pillBgHex by remember { mutableLongStateOf(settings.pillBackgroundColor) }
    var pillTextHex by remember { mutableLongStateOf(settings.pillTextColor) }
    var isLight by remember { mutableStateOf(settings.isLightTheme) }

    var activeSlot by remember { mutableStateOf(ThemeColorSlot.ACCENT) }

    fun hexFor(slot: ThemeColorSlot): Long = when (slot) {
        ThemeColorSlot.ACCENT -> accentHex
        ThemeColorSlot.BACKGROUND -> bgHex
        ThemeColorSlot.SURFACE -> surfaceHex
        ThemeColorSlot.BORDER -> borderHex
        ThemeColorSlot.TEXT -> textHex
        ThemeColorSlot.MUTED -> mutedHex
        ThemeColorSlot.PILL_BG -> pillBgHex
        ThemeColorSlot.PILL_TEXT -> pillTextHex
    }

    fun setHexFor(slot: ThemeColorSlot, hex: Long) {
        when (slot) {
            ThemeColorSlot.ACCENT -> accentHex = hex
            ThemeColorSlot.BACKGROUND -> bgHex = hex
            ThemeColorSlot.SURFACE -> surfaceHex = hex
            ThemeColorSlot.BORDER -> borderHex = hex
            ThemeColorSlot.TEXT -> textHex = hex
            ThemeColorSlot.MUTED -> mutedHex = hex
            ThemeColorSlot.PILL_BG -> pillBgHex = hex
            ThemeColorSlot.PILL_TEXT -> pillTextHex = hex
        }
    }

    fun autoHarmonizeFromAccent(lightMode: Boolean) {
        val hsv = FloatArray(3)
        AndroidColor.colorToHSV(accentHex.toInt(), hsv)
        val hue = hsv[0]
        val sat = hsv[1].coerceIn(0.25f, 0.9f)

        if (!lightMode) {
            bgHex = hsvToHex(hue, (sat * 0.22f).coerceAtMost(0.18f), 0.06f)
            surfaceHex = hsvToHex(hue, (sat * 0.25f).coerceAtMost(0.22f), 0.11f)
            borderHex = hsvToHex(hue, (sat * 0.30f).coerceAtMost(0.26f), 0.22f)
            textHex = hsvToHex(hue, 0.06f, 0.94f)
            mutedHex = hsvToHex(hue, (sat * 0.18f).coerceAtMost(0.16f), 0.58f)
            pillBgHex = hsvToHex(hue, (sat * 0.55f).coerceIn(0.18f, 0.55f), 0.26f)
            pillTextHex = hsvToHex(hue, (sat * 0.45f).coerceAtMost(0.45f), 0.96f)
        } else {
            bgHex = hsvToHex(hue, (sat * 0.08f).coerceAtMost(0.08f), 0.96f)
            surfaceHex = hsvToHex(hue, (sat * 0.04f).coerceAtMost(0.05f), 1.0f)
            borderHex = hsvToHex(hue, (sat * 0.16f).coerceAtMost(0.18f), 0.84f)
            textHex = hsvToHex(hue, (sat * 0.25f).coerceAtMost(0.25f), 0.16f)
            mutedHex = hsvToHex(hue, (sat * 0.18f).coerceAtMost(0.18f), 0.46f)
            pillBgHex = hsvToHex(hue, (sat * 0.26f).coerceAtMost(0.28f), 0.88f)
            pillTextHex = hsvToHex(hue, (sat * 0.85f).coerceIn(0.35f, 0.85f), 0.45f)
        }
        isLight = lightMode
    }

    val editingHex = hexFor(activeSlot)
    val editingHsv = remember(activeSlot, editingHex) {
        FloatArray(3).also { AndroidColor.colorToHSV(editingHex.toInt(), it) }
    }
    var hue by remember(activeSlot) { mutableFloatStateOf(editingHsv[0]) }
    var sat by remember(activeSlot) { mutableFloatStateOf(editingHsv[1]) }
    var value by remember(activeSlot) { mutableFloatStateOf(editingHsv[2]) }
    var hexText by remember(activeSlot, editingHex) {
        mutableStateOf(String.format("%06X", editingHex and 0xFFFFFFL))
    }

    fun updateFromHsv(h: Float, s: Float, v: Float) {
        hue = h.coerceIn(0f, 360f)
        sat = s.coerceIn(0f, 1f)
        value = v.coerceIn(0f, 1f)
        val newHex = hsvToHex(hue, sat, value)
        setHexFor(activeSlot, newHex)
        hexText = String.format("%06X", newHex and 0xFFFFFFL)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        TuiCard(
            contentPadding = PaddingValues(16.dp),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 720.dp)
                .testTag("custom_theme_builder_dialog")
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TuiSectionHeader(text = "Custom Theme Studio")
                    TuiCliLink(
                        label = if (isLight) "polarity: light" else "polarity: dark",
                        onClick = { isLight = !isLight }
                    )
                }

                // Live Preview Card using the custom palette in progress
                val radius = tokens.cardCornerRadius
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(radius))
                        .background(Color(bgHex))
                        .border(1.dp, Color(borderHex), RoundedCornerShape(radius))
                        .padding(10.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "~/preview · custom palette",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(accentHex)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(radius))
                                .background(Color(pillBgHex))
                                .border(1.dp, Color(accentHex).copy(alpha = 0.6f), RoundedCornerShape(radius))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "active pill",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = Color(pillTextHex)
                            )
                        }
                    }
                    Column(
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(radius))
                            .background(Color(surfaceHex))
                            .border(1.dp, Color(borderHex), RoundedCornerShape(radius))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "Primary text headline",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = Color(textHex)
                        )
                        Text(
                            text = "Muted subtitle & secondary info",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(mutedHex)
                        )
                    }
                }

                // Palette Generator shortcuts
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TuiCliLink(
                        label = "auto-generate dark",
                        onClick = { autoHarmonizeFromAccent(lightMode = false) }
                    )
                    TuiCliLink(
                        label = "auto-generate light",
                        onClick = { autoHarmonizeFromAccent(lightMode = true) }
                    )
                }

                // 8 Color Slots Selector Grid
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ThemeColorSlot.entries.forEach { slot ->
                        val selected = slot == activeSlot
                        val slotHex = hexFor(slot)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(tokens.cardCornerRadius))
                                .background(if (selected) tokens.pillBg else tokens.background.copy(alpha = 0.7f))
                                .border(
                                    width = if (selected) 1.5.dp else 1.dp,
                                    color = if (selected) tokens.accent else tokens.border,
                                    shape = RoundedCornerShape(tokens.cardCornerRadius)
                                )
                                .clickable { activeSlot = slot }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color(slotHex))
                                    .border(0.75.dp, tokens.border, RoundedCornerShape(2.dp))
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = slot.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                ),
                                color = if (selected) tokens.pillText else tokens.textPrimary
                            )
                        }
                    }
                }

                // Interactive Saturation / Brightness Canvas + Hue Bar for the selected slot
                Text(
                    text = "Editing: ${activeSlot.label} (#${String.format("%06X", editingHex and 0xFFFFFFL)})",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                    color = tokens.accent
                )

                SaturationValueSquare(
                    hue = hue,
                    sat = sat,
                    value = value,
                    onSatValueChanged = { s, v -> updateFromHsv(hue, s, v) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(tokens.cardCornerRadius))
                        .border(1.dp, tokens.border, RoundedCornerShape(tokens.cardCornerRadius))
                )

                HueRainbowBar(
                    hue = hue,
                    onHueChanged = { h -> updateFromHsv(h, sat, value) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(26.dp)
                        .clip(RoundedCornerShape(tokens.cardCornerRadius))
                        .border(1.dp, tokens.border, RoundedCornerShape(tokens.cardCornerRadius))
                )

                // Hex Direct Entry + Quick Swatches
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(tokens.cardCornerRadius))
                            .background(Color(editingHex))
                            .border(1.dp, tokens.border, RoundedCornerShape(tokens.cardCornerRadius))
                    )
                    Spacer(Modifier.width(10.dp))
                    TuiTextField(
                        value = hexText,
                        onValueChange = { raw ->
                            val clean = raw.uppercase().filter { it in '0'..'9' || it in 'A'..'F' }.take(6)
                            hexText = clean
                            if (clean.length == 6) {
                                runCatching {
                                    val parsed = clean.toLong(16) or 0xFF000000L
                                    setHexFor(activeSlot, parsed)
                                    val hsv = FloatArray(3)
                                    AndroidColor.colorToHSV(parsed.toInt(), hsv)
                                    hue = hsv[0]
                                    sat = hsv[1]
                                    value = hsv[2]
                                }
                            }
                        },
                        label = "Hex (#RRGGBB)",
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = tokens.textMuted)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onApplyCustomTheme(
                                settings.copy(
                                    themePreset = ThemePreset.CUSTOM,
                                    accentColor = accentHex,
                                    backgroundColor = bgHex,
                                    surfaceColor = surfaceHex,
                                    borderColor = borderHex,
                                    textColor = textHex,
                                    mutedTextColor = mutedHex,
                                    pillBackgroundColor = pillBgHex,
                                    pillTextColor = pillTextHex,
                                    isLightTheme = isLight
                                )
                            )
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = tokens.pillBg,
                            contentColor = tokens.pillText
                        ),
                        shape = RoundedCornerShape(tokens.cardCornerRadius),
                        modifier = Modifier.testTag("apply_custom_theme_button")
                    ) {
                        Text("Apply Custom Theme")
                    }
                }
            }
        }
    }
}

@Composable
private fun SaturationValueSquare(
    hue: Float,
    sat: Float,
    value: Float,
    onSatValueChanged: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val hueColor = remember(hue) {
        Color(AndroidColor.HSVToColor(floatArrayOf(hue.coerceIn(0f, 360f), 1f, 1f)))
    }
    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val s = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    val v = 1f - (offset.y / size.height.toFloat()).coerceIn(0f, 1f)
                    onSatValueChanged(s, v)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    val s = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                    val v = 1f - (change.position.y / size.height.toFloat()).coerceIn(0f, 1f)
                    onSatValueChanged(s, v)
                }
            }
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawRect(
                brush = Brush.horizontalGradient(listOf(Color.White, hueColor))
            )
            drawRect(
                brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black))
            )
            val cx = sat.coerceIn(0f, 1f) * size.width
            val cy = (1f - value.coerceIn(0f, 1f)) * size.height
            drawCircle(
                color = Color.White,
                radius = 9.dp.toPx(),
                center = Offset(cx, cy),
                style = Stroke(width = 2.5.dp.toPx())
            )
            drawCircle(
                color = Color.Black.copy(alpha = 0.6f),
                radius = 10.5.dp.toPx(),
                center = Offset(cx, cy),
                style = Stroke(width = 1.dp.toPx())
            )
        }
    }
}

@Composable
private fun HueRainbowBar(
    hue: Float,
    onHueChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val spectrum = remember {
        listOf(
            Color(0xFFFF0000),
            Color(0xFFFFFF00),
            Color(0xFF00FF00),
            Color(0xFF00FFFF),
            Color(0xFF0000FF),
            Color(0xFFFF00FF),
            Color(0xFFFF0000)
        )
    }
    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val h = (offset.x / size.width.toFloat()).coerceIn(0f, 1f) * 360f
                    onHueChanged(h)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    val h = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f) * 360f
                    onHueChanged(h)
                }
            }
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawRect(brush = Brush.horizontalGradient(spectrum))
            val x = (hue.coerceIn(0f, 360f) / 360f) * size.width
            drawLine(
                color = Color.White,
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 3.dp.toPx()
            )
        }
    }
}

private fun hsvToHex(h: Float, s: Float, v: Float): Long {
    val argb = AndroidColor.HSVToColor(
        floatArrayOf(h.coerceIn(0f, 360f), s.coerceIn(0f, 1f), v.coerceIn(0f, 1f))
    )
    return argb.toLong() and 0xFFFFFFFFL
}

package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.ui.components.TuiCard
import com.example.ui.components.TuiSectionHeader
import com.example.ui.theme.LocalLauncherTokens
import kotlin.math.roundToInt

@Composable
fun SettingSliderRow(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueText: String,
    steps: Int = 0,
    onValueChange: (Float) -> Unit
) {
    val tokens = LocalLauncherTokens.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = tokens.textPrimary)
            Text(text = valueText, style = MaterialTheme.typography.labelLarge, color = tokens.accent)
        }
        Slider(
            value = value.coerceIn(valueRange.start, valueRange.endInclusive),
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = tokens.accent,
                activeTrackColor = tokens.accent,
                inactiveTrackColor = tokens.border
            )
        )
    }
}

@Composable
fun SettingToggleRow(
    label: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val tokens = LocalLauncherTokens.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = tokens.textPrimary)
            if (subtitle != null) {
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = tokens.textMuted)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = tokens.pillText,
                checkedTrackColor = tokens.pillBg,
                checkedBorderColor = tokens.accent,
                uncheckedThumbColor = tokens.textMuted,
                uncheckedTrackColor = tokens.background,
                uncheckedBorderColor = tokens.border
            )
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> SettingChipSelector(
    label: String,
    options: List<T>,
    selected: T,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit
) {
    val tokens = LocalLauncherTokens.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = tokens.textPrimary)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            options.forEach { option ->
                val isSelected = option == selected
                val shape = RoundedCornerShape(tokens.cardCornerRadius)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .defaultMinSize(minHeight = 36.dp)
                        .clip(shape)
                        .background(if (isSelected) tokens.pillBg else tokens.background.copy(alpha = 0.65f))
                        .border(
                            1.dp,
                            if (isSelected) tokens.accent else tokens.border,
                            shape
                        )
                        .clickable { onSelect(option) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = optionLabel(option),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        ),
                        color = if (isSelected) tokens.pillText else tokens.textMuted
                    )
                }
            }
        }
    }
}

@Composable
fun SettingColorRow(
    label: String,
    colorHex: Long,
    onColorSelected: (Long) -> Unit
) {
    val tokens = LocalLauncherTokens.current
    var showPicker by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clickable { showPicker = true }
            .padding(vertical = 4.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = tokens.textPrimary)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "#${String.format("%06X", colorHex and 0xFFFFFF)}",
                style = MaterialTheme.typography.labelMedium,
                color = tokens.textMuted
            )
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(tokens.cardCornerRadius))
                    .background(Color(colorHex))
                    .border(1.dp, tokens.border, RoundedCornerShape(tokens.cardCornerRadius))
            )
        }
    }

    if (showPicker) {
        ColorPickerDialog(
            title = label,
            initialColor = colorHex,
            onDismiss = { showPicker = false },
            onConfirm = { newHex ->
                onColorSelected(newHex)
                showPicker = false
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorPickerDialog(
    title: String,
    initialColor: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    val tokens = LocalLauncherTokens.current
    val initial = Color(initialColor)
    var r by remember { mutableFloatStateOf(initial.red * 255f) }
    var g by remember { mutableFloatStateOf(initial.green * 255f) }
    var b by remember { mutableFloatStateOf(initial.blue * 255f) }
    var hexInput by remember { mutableStateOf(String.format("%06X", initialColor and 0xFFFFFF)) }

    val previewColor = Color(
        red = (r.roundToInt().coerceIn(0, 255)) / 255f,
        green = (g.roundToInt().coerceIn(0, 255)) / 255f,
        blue = (b.roundToInt().coerceIn(0, 255)) / 255f
    )

    val swatches = listOf(
        0xFF9D85C4L, 0xFF4ADE80L, 0xFFF59E0BL, 0xFF7AA2F7L, 0xFFF43F5EL,
        0xFFEC4899L, 0xFF22D3EEL, 0xFFFFFFFFL, 0xFF0D0D0FL, 0xFF151518L,
        0xFF2B2B31L, 0xFFF2F1ECL
    )

    Dialog(onDismissRequest = onDismiss) {
        TuiCard(contentPadding = PaddingValues(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TuiSectionHeader(text = "Color · $title")
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(tokens.cardCornerRadius))
                            .background(previewColor)
                            .border(1.dp, tokens.border, RoundedCornerShape(tokens.cardCornerRadius))
                    )
                    Spacer(Modifier.width(12.dp))
                    TuiTextField(
                        value = hexInput,
                        onValueChange = { raw ->
                            val clean = raw.uppercase().filter { it in '0'..'9' || it in 'A'..'F' }.take(6)
                            hexInput = clean
                            if (clean.length == 6) {
                                runCatching {
                                    val parsed = clean.toLong(16) or 0xFF000000L
                                    val c = Color(parsed)
                                    r = c.red * 255f
                                    g = c.green * 255f
                                    b = c.blue * 255f
                                }
                            }
                        },
                        label = "Hex (#RRGGBB)",
                        modifier = Modifier.weight(1f)
                    )
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    swatches.forEach { swatch ->
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(tokens.cardCornerRadius))
                                .background(Color(swatch))
                                .border(1.dp, tokens.border, RoundedCornerShape(tokens.cardCornerRadius))
                                .clickable {
                                    val c = Color(swatch)
                                    r = c.red * 255f
                                    g = c.green * 255f
                                    b = c.blue * 255f
                                    hexInput = String.format("%06X", swatch and 0xFFFFFF)
                                }
                        )
                    }
                }

                SettingSliderRow("R", r, 0f..255f, r.roundToInt().toString()) {
                    r = it
                    val argb = Color(r.roundToInt(), g.roundToInt(), b.roundToInt()).toArgb().toLong() and 0xFFFFFFL
                    hexInput = String.format("%06X", argb)
                }
                SettingSliderRow("G", g, 0f..255f, g.roundToInt().toString()) {
                    g = it
                    val argb = Color(r.roundToInt(), g.roundToInt(), b.roundToInt()).toArgb().toLong() and 0xFFFFFFL
                    hexInput = String.format("%06X", argb)
                }
                SettingSliderRow("B", b, 0f..255f, b.roundToInt().toString()) {
                    b = it
                    val argb = Color(r.roundToInt(), g.roundToInt(), b.roundToInt()).toArgb().toLong() and 0xFFFFFFL
                    hexInput = String.format("%06X", argb)
                }

                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = tokens.textMuted) }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val result = (previewColor.toArgb().toLong() and 0xFFFFFFFFL)
                            onConfirm(result)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = tokens.pillBg,
                            contentColor = tokens.pillText
                        ),
                        shape = RoundedCornerShape(tokens.cardCornerRadius)
                    ) { Text("Apply") }
                }
            }
        }
    }
}

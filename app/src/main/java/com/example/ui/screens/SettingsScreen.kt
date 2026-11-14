package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.AppInfo
import com.example.model.BottomBarStyle
import com.example.model.DoubleTapAction
import com.example.model.DrawerLayoutMode
import com.example.model.DrawerSortOrder
import com.example.model.IconPackInfo
import com.example.model.IconShape
import com.example.model.LauncherFont
import com.example.model.LauncherSettings
import com.example.model.PageTransitionStyle
import com.example.model.ThemePreset
import com.example.model.WallpaperType
import com.example.data.IconPackManager
import com.example.ui.components.AppIconView
import com.example.ui.components.TuiCard
import com.example.ui.components.TuiCliLink
import com.example.ui.components.TuiSectionHeader
import com.example.ui.theme.LocalLauncherTokens
import com.example.ui.theme.toFontFamily
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: LauncherSettings,
    allApps: List<AppInfo>,
    iconPacks: List<IconPackInfo>,
    iconPackManager: IconPackManager,
    initialSection: String = "theme",
    onUpdateSettings: ((LauncherSettings) -> LauncherSettings) -> Unit,
    onApplyPreset: (ThemePreset) -> Unit,
    onPickCustomWallpaper: () -> Unit,
    onApplySystemWallpaper: () -> Unit,
    onEditProfile: () -> Unit,
    onTogglePinDock: (String) -> Unit,
    onUnhideApp: (AppInfo) -> Unit,
    onResetHomeGrid: () -> Unit,
    onResetAllSettings: () -> Unit,
    onOpenDefaultLauncherSettings: () -> Unit,
    onExportJson: () -> String,
    onImportJson: (String) -> Boolean,
    onShowToast: (String) -> Unit,
    onBackHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBackHome() }

    val tokens = LocalLauncherTokens.current
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    var expandedSection by remember { mutableStateOf(initialSection.ifBlank { "theme" }) }
    var monoOnlyFilter by remember { mutableStateOf(settings.font.isMonospace) }
    var themePolarityFilter by remember { mutableStateOf("all") }
    var themeFilterQuery by remember { mutableStateOf("") }
    var showHiddenAppsDialog by remember { mutableStateOf(false) }
    var showDockAppsDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showCustomThemeBuilder by remember { mutableStateOf(false) }

    val sectionIndexMap = remember {
        mapOf(
            "theme" to 1,
            "wallpaper" to 2,
            "fonts" to 3,
            "icons" to 4,
            "grid" to 5,
            "dock" to 6,
            "drawer" to 7,
            "system" to 8
        )
    }

    LaunchedEffect(initialSection) {
        if (initialSection.isNotBlank()) {
            expandedSection = initialSection
            sectionIndexMap[initialSection]?.let { idx ->
                listState.animateScrollToItem(idx)
            }
        }
    }

    val hiddenApps = remember(allApps) { allApps.filter { it.isHidden } }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen")
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        TuiSectionHeader(text = "~/settings & studio")
                        Text(
                            text = "Live theme, monospace styles, wallpaper, icons & grid",
                            style = MaterialTheme.typography.bodySmall,
                            color = tokens.textMuted
                        )
                    }
                    TuiCliLink(label = "home", onClick = onBackHome)
                }

                // Quick-jump chips so Theme, Mono Fonts & key menus are immediately visible
                val quickTabs = listOf(
                    "theme" to "theme",
                    "fonts" to "mono fonts",
                    "wallpaper" to "wallpaper",
                    "icons" to "icons",
                    "grid" to "grid",
                    "dock" to "dock"
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickTabs.forEach { (key, label) ->
                        val active = expandedSection == key
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(tokens.cardCornerRadius))
                                .background(if (active) tokens.pillBg else tokens.surface)
                                .border(
                                    1.dp,
                                    if (active) tokens.accent else tokens.border,
                                    RoundedCornerShape(tokens.cardCornerRadius)
                                )
                                .clickable {
                                    expandedSection = key
                                    sectionIndexMap[key]?.let { idx ->
                                        scope.launch { listState.animateScrollToItem(idx) }
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("settings_jump_$key")
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (active) tokens.pillText else tokens.textPrimary
                            )
                        }
                    }
                }
            }
        }

        // 01 · Theme & Terminal Color Schemes
        item {
            CollapsibleSectionCard(
                title = "01 · Theme & Color Schemes",
                subtitle = settings.themePreset.label,
                expanded = expandedSection == "theme",
                onToggle = { expandedSection = if (expandedSection == "theme") "" else "theme" }
            ) {
                val allPresets = remember { ThemePreset.entries.filter { it != ThemePreset.CUSTOM } }
                val darkCount = remember(allPresets) { allPresets.count { !it.isLight } }
                val lightCount = remember(allPresets) { allPresets.count { it.isLight } }

                // Live Palette Preview Strip
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(tokens.cardCornerRadius))
                        .background(tokens.background.copy(alpha = 0.85f))
                        .border(1.dp, tokens.accent.copy(alpha = 0.5f), RoundedCornerShape(tokens.cardCornerRadius))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "active theme · ${settings.themePreset.label}",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = tokens.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (settings.isLightTheme) "light polarity · terminal palette" else "dark polarity · terminal palette",
                            style = MaterialTheme.typography.labelSmall,
                            color = tokens.textMuted
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(
                            Color(settings.backgroundColor),
                            Color(settings.surfaceColor),
                            Color(settings.accentColor),
                            Color(settings.pillBackgroundColor),
                            Color(settings.textColor)
                        ).forEach { swatch ->
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .background(swatch, RoundedCornerShape(2.dp))
                                    .border(0.75.dp, tokens.border, RoundedCornerShape(2.dp))
                            )
                        }
                    }
                }

                // Polarity Filter Chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val polarityTabs = listOf(
                        "all" to "all (${allPresets.size})",
                        "dark" to "dark ($darkCount)",
                        "light" to "light ($lightCount)"
                    )
                    polarityTabs.forEach { (key, label) ->
                        val selected = themePolarityFilter == key
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(tokens.cardCornerRadius))
                                .background(if (selected) tokens.pillBg else tokens.background.copy(alpha = 0.6f))
                                .border(
                                    1.dp,
                                    if (selected) tokens.accent else tokens.border,
                                    RoundedCornerShape(tokens.cardCornerRadius)
                                )
                                .clickable { themePolarityFilter = key }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selected) tokens.pillText else tokens.textPrimary
                            )
                        }
                    }
                }

                // Quick Scheme Search Filter
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 42.dp)
                        .clip(RoundedCornerShape(tokens.cardCornerRadius))
                        .background(tokens.background.copy(alpha = 0.7f))
                        .border(1.dp, tokens.border, RoundedCornerShape(tokens.cardCornerRadius))
                        .padding(start = 10.dp, end = 4.dp)
                ) {
                    Text(
                        text = "~/",
                        style = MaterialTheme.typography.labelMedium,
                        color = tokens.accent
                    )
                    Spacer(Modifier.width(6.dp))
                    Box(
                        contentAlignment = Alignment.CenterStart,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (themeFilterQuery.isEmpty()) {
                            Text(
                                text = "filter themes (rose pine, catppuccin, gruvbox, nord...)",
                                style = MaterialTheme.typography.bodySmall,
                                color = tokens.textMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        BasicTextField(
                            value = themeFilterQuery,
                            onValueChange = { themeFilterQuery = it },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall.copy(color = tokens.textPrimary),
                            cursorBrush = SolidColor(tokens.accent),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("theme_filter_input")
                        )
                    }
                    if (themeFilterQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { themeFilterQuery = "" },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear theme filter",
                                tint = tokens.textMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                val filteredPresets = remember(allPresets, themePolarityFilter, themeFilterQuery) {
                    val q = themeFilterQuery.trim().lowercase()
                    allPresets.filter { preset ->
                        val matchesPolarity = when (themePolarityFilter) {
                            "dark" -> !preset.isLight
                            "light" -> preset.isLight
                            else -> true
                        }
                        val matchesQuery = q.isEmpty() || preset.label.lowercase().contains(q)
                        matchesPolarity && matchesQuery
                    }
                }

                if (filteredPresets.isEmpty()) {
                    Text(
                        text = "No color schemes match \"$themeFilterQuery\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.textMuted
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        filteredPresets.forEach { preset ->
                            val active = settings.themePreset == preset
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 48.dp)
                                    .clip(RoundedCornerShape(tokens.cardCornerRadius))
                                    .background(if (active) tokens.pillBg else tokens.background.copy(alpha = 0.6f))
                                    .border(
                                        1.dp,
                                        if (active) tokens.accent else tokens.border,
                                        RoundedCornerShape(tokens.cardCornerRadius)
                                    )
                                    .clickable { onApplyPreset(preset) }
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                                    .testTag("theme_preset_${preset.name.lowercase()}")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // 4-color terminal swatch strip
                                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                        listOf(
                                            Color(preset.bgHex),
                                            Color(preset.surfaceHex),
                                            Color(preset.accentHex),
                                            Color(preset.pillTextHex)
                                        ).forEach { c ->
                                            Box(
                                                modifier = Modifier
                                                    .size(13.dp)
                                                    .background(c, RoundedCornerShape(2.dp))
                                                    .border(0.5.dp, Color(preset.borderHex), RoundedCornerShape(2.dp))
                                            )
                                        }
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        text = preset.label,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
                                        ),
                                        color = if (active) tokens.pillText else tokens.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = buildString {
                                        if (active) append("active · ")
                                        append(if (preset.isLight) "light" else "dark")
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (active) tokens.pillText else tokens.textMuted
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))
                // Custom Theme from Scratch Studio Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 52.dp)
                        .clip(RoundedCornerShape(tokens.cardCornerRadius))
                        .background(
                            if (settings.themePreset == ThemePreset.CUSTOM) tokens.pillBg
                            else tokens.background.copy(alpha = 0.75f)
                        )
                        .border(
                            width = if (settings.themePreset == ThemePreset.CUSTOM) 1.5.dp else 1.dp,
                            color = tokens.accent,
                            shape = RoundedCornerShape(tokens.cardCornerRadius)
                        )
                        .clickable { showCustomThemeBuilder = true }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .testTag("open_custom_theme_studio")
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (settings.themePreset == ThemePreset.CUSTOM) {
                                "Custom Theme Studio · active"
                            } else {
                                "Build Custom Theme from Scratch"
                            },
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = if (settings.themePreset == ThemePreset.CUSTOM) tokens.pillText else tokens.accent
                        )
                        Text(
                            text = "HSV palette studio, live preview & auto-harmonizer",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (settings.themePreset == ThemePreset.CUSTOM) {
                                tokens.pillText.copy(alpha = 0.85f)
                            } else {
                                tokens.textMuted
                            }
                        )
                    }
                    Text(
                        text = "open",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (settings.themePreset == ThemePreset.CUSTOM) tokens.pillText else tokens.accent
                    )
                }

                SettingColorRow("Accent Color", settings.accentColor) { hex ->
                    onUpdateSettings { it.copy(themePreset = ThemePreset.CUSTOM, accentColor = hex) }
                }
                SettingColorRow("Background Canvas", settings.backgroundColor) { hex ->
                    onUpdateSettings { it.copy(themePreset = ThemePreset.CUSTOM, backgroundColor = hex) }
                }
                SettingColorRow("Card / Surface", settings.surfaceColor) { hex ->
                    onUpdateSettings { it.copy(themePreset = ThemePreset.CUSTOM, surfaceColor = hex) }
                }
                SettingColorRow("Border Hairline", settings.borderColor) { hex ->
                    onUpdateSettings { it.copy(themePreset = ThemePreset.CUSTOM, borderColor = hex) }
                }
                SettingColorRow("Primary Text", settings.textColor) { hex ->
                    onUpdateSettings { it.copy(themePreset = ThemePreset.CUSTOM, textColor = hex) }
                }
                SettingColorRow("Muted Text", settings.mutedTextColor) { hex ->
                    onUpdateSettings { it.copy(themePreset = ThemePreset.CUSTOM, mutedTextColor = hex) }
                }
                SettingColorRow("Active Pill Fill", settings.pillBackgroundColor) { hex ->
                    onUpdateSettings { it.copy(themePreset = ThemePreset.CUSTOM, pillBackgroundColor = hex) }
                }
                SettingColorRow("Active Pill Text", settings.pillTextColor) { hex ->
                    onUpdateSettings { it.copy(themePreset = ThemePreset.CUSTOM, pillTextColor = hex) }
                }
                SettingToggleRow(
                    label = "Light Mode Contrast",
                    subtitle = "Switch system & M3 color scheme polarity",
                    checked = settings.isLightTheme,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(isLightTheme = v) } }
                )
                SettingSliderRow(
                    label = "Card Surface Opacity",
                    value = settings.surfaceOpacity,
                    valueRange = 0.25f..1f,
                    valueText = "${(settings.surfaceOpacity * 100).roundToInt()}%"
                ) { v -> onUpdateSettings { it.copy(surfaceOpacity = v) } }
                SettingSliderRow(
                    label = "Card Border Width",
                    value = settings.cardBorderWidthDp,
                    valueRange = 0f..2.5f,
                    valueText = String.format("%.1fdp", settings.cardBorderWidthDp)
                ) { v -> onUpdateSettings { it.copy(cardBorderWidthDp = v) } }
                SettingToggleRow(
                    label = "Rounded UI Corners",
                    subtitle = "Smooth rounded corners on cards, panels, buttons & dialogs",
                    checked = settings.cardCornerRadiusDp >= 6f,
                    onCheckedChange = { rounded ->
                        onUpdateSettings { it.copy(cardCornerRadiusDp = if (rounded) 12f else 0f) }
                    }
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val cornerPresets = listOf(
                        "Sharp (0dp)" to 0f,
                        "Subtle (2dp)" to 2f,
                        "Rounded (12dp)" to 12f,
                        "Soft (18dp)" to 18f
                    )
                    cornerPresets.forEach { (label, radius) ->
                        val active = kotlin.math.abs(settings.cardCornerRadiusDp - radius) < 0.8f
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(tokens.cardCornerRadius))
                                .background(if (active) tokens.pillBg else tokens.background.copy(alpha = 0.6f))
                                .border(
                                    1.dp,
                                    if (active) tokens.accent else tokens.border,
                                    RoundedCornerShape(tokens.cardCornerRadius)
                                )
                                .clickable { onUpdateSettings { it.copy(cardCornerRadiusDp = radius) } }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (active) tokens.pillText else tokens.textPrimary
                            )
                        }
                    }
                }
                SettingSliderRow(
                    label = "Card Corner Radius",
                    value = settings.cardCornerRadiusDp,
                    valueRange = 0f..24f,
                    valueText = "${settings.cardCornerRadiusDp.roundToInt()}dp"
                ) { v -> onUpdateSettings { it.copy(cardCornerRadiusDp = v) } }
            }
        }

        // 02 · Wallpaper Studio
        item {
            CollapsibleSectionCard(
                title = "02 · Wallpaper Studio",
                subtitle = settings.wallpaperType.label,
                expanded = expandedSection == "wallpaper",
                onToggle = { expandedSection = if (expandedSection == "wallpaper") "" else "wallpaper" }
            ) {
                SettingChipSelector(
                    label = "Wallpaper Engine",
                    options = WallpaperType.entries,
                    selected = settings.wallpaperType,
                    optionLabel = { it.label },
                    onSelect = { type ->
                        if (type == WallpaperType.CUSTOM_IMAGE && settings.customWallpaperPath.isBlank()) {
                            onPickCustomWallpaper()
                        } else {
                            onUpdateSettings { it.copy(wallpaperType = type) }
                        }
                    }
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TuiCliLink(label = "pick gallery image", onClick = onPickCustomWallpaper)
                    if (settings.customWallpaperPath.isNotBlank()) {
                        TuiCliLink(label = "set as system wallpaper", onClick = onApplySystemWallpaper)
                    }
                }
                SettingSliderRow(
                    label = "Wallpaper Dim Scrim",
                    value = settings.wallpaperDim,
                    valueRange = 0f..0.9f,
                    valueText = "${(settings.wallpaperDim * 100).roundToInt()}%"
                ) { v -> onUpdateSettings { it.copy(wallpaperDim = v) } }
                SettingSliderRow(
                    label = "Wallpaper Blur (Android 12+)",
                    value = settings.wallpaperBlurDp,
                    valueRange = 0f..24f,
                    valueText = "${settings.wallpaperBlurDp.roundToInt()}dp"
                ) { v -> onUpdateSettings { it.copy(wallpaperBlurDp = v) } }
                SettingToggleRow(
                    label = "Page Scroll Parallax",
                    subtitle = "Subtle horizontal depth shift across home pages",
                    checked = settings.wallpaperParallax,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(wallpaperParallax = v) } }
                )
            }
        }

        // 03 · Monospace & Font Styles
        item {
            CollapsibleSectionCard(
                title = "03 · Monospace & Font Styles",
                subtitle = "${settings.font.displayName} · ${settings.font.tag}",
                expanded = expandedSection == "fonts",
                onToggle = { expandedSection = if (expandedSection == "fonts") "" else "fonts" }
            ) {
                // Live Terminal Preview Card
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(tokens.cardCornerRadius))
                        .background(tokens.background.copy(alpha = 0.85f))
                        .border(1.dp, tokens.accent.copy(alpha = 0.5f), RoundedCornerShape(tokens.cardCornerRadius))
                        .padding(10.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "terminal preview · ${settings.font.displayName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = tokens.accent
                        )
                        Text(
                            text = "0123456789",
                            style = MaterialTheme.typography.labelSmall,
                            color = tokens.textMuted
                        )
                    }
                    Text(
                        text = "~/home · user@tilde:~$ ls -la",
                        style = MaterialTheme.typography.titleMedium,
                        color = tokens.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "drwxr-xr-x  tilde  42 apps indexed · 14:32 · 68%",
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Filter between Monospace Terminal Styles & All Fonts
                val monoFonts = remember { LauncherFont.entries.filter { it.isMonospace } }
                val displayedFonts = if (monoOnlyFilter) monoFonts else LauncherFont.entries
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val chips = listOf(
                        true to "monospace (${monoFonts.size})",
                        false to "all fonts (${LauncherFont.entries.size})"
                    )
                    chips.forEach { (isMono, label) ->
                        val selected = monoOnlyFilter == isMono
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(tokens.cardCornerRadius))
                                .background(if (selected) tokens.pillBg else tokens.background.copy(alpha = 0.6f))
                                .border(
                                    1.dp,
                                    if (selected) tokens.accent else tokens.border,
                                    RoundedCornerShape(tokens.cardCornerRadius)
                                )
                                .clickable { monoOnlyFilter = isMono }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selected) tokens.pillText else tokens.textPrimary
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    displayedFonts.forEach { font ->
                        val active = settings.font == font
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .clip(RoundedCornerShape(tokens.cardCornerRadius))
                                .background(if (active) tokens.pillBg else tokens.background.copy(alpha = 0.6f))
                                .border(
                                    1.dp,
                                    if (active) tokens.accent else tokens.border,
                                    RoundedCornerShape(tokens.cardCornerRadius)
                                )
                                .clickable { onUpdateSettings { it.copy(font = font) } }
                                .padding(horizontal = 12.dp, vertical = 9.dp)
                                .testTag("font_option_${font.name.lowercase()}")
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${font.displayName} — ~/home",
                                    fontFamily = font.toFontFamily(),
                                    fontWeight = if (settings.boldTerminalText) FontWeight.Bold else FontWeight.Normal,
                                    fontStyle = if (settings.italicizeHeaders) FontStyle.Italic else FontStyle.Normal,
                                    fontSize = 14.sp,
                                    color = if (active) tokens.pillText else tokens.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "user@tilde:~$ 09:42 · AaBbZz",
                                    fontFamily = font.toFontFamily(),
                                    fontSize = 11.sp,
                                    color = if (active) tokens.pillText.copy(alpha = 0.8f) else tokens.textMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (active) "active · ${font.tag}" else font.tag,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (active) tokens.pillText else tokens.textMuted
                            )
                        }
                    }
                }

                // Quick Terminal Style Presets
                Text("Terminal Aesthetic Presets", style = MaterialTheme.typography.bodyMedium, color = tokens.textPrimary)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val stylePresets = listOf(
                        Triple("Slanted TUI", true to false, 0.02f),
                        Triple("Upright Raw", false to false, 0.0f),
                        Triple("Bold Shell", false to true, 0.02f),
                        Triple("Compact Dense", false to false, -0.01f),
                        Triple("Wide CRT", true to true, 0.08f)
                    )
                    stylePresets.forEach { (label, flags, spacing) ->
                        val isMatch = settings.italicizeHeaders == flags.first &&
                            settings.boldTerminalText == flags.second &&
                            kotlin.math.abs(settings.letterSpacingEm - spacing) < 0.008f
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(tokens.cardCornerRadius))
                                .background(if (isMatch) tokens.pillBg else tokens.background.copy(alpha = 0.6f))
                                .border(
                                    1.dp,
                                    if (isMatch) tokens.accent else tokens.border,
                                    RoundedCornerShape(tokens.cardCornerRadius)
                                )
                                .clickable {
                                    onUpdateSettings {
                                        it.copy(
                                            italicizeHeaders = flags.first,
                                            boldTerminalText = flags.second,
                                            letterSpacingEm = spacing
                                        )
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isMatch) tokens.pillText else tokens.textPrimary
                            )
                        }
                    }
                }

                SettingToggleRow(
                    label = "Bold Terminal Weight",
                    subtitle = "Crisper, thicker monospace glyphs across UI & widgets",
                    checked = settings.boldTerminalText,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(boldTerminalText = v) } }
                )
                SettingToggleRow(
                    label = "Italicize Headers & Titles",
                    subtitle = "Signature slanted monospace titles from reference UI",
                    checked = settings.italicizeHeaders,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(italicizeHeaders = v) } }
                )
                SettingToggleRow(
                    label = "Uppercase Section Labels",
                    checked = settings.uppercaseSectionHeaders,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(uppercaseSectionHeaders = v) } }
                )
                SettingSliderRow(
                    label = "Global Font Scale",
                    value = settings.fontScale,
                    valueRange = 0.75f..1.45f,
                    valueText = "${String.format("%.2f", settings.fontScale)}x"
                ) { v -> onUpdateSettings { it.copy(fontScale = v) } }
                SettingSliderRow(
                    label = "App Label Font Size",
                    value = settings.iconLabelSizeSp,
                    valueRange = 9f..16f,
                    valueText = "${String.format("%.1f", settings.iconLabelSizeSp)}sp"
                ) { v -> onUpdateSettings { it.copy(iconLabelSizeSp = v) } }
                SettingSliderRow(
                    label = "Clock Display Size",
                    value = settings.clockFontSizeSp,
                    valueRange = 28f..64f,
                    valueText = "${settings.clockFontSizeSp.roundToInt()}sp"
                ) { v -> onUpdateSettings { it.copy(clockFontSizeSp = v) } }
                SettingSliderRow(
                    label = "Letter Spacing / Tracking",
                    value = settings.letterSpacingEm,
                    valueRange = -0.02f..0.15f,
                    valueText = "${String.format("%.2f", settings.letterSpacingEm)}em"
                ) { v -> onUpdateSettings { it.copy(letterSpacingEm = v) } }
            }
        }

        // 04 · Icons & Icon Packs
        item {
            CollapsibleSectionCard(
                title = "04 · Icons & Icon Packs",
                subtitle = iconPacks.firstOrNull { it.id == settings.selectedIconPack }?.label ?: "Custom",
                expanded = expandedSection == "icons",
                onToggle = { expandedSection = if (expandedSection == "icons") "" else "icons" }
            ) {
                Text("Available Icon Packs & Styles", style = MaterialTheme.typography.bodyMedium, color = tokens.textPrimary)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    iconPacks.forEach { pack ->
                        val active = settings.selectedIconPack == pack.id
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(tokens.cardCornerRadius))
                                .background(if (active) tokens.pillBg else tokens.background.copy(alpha = 0.6f))
                                .border(
                                    1.dp,
                                    if (active) tokens.accent else tokens.border,
                                    RoundedCornerShape(tokens.cardCornerRadius)
                                )
                                .clickable { onUpdateSettings { it.copy(selectedIconPack = pack.id) } }
                                .padding(horizontal = 12.dp, vertical = 9.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = pack.label,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = if (active) tokens.pillText else tokens.textPrimary
                                )
                                Text(
                                    text = pack.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (active) tokens.pillText.copy(alpha = 0.8f) else tokens.textMuted
                                )
                            }
                            if (active) {
                                Text("active", style = MaterialTheme.typography.labelSmall, color = tokens.accent)
                            }
                        }
                    }
                }
                // Live Icon Shape Preview
                val previewApps = remember(allApps) { allApps.filter { !it.isHidden }.take(4) }
                if (previewApps.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(tokens.cardCornerRadius))
                            .background(tokens.background.copy(alpha = 0.8f))
                            .border(1.dp, tokens.border, RoundedCornerShape(tokens.cardCornerRadius))
                            .padding(vertical = 10.dp, horizontal = 8.dp)
                    ) {
                        previewApps.forEach { app ->
                            AppIconView(
                                app = app,
                                settings = settings,
                                iconPackManager = iconPackManager,
                                sizeOverride = 44.dp
                            )
                        }
                    }
                }

                SettingToggleRow(
                    label = "Round App Icons",
                    subtitle = "Toggle between default square icons and rounded corners",
                    checked = settings.iconShape != IconShape.SHARP,
                    onCheckedChange = { rounded ->
                        onUpdateSettings {
                            it.copy(
                                iconShape = if (rounded) IconShape.ROUNDED else IconShape.SHARP,
                                iconCornerRadiusDp = if (rounded && it.iconCornerRadiusDp < 4f) 10f else it.iconCornerRadiusDp
                            )
                        }
                    }
                )
                SettingChipSelector(
                    label = "Icon Mask Shape",
                    options = IconShape.entries,
                    selected = settings.iconShape,
                    optionLabel = { it.label },
                    onSelect = { shape -> onUpdateSettings { it.copy(iconShape = shape) } }
                )
                SettingSliderRow(
                    label = "Icon Size",
                    value = settings.iconSizeDp,
                    valueRange = 34f..68f,
                    valueText = "${settings.iconSizeDp.roundToInt()}dp"
                ) { v -> onUpdateSettings { it.copy(iconSizeDp = v) } }
                if (settings.iconShape == IconShape.ROUNDED) {
                    SettingSliderRow(
                        label = "Icon Corner Radius",
                        value = settings.iconCornerRadiusDp,
                        valueRange = 2f..24f,
                        valueText = "${settings.iconCornerRadiusDp.roundToInt()}dp"
                    ) { v -> onUpdateSettings { it.copy(iconCornerRadiusDp = v) } }
                }
                SettingSliderRow(
                    label = "Icon Inner Padding",
                    value = settings.iconPaddingDp,
                    valueRange = 0f..12f,
                    valueText = "${settings.iconPaddingDp.roundToInt()}dp"
                ) { v -> onUpdateSettings { it.copy(iconPaddingDp = v) } }
                SettingToggleRow(
                    label = "Show App Labels",
                    checked = settings.showIconLabels,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(showIconLabels = v) } }
                )
                SettingChipSelector(
                    label = "Max Label Lines",
                    options = listOf(1, 2),
                    selected = settings.maxLabelLines,
                    optionLabel = { "$it line${if (it > 1) "s" else ""}" },
                    onSelect = { lines -> onUpdateSettings { it.copy(maxLabelLines = lines) } }
                )
            }
        }

        // 05 · Home Grid & Gestures
        item {
            CollapsibleSectionCard(
                title = "05 · Home Grid & Gestures",
                subtitle = "${settings.gridColumns}x${settings.gridRows} grid",
                expanded = expandedSection == "grid",
                onToggle = { expandedSection = if (expandedSection == "grid") "" else "grid" }
            ) {
                SettingSliderRow(
                    label = "Grid Columns",
                    value = settings.gridColumns.toFloat(),
                    valueRange = 3f..7f,
                    steps = 3,
                    valueText = "${settings.gridColumns}"
                ) { v -> onUpdateSettings { it.copy(gridColumns = v.roundToInt()) } }
                SettingSliderRow(
                    label = "Grid Rows",
                    value = settings.gridRows.toFloat(),
                    valueRange = 4f..8f,
                    steps = 3,
                    valueText = "${settings.gridRows}"
                ) { v -> onUpdateSettings { it.copy(gridRows = v.roundToInt()) } }
                SettingSliderRow(
                    label = "Grid Horizontal Margin",
                    value = settings.gridHorizontalMarginDp,
                    valueRange = 0f..24f,
                    valueText = "${settings.gridHorizontalMarginDp.roundToInt()}dp"
                ) { v -> onUpdateSettings { it.copy(gridHorizontalMarginDp = v) } }
                SettingSliderRow(
                    label = "Cell Gap Spacing",
                    value = settings.gridCellGapDp,
                    valueRange = 2f..16f,
                    valueText = "${settings.gridCellGapDp.roundToInt()}dp"
                ) { v -> onUpdateSettings { it.copy(gridCellGapDp = v) } }
                SettingChipSelector(
                    label = "Page Transition Effect",
                    options = PageTransitionStyle.entries,
                    selected = settings.pageTransitionStyle,
                    optionLabel = { it.label },
                    onSelect = { p -> onUpdateSettings { it.copy(pageTransitionStyle = p) } }
                )
                SettingChipSelector(
                    label = "Double-Tap Empty Space",
                    options = DoubleTapAction.entries,
                    selected = settings.doubleTapAction,
                    optionLabel = { it.label },
                    onSelect = { d -> onUpdateSettings { it.copy(doubleTapAction = d) } }
                )
                SettingToggleRow(
                    label = "Show Grid Cell Outlines",
                    subtitle = "Minimalist wireframe cell borders on home screen",
                    checked = settings.showGridCellOutlines,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(showGridCellOutlines = v) } }
                )
                SettingToggleRow(
                    label = "Show Page Indicator",
                    checked = settings.showPageIndicator,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(showPageIndicator = v) } }
                )
                SettingToggleRow(
                    label = "Show Subtle Workspace Header",
                    checked = settings.showTopStatusPill,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(showTopStatusPill = v) } }
                )
                SettingToggleRow(
                    label = "Swipe Up for App Drawer",
                    checked = settings.swipeUpForDrawer,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(swipeUpForDrawer = v) } }
                )
                SettingToggleRow(
                    label = "Lock Home Screen Layout",
                    subtitle = "Prevent accidental moves, edits or deletions",
                    checked = settings.lockHomeLayout,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(lockHomeLayout = v) } }
                )
                SettingToggleRow(
                    label = "24-Hour Clock Format",
                    checked = settings.clock24Hour,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(clock24Hour = v) } }
                )
                SettingToggleRow(
                    label = "Show Clock Seconds",
                    checked = settings.clockShowSeconds,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(clockShowSeconds = v) } }
                )
                TuiCliLink(label = "reset home grid layout", onClick = onResetHomeGrid)
            }
        }

        // 06 · Bottom Panel & Dock
        item {
            CollapsibleSectionCard(
                title = "06 · Bottom Panel & Dock",
                subtitle = settings.bottomBarStyle.label,
                expanded = expandedSection == "dock",
                onToggle = { expandedSection = if (expandedSection == "dock") "" else "dock" }
            ) {
                SettingChipSelector(
                    label = "Bottom Panel Frame",
                    options = BottomBarStyle.entries,
                    selected = settings.bottomBarStyle,
                    optionLabel = { it.label },
                    onSelect = { b -> onUpdateSettings { it.copy(bottomBarStyle = b) } }
                )
                SettingToggleRow(
                    label = "Show Search Bar",
                    checked = settings.showSearchInBottomBar,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(showSearchInBottomBar = v) } }
                )
                SettingToggleRow(
                    label = "Inline Calculator (=math)",
                    subtitle = "Evaluate math expressions directly in search",
                    checked = settings.enableSearchCalculator,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(enableSearchCalculator = v) } }
                )
                SettingToggleRow(
                    label = "Show Primary ~/ Nav Shortcuts",
                    checked = settings.showNavShortcuts,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(showNavShortcuts = v) } }
                )
                TuiCliLink(
                    label = "manage pinned dock apps (${settings.pinnedDockPackages.size}/4)",
                    onClick = { showDockAppsDialog = true }
                )
            }
        }

        // 07 · App Drawer
        item {
            CollapsibleSectionCard(
                title = "07 · App Drawer",
                subtitle = "${settings.drawerLayoutMode.label} · ${settings.drawerColumns} cols",
                expanded = expandedSection == "drawer",
                onToggle = { expandedSection = if (expandedSection == "drawer") "" else "drawer" }
            ) {
                SettingChipSelector(
                    label = "Drawer Layout",
                    options = DrawerLayoutMode.entries,
                    selected = settings.drawerLayoutMode,
                    optionLabel = { it.label },
                    onSelect = { d -> onUpdateSettings { it.copy(drawerLayoutMode = d) } }
                )
                SettingChipSelector(
                    label = "Default Sort Order",
                    options = DrawerSortOrder.entries,
                    selected = settings.drawerSortOrder,
                    optionLabel = { it.label },
                    onSelect = { s -> onUpdateSettings { it.copy(drawerSortOrder = s) } }
                )
                SettingSliderRow(
                    label = "Drawer Grid Columns",
                    value = settings.drawerColumns.toFloat(),
                    valueRange = 3f..6f,
                    steps = 2,
                    valueText = "${settings.drawerColumns}"
                ) { v -> onUpdateSettings { it.copy(drawerColumns = v.roundToInt()) } }
                SettingToggleRow(
                    label = "Show Settings Menu in Drawer",
                    checked = settings.showSettingsInDrawer,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(showSettingsInDrawer = v) } }
                )
                TuiCliLink(
                    label = "manage hidden apps (${hiddenApps.size})",
                    onClick = { showHiddenAppsDialog = true }
                )
            }
        }

        // 08 · System, Profile & Backup
        item {
            CollapsibleSectionCard(
                title = "08 · System, Profile & Backup",
                subtitle = "Default launcher & JSON export",
                expanded = expandedSection == "system",
                onToggle = { expandedSection = if (expandedSection == "system") "" else "system" }
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TuiCliLink(label = "set as default home app", onClick = onOpenDefaultLauncherSettings)
                    TuiCliLink(label = "edit profile card", onClick = onEditProfile)
                    TuiCliLink(label = "export / import json", onClick = { showBackupDialog = true })
                    TuiCliLink(
                        label = "factory reset theme",
                        color = MaterialTheme.colorScheme.error,
                        onClick = onResetAllSettings
                    )
                }
            }
        }
    }

    if (showHiddenAppsDialog) {
        Dialog(onDismissRequest = { showHiddenAppsDialog = false }) {
            TuiCard(contentPadding = PaddingValues(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TuiSectionHeader(text = "Hidden apps (${hiddenApps.size})")
                    if (hiddenApps.isEmpty()) {
                        Text(
                            text = "No hidden apps. Long-press any app in the drawer to hide it.",
                            style = MaterialTheme.typography.bodySmall,
                            color = tokens.textMuted
                        )
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 260.dp)) {
                            items(hiddenApps, key = { it.componentKey }) { app ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                ) {
                                    Text(app.displayLabel, style = MaterialTheme.typography.bodyMedium, color = tokens.textPrimary)
                                    TuiCliLink(label = "unhide", onClick = { onUnhideApp(app) })
                                }
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        TuiCliLink(label = "close", onClick = { showHiddenAppsDialog = false })
                    }
                }
            }
        }
    }

    if (showDockAppsDialog) {
        Dialog(onDismissRequest = { showDockAppsDialog = false }) {
            TuiCard(contentPadding = PaddingValues(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TuiSectionHeader(text = "Pinned dock apps (${settings.pinnedDockPackages.size}/4)")
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(allApps.filter { !it.isHidden }, key = { it.componentKey }) { app ->
                            val pinned = settings.pinnedDockPackages.contains(app.packageName)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onTogglePinDock(app.packageName) }
                                    .padding(vertical = 7.dp)
                            ) {
                                Text(
                                    text = app.displayLabel,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (pinned) tokens.accent else tokens.textPrimary
                                )
                                Text(
                                    text = if (pinned) "[pinned]" else "[pin]",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (pinned) tokens.accent else tokens.textMuted
                                )
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        TuiCliLink(label = "done", onClick = { showDockAppsDialog = false })
                    }
                }
            }
        }
    }

    if (showBackupDialog) {
        var jsonText by remember { mutableStateOf(onExportJson()) }
        Dialog(onDismissRequest = { showBackupDialog = false }) {
            TuiCard(contentPadding = PaddingValues(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    TuiSectionHeader(text = "Config JSON backup")
                    TuiTextField(
                        value = jsonText,
                        onValueChange = { jsonText = it },
                        label = "LauncherSettings JSON",
                        singleLine = false,
                        modifier = Modifier.heightIn(max = 220.dp)
                    )
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TuiCliLink(
                            label = "copy json",
                            onClick = {
                                clipboard.setText(AnnotatedString(jsonText))
                                onShowToast("Copied config JSON to clipboard")
                            }
                        )
                        Row {
                            TextButton(onClick = { showBackupDialog = false }) {
                                Text("Close", color = tokens.textMuted)
                            }
                            Button(
                                onClick = {
                                    if (onImportJson(jsonText)) {
                                        showBackupDialog = false
                                    } else {
                                        onShowToast("Invalid JSON format")
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = tokens.pillBg,
                                    contentColor = tokens.pillText
                                ),
                                shape = RoundedCornerShape(tokens.cardCornerRadius)
                            ) { Text("Import") }
                        }
                    }
                }
            }
        }
    }

    if (showCustomThemeBuilder) {
        CustomThemeBuilderDialog(
            settings = settings,
            onApplyCustomTheme = { customSettings ->
                onUpdateSettings { customSettings }
                onShowToast("Custom theme applied")
            },
            onDismiss = { showCustomThemeBuilder = false }
        )
    }
}

@Composable
private fun CollapsibleSectionCard(
    title: String,
    subtitle: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    val tokens = LocalLauncherTokens.current
    TuiCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .clickable(onClick = onToggle)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    TuiSectionHeader(text = title, color = if (expanded) tokens.accent else tokens.textMuted)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.textPrimary
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse $title" else "Expand $title",
                    tint = tokens.accent
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    content()
                }
            }
        }
    }
}

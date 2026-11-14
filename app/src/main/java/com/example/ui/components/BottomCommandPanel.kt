package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.IconPackManager
import com.example.model.AppInfo
import com.example.model.BottomBarStyle
import com.example.model.LauncherRoute
import com.example.model.LauncherSettings
import com.example.ui.theme.LocalLauncherTokens
import com.example.viewmodel.MathEvaluator

@Composable
fun BottomCommandPanel(
    settings: LauncherSettings,
    activeRoute: LauncherRoute,
    isDrawerOpen: Boolean,
    searchQuery: String,
    pinnedApps: List<AppInfo>,
    searchResults: List<AppInfo>,
    iconPackManager: IconPackManager,
    onSearchQueryChange: (String) -> Unit,
    onSubmitSearch: () -> Unit,
    onNavigate: (LauncherRoute) -> Unit,
    onToggleDrawer: () -> Unit,
    onLaunchApp: (AppInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalLauncherTokens.current
    val corner = when (settings.bottomBarStyle) {
        BottomBarStyle.TUI_PANEL -> tokens.cardCornerRadius
        BottomBarStyle.FLOATING_PILL -> 18.dp
        BottomBarStyle.MINIMAL_DOCK -> 0.dp
    }
    val shape = RoundedCornerShape(corner)
    val outerPadding = when (settings.bottomBarStyle) {
        BottomBarStyle.TUI_PANEL -> 0.dp
        BottomBarStyle.FLOATING_PILL -> 12.dp
        BottomBarStyle.MINIMAL_DOCK -> 4.dp
    }
    val borderWidth = if (settings.bottomBarStyle == BottomBarStyle.MINIMAL_DOCK) 0.dp else tokens.borderWidth
    val panelBg = if (settings.bottomBarStyle == BottomBarStyle.MINIMAL_DOCK) {
        tokens.background.copy(alpha = 0.72f)
    } else {
        tokens.surface
    }

    val mathAnswer = if (settings.enableSearchCalculator && searchQuery.isNotBlank()) {
        MathEvaluator.evaluateIfExpression(searchQuery)
    } else {
        null
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = outerPadding, vertical = if (outerPadding > 0.dp) 6.dp else 0.dp)
            .clip(shape)
            .background(panelBg)
            .then(
                if (borderWidth > 0.dp) {
                    Modifier.border(borderWidth, tokens.border, shape)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        // Inline Search Results Strip (when user types in bottom search bar on Home)
        AnimatedVisibility(visible = searchQuery.isNotBlank() && !isDrawerOpen) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                if (mathAnswer != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(tokens.cardCornerRadius))
                            .background(tokens.pillBg)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "= $mathAnswer",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = tokens.pillText
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                }
                if (searchResults.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(searchResults.take(10), key = { it.componentKey }) { app ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .defaultMinSize(minWidth = 60.dp, minHeight = 48.dp)
                                    .clip(RoundedCornerShape(tokens.cardCornerRadius))
                                    .clickable { onLaunchApp(app) }
                                    .padding(6.dp)
                            ) {
                                AppIconView(
                                    app = app,
                                    settings = settings,
                                    iconPackManager = iconPackManager,
                                    sizeOverride = 38.dp
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = app.displayLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = tokens.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.width(64.dp)
                                )
                            }
                        }
                    }
                } else if (mathAnswer == null && !searchQuery.trim().startsWith(">")) {
                    Text(
                        text = "no matching apps for \"$searchQuery\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.textMuted,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Optional Pinned Dock Apps Row (Centered)
        if (pinnedApps.isNotEmpty() && searchQuery.isBlank()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                pinnedApps.forEach { app ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .clip(RoundedCornerShape(tokens.cardCornerRadius))
                            .clickable { onLaunchApp(app) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        AppIconView(
                            app = app,
                            settings = settings,
                            iconPackManager = iconPackManager,
                            sizeOverride = 40.dp
                        )
                        if (settings.showIconLabels) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = app.displayLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = tokens.textMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Integrated Search Bar + App Drawer Icon
        if (settings.showSearchInBottomBar) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(tokens.cardCornerRadius))
                        .background(tokens.background.copy(alpha = 0.8f))
                        .border(1.dp, tokens.border, RoundedCornerShape(tokens.cardCornerRadius))
                        .padding(horizontal = 10.dp)
                ) {
                    NerdGlyphIcon(
                        glyph = NerdGlyph.PROMPT,
                        tint = tokens.accent,
                        size = 14.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        contentAlignment = Alignment.CenterStart,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "~/search apps, =math, >cmd",
                                style = MaterialTheme.typography.bodyMedium,
                                color = tokens.textMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = tokens.textPrimary
                            ),
                            cursorBrush = SolidColor(tokens.accent),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                            keyboardActions = KeyboardActions(onGo = { onSubmitSearch() }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("bottom_search_input")
                        )
                    }
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { onSearchQueryChange("") },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = tokens.textMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // Centered Primary Navigation Bar with Nerd Font Glyphs + Text
        if (settings.showNavShortcuts) {
            if (settings.showSearchInBottomBar) {
                Spacer(Modifier.height(6.dp))
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bottom_nav_center_bar")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    TuiPillTab(
                        label = "~/home",
                        glyph = NerdGlyph.HOME,
                        selected = activeRoute == LauncherRoute.HOME && !isDrawerOpen,
                        onClick = { onNavigate(LauncherRoute.HOME) },
                        modifier = Modifier.testTag("nav_home_tab")
                    )
                    TuiPillTab(
                        label = "~/apps",
                        glyph = NerdGlyph.APPS,
                        selected = isDrawerOpen,
                        onClick = onToggleDrawer,
                        modifier = Modifier.testTag("nav_apps_tab")
                    )
                    TuiPillTab(
                        label = "~/widgets",
                        glyph = NerdGlyph.WIDGETS,
                        selected = activeRoute == LauncherRoute.WIDGETS && !isDrawerOpen,
                        onClick = { onNavigate(LauncherRoute.WIDGETS) },
                        modifier = Modifier.testTag("nav_widgets_tab")
                    )
                    TuiPillTab(
                        label = "~/settings",
                        glyph = NerdGlyph.SETTINGS,
                        selected = activeRoute == LauncherRoute.SETTINGS && !isDrawerOpen,
                        onClick = { onNavigate(LauncherRoute.SETTINGS) },
                        modifier = Modifier.testTag("nav_settings_tab")
                    )
                }
            }
        }
    }
}

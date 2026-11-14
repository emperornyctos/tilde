package com.example.ui.screens

import android.appwidget.AppWidgetProviderInfo
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.BuiltInWidgetType
import com.example.model.SystemWidgetGroup
import com.example.ui.components.TuiCard
import com.example.ui.components.TuiCliLink
import com.example.ui.components.TuiSectionHeader
import com.example.ui.components.toImageBitmap
import com.example.ui.theme.LocalLauncherTokens

@Composable
fun WidgetHubScreen(
    systemWidgetGroups: List<SystemWidgetGroup>,
    searchQuery: String,
    onAddBuiltInWidget: (BuiltInWidgetType) -> Unit,
    onSelectSystemWidget: (AppWidgetProviderInfo) -> Unit,
    onBackHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBackHome() }

    val context = LocalContext.current
    val pm = context.packageManager
    val tokens = LocalLauncherTokens.current

    val filteredBuiltIn = remember(searchQuery) {
        if (searchQuery.isBlank()) BuiltInWidgetType.entries
        else {
            val q = searchQuery.trim().lowercase()
            BuiltInWidgetType.entries.filter {
                it.title.lowercase().contains(q) || it.description.lowercase().contains(q)
            }
        }
    }

    val filteredGroups = remember(systemWidgetGroups, searchQuery) {
        if (searchQuery.isBlank()) systemWidgetGroups
        else {
            val q = searchQuery.trim().lowercase()
            systemWidgetGroups.mapNotNull { group ->
                val matchesApp = group.appName.lowercase().contains(q)
                val widgets = if (matchesApp) group.widgets else group.widgets.filter {
                    it.loadLabel(pm).lowercase().contains(q)
                }
                if (widgets.isEmpty()) null else group.copy(widgets = widgets)
            }
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxSize()
            .testTag("widget_hub_screen")
    ) {
        item {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    TuiSectionHeader(text = "~/widgets")
                    Text(
                        text = "Built-in TUI blocks & Android AppWidgets",
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.textMuted
                    )
                }
                TuiCliLink(label = "back", onClick = onBackHome)
            }
        }

        item {
            TuiSectionHeader(text = "01 · Built-in terminal widgets")
        }

        items(filteredBuiltIn) { widget ->
            TuiCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAddBuiltInWidget(widget) },
                contentPadding = PaddingValues(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(tokens.cardCornerRadius))
                            .background(tokens.pillBg)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Widgets,
                            contentDescription = widget.title,
                            tint = tokens.pillText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${widget.title} · ${widget.defaultColSpan}x${widget.defaultRowSpan}",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = tokens.textPrimary
                        )
                        Text(
                            text = widget.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = tokens.textMuted
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add ${widget.title}",
                        tint = tokens.accent
                    )
                }
            }
        }

        item {
            Spacer(Modifier.height(6.dp))
            TuiSectionHeader(text = "02 · Android system widgets (${filteredGroups.sumOf { it.widgets.size }})")
        }

        if (filteredGroups.isEmpty()) {
            item {
                TuiCard(contentPadding = PaddingValues(14.dp)) {
                    Text(
                        text = "No external system AppWidgets available on this device/profile.",
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.textMuted
                    )
                }
            }
        } else {
            items(filteredGroups, key = { it.packageName }) { group ->
                TuiCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = group.appName,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = tokens.accent
                        )
                        group.widgets.forEach { info ->
                            val label = remember(info) { info.loadLabel(pm) }
                            val preview = remember(info) {
                                runCatching { info.loadPreviewImage(context, 0) }.getOrNull()
                                    ?: runCatching { info.loadIcon(context, 0) }.getOrNull()
                            }
                            val bmp = remember(preview) { preview?.toImageBitmap() }
                            val spanW = (info.minWidth / 72).coerceAtLeast(1)
                            val spanH = (info.minHeight / 72).coerceAtLeast(1)

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(tokens.cardCornerRadius))
                                    .background(tokens.background.copy(alpha = 0.6f))
                                    .border(
                                        0.75.dp,
                                        tokens.border,
                                        RoundedCornerShape(tokens.cardCornerRadius)
                                    )
                                    .clickable { onSelectSystemWidget(info) }
                                    .padding(10.dp)
                            ) {
                                if (bmp != null) {
                                    Image(
                                        bitmap = bmp,
                                        contentDescription = label,
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = tokens.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Recommended ${spanW}x${spanH} cells",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = tokens.textMuted
                                    )
                                }
                                TuiCliLink(label = "+ add", onClick = { onSelectSystemWidget(info) })
                            }
                        }
                    }
                }
            }
        }
    }
}

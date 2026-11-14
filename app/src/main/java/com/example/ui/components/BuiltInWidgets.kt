package com.example.ui.components

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.IconPackManager
import com.example.data.QuickNoteEntity
import com.example.model.AppInfo
import com.example.model.BuiltInWidgetType
import com.example.model.LauncherSettings
import com.example.ui.theme.LocalLauncherTokens
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BuiltInWidgetRenderer(
    widgetType: BuiltInWidgetType,
    widgetConfig: String? = null,
    settings: LauncherSettings,
    allApps: List<AppInfo>,
    quickNotes: List<QuickNoteEntity>,
    mediaState: com.example.data.MediaPlaybackState,
    iconPackManager: IconPackManager,
    onLaunchApp: (AppInfo) -> Unit,
    onEditProfile: (String) -> Unit,
    onEditWelcomeQuote: () -> Unit,
    onPickWidgetPhoto: () -> Unit,
    onMediaTogglePlayPause: () -> Unit,
    onMediaSkipNext: () -> Unit,
    onMediaSkipPrevious: () -> Unit,
    onOpenMediaPlayer: () -> Unit,
    onGrantMediaAccess: () -> Unit,
    onAddQuickNote: (String) -> Unit,
    onToggleQuickNote: (QuickNoteEntity) -> Unit,
    onDeleteQuickNote: (Long) -> Unit,
    isInteractive: Boolean = true,
    modifier: Modifier = Modifier
) {
    when (widgetType) {
        BuiltInWidgetType.PROFILE_CARD -> ProfileCardWidget(
            settings = settings,
            onEditField = { if (isInteractive) onEditProfile(it) },
            isInteractive = isInteractive,
            modifier = modifier
        )
        BuiltInWidgetType.WELCOME_QUOTE -> WelcomeQuoteWidget(
            settings = settings,
            onEdit = { if (isInteractive) onEditWelcomeQuote() },
            isInteractive = isInteractive,
            modifier = modifier
        )
        BuiltInWidgetType.MOST_USED_APPS -> MostUsedAppsWidget(
            settings = settings,
            allApps = allApps,
            iconPackManager = iconPackManager,
            onLaunchApp = { if (isInteractive) onLaunchApp(it) },
            modifier = modifier
        )
        BuiltInWidgetType.TERMINAL_CLOCK -> TerminalClockWidget(
            settings = settings,
            modifier = modifier
        )
        BuiltInWidgetType.SYSTEM_TELEMETRY -> SystemTelemetryWidget(
            modifier = modifier
        )
        BuiltInWidgetType.QUICK_NOTES -> QuickNotesWidget(
            notes = quickNotes,
            onAddNote = onAddQuickNote,
            onToggleNote = onToggleQuickNote,
            onDeleteNote = onDeleteQuickNote,
            modifier = modifier
        )
        BuiltInWidgetType.FOCUS_TIMER -> FocusTimerWidget(
            modifier = modifier
        )
        BuiltInWidgetType.PHOTO_FRAME, BuiltInWidgetType.PHOTO_FRAME_WIDE -> PhotoFrameWidget(
            widgetConfig = widgetConfig,
            isInteractive = isInteractive,
            onPickPhoto = onPickWidgetPhoto,
            modifier = modifier
        )
        BuiltInWidgetType.ANALOG_CLOCK -> AnalogClockWidget(
            modifier = modifier
        )
        BuiltInWidgetType.MINI_CALENDAR -> MiniCalendarWidget(
            modifier = modifier
        )
        BuiltInWidgetType.NOW_PLAYING -> NowPlayingWidget(
            mediaState = mediaState,
            isInteractive = isInteractive,
            onTogglePlayPause = onMediaTogglePlayPause,
            onSkipNext = onMediaSkipNext,
            onSkipPrevious = onMediaSkipPrevious,
            onOpenPlayer = onOpenMediaPlayer,
            onGrantMediaAccess = onGrantMediaAccess,
            modifier = modifier
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileCardWidget(
    settings: LauncherSettings,
    onEditField: (String) -> Unit,
    isInteractive: Boolean = true,
    modifier: Modifier = Modifier
) {
    val tokens = LocalLauncherTokens.current
    TuiCard(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(10.dp)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val boxW = maxWidth
            val boxH = maxHeight
            val showBanner = boxH >= 185.dp && boxW >= 180.dp
            val showBio = boxH >= 135.dp && boxW >= 150.dp
            val showActionRow = boxH >= 220.dp && boxW >= 220.dp
            val showSetPfp = boxW >= 220.dp

            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TuiSectionHeader(text = "Profile")
                    if (isInteractive && boxW >= 140.dp) {
                        TuiCliLink(label = "edit", onClick = { onEditField("all") })
                    }
                }

                if (showBanner) {
                    Spacer(Modifier.height(6.dp))
                    val bannerFile = settings.profileBannerPath.takeIf { it.isNotBlank() }?.let { File(it) }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (boxH >= 260.dp) 62.dp else 44.dp)
                            .clip(RoundedCornerShape(tokens.cardCornerRadius))
                            .border(1.dp, tokens.border, RoundedCornerShape(tokens.cardCornerRadius))
                            .then(if (isInteractive) Modifier.clickable { onEditField("banner") } else Modifier)
                    ) {
                        if (bannerFile != null && bannerFile.exists()) {
                            AsyncImage(
                                model = bannerFile,
                                contentDescription = "Profile banner",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawRect(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            tokens.pillBg,
                                            tokens.surface,
                                            tokens.accent.copy(alpha = 0.25f)
                                        )
                                    )
                                )
                                var x = 0f
                                while (x < size.width) {
                                    drawLine(
                                        color = tokens.accent.copy(alpha = 0.14f),
                                        start = Offset(x, 0f),
                                        end = Offset(x + 24f, size.height),
                                        strokeWidth = 1f
                                    )
                                    x += 18f
                                }
                            }
                            Text(
                                text = "~/workspace/banner",
                                style = MaterialTheme.typography.labelSmall,
                                color = tokens.pillText.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .padding(horizontal = 10.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))

                // Avatar + Name + [set pfp]
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val pfpFile = settings.profileAvatarPath.takeIf { it.isNotBlank() }?.let { File(it) }
                    val avatarSize = if (boxH < 100.dp) 32.dp else 42.dp
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(avatarSize)
                            .clip(RoundedCornerShape(tokens.cardCornerRadius))
                            .background(tokens.pillBg)
                            .border(1.dp, tokens.border, RoundedCornerShape(tokens.cardCornerRadius))
                            .then(if (isInteractive) Modifier.clickable { onEditField("pfp") } else Modifier)
                    ) {
                        if (pfpFile != null && pfpFile.exists()) {
                            AsyncImage(
                                model = pfpFile,
                                contentDescription = "Profile avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Terminal,
                                contentDescription = "Avatar placeholder",
                                tint = tokens.pillText,
                                modifier = Modifier.size(avatarSize * 0.55f)
                            )
                        }
                    }
                    if (boxW >= 115.dp) {
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = settings.profileDisplayName,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontStyle = if (tokens.italicizeHeaders) FontStyle.Italic else FontStyle.Normal
                                ),
                                color = tokens.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "(${settings.profilePronouns}) · ${settings.profileSubtitle}",
                                style = MaterialTheme.typography.bodySmall,
                                color = tokens.textMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (showSetPfp && isInteractive) {
                        TuiCliLink(label = "set pfp", onClick = { onEditField("pfp") })
                    }
                }

                if (showBio) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = settings.profileBio,
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.textPrimary.copy(alpha = 0.9f),
                        maxLines = if (boxH >= 260.dp) 3 else 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }

                if (showActionRow && isInteractive) {
                    Spacer(Modifier.height(4.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        TuiCliLink(label = "banner", onClick = { onEditField("banner") })
                        TuiCliLink(label = "pronouns", onClick = { onEditField("all") })
                        TuiCliLink(label = "bio", onClick = { onEditField("all") })
                    }
                }
            }
        }
    }
}

@Composable
fun WelcomeQuoteWidget(
    settings: LauncherSettings,
    onEdit: () -> Unit,
    isInteractive: Boolean = true,
    modifier: Modifier = Modifier
) {
    val tokens = LocalLauncherTokens.current
    TuiCard(
        modifier = modifier
            .fillMaxSize()
            .then(if (isInteractive) Modifier.clickable(onClick = onEdit) else Modifier),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val boxW = maxWidth
            val boxH = maxHeight
            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                if (boxH >= 60.dp) {
                    TuiSectionHeader(text = if (boxW >= 180.dp) "Welcome — tap to edit" else "Welcome")
                    Spacer(Modifier.height(3.dp))
                }
                Text(
                    text = settings.profileWelcomeQuote,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontStyle = if (tokens.italicizeHeaders) FontStyle.Italic else FontStyle.Normal
                    ),
                    color = tokens.textPrimary,
                    maxLines = if (boxH >= 110.dp) 4 else 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun MostUsedAppsWidget(
    settings: LauncherSettings,
    allApps: List<AppInfo>,
    iconPackManager: IconPackManager,
    onLaunchApp: (AppInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalLauncherTokens.current
    val visibleApps = remember(allApps) {
        val nonHidden = allApps.filter { !it.isHidden }
        val sorted = nonHidden.sortedWith(
            compareByDescending<AppInfo> { it.launchCount }.thenBy { it.displayLabel.lowercase() }
        )
        sorted.take(5)
    }

    TuiCard(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TuiSectionHeader(text = "Most Launched")
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${allApps.count { !it.isHidden }} apps in library",
                style = MaterialTheme.typography.bodySmall,
                color = tokens.textMuted
            )
            Spacer(Modifier.height(6.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(visibleApps, key = { it.componentKey }) { app ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(tokens.cardCornerRadius))
                            .clickable { onLaunchApp(app) }
                            .padding(vertical = 2.dp, horizontal = 2.dp)
                    ) {
                        AppIconView(
                            app = app,
                            settings = settings,
                            iconPackManager = iconPackManager,
                            sizeOverride = 34.dp
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = app.displayLabel,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontStyle = if (tokens.italicizeHeaders) FontStyle.Italic else FontStyle.Normal
                                ),
                                color = tokens.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (app.launchCount > 0) "${app.launchCount} launches · ${app.packageName}"
                                else app.packageName,
                                style = MaterialTheme.typography.bodySmall,
                                color = tokens.textMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TerminalClockWidget(
    settings: LauncherSettings,
    modifier: Modifier = Modifier
) {
    val tokens = LocalLauncherTokens.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(settings.clockShowSeconds) {
        while (true) {
            now = System.currentTimeMillis()
            delay(if (settings.clockShowSeconds) 1000L else 10_000L)
        }
    }

    val timeFormat = remember(settings.clock24Hour, settings.clockShowSeconds) {
        val pattern = when {
            settings.clock24Hour && settings.clockShowSeconds -> "HH:mm:ss"
            settings.clock24Hour -> "HH:mm"
            settings.clockShowSeconds -> "hh:mm:ss a"
            else -> "hh:mm a"
        }
        SimpleDateFormat(pattern, Locale.getDefault())
    }
    val dateFormat = remember { SimpleDateFormat("EEE, MMM dd yyyy", Locale.getDefault()) }

    TuiCard(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize()
        ) {
            Column {
                TuiSectionHeader(text = "Clock")
                Spacer(Modifier.height(2.dp))
                Text(
                    text = dateFormat.format(Date(now)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = tokens.textMuted
                )
            }
            Text(
                text = timeFormat.format(Date(now)),
                style = MaterialTheme.typography.displayLarge,
                color = tokens.accent,
                maxLines = 1
            )
        }
    }
}

@Composable
fun SystemTelemetryWidget(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tokens = LocalLauncherTokens.current
    var tick by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(5000L)
            tick++
        }
    }

    val telemetry = remember(tick) {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val batteryPct = if (level >= 0 && scale > 0) (level * 100) / scale else 100
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am?.getMemoryInfo(memInfo)
        val totalRamGb = memInfo.totalMem / (1024f * 1024f * 1024f)
        val availRamGb = memInfo.availMem / (1024f * 1024f * 1024f)
        val usedRamPct = if (memInfo.totalMem > 0) {
            1f - (memInfo.availMem.toFloat() / memInfo.totalMem.toFloat())
        } else 0.4f

        val stat = StatFs(Environment.getDataDirectory().path)
        val totalBytes = stat.totalBytes.toFloat()
        val freeBytes = stat.availableBytes.toFloat()
        val usedStoragePct = if (totalBytes > 0f) 1f - (freeBytes / totalBytes) else 0.5f
        val freeGb = freeBytes / (1024f * 1024f * 1024f)

        Triple(
            Pair(batteryPct, charging),
            Pair(usedRamPct, "${String.format(Locale.US, "%.1f", totalRamGb - availRamGb)}/${String.format(Locale.US, "%.1f", totalRamGb)} GB"),
            Pair(usedStoragePct, "${String.format(Locale.US, "%.1f", freeGb)} GB free")
        )
    }

    TuiCard(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize()
        ) {
            TuiSectionHeader(text = "System Telemetry")
            TelemetryBar(
                label = if (telemetry.first.second) "Battery [charging]" else "Battery",
                valueLabel = "${telemetry.first.first}%",
                fraction = telemetry.first.first / 100f
            )
            TelemetryBar(
                label = "Memory",
                valueLabel = telemetry.second.second,
                fraction = telemetry.second.first
            )
            TelemetryBar(
                label = "Storage",
                valueLabel = telemetry.third.second,
                fraction = telemetry.third.first
            )
        }
    }
}

@Composable
private fun TelemetryBar(label: String, valueLabel: String, fraction: Float) {
    val tokens = LocalLauncherTokens.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = tokens.textMuted)
            Text(valueLabel, style = MaterialTheme.typography.labelSmall, color = tokens.textPrimary)
        }
        Spacer(Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(tokens.background, RoundedCornerShape(tokens.cardCornerRadius))
                .border(0.5.dp, tokens.border, RoundedCornerShape(tokens.cardCornerRadius))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0.02f, 1f))
                    .height(6.dp)
                    .background(tokens.accent, RoundedCornerShape(tokens.cardCornerRadius))
            )
        }
    }
}

@Composable
fun QuickNotesWidget(
    notes: List<QuickNoteEntity>,
    onAddNote: (String) -> Unit,
    onToggleNote: (QuickNoteEntity) -> Unit,
    onDeleteNote: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalLauncherTokens.current
    var draft by remember { mutableStateOf("") }

    TuiCard(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TuiSectionHeader(text = "Quick Notes")
            Spacer(Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, tokens.border, RoundedCornerShape(tokens.cardCornerRadius))
                    .background(tokens.background.copy(alpha = 0.7f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    if (draft.isEmpty()) {
                        Text(
                            text = "> add note...",
                            style = MaterialTheme.typography.bodySmall,
                            color = tokens.textMuted
                        )
                    }
                    BasicTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall.copy(color = tokens.textPrimary),
                        cursorBrush = SolidColor(tokens.accent),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                IconButton(
                    onClick = {
                        if (draft.isNotBlank()) {
                            onAddNote(draft)
                            draft = ""
                        }
                    },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add quick note",
                        tint = tokens.accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(notes, key = { it.id }) { note ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleNote(note) }
                            .padding(vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = if (note.isDone) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                            contentDescription = "Toggle note",
                            tint = if (note.isDone) tokens.accent else tokens.textMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = note.text,
                            style = MaterialTheme.typography.bodySmall.copy(
                                textDecoration = if (note.isDone) TextDecoration.LineThrough else TextDecoration.None
                            ),
                            color = if (note.isDone) tokens.textMuted else tokens.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { onDeleteNote(note.id) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Delete note",
                                tint = tokens.textMuted,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FocusTimerWidget(
    modifier: Modifier = Modifier
) {
    val tokens = LocalLauncherTokens.current
    var remainingSeconds by remember { mutableIntStateOf(25 * 60) }
    var running by remember { mutableStateOf(false) }

    LaunchedEffect(running) {
        while (running && remainingSeconds > 0) {
            delay(1000L)
            remainingSeconds--
            if (remainingSeconds == 0) running = false
        }
    }

    val mm = remainingSeconds / 60
    val ss = remainingSeconds % 60
    val formatted = String.format(Locale.US, "%02d:%02d", mm, ss)

    TuiCard(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize()
        ) {
            TuiSectionHeader(text = "Focus Timer")
            Text(
                text = formatted,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = if (running) tokens.accent else tokens.textPrimary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TuiCliLink(
                    label = if (running) "pause" else "start",
                    onClick = { running = !running }
                )
                TuiCliLink(
                    label = "reset",
                    onClick = {
                        running = false
                        remainingSeconds = 25 * 60
                    }
                )
            }
        }
    }
}

package com.example.model

import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.graphics.drawable.Drawable

data class AppInfo(
    val packageName: String,
    val activityName: String,
    val label: String,
    val customLabel: String? = null,
    val customIconUri: String? = null,
    val customPackPackage: String? = null,
    val customPackDrawable: String? = null,
    val isHidden: Boolean = false,
    val launchCount: Int = 0,
    val installTime: Long = 0L,
    val systemIcon: Drawable? = null
) {
    val displayLabel: String get() = customLabel?.takeIf { it.isNotBlank() } ?: label
    val componentKey: String get() = "$packageName/$activityName"
    val componentName: ComponentName get() = ComponentName(packageName, activityName)
}

enum class HomeItemType {
    APP,
    FOLDER,
    BUILT_IN_WIDGET,
    SYSTEM_WIDGET
}

enum class BuiltInWidgetType(
    val title: String,
    val description: String,
    val defaultColSpan: Int,
    val defaultRowSpan: Int
) {
    PROFILE_CARD(
        title = "TUI Profile Card",
        description = "Editable banner, avatar, handle, pronouns & bio",
        defaultColSpan = 4,
        defaultRowSpan = 3
    ),
    WELCOME_QUOTE(
        title = "Status Banner",
        description = "Minimalist editable welcome / status quote block",
        defaultColSpan = 4,
        defaultRowSpan = 1
    ),
    MOST_USED_APPS(
        title = "Most Launched",
        description = "Top frequently opened apps list with launch counts",
        defaultColSpan = 4,
        defaultRowSpan = 2
    ),
    TERMINAL_CLOCK(
        title = "Terminal Clock",
        description = "Live monospace clock, date & day readout",
        defaultColSpan = 4,
        defaultRowSpan = 1
    ),
    SYSTEM_TELEMETRY(
        title = "System Telemetry",
        description = "Live battery, RAM & storage monitor bars",
        defaultColSpan = 4,
        defaultRowSpan = 2
    ),
    QUICK_NOTES(
        title = "Quick Notes",
        description = "Interactive scratchpad notes right on home grid",
        defaultColSpan = 4,
        defaultRowSpan = 2
    ),
    FOCUS_TIMER(
        title = "Focus Timer",
        description = "Interactive Pomodoro & focus countdown timer",
        defaultColSpan = 2,
        defaultRowSpan = 2
    ),
    PHOTO_FRAME(
        title = "Photo Frame",
        description = "Custom cropped photo or animated GIF widget (2x2)",
        defaultColSpan = 2,
        defaultRowSpan = 2
    ),
    PHOTO_FRAME_WIDE(
        title = "Photo Frame Wide",
        description = "Wide 4x2 photo or animated GIF banner widget",
        defaultColSpan = 4,
        defaultRowSpan = 2
    ),
    ANALOG_CLOCK(
        title = "Analog Clock",
        description = "Real mechanical dial clock with ticking second hand",
        defaultColSpan = 2,
        defaultRowSpan = 2
    ),
    MINI_CALENDAR(
        title = "Month Calendar",
        description = "Compact monthly monospace calendar with today highlighted",
        defaultColSpan = 2,
        defaultRowSpan = 2
    ),
    NOW_PLAYING(
        title = "Now Playing",
        description = "Live 4x2 controller & track art for any active music player",
        defaultColSpan = 4,
        defaultRowSpan = 2
    )
}

data class HomeGridItem(
    val id: Long = 0L,
    val page: Int = 0,
    val col: Int = 0,
    val row: Int = 0,
    val colSpan: Int = 1,
    val rowSpan: Int = 1,
    val type: HomeItemType,
    val packageName: String? = null,
    val activityName: String? = null,
    val folderTitle: String? = null,
    val folderAppKeys: List<String> = emptyList(),
    val builtInWidgetType: BuiltInWidgetType? = null,
    val appWidgetId: Int = -1,
    val widgetConfig: String? = null
)

data class IconPackInfo(
    val id: String,
    val packageName: String?,
    val label: String,
    val description: String,
    val isBuiltIn: Boolean,
    val builtInStyle: BuiltInIconStyle? = null
)

data class SystemWidgetGroup(
    val appName: String,
    val packageName: String,
    val widgets: List<AppWidgetProviderInfo>
)

enum class LauncherRoute {
    HOME,
    WIDGETS,
    SETTINGS
}

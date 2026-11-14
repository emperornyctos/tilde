package com.example.model

import com.squareup.moshi.JsonClass

enum class ThemePreset(
    val label: String,
    val accentHex: Long,
    val bgHex: Long,
    val surfaceHex: Long,
    val borderHex: Long,
    val textHex: Long,
    val mutedHex: Long,
    val pillBgHex: Long,
    val pillTextHex: Long,
    val isLight: Boolean = false
) {
    TERMINAL_VIOLET(
        label = "Terminal Violet",
        accentHex = 0xFF9D85C4,
        bgHex = 0xFF0D0D0F,
        surfaceHex = 0xFF151518,
        borderHex = 0xFF2B2B31,
        textHex = 0xFFECEBF0,
        mutedHex = 0xFF8C8A96,
        pillBgHex = 0xFF3B2E4D,
        pillTextHex = 0xFFCDBCE8
    ),
    ROSE_PINE(
        label = "Rosé Pine",
        accentHex = 0xFFEBBCBA,
        bgHex = 0xFF191724,
        surfaceHex = 0xFF1F1D2E,
        borderHex = 0xFF403D52,
        textHex = 0xFFE0DEF4,
        mutedHex = 0xFF908CAA,
        pillBgHex = 0xFF26233A,
        pillTextHex = 0xFFEBBCBA
    ),
    ROSE_PINE_MOON(
        label = "Rosé Pine Moon",
        accentHex = 0xFFC4A7E7,
        bgHex = 0xFF232136,
        surfaceHex = 0xFF2A273F,
        borderHex = 0xFF44415A,
        textHex = 0xFFE0DEF4,
        mutedHex = 0xFF908CAA,
        pillBgHex = 0xFF393552,
        pillTextHex = 0xFFEA9A97
    ),
    ROSE_PINE_DAWN(
        label = "Rosé Pine Dawn",
        accentHex = 0xFF907AA9,
        bgHex = 0xFFFAF4ED,
        surfaceHex = 0xFFFFFAF3,
        borderHex = 0xFFDFDAD9,
        textHex = 0xFF575279,
        mutedHex = 0xFF9893A5,
        pillBgHex = 0xFFF2E9E1,
        pillTextHex = 0xFFB4637A,
        isLight = true
    ),
    CATPPUCCIN_MOCHA(
        label = "Catppuccin Mocha",
        accentHex = 0xFFCBA6F7,
        bgHex = 0xFF1E1E2E,
        surfaceHex = 0xFF252538,
        borderHex = 0xFF45475A,
        textHex = 0xFFCDD6F4,
        mutedHex = 0xFFA6ADC8,
        pillBgHex = 0xFF313244,
        pillTextHex = 0xFFF5C2E7
    ),
    CATPPUCCIN_MACCHIATO(
        label = "Catppuccin Macchiato",
        accentHex = 0xFF8AADF4,
        bgHex = 0xFF24273A,
        surfaceHex = 0xFF2B2F44,
        borderHex = 0xFF494D64,
        textHex = 0xFFCAD3F5,
        mutedHex = 0xFFA5ADCB,
        pillBgHex = 0xFF363A4F,
        pillTextHex = 0xFFB7BDF8
    ),
    CATPPUCCIN_FRAPPE(
        label = "Catppuccin Frappé",
        accentHex = 0xFFF4B8E4,
        bgHex = 0xFF303446,
        surfaceHex = 0xFF383C52,
        borderHex = 0xFF51576D,
        textHex = 0xFFC6D0F5,
        mutedHex = 0xFFA5ADCE,
        pillBgHex = 0xFF414559,
        pillTextHex = 0xFFCA9EE6
    ),
    CATPPUCCIN_LATTE(
        label = "Catppuccin Latte",
        accentHex = 0xFF8839EF,
        bgHex = 0xFFEFF1F5,
        surfaceHex = 0xFFFFFFFF,
        borderHex = 0xFFCCD0DA,
        textHex = 0xFF4C4F69,
        mutedHex = 0xFF6C6F85,
        pillBgHex = 0xFFDCE0E8,
        pillTextHex = 0xFF6629C7,
        isLight = true
    ),
    GRUVBOX_DARK(
        label = "Gruvbox Dark",
        accentHex = 0xFFFABD2F,
        bgHex = 0xFF282828,
        surfaceHex = 0xFF32302F,
        borderHex = 0xFF504945,
        textHex = 0xFFEBDBB2,
        mutedHex = 0xFFA89984,
        pillBgHex = 0xFF3C3836,
        pillTextHex = 0xFFFE8019
    ),
    GRUVBOX_HARD(
        label = "Gruvbox Dark Hard",
        accentHex = 0xFFB8BB26,
        bgHex = 0xFF1D2021,
        surfaceHex = 0xFF282828,
        borderHex = 0xFF504945,
        textHex = 0xFFFBF1C7,
        mutedHex = 0xFF928374,
        pillBgHex = 0xFF32302F,
        pillTextHex = 0xFFB8BB26
    ),
    GRUVBOX_LIGHT(
        label = "Gruvbox Light",
        accentHex = 0xFFAF3A03,
        bgHex = 0xFFFBF1C7,
        surfaceHex = 0xFFF2E5BC,
        borderHex = 0xFFD5C4A1,
        textHex = 0xFF3C3836,
        mutedHex = 0xFF7C6F64,
        pillBgHex = 0xFFEBDBB2,
        pillTextHex = 0xFF9D0006,
        isLight = true
    ),
    TOKYO_NIGHT(
        label = "Tokyo Night",
        accentHex = 0xFF7AA2F7,
        bgHex = 0xFF13151F,
        surfaceHex = 0xFF1A1D2B,
        borderHex = 0xFF2E344D,
        textHex = 0xFFC0CAF5,
        mutedHex = 0xFF7882A8,
        pillBgHex = 0xFF273459,
        pillTextHex = 0xFFA9C3FF
    ),
    TOKYO_NIGHT_STORM(
        label = "Tokyo Night Storm",
        accentHex = 0xFF7DCFFF,
        bgHex = 0xFF24283B,
        surfaceHex = 0xFF292E42,
        borderHex = 0xFF3B4261,
        textHex = 0xFFC0CAF5,
        mutedHex = 0xFF737AA2,
        pillBgHex = 0xFF2E354D,
        pillTextHex = 0xFFBB9AF7
    ),
    TOKYO_NIGHT_DAY(
        label = "Tokyo Night Day",
        accentHex = 0xFF2E7DE9,
        bgHex = 0xFFE1E2E7,
        surfaceHex = 0xFFE9E9ED,
        borderHex = 0xFFC4C8DA,
        textHex = 0xFF2A3045,
        mutedHex = 0xFF6172B0,
        pillBgHex = 0xFFD0D5E3,
        pillTextHex = 0xFF1D5BC2,
        isLight = true
    ),
    NORD(
        label = "Nord",
        accentHex = 0xFF88C0D0,
        bgHex = 0xFF2E3440,
        surfaceHex = 0xFF3B4252,
        borderHex = 0xFF4C566A,
        textHex = 0xFFECEFF4,
        mutedHex = 0xFFA5B1C7,
        pillBgHex = 0xFF434C5E,
        pillTextHex = 0xFF8FBCBB
    ),
    DRACULA(
        label = "Dracula",
        accentHex = 0xFFBD93F9,
        bgHex = 0xFF282A36,
        surfaceHex = 0xFF303341,
        borderHex = 0xFF44475A,
        textHex = 0xFFF8F8F2,
        mutedHex = 0xFF8E9BBF,
        pillBgHex = 0xFF44475A,
        pillTextHex = 0xFFFF79C6
    ),
    SOLARIZED_DARK(
        label = "Solarized Dark",
        accentHex = 0xFF2AA198,
        bgHex = 0xFF002B36,
        surfaceHex = 0xFF073642,
        borderHex = 0xFF264B56,
        textHex = 0xFFEEE8D5,
        mutedHex = 0xFF839496,
        pillBgHex = 0xFF0D4452,
        pillTextHex = 0xFF2AA198
    ),
    SOLARIZED_LIGHT(
        label = "Solarized Light",
        accentHex = 0xFF268BD2,
        bgHex = 0xFFFDF6E3,
        surfaceHex = 0xFFEEE8D5,
        borderHex = 0xFFD3CCB8,
        textHex = 0xFF4E6268,
        mutedHex = 0xFF839496,
        pillBgHex = 0xFFE3DCC5,
        pillTextHex = 0xFF1F6FA8,
        isLight = true
    ),
    ONE_DARK(
        label = "One Dark",
        accentHex = 0xFF61AFEF,
        bgHex = 0xFF282C34,
        surfaceHex = 0xFF2E333D,
        borderHex = 0xFF3E4451,
        textHex = 0xFFABB2BF,
        mutedHex = 0xFF7D8596,
        pillBgHex = 0xFF353B45,
        pillTextHex = 0xFFC678DD
    ),
    MONOKAI_PRO(
        label = "Monokai Pro",
        accentHex = 0xFFFFD866,
        bgHex = 0xFF2D2A2E,
        surfaceHex = 0xFF383539,
        borderHex = 0xFF524E54,
        textHex = 0xFFFCFCFA,
        mutedHex = 0xFF939293,
        pillBgHex = 0xFF403E41,
        pillTextHex = 0xFFFF6188
    ),
    KANAGAWA(
        label = "Kanagawa",
        accentHex = 0xFF7E9CD8,
        bgHex = 0xFF1F1F28,
        surfaceHex = 0xFF2A2A37,
        borderHex = 0xFF363646,
        textHex = 0xFFDCD7BA,
        mutedHex = 0xFF8C8B80,
        pillBgHex = 0xFF2D4F67,
        pillTextHex = 0xFF7FB4CA
    ),
    EVERFOREST_DARK(
        label = "Everforest Dark",
        accentHex = 0xFFA7C080,
        bgHex = 0xFF2D353B,
        surfaceHex = 0xFF343F44,
        borderHex = 0xFF475258,
        textHex = 0xFFD3C6AA,
        mutedHex = 0xFF859289,
        pillBgHex = 0xFF3D484D,
        pillTextHex = 0xFF83C092
    ),
    EVERFOREST_LIGHT(
        label = "Everforest Light",
        accentHex = 0xFF7A8C00,
        bgHex = 0xFFFDF6E3,
        surfaceHex = 0xFFF4F0D9,
        borderHex = 0xFFDFDBC8,
        textHex = 0xFF5C6A72,
        mutedHex = 0xFF829181,
        pillBgHex = 0xFFE6E2CC,
        pillTextHex = 0xFF5F7000,
        isLight = true
    ),
    AYU_MIRAGE(
        label = "Ayu Mirage",
        accentHex = 0xFFFFCC66,
        bgHex = 0xFF1F2430,
        surfaceHex = 0xFF242936,
        borderHex = 0xFF333B4C,
        textHex = 0xFFCBCCC6,
        mutedHex = 0xFF707A8C,
        pillBgHex = 0xFF33415E,
        pillTextHex = 0xFF73D0FF
    ),
    PALENIGHT(
        label = "Material Palenight",
        accentHex = 0xFFC792EA,
        bgHex = 0xFF292D3E,
        surfaceHex = 0xFF32374D,
        borderHex = 0xFF444A66,
        textHex = 0xFFA6ACCD,
        mutedHex = 0xFF7982A8,
        pillBgHex = 0xFF3E445E,
        pillTextHex = 0xFF82AAFF
    ),
    HORIZON(
        label = "Horizon Dark",
        accentHex = 0xFFE95678,
        bgHex = 0xFF1C1E26,
        surfaceHex = 0xFF232530,
        borderHex = 0xFF393C4D,
        textHex = 0xFFE0E0E0,
        mutedHex = 0xFF8B8E9B,
        pillBgHex = 0xFF2E303E,
        pillTextHex = 0xFFFAB795
    ),
    SYNTHWAVE_84(
        label = "Synthwave '84",
        accentHex = 0xFFFF7EDB,
        bgHex = 0xFF262335,
        surfaceHex = 0xFF2E2942,
        borderHex = 0xFF493E6E,
        textHex = 0xFFF8F8FF,
        mutedHex = 0xFF949BCF,
        pillBgHex = 0xFF3B2654,
        pillTextHex = 0xFF36F9F6
    ),
    CYBERPUNK(
        label = "Cyberpunk Neon",
        accentHex = 0xFF00F0FF,
        bgHex = 0xFF0A0B14,
        surfaceHex = 0xFF121426,
        borderHex = 0xFF292D4E,
        textHex = 0xFFEEF1FF,
        mutedHex = 0xFF868EC2,
        pillBgHex = 0xFF2B123F,
        pillTextHex = 0xFFFF2A85
    ),
    VESPER(
        label = "Vesper",
        accentHex = 0xFFFFC799,
        bgHex = 0xFF101010,
        surfaceHex = 0xFF161616,
        borderHex = 0xFF282828,
        textHex = 0xFFFFFFFF,
        mutedHex = 0xFFA0A0A0,
        pillBgHex = 0xFF26211C,
        pillTextHex = 0xFF99FFE4
    ),
    OXOCARBON(
        label = "Oxocarbon",
        accentHex = 0xFF78A9FF,
        bgHex = 0xFF161616,
        surfaceHex = 0xFF262626,
        borderHex = 0xFF393939,
        textHex = 0xFFF2F4F8,
        mutedHex = 0xFF8D8D8D,
        pillBgHex = 0xFF263859,
        pillTextHex = 0xFF33B1FF
    ),
    PHOSPHOR_GREEN(
        label = "Phosphor CRT",
        accentHex = 0xFF4ADE80,
        bgHex = 0xFF070B08,
        surfaceHex = 0xFF0E1510,
        borderHex = 0xFF1E3324,
        textHex = 0xFFE2F5E8,
        mutedHex = 0xFF6FA37F,
        pillBgHex = 0xFF163823,
        pillTextHex = 0xFF86EFAC
    ),
    AMBER_TERMINAL(
        label = "Amber Monochrome",
        accentHex = 0xFFF59E0B,
        bgHex = 0xFF0C0A06,
        surfaceHex = 0xFF16120B,
        borderHex = 0xFF332814,
        textHex = 0xFFFEF3C7,
        mutedHex = 0xFFA68B5B,
        pillBgHex = 0xFF3B2A0E,
        pillTextHex = 0xFFFCD34D
    ),
    CRIMSON_MINIMAL(
        label = "Crimson Shell",
        accentHex = 0xFFF43F5E,
        bgHex = 0xFF0E0A0C,
        surfaceHex = 0xFF181114,
        borderHex = 0xFF362129,
        textHex = 0xFFFBECEF,
        mutedHex = 0xFF9C7B85,
        pillBgHex = 0xFF421826,
        pillTextHex = 0xFFFDA4AF
    ),
    OLED_OBSIDIAN(
        label = "OLED Obsidian",
        accentHex = 0xFFE4E4E7,
        bgHex = 0xFF000000,
        surfaceHex = 0xFF0A0A0B,
        borderHex = 0xFF222226,
        textHex = 0xFFFAFAFA,
        mutedHex = 0xFF888890,
        pillBgHex = 0xFF27272A,
        pillTextHex = 0xFFFFFFFF
    ),
    PAPER_LIGHT(
        label = "E-Ink Paper",
        accentHex = 0xFF5B458C,
        bgHex = 0xFFF2F1EC,
        surfaceHex = 0xFFFFFFFF,
        borderHex = 0xFFD4D1C8,
        textHex = 0xFF18181B,
        mutedHex = 0xFF6B6964,
        pillBgHex = 0xFFE4DDF2,
        pillTextHex = 0xFF3E2C66,
        isLight = true
    ),
    CUSTOM(
        label = "Custom Palette",
        accentHex = 0xFF9D85C4,
        bgHex = 0xFF0D0D0F,
        surfaceHex = 0xFF151518,
        borderHex = 0xFF2B2B31,
        textHex = 0xFFECEBF0,
        mutedHex = 0xFF8C8A96,
        pillBgHex = 0xFF3B2E4D,
        pillTextHex = 0xFFCDBCE8
    )
}

enum class LauncherFont(
    val displayName: String,
    val tag: String,
    val isMonospace: Boolean = true
) {
    DM_MONO("DM Mono", "mono · default", true),
    SPACE_MONO("Space Mono", "mono · retro", true),
    JETBRAINS_MONO("JetBrains Mono", "mono · code", true),
    IBM_PLEX_MONO("IBM Plex Mono", "mono · crisp", true),
    FIRA_CODE("Fira Code", "mono · dev", true),
    UBUNTU_MONO("Ubuntu Mono", "mono · linux", true),
    ROBOTO_MONO("Roboto Mono", "mono · sharp", true),
    SOURCE_CODE_PRO("Source Code Pro", "mono · classic", true),
    INCONSOLATA("Inconsolata", "mono · narrow", true),
    VT323("VT323", "mono · crt", true),
    SYSTEM_MONO("System Monospace", "mono · android", true),
    INTER("Inter", "sans · clean", false),
    SPACE_GROTESK("Space Grotesk", "sans · tech", false),
    PLAYFAIR_DISPLAY("Playfair Display", "serif · editorial", false)
}

enum class IconShape(val label: String) {
    SHARP("Square"),
    ROUNDED("Rounded"),
    SQUIRCLE("Squircle"),
    CIRCLE("Circle"),
    TEARDROP("Teardrop")
}

enum class BuiltInIconStyle(val id: String, val label: String, val description: String) {
    ORIGINAL("builtin:original", "System Default", "Original unmodified app icons"),
    MONOCHROME_TINT("builtin:mono", "Monochrome Tint", "Desaturated icons tinted with accent"),
    TUI_FRAMED("builtin:framed", "TUI Box Frame", "1dp terminal frame around crisp icons"),
    GLYPH_INVERT("builtin:invert", "Negative Phosphor", "High-contrast inverted terminal look"),
    DUOTONE_PHOSPHOR("builtin:duotone", "CRT Duotone", "Deep charcoal & accent duotone filter")
}

enum class WallpaperType(val label: String) {
    TERMINAL_GRID("Terminal Grid"),
    DOT_MATRIX("Dot Matrix"),
    CRT_SCANLINES("CRT Scanlines"),
    RADIAL_GLOW("Radial Glow"),
    SOLID("Solid Canvas"),
    CUSTOM_IMAGE("Custom Image")
}

enum class BottomBarStyle(val label: String) {
    TUI_PANEL("TUI Panel"),
    FLOATING_PILL("Floating Bar"),
    MINIMAL_DOCK("Borderless Dock")
}

enum class DrawerLayoutMode(val label: String) {
    GRID("Grid"),
    LIST("Detailed List"),
    COMPACT_INDEX("A-Z Index")
}

enum class DrawerSortOrder(val label: String) {
    ALPHABETICAL("A → Z"),
    MOST_USED("Most Used"),
    RECENTLY_INSTALLED("Recently Installed")
}

enum class PageTransitionStyle(val label: String) {
    SLIDE("Standard Slide"),
    FADE("Crossfade"),
    SCALE("Depth Scale"),
    TERMINAL_SNAP("Terminal Snap")
}

enum class DoubleTapAction(val label: String) {
    NONE("Disabled"),
    SEARCH("Focus Search"),
    APP_DRAWER("Open App Drawer"),
    ZEN_MODE("Toggle Zen Mode"),
    SETTINGS("Open Settings")
}

@JsonClass(generateAdapter = true)
data class LauncherSettings(
    // Theme & Colors
    val themePreset: ThemePreset = ThemePreset.TERMINAL_VIOLET,
    val accentColor: Long = ThemePreset.TERMINAL_VIOLET.accentHex,
    val backgroundColor: Long = ThemePreset.TERMINAL_VIOLET.bgHex,
    val surfaceColor: Long = ThemePreset.TERMINAL_VIOLET.surfaceHex,
    val borderColor: Long = ThemePreset.TERMINAL_VIOLET.borderHex,
    val textColor: Long = ThemePreset.TERMINAL_VIOLET.textHex,
    val mutedTextColor: Long = ThemePreset.TERMINAL_VIOLET.mutedHex,
    val pillBackgroundColor: Long = ThemePreset.TERMINAL_VIOLET.pillBgHex,
    val pillTextColor: Long = ThemePreset.TERMINAL_VIOLET.pillTextHex,
    val isLightTheme: Boolean = false,
    val surfaceOpacity: Float = 0.92f,
    val cardBorderWidthDp: Float = 1.0f,
    val cardCornerRadiusDp: Float = 2.0f,

    // Wallpaper
    val wallpaperType: WallpaperType = WallpaperType.DOT_MATRIX,
    val customWallpaperPath: String = "",
    val wallpaperDim: Float = 0.35f,
    val wallpaperBlurDp: Float = 0f,
    val wallpaperParallax: Boolean = true,

    // Typography
    val font: LauncherFont = LauncherFont.DM_MONO,
    val fontScale: Float = 1.0f,
    val iconLabelSizeSp: Float = 11.5f,
    val clockFontSizeSp: Float = 44f,
    val letterSpacingEm: Float = 0.02f,
    val italicizeHeaders: Boolean = true,
    val boldTerminalText: Boolean = false,
    val uppercaseSectionHeaders: Boolean = false,

    // Icons & Icon Packs
    val selectedIconPack: String = BuiltInIconStyle.ORIGINAL.id,
    val iconShape: IconShape = IconShape.SHARP,
    val iconCornerRadiusDp: Float = 10f,
    val iconSizeDp: Float = 48f,
    val iconPaddingDp: Float = 0f,
    val showIconLabels: Boolean = true,
    val maxLabelLines: Int = 1,
    val showNotificationDots: Boolean = true,

    // Home Grid & Gestures
    val gridColumns: Int = 4,
    val gridRows: Int = 6,
    val gridHorizontalMarginDp: Float = 12f,
    val gridCellGapDp: Float = 8f,
    val pageTransitionStyle: PageTransitionStyle = PageTransitionStyle.SLIDE,
    val showPageIndicator: Boolean = true,
    val showTopStatusPill: Boolean = false,
    val lockHomeLayout: Boolean = false,
    val swipeUpForDrawer: Boolean = true,
    val doubleTapAction: DoubleTapAction = DoubleTapAction.SEARCH,
    val clock24Hour: Boolean = true,
    val clockShowSeconds: Boolean = false,
    val showGridCellOutlines: Boolean = false,

    // Bottom Panel & Dock
    val bottomBarStyle: BottomBarStyle = BottomBarStyle.TUI_PANEL,
    val showSearchInBottomBar: Boolean = true,
    val enableSearchCalculator: Boolean = true,
    val showNavShortcuts: Boolean = true,
    val pinnedDockPackages: List<String> = emptyList(),

    // App Drawer
    val drawerLayoutMode: DrawerLayoutMode = DrawerLayoutMode.GRID,
    val drawerColumns: Int = 4,
    val drawerSortOrder: DrawerSortOrder = DrawerSortOrder.ALPHABETICAL,
    val autoFocusDrawerSearch: Boolean = false,
    val showSettingsInDrawer: Boolean = true,

    // Editable TUI Profile Card Data
    val profileDisplayName: String = "User ~",
    val profilePronouns: String = "they/them",
    val profileSubtitle: String = "custom terminal workspace",
    val profileBio: String = "minimalist android launcher · tap [edit] to customize banner, avatar, bio, and widgets.",
    val profileWelcomeQuote: String = "Stay focused. Keep building.",
    val profileAvatarPath: String = "",
    val profileBannerPath: String = ""
)

package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.LauncherFont
import com.example.model.LauncherSettings

val DmMonoFamily = FontFamily(Font(R.font.dm_mono, FontWeight.Normal))
val SpaceMonoFamily = FontFamily(Font(R.font.space_mono, FontWeight.Normal))
val JetBrainsMonoFamily = FontFamily(Font(R.font.jetbrains_mono, FontWeight.Normal))
val IbmPlexMonoFamily = FontFamily(Font(R.font.ibm_plex_mono, FontWeight.Normal))
val FiraCodeFamily = FontFamily(Font(R.font.fira_code, FontWeight.Normal))
val UbuntuMonoFamily = FontFamily(Font(R.font.ubuntu_mono, FontWeight.Normal))
val RobotoMonoFamily = FontFamily(Font(R.font.roboto_mono, FontWeight.Normal))
val SourceCodeProFamily = FontFamily(Font(R.font.source_code_pro, FontWeight.Normal))
val InconsolataFamily = FontFamily(Font(R.font.inconsolata, FontWeight.Normal))
val Vt323Family = FontFamily(Font(R.font.vt323, FontWeight.Normal))
val InterFamily = FontFamily(Font(R.font.inter, FontWeight.Normal))
val SpaceGroteskFamily = FontFamily(Font(R.font.space_grotesk, FontWeight.Normal))
val PlayfairDisplayFamily = FontFamily(Font(R.font.playfair_display, FontWeight.Normal))

fun LauncherFont.toFontFamily(): FontFamily = when (this) {
    LauncherFont.DM_MONO -> DmMonoFamily
    LauncherFont.SPACE_MONO -> SpaceMonoFamily
    LauncherFont.JETBRAINS_MONO -> JetBrainsMonoFamily
    LauncherFont.IBM_PLEX_MONO -> IbmPlexMonoFamily
    LauncherFont.FIRA_CODE -> FiraCodeFamily
    LauncherFont.UBUNTU_MONO -> UbuntuMonoFamily
    LauncherFont.ROBOTO_MONO -> RobotoMonoFamily
    LauncherFont.SOURCE_CODE_PRO -> SourceCodeProFamily
    LauncherFont.INCONSOLATA -> InconsolataFamily
    LauncherFont.VT323 -> Vt323Family
    LauncherFont.SYSTEM_MONO -> FontFamily.Monospace
    LauncherFont.INTER -> InterFamily
    LauncherFont.SPACE_GROTESK -> SpaceGroteskFamily
    LauncherFont.PLAYFAIR_DISPLAY -> PlayfairDisplayFamily
}

fun buildLauncherTypography(settings: LauncherSettings): Typography {
    val family = settings.font.toFontFamily()
    val scale = settings.fontScale.coerceIn(0.75f, 1.5f)
    val letterSpacing = (settings.letterSpacingEm * 14f).sp
    val headerStyle = if (settings.italicizeHeaders) FontStyle.Italic else FontStyle.Normal
    val baseWeight = if (settings.boldTerminalText) FontWeight.SemiBold else FontWeight.Normal
    val mediumWeight = if (settings.boldTerminalText) FontWeight.Bold else FontWeight.Medium

    return Typography(
        displayLarge = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Bold,
            fontSize = (settings.clockFontSizeSp * scale).sp,
            lineHeight = (settings.clockFontSizeSp * scale * 1.12f).sp,
            letterSpacing = letterSpacing
        ),
        headlineMedium = TextStyle(
            fontFamily = family,
            fontWeight = if (settings.boldTerminalText) FontWeight.Bold else FontWeight.SemiBold,
            fontStyle = headerStyle,
            fontSize = (20f * scale).sp,
            lineHeight = (26f * scale).sp,
            letterSpacing = letterSpacing
        ),
        titleLarge = TextStyle(
            fontFamily = family,
            fontWeight = if (settings.boldTerminalText) FontWeight.Bold else FontWeight.SemiBold,
            fontStyle = headerStyle,
            fontSize = (17f * scale).sp,
            lineHeight = (22f * scale).sp,
            letterSpacing = letterSpacing
        ),
        titleMedium = TextStyle(
            fontFamily = family,
            fontWeight = mediumWeight,
            fontStyle = headerStyle,
            fontSize = (15f * scale).sp,
            lineHeight = (20f * scale).sp,
            letterSpacing = letterSpacing
        ),
        titleSmall = TextStyle(
            fontFamily = family,
            fontWeight = mediumWeight,
            fontSize = (13f * scale).sp,
            lineHeight = (18f * scale).sp,
            letterSpacing = letterSpacing
        ),
        bodyLarge = TextStyle(
            fontFamily = family,
            fontWeight = baseWeight,
            fontSize = (14f * scale).sp,
            lineHeight = (20f * scale).sp,
            letterSpacing = letterSpacing
        ),
        bodyMedium = TextStyle(
            fontFamily = family,
            fontWeight = baseWeight,
            fontSize = (12.5f * scale).sp,
            lineHeight = (18f * scale).sp,
            letterSpacing = letterSpacing
        ),
        bodySmall = TextStyle(
            fontFamily = family,
            fontWeight = baseWeight,
            fontSize = (11f * scale).sp,
            lineHeight = (15f * scale).sp,
            letterSpacing = letterSpacing
        ),
        labelLarge = TextStyle(
            fontFamily = family,
            fontWeight = mediumWeight,
            fontSize = (12.5f * scale).sp,
            lineHeight = (16f * scale).sp,
            letterSpacing = letterSpacing
        ),
        labelMedium = TextStyle(
            fontFamily = family,
            fontWeight = mediumWeight,
            fontSize = (11f * scale).sp,
            lineHeight = (14f * scale).sp,
            letterSpacing = (letterSpacing.value + 0.8f).sp
        ),
        labelSmall = TextStyle(
            fontFamily = family,
            fontWeight = baseWeight,
            fontSize = (settings.iconLabelSizeSp * scale).sp,
            lineHeight = ((settings.iconLabelSizeSp + 3f) * scale).sp,
            letterSpacing = letterSpacing
        )
    )
}

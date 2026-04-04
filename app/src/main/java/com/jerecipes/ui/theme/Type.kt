package com.jerecipes.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.jerecipes.R

// ── Font provider ─────────────────────────────────────────────────────────────
// Exported so FontShowcaseScreen can reuse it for its Google Fonts showcase.
// IMPORTANT: Do NOT use this provider for Fraunces or Roboto Flex — those are
// variable fonts. GoogleFont.Provider silently fails on variable fonts and falls
// back to the system default with no error. See ground rules for full details.
val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

// ── Fraunces — local static .ttf files ───────────────────────────────────────
// Fraunces is a variable font. The fraunces_*.ttf files in res/font are genuine
// static instances sourced from the variable font at specific points on the wght
// axis. They are NOT identical copies — each maps to a distinct weight.
// Load them as separate weight entries (the original working approach).
val FrauncesFontFamily = FontFamily(
    androidx.compose.ui.text.font.Font(R.font.fraunces_regular, FontWeight.Normal),
    androidx.compose.ui.text.font.Font(R.font.fraunces_bold, FontWeight.Bold),
    androidx.compose.ui.text.font.Font(R.font.fraunces_semi_bold, FontWeight.SemiBold),
    androidx.compose.ui.text.font.Font(R.font.fraunces_extra_bold, FontWeight.ExtraBold),
    androidx.compose.ui.text.font.Font(R.font.fraunces_regular, FontWeight.Normal, style = FontStyle.Italic),
    androidx.compose.ui.text.font.Font(R.font.fraunces_bold, FontWeight.Bold, style = FontStyle.Italic),
    androidx.compose.ui.text.font.Font(R.font.fraunces_extra_bold, FontWeight.ExtraBold, style = FontStyle.Italic)
)

// ── Body font — Be Vietnam Pro, genuine static .ttf instances ────────────────
// Roboto Flex has no static .ttf distribution. Be Vietnam Pro is already
// bundled in res/font as true static instances (clearly different file sizes).
val BodyFontFamily = FontFamily(
    androidx.compose.ui.text.font.Font(R.font.be_vietnam_pro_regular, FontWeight.Normal),
    androidx.compose.ui.text.font.Font(R.font.be_vietnam_pro_medium, FontWeight.Medium),
    androidx.compose.ui.text.font.Font(R.font.be_vietnam_pro_semi_bold, FontWeight.SemiBold),
    androidx.compose.ui.text.font.Font(R.font.be_vietnam_pro_bold, FontWeight.Bold)
)

// ── Typography scale ──────────────────────────────────────────────────────────
// M3 Expressive token split:
//   display* / headline*  → FrauncesFontFamily  (expressive brand moments, ≥24sp)
//   title*                → BodyFontFamily SemiBold (UI chrome — legibility first)
//   body* / label*        → BodyFontFamily (functional reading text)
val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = FrauncesFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = FrauncesFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = FrauncesFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FrauncesFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FrauncesFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FrauncesFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

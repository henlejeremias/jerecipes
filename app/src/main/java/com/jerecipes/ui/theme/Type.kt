package com.jerecipes.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.jerecipes.R

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private fun robotoFont(
    weight: FontWeight,
    style: FontStyle = FontStyle.Normal
) = Font(
    resId = if (style == FontStyle.Italic) {
        R.font.roboto_variable_italic
    } else {
        R.font.roboto_variable
    },
    weight = weight,
    style = style,
    variationSettings = FontVariation.Settings(
        FontVariation.weight(weight.weight)
    )
)

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private fun robotoFlexFont(weight: FontWeight) = Font(
    resId = R.font.roboto_flex_variable,
    weight = weight,
    variationSettings = FontVariation.Settings(
        FontVariation.weight(weight.weight)
    )
)

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private fun frauncesFont(
    weight: FontWeight,
    style: FontStyle = FontStyle.Normal,
    width: Float = 100f,
    opticalSize: Float = 72f
) = Font(
    resId = if (style == FontStyle.Italic) {
        R.font.fraunces_variable_italic
    } else {
        R.font.fraunces_variable
    },
    weight = weight,
    style = style,
    variationSettings = FontVariation.Settings(
        FontVariation.weight(weight.weight),
        FontVariation.Setting("wdth", width),
        FontVariation.Setting("opsz", opticalSize)
    )
)

// Default app font architecture:
// - Roboto is the base family for the full Material 3 token set.
// - Roboto Flex is bundled for later selective experiments, not the default.
val AppFontFamily = FontFamily(
    robotoFont(FontWeight.Normal),
    robotoFont(FontWeight.Medium),
    robotoFont(FontWeight.SemiBold),
    robotoFont(FontWeight.Bold),
    robotoFont(FontWeight.ExtraBold),
    robotoFont(FontWeight.Black),
    robotoFont(FontWeight.Normal, FontStyle.Italic),
    robotoFont(FontWeight.Medium, FontStyle.Italic),
    robotoFont(FontWeight.SemiBold, FontStyle.Italic),
    robotoFont(FontWeight.Bold, FontStyle.Italic),
    robotoFont(FontWeight.Black, FontStyle.Italic)
)

val RobotoFlexFontFamily = FontFamily(
    robotoFlexFont(FontWeight.Normal),
    robotoFlexFont(FontWeight.Medium),
    robotoFlexFont(FontWeight.SemiBold),
    robotoFlexFont(FontWeight.Bold),
    robotoFlexFont(FontWeight.ExtraBold),
    robotoFlexFont(FontWeight.Black)
)

val FrauncesFontFamily = FontFamily(
    frauncesFont(FontWeight.Normal),
    frauncesFont(FontWeight.SemiBold),
    frauncesFont(FontWeight.Bold),
    frauncesFont(FontWeight.ExtraBold),
    frauncesFont(FontWeight.Black),
    frauncesFont(FontWeight.Normal, FontStyle.Italic),
    frauncesFont(FontWeight.Bold, FontStyle.Italic),
    frauncesFont(FontWeight.Black, FontStyle.Italic)
)

fun recipeTitleTextStyle(color: Color) = TextStyle(
    fontFamily = FrauncesFontFamily,
    fontWeight = FontWeight.Black,
    fontSize = 32.sp,
    lineHeight = 34.sp,
    color = color
)

fun recipeDetailTitleTextStyle(color: Color) = TextStyle(
    fontFamily = FrauncesFontFamily,
    fontWeight = FontWeight.Black,
    fontSize = 52.sp,
    lineHeight = 56.sp,
    color = color
)

val Typography = Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontFamily = AppFontFamily),
        displayMedium = displayMedium.copy(fontFamily = AppFontFamily),
        displaySmall = displaySmall.copy(fontFamily = AppFontFamily),
        headlineLarge = headlineLarge.copy(fontFamily = AppFontFamily),
        headlineMedium = headlineMedium.copy(fontFamily = AppFontFamily),
        headlineSmall = headlineSmall.copy(fontFamily = AppFontFamily),
        titleLarge = titleLarge.copy(fontFamily = AppFontFamily),
        titleMedium = titleMedium.copy(fontFamily = AppFontFamily),
        titleSmall = titleSmall.copy(fontFamily = AppFontFamily),
        bodyLarge = bodyLarge.copy(fontFamily = AppFontFamily),
        bodyMedium = bodyMedium.copy(fontFamily = AppFontFamily),
        bodySmall = bodySmall.copy(fontFamily = AppFontFamily),
        labelLarge = labelLarge.copy(fontFamily = AppFontFamily),
        labelMedium = labelMedium.copy(fontFamily = AppFontFamily),
        labelSmall = labelSmall.copy(fontFamily = AppFontFamily)
    )
}

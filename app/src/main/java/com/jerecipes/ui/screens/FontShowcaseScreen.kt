package com.jerecipes.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jerecipes.R
import com.jerecipes.ui.theme.AppFontFamily
import com.jerecipes.ui.theme.FrauncesFontFamily
import com.jerecipes.ui.theme.RobotoFlexFontFamily

private data class FlexVariant(
    val label: String,
    val family: FontFamily,
    val weight: FontWeight,
    val sample: String
)

private data class SpecimenCardModel(
    val caption: String,
    val family: FontFamily,
    val weight: FontWeight,
    val sample: String,
    val style: FontStyle = FontStyle.Normal
)

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private fun robotoFlexVariantFamily(
    weight: FontWeight,
    width: Float = 100f,
    grade: Float = 0f,
    opticalSize: Float = 14f
) = FontFamily(
    Font(
        resId = R.font.roboto_flex_variable,
        weight = weight,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(weight.weight),
            FontVariation.Setting("wdth", width),
            FontVariation.Setting("GRAD", grade),
            FontVariation.Setting("opsz", opticalSize)
        )
    )
)

private val plusJakartaFamily = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans, FontWeight.Bold),
    Font(R.font.plus_jakarta_sans, FontWeight.ExtraBold)
)

private val flexVariants = listOf(
    FlexVariant("Regular", RobotoFlexFontFamily, FontWeight.Normal, "Roboto Flex"),
    FlexVariant("Medium", RobotoFlexFontFamily, FontWeight.Medium, "Roboto Flex"),
    FlexVariant("Bold", RobotoFlexFontFamily, FontWeight.Bold, "Roboto Flex"),
    FlexVariant("ExtraBold", RobotoFlexFontFamily, FontWeight.ExtraBold, "Roboto Flex"),
    FlexVariant("Black", RobotoFlexFontFamily, FontWeight.Black, "Roboto Flex"),
    FlexVariant(
        "Wide Black",
        robotoFlexVariantFamily(
            weight = FontWeight.Black,
            width = 125f,
            grade = 25f,
            opticalSize = 48f
        ),
        FontWeight.Black,
        "Roboto Flex"
    ),
    FlexVariant(
        "Tight Black",
        robotoFlexVariantFamily(
            weight = FontWeight.Black,
            width = 85f,
            grade = 50f,
            opticalSize = 48f
        ),
        FontWeight.Black,
        "Roboto Flex"
    ),
    FlexVariant(
        "Dense Black",
        robotoFlexVariantFamily(
            weight = FontWeight.Black,
            width = 95f,
            grade = 100f,
            opticalSize = 60f
        ),
        FontWeight.Black,
        "Roboto Flex"
    )
)

private val expressiveCards = listOf(
    SpecimenCardModel(
        caption = "Fraunces Black",
        family = FrauncesFontFamily,
        weight = FontWeight.Black,
        sample = "Roasted Tomato Soup"
    ),
    SpecimenCardModel(
        caption = "Fraunces Italic",
        family = FrauncesFontFamily,
        weight = FontWeight.Black,
        style = FontStyle.Italic,
        sample = "Burnt Butter Pasta"
    ),
    SpecimenCardModel(
        caption = "Roboto Flex Wide Black",
        family = robotoFlexVariantFamily(
            weight = FontWeight.Black,
            width = 130f,
            grade = 50f,
            opticalSize = 64f
        ),
        weight = FontWeight.Black,
        sample = "Kitchen Notes"
    ),
    SpecimenCardModel(
        caption = "Roboto Flex Tight Black",
        family = robotoFlexVariantFamily(
            weight = FontWeight.Black,
            width = 82f,
            grade = 75f,
            opticalSize = 64f
        ),
        weight = FontWeight.Black,
        sample = "Pan Sauce"
    ),
    SpecimenCardModel(
        caption = "Plus Jakarta Sans ExtraBold",
        family = plusJakartaFamily,
        weight = FontWeight.ExtraBold,
        sample = "Crisp Greens"
    ),
    SpecimenCardModel(
        caption = "Roboto Black Italic",
        family = AppFontFamily,
        weight = FontWeight.Black,
        style = FontStyle.Italic,
        sample = "Late Lunch"
    ),
    SpecimenCardModel(
        caption = "Android Serif Bold Italic",
        family = FontFamily.Serif,
        weight = FontWeight.Bold,
        style = FontStyle.Italic,
        sample = "Olive Oil Cake"
    )
)

@Composable
fun FontShowcaseScreen(onBack: () -> Unit) {
    BackHandler { onBack() }

    val colors = MaterialTheme.colorScheme

    Scaffold(
        containerColor = colors.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(modifier = Modifier.fillMaxWidth()) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                }
            }

            item { HeroCard(colors) }
            item { FlexWeightsCard(colors) }

            items(expressiveCards) { card ->
                ExpressiveCard(card, colors)
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun HeroCard(colors: ColorScheme) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = colors.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            colors.primary.copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    )
                )
        ) {
            Column(modifier = Modifier.padding(28.dp)) {
                Text(
                    text = "Roboto Flex",
                    style = TextStyle(
                        fontFamily = RobotoFlexFontFamily,
                        fontWeight = FontWeight.Black,
                        fontSize = 56.sp,
                        lineHeight = 58.sp,
                        color = colors.onPrimaryContainer
                    )
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Heavy, clean, Android-native",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = AppFontFamily,
                        color = colors.onPrimaryContainer.copy(alpha = 0.84f)
                    )
                )

                Spacer(Modifier.height(18.dp))

                Text(
                    text = "Black  Wide  Tight  Serif",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = AppFontFamily,
                        color = colors.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                )
            }
        }
    }
}

@Composable
private fun FlexWeightsCard(colors: ColorScheme) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            flexVariants.forEachIndexed { index, spec ->
                WeightRow(
                    family = spec.family,
                    label = spec.label,
                    weight = spec.weight,
                    sample = spec.sample,
                    color = colors.onSurface
                )

                if (index != flexVariants.lastIndex) {
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun ExpressiveCard(model: SpecimenCardModel, colors: ColorScheme) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = model.sample,
                style = TextStyle(
                    fontFamily = model.family,
                    fontWeight = model.weight,
                    fontStyle = model.style,
                    fontSize = 32.sp,
                    lineHeight = 36.sp,
                    color = colors.onSurface
                )
            )

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.5f))
            Spacer(Modifier.height(10.dp))

            Text(
                text = "The quick brown fox jumps over the lazy dog.",
                style = TextStyle(
                    fontFamily = model.family,
                    fontWeight = FontWeight.Normal,
                    fontStyle = model.style,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = colors.onSurfaceVariant
                )
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = model.caption,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = AppFontFamily,
                    color = colors.primary
                )
            )
        }
    }
}

@Composable
private fun WeightRow(
    family: FontFamily,
    label: String,
    weight: FontWeight,
    sample: String,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = sample,
            style = TextStyle(
                fontFamily = family,
                fontWeight = weight,
                fontSize = 24.sp,
                color = color
            )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = AppFontFamily,
                color = color.copy(alpha = 0.6f)
            )
        )
    }
}

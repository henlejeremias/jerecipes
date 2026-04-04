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
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jerecipes.R
import com.jerecipes.ui.theme.BodyFontFamily
import com.jerecipes.ui.theme.FrauncesFontFamily

private data class PairingOption(
    val title: String,
    val note: String,
    val recommendation: String,
    val displayName: String,
    val displayFamily: FontFamily,
    val uiName: String,
    val uiFamily: FontFamily
)

private data class FontCandidate(
    val name: String,
    val role: String,
    val summary: String,
    val recommendation: String,
    val family: FontFamily,
    val previewWeight: FontWeight = FontWeight.Bold
)

private val PlusJakartaFamily = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans, FontWeight.Bold),
    Font(R.font.plus_jakarta_sans, FontWeight.ExtraBold)
)

private val displayCandidates = listOf(
    FontCandidate(
        name = "Fraunces",
        role = "Display",
        summary = "Strong and expressive. Best fit for hero titles, recipe names, and major section headings.",
        recommendation = "Best overall display choice for this app.",
        family = FrauncesFontFamily,
        previewWeight = FontWeight.ExtraBold
    ),
    FontCandidate(
        name = "Plus Jakarta Sans",
        role = "Display Sans",
        summary = "Bold without looking loud. Good if you want a cleaner, more product-style headline voice.",
        recommendation = "Best sans-serif display alternative.",
        family = PlusJakartaFamily,
        previewWeight = FontWeight.ExtraBold
    ),
    FontCandidate(
        name = "Android Serif",
        role = "System",
        summary = "Stable and simple. Useful as a reference point, but less distinctive than Fraunces.",
        recommendation = "Only use if you want zero extra assets beyond system fonts.",
        family = FontFamily.Serif,
        previewWeight = FontWeight.Bold
    )
)

private val uiCandidates = listOf(
    FontCandidate(
        name = "Be Vietnam Pro",
        role = "UI / Body",
        summary = "Clean and compact. Reads well in forms, chips, labels, and dense recipe details.",
        recommendation = "Best bundled font for titles, body, and labels.",
        family = BodyFontFamily,
        previewWeight = FontWeight.SemiBold
    ),
    FontCandidate(
        name = "Android Sans",
        role = "System",
        summary = "Most natural Android feel. Very safe for standard UI elements and small text.",
        recommendation = "Best choice if you want the most native Android body text.",
        family = FontFamily.SansSerif,
        previewWeight = FontWeight.SemiBold
    ),
    FontCandidate(
        name = "Plus Jakarta Sans",
        role = "UI / Alt",
        summary = "Polished and modern. Better for larger titles and cards than for the smallest metadata.",
        recommendation = "Use for title-heavy surfaces, not as the only text face.",
        family = PlusJakartaFamily,
        previewWeight = FontWeight.Bold
    )
)

private val pairingOptions = listOf(
    PairingOption(
        title = "Recommended pairing",
        note = "Expressive headings with quiet UI text.",
        recommendation = "Use this if you want the strongest Material 3 Expressive look.",
        displayName = "Fraunces",
        displayFamily = FrauncesFontFamily,
        uiName = "Be Vietnam Pro",
        uiFamily = BodyFontFamily
    ),
    PairingOption(
        title = "Most Android-native pairing",
        note = "Keep the display personality, but let controls feel fully native.",
        recommendation = "Use this if standard elements should feel especially natural on Android.",
        displayName = "Fraunces",
        displayFamily = FrauncesFontFamily,
        uiName = "Android Sans",
        uiFamily = FontFamily.SansSerif
    ),
    PairingOption(
        title = "Sans-led alternative",
        note = "Cleaner and more product-like, with less serif contrast.",
        recommendation = "Use this if you want a bold app voice without leaning editorial.",
        displayName = "Plus Jakarta Sans",
        displayFamily = PlusJakartaFamily,
        uiName = "Android Sans",
        uiFamily = FontFamily.SansSerif
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FontShowcaseScreen(onBack: () -> Unit) {
    BackHandler { onBack() }

    val colors = MaterialTheme.colorScheme

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Font Showcase",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.background
                )
            )
        },
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
                IntroCard(colors = colors)
            }

            item {
                SectionLabel(
                    title = "Recommended Pairings",
                    body = "These are stable combinations that fit the current app direction."
                )
            }

            items(pairingOptions) { pairing ->
                PairingCard(pairing = pairing, colors = colors)
            }

            item {
                SectionLabel(
                    title = "Display Candidates",
                    body = "For big, bold, expressive moments."
                )
            }

            items(displayCandidates) { candidate ->
                CandidateCard(candidate = candidate, colors = colors)
            }

            item {
                SectionLabel(
                    title = "UI And Body Candidates",
                    body = "For titles, controls, labels, and reading text."
                )
            }

            items(uiCandidates) { candidate ->
                CandidateCard(candidate = candidate, colors = colors)
            }

            item {
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun IntroCard(colors: ColorScheme) {
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
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = colors.primary.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = "Production-safe only",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = colors.primary
                        )
                    )
                }

                Spacer(Modifier.height(18.dp))

                Text(
                    text = "Fraunces",
                    style = TextStyle(
                        fontFamily = FrauncesFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 54.sp,
                        lineHeight = 56.sp,
                        color = colors.onPrimaryContainer
                    )
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    text = "Big display energy with restrained Android UI text.",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = BodyFontFamily,
                        color = colors.onPrimaryContainer.copy(alpha = 0.86f)
                    )
                )

                Spacer(Modifier.height(18.dp))
                HorizontalDivider(color = colors.onPrimaryContainer.copy(alpha = 0.14f))
                Spacer(Modifier.height(18.dp))

                Text(
                    text = "Everything on this screen is stable for production in this app: bundled local font files or Android system families.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = BodyFontFamily,
                        color = colors.onPrimaryContainer.copy(alpha = 0.82f)
                    )
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Current recommendation: Fraunces for display and headline styles, Be Vietnam Pro or Android Sans for standard elements.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = BodyFontFamily,
                        color = colors.onPrimaryContainer.copy(alpha = 0.82f)
                    )
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PairingCard(pairing: PairingOption, colors: ColorScheme) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Tag(
                text = pairing.title,
                colors = colors
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Chocolate Tahini Cake",
                style = TextStyle(
                    fontFamily = pairing.displayFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 34.sp,
                    lineHeight = 38.sp,
                    color = colors.onSurface
                )
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = pairing.note,
                style = TextStyle(
                    fontFamily = pairing.uiFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = colors.onSurfaceVariant
                )
            )

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.5f))
            Spacer(Modifier.height(14.dp))

            PairingRow(
                label = "Display",
                fontName = pairing.displayName,
                family = pairing.displayFamily,
                colors = colors
            )
            Spacer(Modifier.height(10.dp))
            PairingRow(
                label = "UI / Body",
                fontName = pairing.uiName,
                family = pairing.uiFamily,
                colors = colors
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = pairing.recommendation,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = pairing.uiFamily,
                    color = colors.primary
                )
            )
        }
    }
}

@Composable
private fun PairingRow(
    label: String,
    fontName: String,
    family: FontFamily,
    colors: ColorScheme
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = colors.onSurfaceVariant
        )
        Text(
            text = fontName,
            style = TextStyle(
                fontFamily = family,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = colors.onSurface
            )
        )
    }
}

@Composable
private fun CandidateCard(candidate: FontCandidate, colors: ColorScheme) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = candidate.name,
                        style = TextStyle(
                            fontFamily = candidate.family,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            color = colors.onSurface
                        )
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = candidate.role,
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.primary
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Roasted Tomato Soup",
                style = TextStyle(
                    fontFamily = candidate.family,
                    fontWeight = candidate.previewWeight,
                    fontSize = 32.sp,
                    lineHeight = 36.sp,
                    color = colors.onSurface
                )
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Simple, stable typography for cards, recipes, and controls.",
                style = TextStyle(
                    fontFamily = candidate.family,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = colors.onSurfaceVariant
                )
            )

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.5f))
            Spacer(Modifier.height(14.dp))

            WeightLadder(
                family = candidate.family,
                color = colors.onSurface
            )

            Spacer(Modifier.height(14.dp))

            Text(
                text = candidate.summary,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = BodyFontFamily,
                    color = colors.onSurfaceVariant
                )
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = candidate.recommendation,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = BodyFontFamily,
                    color = colors.primary
                )
            )
        }
    }
}

@Composable
private fun Tag(text: String, colors: ColorScheme) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = colors.secondaryContainer
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = colors.onSecondaryContainer
            )
        )
    }
}

@Composable
private fun WeightLadder(family: FontFamily, color: Color) {
    val weights = listOf(
        FontWeight.Normal to "Regular",
        FontWeight.Medium to "Medium",
        FontWeight.SemiBold to "SemiBold",
        FontWeight.Bold to "Bold",
        FontWeight.ExtraBold to "ExtraBold"
    )

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        weights.forEach { (weight, label) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Typography",
                    style = TextStyle(
                        fontFamily = family,
                        fontWeight = weight,
                        fontSize = 17.sp,
                        color = color
                    )
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = BodyFontFamily,
                        color = color.copy(alpha = 0.5f)
                    )
                )
            }
        }
    }
}

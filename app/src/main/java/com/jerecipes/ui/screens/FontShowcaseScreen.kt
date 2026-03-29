package com.jerecipes.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font as GFont
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jerecipes.R
import com.jerecipes.ui.theme.provider as globalFontProvider
import com.jerecipes.ui.theme.FrauncesFontFamily as GlobalFrauncesFamily

// Using the provider from Type.kt to ensure consistency across the app.
private val fontProvider = globalFontProvider

// ── Font families for this showcase ──────────────────────────────────────────

private fun googleFontFamily(name: String, vararg weights: FontWeight): FontFamily {
    val fonts = mutableListOf<Font>()
    val gf = GoogleFont(name)
    for (w in weights) {
        fonts += GFont(googleFont = gf, fontProvider = fontProvider, weight = w)
        fonts += GFont(googleFont = gf, fontProvider = fontProvider, weight = w, style = FontStyle.Italic)
    }
    return FontFamily(fonts)
}

// Each entry = one specimen card
private data class FontEntry(
    val name: String,
    val tagline: String,
    val body: String,
    val family: FontFamily,
    val accentAlpha: Float = 0.12f
)

// Using the global local-bundled family for Fraunces
private val FrauncesFamily = GlobalFrauncesFamily
private val PlayfairFamily = googleFontFamily(
    "Playfair Display",
    FontWeight.Normal, FontWeight.Medium, FontWeight.Bold, FontWeight.ExtraBold
)
private val RobotoSerifFamily = googleFontFamily(
    "Roboto Serif",
    FontWeight.Normal, FontWeight.Medium, FontWeight.Bold
)
private val CormorantFamily = googleFontFamily(
    "Cormorant Garamond",
    FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold
)
private val DmSerifFamily = googleFontFamily(
    "DM Serif Display",
    FontWeight.Normal
)
private val EBGaramondFamily = googleFontFamily(
    "EB Garamond",
    FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold
)
private val YesEvaFamily = googleFontFamily(
    "Yeseva One",
    FontWeight.Normal
)
private val NunitoFamily = googleFontFamily(
    "Nunito",
    FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold, FontWeight.ExtraBold
)
private val OutfitFamily = googleFontFamily(
    "Outfit",
    FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold, FontWeight.ExtraBold
)
private val SpaceGroteskFamily = googleFontFamily(
    "Space Grotesk",
    FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold
)
private val CabinFamily = googleFontFamily(
    "Cabin",
    FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold
)
// Use the LOCAL font that is already in the project!
private val PlusJakartaFamily = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans, FontWeight.Bold),
    Font(R.font.plus_jakarta_sans, FontWeight.ExtraBold)
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FontShowcaseScreen(onBack: () -> Unit) {
    // Intercept system back button to go back to the library view
    BackHandler { onBack() }

    val colorScheme = MaterialTheme.colorScheme

    val fontEntries = remember {
        listOf(
            FontEntry(
                name    = "Fraunces",
                tagline = "Optical size · variable · soft-serif",
                body    = "A display typeface designed for editorial use — its soft serifs and expressive details make every headline feel considered and warm. Perfect for recipe titles.",
                family  = FrauncesFamily
            ),
            FontEntry(
                name    = "Playfair Display",
                tagline = "High-contrast · editorial · transitional",
                body    = "Inspired by 18th century type, Playfair pairs razor-thin hairlines with bold strokes to create effortless typographic drama.",
                family  = PlayfairFamily
            ),
            FontEntry(
                name    = "Roboto Serif",
                tagline = "Variable · workhorse · neutral",
                body    = "Google's own serif companion to Roboto — range from a bookish regular to a punchy display weight with the same optical family feel.",
                family  = RobotoSerifFamily
            ),
            FontEntry(
                name    = "Cormorant Garamond",
                tagline = "Ultra-elegant · luxury · condensed",
                body    = "Derived from the great Claude Garamond. Its extreme weight contrast and tight spacing evoke perfumery catalogues and fine dining menus.",
                family  = CormorantFamily
            ),
            FontEntry(
                name    = "DM Serif Display",
                tagline = "Refined · high-contrast · short texts",
                body    = "Built for maximum impact at large sizes — the bold strokes and open counters keep this typeface extremely readable even on small screens.",
                family  = DmSerifFamily
            ),
            FontEntry(
                name    = "EB Garamond",
                tagline = "Renaissance · warm · scholarly",
                body    = "A faithful digital revival of Garamond's 16th-century punchcuts. When you want type that whispers with authority, this is the one.",
                family  = EBGaramondFamily
            ),
            FontEntry(
                name    = "Yeseva One",
                tagline = "Display · decorative · Slavic roots",
                body    = "A bold display serif with a pleasantly irregular humanist rhythm. Great for poster-style headings that need personality beyond the norm.",
                family  = YesEvaFamily
            ),
            FontEntry(
                name    = "Nunito",
                tagline = "Rounded · friendly · versatile sans",
                body    = "Rounded terminals give every letter a gentle warmth that makes body text feel approachable — common in onboarding flows and lifestyle apps.",
                family  = NunitoFamily
            ),
            FontEntry(
                name    = "Outfit",
                tagline = "Geometric · precise · tech-flavoured",
                body    = "Clean construction with subtle personality. Outfit excels in product UIs where clarity meets contemporary style.",
                family  = OutfitFamily
            ),
            FontEntry(
                name    = "Space Grotesk",
                tagline = "Quirky · distinct · mono-influenced",
                body    = "Inherited its metrics from Space Mono but embraced the proportional world. Ideal when you need a grotesk with a uniquely digital character.",
                family  = SpaceGroteskFamily
            ),
            FontEntry(
                name    = "Cabin",
                tagline = "Humanist sans · solid · trustworthy",
                body    = "Influenced by classic humanist sans-serifs, Cabin is a workhorse that brings warmth and readability to long-form content.",
                family  = CabinFamily
            ),
            FontEntry(
                name    = "Plus Jakarta Sans",
                tagline = "Modern · premium · versatile",
                body    = "A contemporary interpretation of Jakarta's urban energy — precise yet friendly, making it a top pick for modern product design systems.",
                family  = PlusJakartaFamily
            ),
        )
    }

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
                    containerColor = colorScheme.background
                )
            )
        },
        containerColor = colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                horizontal = 20.dp,
                vertical   = 12.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header specimen — full-bleed hero card for Fraunces
            item {
                HeroFontCard(
                    entry      = fontEntries.first(),
                    colors     = colorScheme
                )
                Spacer(Modifier.height(4.dp))
            }

            // Remaining fonts
            items(fontEntries.drop(1)) { entry ->
                SpecimenCard(entry = entry, colors = colorScheme)
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

// ── Hero card — large immersive specimen for Fraunces ────────────────────────
@Composable
private fun HeroFontCard(entry: FontEntry, colors: ColorScheme) {
    Card(
        shape  = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = colors.primaryContainer),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Gradient wash in the card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                colors.primary.copy(alpha = 0.08f),
                                Color.Transparent
                            )
                        )
                    )
            )
            Column(modifier = Modifier.padding(28.dp)) {
                // Eyebrow chip
                Surface(
                    shape  = CircleShape,
                    color  = colors.primary.copy(alpha = 0.18f),
                    modifier = Modifier.clip(CircleShape)
                ) {
                    Text(
                        "★  Featured",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        style = TextStyle(
                            fontFamily = entry.family,
                            fontWeight = FontWeight.SemiBold,
                            fontSize   = 12.sp,
                            color      = colors.primary
                        )
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Large display specimen
                Text(
                    "Fraunces",
                    style = TextStyle(
                        fontFamily = entry.family,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize   = 56.sp,
                        lineHeight = 58.sp,
                        color      = colors.onPrimaryContainer
                    )
                )
                Text(
                    "— a wonky, optical\nsize display face",
                    style = TextStyle(
                        fontFamily = entry.family,
                        fontWeight = FontWeight.Normal,
                        fontStyle  = FontStyle.Italic,
                        fontSize   = 22.sp,
                        lineHeight = 28.sp,
                        color      = colors.onPrimaryContainer.copy(alpha = 0.75f)
                    )
                )

                Spacer(Modifier.height(20.dp))
                HorizontalDivider(color = colors.onPrimaryContainer.copy(alpha = 0.15f))
                Spacer(Modifier.height(16.dp))

                // Weight ladder
                WeightLadder(
                    family = entry.family,
                    color  = colors.onPrimaryContainer
                )

                Spacer(Modifier.height(20.dp))

                // Tag row
                Text(
                    entry.tagline,
                    style = TextStyle(
                        fontFamily = entry.family,
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 11.sp,
                        letterSpacing = 1.8.sp,
                        color = colors.primary
                    )
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    entry.body,
                    style = TextStyle(
                        fontFamily = entry.family,
                        fontWeight = FontWeight.Normal,
                        fontSize   = 15.sp,
                        lineHeight = 22.sp,
                        color      = colors.onPrimaryContainer.copy(alpha = 0.80f)
                    )
                )
            }
        }
    }
}

// ── Regular specimen card ─────────────────────────────────────────────────────
@Composable
private fun SpecimenCard(entry: FontEntry, colors: ColorScheme) {
    // Subtle tap scale animation
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue    = if (pressed) 0.98f else 1f,
        animationSpec  = spring(dampingRatio = 0.6f, stiffness = 500f),
        label          = "cardScale"
    )

    Card(
        shape     = RoundedCornerShape(24.dp),
        colors    = CardDefaults.cardColors(
            containerColor = colors.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier  = Modifier
            .fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(24.dp)) {

            // Name + category row
            Row(
                verticalAlignment    = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        entry.name,
                        style = TextStyle(
                            fontFamily = entry.family,
                            fontWeight = FontWeight.Bold,
                            fontSize   = 22.sp,
                            color      = colors.onSurface
                        )
                    )
                    Text(
                        entry.tagline,
                        style = TextStyle(
                            fontFamily = MaterialTheme.typography.labelSmall.fontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize   = 10.5.sp,
                            letterSpacing = 1.2.sp,
                            color = colors.primary
                        )
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Large display text
            Text(
                "Aa Bb Cc",
                style = TextStyle(
                    fontFamily = entry.family,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize   = 40.sp,
                    lineHeight = 44.sp,
                    color      = colors.onSurface
                )
            )

            Spacer(Modifier.height(10.dp))

            // Weight ladder
            WeightLadder(family = entry.family, color = colors.onSurface)

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.5f))
            Spacer(Modifier.height(14.dp))

            // Body specimen
            Text(
                entry.body,
                style = TextStyle(
                    fontFamily = entry.family,
                    fontWeight = FontWeight.Normal,
                    fontSize   = 14.sp,
                    lineHeight = 21.sp,
                    color      = colors.onSurfaceVariant
                )
            )

            Spacer(Modifier.height(12.dp))

            // Italic line
            Text(
                "The quick brown fox jumps over the lazy dog.",
                style = TextStyle(
                    fontFamily = entry.family,
                    fontWeight = FontWeight.Normal,
                    fontStyle  = FontStyle.Italic,
                    fontSize   = 13.sp,
                    lineHeight = 19.sp,
                    color = colors.onSurfaceVariant.copy(alpha = 0.7f)
                )
            )
        }
    }
}

// ── Weight ladder — shows a stack of weights ─────────────────────────────────
@Composable
private fun WeightLadder(family: FontFamily, color: Color) {
    val weights = listOf(
        FontWeight.Normal     to "Regular",
        FontWeight.Medium     to "Medium",
        FontWeight.SemiBold   to "SemiBold",
        FontWeight.Bold       to "Bold",
        FontWeight.ExtraBold  to "ExtraBold"
    )
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        weights.forEach { (weight, label) ->
            Row(
                verticalAlignment    = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Typography",
                    style = TextStyle(
                        fontFamily = family,
                        fontWeight = weight,
                        fontSize   = 17.sp,
                        color      = color
                    ),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    label,
                    style = TextStyle(
                        fontFamily  = MaterialTheme.typography.labelSmall.fontFamily,
                        fontWeight  = FontWeight.Normal,
                        fontSize    = 10.sp,
                        color       = color.copy(alpha = 0.45f),
                        letterSpacing = 0.5.sp
                    )
                )
            }
        }
    }
}

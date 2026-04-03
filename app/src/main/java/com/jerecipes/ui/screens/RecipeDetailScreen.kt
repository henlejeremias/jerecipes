@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalSharedTransitionApi::class,
)

package com.jerecipes.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.carousel.CarouselDefaults
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.jerecipes.data.model.Ingredient
import com.jerecipes.data.model.Recipe
import com.jerecipes.data.model.RecipeRating
import com.jerecipes.ui.theme.ContainerTransformFadeIn
import com.jerecipes.ui.theme.ContainerTransformFadeOut
import com.jerecipes.ui.theme.ExpressiveSpring
import com.jerecipes.ui.theme.FrauncesFontFamily

internal data class RecipeFact(
    val label: String,
    val value: String,
    val unit: String,
    val icon: ImageVector
)

@Composable
fun RecipeDetailScreen(
    recipe: Recipe,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onBack: () -> Unit,
    onEditClick: () -> Unit,
    onRatingChange: (RecipeRating) -> Unit,
    initialScrollOffset: Int = 0
) {
    val scrollState = rememberScrollState()
    val uriHandler = LocalUriHandler.current

    BackHandler { onBack() }

    LaunchedEffect(initialScrollOffset) {
        if (initialScrollOffset > 0) {
            scrollState.scrollTo(initialScrollOffset)
        }
    }

    with(sharedTransitionScope) {
        Scaffold(

            topBar = {
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        FilledIconButton(
                            onClick = onBack,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.padding(start = 4.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        FilledIconButton(
                            onClick = onEditClick,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Edit")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.surface
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .fillMaxSize()
                    .sharedBounds(
                        rememberSharedContentState(key = "card-${recipe.id}"),
                        animatedVisibilityScope = animatedVisibilityScope,
                        boundsTransform = { _, _ -> ExpressiveSpring },
                        enter = fadeIn(ContainerTransformFadeIn),
                        exit = fadeOut(ContainerTransformFadeOut),
                        clipInOverlayDuringTransition = OverlayClip(RectangleShape)
                    )
            ) {

                with(animatedVisibilityScope) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp)
                ) {
                    AsyncImage(
                        model = recipe.images.firstOrNull(),
                        contentDescription = recipe.title,
                        modifier = Modifier
                            .fillMaxSize()

                            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                            .sharedElement(
                                rememberSharedContentState(key = "image-${recipe.id}"),
                                animatedVisibilityScope = animatedVisibilityScope,
                                boundsTransform = { _, _ -> ExpressiveSpring }
                            ),
                        contentScale = ContentScale.Crop
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .animateEnterExit(
                                enter = fadeIn(ContainerTransformFadeIn),
                                exit  = fadeOut(ContainerTransformFadeOut)
                            )
                            .background(
                                Brush.verticalGradient(
                                    colorStops = arrayOf(
                                        0.0f to Color.Transparent,
                                        0.55f to Color.Transparent,
                                        1.0f to MaterialTheme.colorScheme.surface
                                    )
                                )
                            )
                    )

                    Text(
                        text = recipe.title,
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontFamily = FrauncesFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontStyle = FontStyle.Italic
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(horizontal = 20.dp, vertical = 20.dp)
                            .sharedElement(
                                rememberSharedContentState(key = "title-${recipe.id}"),
                                animatedVisibilityScope = animatedVisibilityScope,
                                boundsTransform = { _, _ -> ExpressiveSpring }
                            )
                    )
                }
                }

                Spacer(Modifier.height(16.dp))
                RecipeFactsCarousel(recipe = recipe)

                if (recipe.ingredients.isNotEmpty()) {
                    Spacer(Modifier.height(20.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        recipe.ingredients.forEach { ingredient ->
                            IngredientPillRow(ingredient = ingredient)
                        }
                    }
                }

                if (recipe.instructions.isNotEmpty()) {
                    Spacer(Modifier.height(20.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        recipe.instructions.forEach { step ->
                            Surface(
                                shape = MaterialTheme.shapes.extraLarge,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = step,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))
                RatingButtonGroup(
                    currentRating = recipe.parsedRating,
                    onRatingChange = onRatingChange,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )

                if (recipe.source != null) {
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        TextButton(
                            onClick = { uriHandler.openUri(recipe.source) },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Icon(
                                Icons.AutoMirrored.Outlined.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "View original source",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }

                Spacer(Modifier.height(80.dp))
            }
        }
    }
}

@Composable
internal fun RecipeFactsCarousel(recipe: Recipe) {
    val facts = buildList {
        recipe.calories?.let {
            add(RecipeFact("Calories", "$it", "kcal", Icons.Outlined.LocalFireDepartment))
        }
        recipe.prepTime?.let {
            add(RecipeFact("Prep Time", "$it", "min", Icons.Outlined.Timer))
        }
        recipe.waitTime?.let {
            add(RecipeFact("Wait Time", "$it", "min", Icons.Outlined.HourglassBottom))
        }
        recipe.protein?.let {
            add(RecipeFact("Protein", "$it", "g", Icons.Outlined.FitnessCenter))
        }
        recipe.carbs?.let {
            add(RecipeFact("Carbs", "$it", "g", Icons.Outlined.Grain))
        }
        recipe.fat?.let {
            add(RecipeFact("Fat", "$it", "g", Icons.Outlined.WaterDrop))
        }
    }

    if (facts.isEmpty()) return

    val carouselState = rememberCarouselState { facts.size }

    HorizontalMultiBrowseCarousel(
        state = carouselState,
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .height(170.dp),
        preferredItemWidth = 150.dp,
        itemSpacing = 12.dp,
        flingBehavior = CarouselDefaults.multiBrowseFlingBehavior(state = carouselState)
    ) { index ->
        val fact = facts[index]
        val containerColor = MaterialTheme.colorScheme.secondaryContainer
        val contentColor = MaterialTheme.colorScheme.onSecondaryContainer

        Surface(
            modifier = Modifier
                .fillMaxHeight()
                .maskClip(MaterialTheme.shapes.extraLarge),
            color = containerColor,
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.Start
            ) {

                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = contentColor.copy(alpha = 0.12f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = fact.icon,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Column {
                    Text(
                        text = fact.label.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        ),
                        color = contentColor.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = fact.value,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Black
                            ),
                            color = contentColor
                        )
                        Text(
                            text = fact.unit,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = contentColor.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IngredientPillRow(ingredient: Ingredient) {
    val amountStr = ingredient.amount?.let {
        if (it == it.toLong().toDouble()) it.toLong().toString()
        else it.toString()
    } ?: ""
    val unitStr = ingredient.unit ?: ""
    val quantityLabel = buildString {
        if (amountStr.isNotEmpty()) append(amountStr)
        if (amountStr.isNotEmpty() && unitStr.isNotEmpty()) append(" ")
        if (unitStr.isNotEmpty()) append(unitStr)
    }
    val hasQuantity = quantityLabel.isNotEmpty()

    val outerHeight = 56.dp
    val innerHeight = 36.dp
    val concentricPadding = (outerHeight - innerHeight) / 2

    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .fillMaxWidth()
            .height(outerHeight)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = 24.dp,
                    end = concentricPadding
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            Text(
                text = ingredient.name,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            if (hasQuantity) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.height(innerHeight)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = quantityLabel,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            } else {

                Spacer(modifier = Modifier.size(innerHeight))
            }
        }
    }
}

@Composable
private fun RatingButtonGroup(
    currentRating: RecipeRating,
    onRatingChange: (RecipeRating) -> Unit,
    modifier: Modifier = Modifier
) {
    data class RatingOption(val rating: RecipeRating, val label: String, val icon: ImageVector)

    val options = listOf(
        RatingOption(RecipeRating.TOP,  "top",  Icons.Outlined.Favorite),
        RatingOption(RecipeRating.GOOD, "good", Icons.Outlined.ThumbUp),
        RatingOption(RecipeRating.MID,  "mid",  Icons.Outlined.SentimentNeutral),
        RatingOption(RecipeRating.NEW,  "new",  Icons.Outlined.QuestionMark),
    )

    Column(modifier = modifier.fillMaxWidth()) {

        Surface(
            modifier = Modifier.fillMaxWidth().height(64.dp),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                options.forEach { opt ->
                    val isSelected = currentRating == opt.rating

                    val cornerSize by animateDpAsState(
                        targetValue = if (isSelected) 30.dp else 12.dp,
                        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
                        label = "shapeMorph"
                    )

                    Surface(
                        modifier = Modifier
                            .weight(if (isSelected) 1.5f else 1f)
                            .fillMaxHeight()
                            .clickable { onRatingChange(opt.rating) },
                        shape = RoundedCornerShape(cornerSize),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            Color.Transparent
                        }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = opt.icon,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                       else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            AnimatedVisibility(visible = isSelected) {
                                Row {
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        opt.label,
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


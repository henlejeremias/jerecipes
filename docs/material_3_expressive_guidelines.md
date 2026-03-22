# Material 3 Expressive Guidelines for Jerecipes

To achieve the "premium native Google application" look requested, Jerecipes relies exclusively on official Material 3 guidelines using the **Expressive** theme dimension, which employs more dramatic proportions, dynamic colors, and high-quality motion. 

## 1. Dynamic Color and Theming
- **Dynamic scheme**: Always extract colors from the user's wallpaper (`dynamicColorScheme` in Jetpack Compose).
- No hardcoded branding colors; fallback to a neutral, harmonic baseline (e.g., Google's baseline tonal palettes) if dynamic colors are unavailable.
- **Expressive Tokens**: Make use of `surfaceContainerHighest` and `secondaryContainer` for cards to create depth rather than relying on heavy shadows.

## 2. Expressive Typography
- Use the standard modern fonts (usually Roboto/Google Sans, but rely on the system M3 typography tokens).
- Use **Display** sizes (`displayLarge`, `displayMedium`) for prominent app bar titles ("Recipe Library" screen).
- Make sure spacing values correspond with M3 Expressive layout (e.g., margins often jump to 24dp or 32dp instead of 16dp).

## 3. Motion & Animations (Core Requirement)
Spring animations are a strict requirement for the M3 Expressive showcase context.
- **Container Transform**: When clicking a recipe in the "Recipe Library with Motion", the card MUST seamlessly expand into the "Recipe Detail" screen using a `SharedTransitionLayout` / Container Transform pattern. 
- **Shared Axis Transform**: Use the X-axis or Z-axis transform when entering the "Edit Recipe" view or navigating to different primary destinations.
- **Spring Specs**: Avoid standard `tween`s. Use `spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioNoBouncy)` for smooth, physically-based easing on elements such as buttons, floating action buttons, and modal bottom sheets.

## 4. Haptics
Rigorous haptic feedback sets a premium app apart from standard ones.
- **Click Actions**: Use `HapticFeedbackType.TextHandleMove` or similar light vibrations for standard toggle switches and icons.
- **Long Press / Drag**: Use `HapticFeedbackType.LongPress` when initiating a drag-and-drop on list items (e.g. reordering recipe steps).
- **Completion**: Trigger a noticeable haptic click when a recipe is successfully generated via Gemini and saved.

## 5. Components
- Favor large, pill-shaped FABs (`ExtendedFloatingActionButton`) and prominent Chip groups for filtering ingredients or sources.
- Do not use proprietary UI elements (like iOS-style toggles or custom dialogs). Stick to standard AlertDialogs, BottomSheets, and M3 TopAppBars.

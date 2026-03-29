# Jerecipes Design System & Application Specification

## 1. Project Identity
**Project Name:** Jerecipes
**Tagline:** "Your Culinary Life."
**Core Purpose:** A high-fidelity Android application for managing a personal recipe collection, featuring AI-powered recipe parsing (leveraging Gemini) and a premium native UI.

---

## 2. Design Foundation (Tokens)

The application adheres strictly to **Material 3 (M3)** standards, utilizing the latest adaptive color schemes and expressive motion.

### 2.1 Color Palette
Jerecipes uses a standard Material 3 color scale centered around Purple and Pink accents.

| Token | Light Mode Value | Dark Mode Value | Usage |
| :--- | :--- | :--- | :--- |
| **Primary** | `#6650a4` (Purple 40) | `#D0BCFF` (Purple 80) | Key buttons, active states, branding titles. |
| **Secondary** | `#625b71` (PurpleGrey 40) | `#CCC2DC` (PurpleGrey 80) | Secondary actions, Chips, inner pill backgrounds. |
| **Tertiary** | `#7D5260` (Pink 40) | `#EFB8C8` (Pink 80) | Accent highlights. |
| **Surface** | Material 3 Neutral | Material 3 Neutral | Base layer of the application. |
| **Surface Container**| Varying elevations | Varying elevations | Cards, search bars, and input fields. |

### 2.2 Typography
The app uses the standard **Material 3 Typography Scale** for consistent hierarchy. All fonts are system-default for a native "Pixel-perfect" feel.

- **Display Small/Medium:** Used for branding ("Jerecipes") with **ExtraBold** weight and `-1.sp` letter spacing on the Library screen.
- **Headline Small/Medium:** Used for screen titles and recipe titles in cards/details.
- **Title Large:** Used for section headers like "Ingredients" or "Instructions."
- **Body Large:** Used for secondary descriptions and search bar input.
- **Label Small:** Used for metadata like "CAL" chips and legal footers.

### 2.3 Shapes & Radius
- **Major Components (Cards, Bottom Sheets):** `24.dp` or `28.dp` rounded corners.
- **Micro Components (Chips, Search Bar, Pill Editors):** `CircleShape` (fully rounded).
- **Floating Action Button (FAB):** `RoundedCornerShape(28.dp)`.

### 2.4 Motion & Transitions
Jerecipes features **Expressive Motion** using Shared Element Transitions:
- **Spring Spec:** `DampingRatio = 0.8f`, `Stiffness = 380f` (referred to as `ExpressiveSpring`).
- **Shared Elements:** Recipe images, titles, and card containers morph seamlessly between the Library and Detail screens.

---

## 3. Screen Specifications

### 3.1 Login Screen
A clean, centered splash-into-auth screen.
- **Top:** A large `RestaurantMenu` icon in Primary color.
- **Center:** "Jerecipes" in `DisplayMedium` font, followed by the tagline in `TitleLarge` (OnSurfaceVariant color).
- **Interactive:** A `Card` containing the text "Sign in to your kitchen" and a full-width `Button` labeled "Continue with Google."
- **Footer:** Tertiary legal text aligned to the bottom.

### 3.2 Recipe Library (Main)
The central hub for the user's culinary collection.
- **Large Top App Bar:** Features the "Jerecipes" logo on the left and a circular profile photo (or `Person` icon) on the right. Collapses into a thin bar on scroll.
- **Search Bar:** A custom floating pill (`64.dp` height) with a `Search` icon. It uses a high-elevation shadow (`12.dp`) with a primary-colored ambient tint for a glassmorphic effect.
- **Grid Layout:** An adaptive vertical grid (minimum column width `300.dp`) with `24.dp` spacing.
- **Empty State:** A large centered layout with a Circular Person icon, "Your collection is empty" headline, and a helpful onboarding description.
- **Large FAB:** A primary-colored button with a large `Add` icon for creating new recipes.

### 3.3 Recipe Card
Individual units within the Library grid.
- **Aspect Ratio:** `4:5` vertical aspect ratio.
- **Visuals:** A full-bleed image with `24.dp` rounded corners. If no image exists, a `surfaceVariant` background with a central `Restaurant` icon is shown.
- **Metadata Chip:** A semi-transparent white pill in the `BottomStart` of the image showing calories (e.g., "500 CAL").
- **Content:** The recipe title in `HeadlineSmall` (bold) and a 2-line comment/description in `BodySmall`.

### 3.4 Recipe Detail Screen
Full-screen view of a specific recipe.
- **Header:** Full-width hero image (`300.dp` height) that morphs from the library card.
- **Top Bar:** Simple back button, "Recipe Details" title, and an `Edit` action icon.
- **Metrics Row:** `AssistChips` for prep time (e.g., "20m Prep") and calories.
- **Content Sections:**
    - **Ingredients:** Bulleted list using `•` symbols.
    - **Instructions:** Numbered list (1, 2, 3...).
- **Action:** A "View Original Source" button (primary style) with an `OpenInNew` icon at the bottom.

### 3.5 Create Recipe Bottom Sheet
A modal interface triggered by the FAB.
- **Title:** "New Recipe" headline.
- **Input:** A large `OutlinedTextField` for pasting URLs or raw text.
- **Media Upload:** An `OutlinedButton` ("Add Photo or Screenshot") that triggers a system image picker. Shows a `200.dp` preview once a photo is selected.
- **AI Action:** A primary button labeled "Create Recipe" with an `AutoAwesome` icon. Displays a `CircularProgressIndicator` while Gemini parses the content.

### 3.6 Edit Recipe Screen
A detailed form-based screen for manual adjustments.
- **Hero Edit:** A `200.dp` image preview at the top. Tapping it re-opens the image picker.
- **Text Fields:** `OutlinedTextFields` for Title, Comment, and Source URL.
- **Ingredient Pill Editor:** A custom layout for ingredients.
    - An outer pill (`surfaceVariant`) containing the ingredient name.
    - An inner nested pill (`secondaryContainer`) on the right containing the amount and unit.
    - A trailing `Delete` icon.
- **Instructions:** Individual `OutlinedTextFields` per step, each with a `Delete` button.
- **Top Bar:** A "Close" icon on the left and a "Save" text button on the right.

---

## 4. Technical Features & Logic
- **Cloud Integration:** Real-time synchronization with Firestore.
- **Authentication:** Firebase Google Sign-In.
- **AI Engine:** Integration with Vertex AI / Gemini for structured recipe extraction from unstructured text or images.
- **Image Handling:** Coil-based async image loading with hardware/software bitmap decoding support for various Android versions.

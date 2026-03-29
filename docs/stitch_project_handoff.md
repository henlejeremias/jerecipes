# Jerecipes Project Overview & Handoff Document

This document provides a comprehensive breakdown of the **Jerecipes** Android application for integration with Stitch.

## 1. Project Mission
Jerecipes is a premium, AI-powered recipe companion. Its primary goal is to eliminate the friction of manual data entry by using Gemini to instantly parse recipes from screenshots, URLs, or casual text, and synchronizing them across devices using Firebase.

---

## 2. Core Features
- **Magic Parsing:** Uses Gemini 1.5 Flash to extract structured recipe data (ingredients, instructions, nutrition) from any unstructured input.
- **Visual Collection:** Multi-modal image support with Firebase Storage integration.
- **Smart Organization:** Real-time synchronization with Firestore and local caching.
- **Shared Transitions:** High-fidelity animations between the Library and Detail views.

---

## 3. Screen-by-Screen Breakdown

### A. Login Screen (`LoginScreen.kt`)
- **Vibe:** Clean, minimalist welcome.
- **Elements:** 
  - Application branding ("Jerecipes").
  - "Continue with Google" button.
  - Subtle background gradients (Material 3 Surface).
- **Implementation:** Firebase Authentication (Google ID Provider).

### B. Recipe Library (`RecipeLibraryScreen.kt`)
- **Vibe:** Editorial, magazine-style grid.
- **Layout:** `LazyVerticalGrid` with adaptive columns (min 300dp).
- **Top Bar:** `LargeTopAppBar` with "Jerecipes" in ExtraBold, letter-spaced -1sp.
- **Search:** A custom `RecipeSearchBar` with high shadow elevation (12.dp), glassmorphic/surface-highest color, and a circular shape.
- **Cards (`RecipeCard`):** 
  - 4:5 aspect ratio images.
  - Floating "CAL" metric pill.
  - Shared element transitions for images and titles.
- **FAB:** Large Floating Action Button for adding new recipes.

### C. Recipe Detail (`RecipeDetailScreen.kt`)
- **Vibe:** Focus on imagery and clarity.
- **Hero Image:** 300dp height, `ContentScale.Crop`, shared from the card.
- **Metrics:** `AssistChip` rows for Prep Time and Calories.
- **Lists:** Bulleted ingredients and numbered instructions.
- **Action:** Primary button to "View Original Source" (opening URLs in a browser).

### D. Create Recipe Bottom Sheet (`CreateRecipeBottomSheet.kt`)
- **Vibe:** Input-focused, functional.
- **Input:** Large `OutlinedTextField` for links or text blobs.
- **Media:** "Add Photo or Screenshot" button that launches the system image picker.
- **Action:** "Create Recipe" button with `CircularProgressIndicator` during AI parsing.

### E. Edit Recipe Screen (`EditRecipeScreen.kt`)
- **Vibe:** Heavy data entry made easy.
- **Media Edit:** Tappable hero image to replace photos.
- **Dynamic Lists:** 
  - `IngredientPillEditor`: A custom horizontal pill with separate fields for Name and Amount/Unit.
  - `Instructions`: Deletable text fields for each step.

---

## 4. Design System & Tokens (Current)

### Typography
- **Headlines:** Material 3 `displaySmall` (ExtraBold) for branding.
- **Body:** Standard Material 3 `bodyMedium`/`bodyLarge`.
- **Labels:** Bold `labelSmall` for metric pills.

### Color Palette
- **Primary:** Material 3 Primary (`#D0BCFF` system default or theme-defined).
- **Surface:** Uses `surfaceContainerHighest` for interactive elements like the Search Bar.
- **Accents:** High-contrast White/Primary for floating labels over imagery.

### Shapes
- **Extra Large:** 28.dp for FABs.
- **Large:** 24.dp for Recipe Cards and Bottom Sheets.
- **Full:** `CircleShape` for pills, avatars, and search input.

---

## 5. Technical Implementation Details

| Layer | Technology |
| :--- | :--- |
| **Logic** | Kotlin Coroutines, Flow, ViewModel |
| **UI** | Jetpack Compose (Material 3) |
| **AI** | Google AI SDK (Gemini 1.5 Flash) |
| **Database** | Firebase Firestore |
| **Auth** | Firebase Auth (Google Sign-In) |
| **Storage** | Firebase Storage (JPEG 85%, scaled to 1600px) |
| **Images** | Coil (AsyncImage) |

---

## 6. Image Pipeline Logic
1.  **Capture:** UI picks `Uri`.
2.  **Decode:** Decoded to `Bitmap` (Software Allocator).
3.  **Process:** Sent to Gemini for data extraction (JSON format).
4.  **Upload:** Scaled to 1600px max dimension, compressed to JPEG, uploaded to `recipes/{userId}/{uuid}.jpg`.
5.  **Record:** Download URL saved to Firestore `images` array.
6.  **Display:** Coil loads the URL with cache support.

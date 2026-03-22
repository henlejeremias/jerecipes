# Android Studio Panda 2 Bridge for Jerecipes

This document serves as the prompt/guide you will hand over to **Android Studio Panda 2** (the IDE AI) when initiating the codebase.

## 1. Project Initialization
Tell Panda 2: 
> "I am starting 'Jerecipes', a premium Material 3 Expressive Android application in Jetpack Compose. Initialize a new empty Compose Activity project targeting Android 14 (API 34) minimum or using the latest Material 3 libraries."

## 2. Using the Stitch Prototypes
The design prototypes in the `jerecipes/design/` workspace are currently structured as HTML/CSS artifacts.
Instruct Panda 2: 
> "In my workspace under `design/`, there are HTML equivalents of 4 screens: 'Recipe Library with Motion', 'Edit Recipe Refined', 'Recipe Detail with Motion', and 'Recipe Detail Refined'. Please study the screenshots and HTML structure to replicate these pixel-perfect screens in Jetpack Compose. Adhere strictly to the Expressive Material 3 tokens."

- **Panda 2 Workflow**: Panda will analyze the DOM of the Stitch output and translate it into `@Composable` functions. It will reference the `screenshot.png` files for visual validation of spacing and proportion.

## 3. Firebase & Authentication Setup
Instruct Panda 2 to set up the backend connections:
- **Dependencies**: Add `firebase-firestore-ktx`, `firebase-auth-ktx`, and `firebase-storage`.
- **Auth Flow**: Implement Google Sign-In as the primary mechanism using `Credential Manager` (the modern Android standard). Add email/password fallback if Google Services aren't available on the vibe-coded device.
- **Data Layer**: Create Kotlin Data Classes reflecting the schemas identified in `firebase_schema.md`. Use Firebase asynchronous coroutine tasks to retrieve and persist recipes.

## 4. Animation Directives
When having Panda 2 build the UI, explicitly remind it:
> "Implement Shared Element Transitions (Container Transforms) between the 'Recipe Library' cards and the 'Recipe Detail' screen, using low-stiffness, non-bouncy spring physics."

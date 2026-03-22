# App Setup and Architecture Plan for Jerecipes

We are setting up the groundwork for the "Jerecipes" Android application, which will serve as a heavily stylized, Material 3 Expressive showcase recipe collector.

## Proposed Setup

### Backend & Cloud Infrastructure
We will use your existing Firebase project (`jerecipes-3b135`).
- **Authentication**: Set up Firebase Authentication. We will prioritize Google Sign-In as it's the standard for Android native apps, and configure standard Email/Password as a fallback or alternative for vibe-coded flexibility.
- **Firestore DB**: 
  - `users` collection: storing user preferences or profile data.
  - `recipes` collection schema: 
    - `id` (String)
    - `title` (String)
    - `ingredients` (Array of objects: name, amount, unit — mapped so commodities like salt/pepper don't require amounts)
    - `instructions` (Array of Strings or single formatted text block)
    - `source` (String - URL, YouTube link, or freeform text)
    - `images` (Array of URIs pointing to Cloud Storage)
    - `comment` (String)
- **Storage**: Firebase Cloud Storage will handle the optional images uploaded for recipes.

### Material 3 Expressive UI Principles
Because this project must look like a showcase for Material 3 Expressive, the bridging documentation for Panda 2 will emphasize:
- **Color System**: Enforcing Android 12+ dynamic colors (`MaterialTheme.colorScheme`).
- **Motion & Haptics**: Heavily relying on Container Transforms when opening a detailed view from the recipe library. We will specify rigorous haptics and spring-based animations to achieve a premium "native Google app" feel.
- **Design Elements**: Strict usage of Material Design 3 components without proprietary overrides, scaled with Expressive typography (larger headings, extra spacing, pill-shaped cards/chips).

### Gemini API Integration Strategy
- **Ingestion Flow**: We will define the prompt structure and data flow using a model from the **Gemini 3 family**. Multimodality (URL, string, or image in a "creation text box") will be handled by sending the payload directly to the Gemini Vision/Pro API, which will parse it into the standardized JSON format defined by our Firestore `recipes` schema.
- **Editing Flow**: Users will be able to override or edit the parsed results in the "edit recipe view."

# Firebase Schema for Jerecipes

This document outlines the Firestore NoSQL schema for the Jerecipes app.

## Collection: `users`
Stores user-specific settings. Authenticated via Google Sign-In or Firebase Auth.
**Document ID:** `uid` (from Firebase Auth)
- `displayName`: string
- `email`: string
- `createdAt`: timestamp

## Collection: `recipes`
The core collection storing recipes.
**Document ID:** Auto-generated ID, or URL hash if ingested uniquely.
- `title`: string (e.g., "Grandma's Apple Pie")
- `ingredients`: array of maps
  - `name`: string ("Flour", "Salt")
  - `amount`: number | null (e.g. 500, or null for commodities like salt/pepper)
  - `unit`: string | null ("g", "tbsp", or null)
- `instructions`: string (Markdown formatted text block, or simple newlines)
- `source`: string (URL to blog, YouTube link, or freeform text like "from Mum")
- `images`: array of strings (Firebase Storage URLs or original URIs)
- `comment`: string (User's personal comment, what to pay attention to)
- `createdBy`: string (Reference to a `users` uid)
- `createdAt`: timestamp

## Security Rules (Draft)
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /recipes/{recipeId} {
      // Allow read/write only if the recipe belongs to the authenticated user.
      allow read, write: if request.auth != null && resource.data.createdBy == request.auth.uid;
      // Allow create if assigning to self
      allow create: if request.auth != null && request.resource.data.createdBy == request.auth.uid;
    }
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

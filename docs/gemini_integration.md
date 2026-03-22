# Gemini API Integration for Jerecipes

This document outlines the strategy for handling multi-modal recipe ingestion (URLs, Free-form Text, Images) using the Gemini 3 API family. 

## Model Choice
- **Primary Model**: `gemini-3-pro` (or equivalent stable Gemini 3 vision-capable model).
- The Gemini 3 family natively supports multimodal inputs (text, images, and embedded URLs/video links if supported by the SDK context).

## Multimodal Handling Strategy
When the user submits data to the "Creation Text Box", the app will package the payload for the API based on what it is:
1. **Plain Text / URL**: Sent as a standard text `Part` in the request.
2. **Screenshots / Images**: Converted to base64, resized to an optimal dimension to save tokens (e.g. max 1024x1024), and sent as an `inline_data` `Part` with the corresponding mime type (e.g., `image/jpeg`).
3. **Combination**: If the user pastes a picture AND writes "from Mum", BOTH are sent as sequential parts in the same request string.

## System Prompt Definition
You must enforce a strict JSON schema output by using **Structured Outputs** (or `response_mime_type: "application/json"` with a schema).

**Prompt Instructions**:
```text
You are the parsing engine for "Jerecipes", a recipe collection application.
Your task is to extract a recipe from the provided input (which may be a screenshot of a cookbook, a URL, a pasted blog post, or freeform text). 
Format the output EXACTLY matching the required JSON schema. 

Rules:
1. "title": Provide a concise, appealing name for the recipe.
2. "ingredients": Extract all ingredients. If it is a commodity like "salt to taste", "pepper", "olive oil for frying", leave the "amount" and "unit" fields null. Otherwise, extract the numeric amount and the unit.
3. "instructions": Provide a clean, sequentially numbered markdown string for the recipe steps.
4. "source": Extract the URL if provided, or the contextual source (e.g., "From a photo of a book").
```

**JSON Schema Representation**:
Request that the model output strictly adheres to the Firestore `recipes` schema array types.

## Fallback and Editing
Because AI extraction isn't flawless:
- Once Gemini returns the JSON, do NOT save it directly to the database.
- Instead, populate the "Edit Recipe Refined" screen (from the Stitch prototype) with the JSON data to let the user review, adjust amounts, and fix any hallucinations.
- The user taps "Save" to commit it to Firestore.

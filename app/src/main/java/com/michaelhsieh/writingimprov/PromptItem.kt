package com.michaelhsieh.writingimprov

import java.io.Serializable

/**
 * One possible practice prompt.
 * When user does individual practice, the app randomly selects one of these prompt items
 * to display to the user.
 */
data class PromptItem(
    // Need to initialize fields to get objects from Firestore
    // Or will get error:
    // Could not deserialize object.
    // Class com.example.dotdot.Member does not define a no-argument constructor.
    // If you are using ProGuard, make sure these constructors are not stripped

    // A unique ID, generated when prompt was created
    val id:String = "",

    // The prompt text
    val prompt: String = "",

    // Timestamp when this prompt was created. UNIX time in seconds
    val timestamp:Long = 0
):Serializable {
    // Serializable to pass object between Fragments

    // To debug
    override fun toString(): String {
        return "id: $id, prompt: $prompt, timestamp: $timestamp"
    }
}
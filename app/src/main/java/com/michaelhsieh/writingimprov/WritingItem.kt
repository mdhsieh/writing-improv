package com.michaelhsieh.writingimprov

import java.io.Serializable

/**
 * One piece of user-submitted writing represented in
 * My Writing RecyclerView row.
 */
data class WritingItem(
    // Need to initialize fields to get objects from Firestore
    // Or will get error:
    // Could not deserialize object.
    // Class com.example.dotdot.Member does not define a no-argument constructor.
    // If you are using ProGuard, make sure these constructors are not stripped

    // A unique ID, generated when writing was submitted
    val id:String = "",
    // The title of the piece of writing,
    // either [Challenge from currentUsername] or [Challenge sent to receiverUsername]
    var name: String = "",

    val prompt: String = "",

    val time: String = "",

    val thumbUrl: String = "",

    val url: String = "",

    val writing:String = "",

    // String ID of the challenge this writing came from,
    // or empty String if writing is practice instead
    val challengeId:String = ""
):Serializable {
    // Serializable to pass object between Fragments

    // To debug
    override fun toString(): String {
        return "id: $id, name: $name, prompt: $prompt, time: $time, url: $url, thumbnail url: $thumbUrl, writing: $writing, challenge id: $challengeId"
    }
}
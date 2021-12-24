package com.michaelhsieh.writingimprov

import java.io.Serializable

/**
 * One challenge to an author. Text prompt with image
 */
data class ChallengeItem(
    // Need to initialize fields to get objects from Firestore
    // Or will get error:
    // Could not deserialize object.
    // Class com.example.dotdot.Member does not define a no-argument constructor.
    // If you are using ProGuard, make sure these constructors are not stripped

    // A unique ID, generated when challenge was sent
    val id:String = "",
    // The title of the challenge
    val name: String = "",

    val prompt: String = "",

    val time: String = "",

    val thumbUrl: String = "",

    val url: String = "",

//    @field:JvmField // use this annotation if your Boolean field is prefixed with 'is'
//    val isCompleted:Boolean = false
    val completed:Boolean = false,

    // email of whoever sent the challenge
    val senderId:String="",

    // email of whoever is receiving the challenge
    val receiverId:String=""
):Serializable {
    // Serializable to pass object between Fragments

    // To debug
    override fun toString(): String {
        return "id: $id, name: $name, prompt: $prompt, time: $time, url: $url, thumbnail url: $thumbUrl, is completed: $completed, sender: $senderId, receiver: $receiverId"
    }
}
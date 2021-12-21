package com.michaelhsieh.writingimprov

import java.io.Serializable

/**
 * One author the user can challenge with a prompt represented in
 * AuthorsFragment RecyclerView row.
 */
data class AuthorItem(
    // Need to initialize fields to get objects from Firestore
    // Or will get error:
    // Could not deserialize object.
    // Class com.example.dotdot.Member does not define a no-argument constructor.
    // If you are using ProGuard, make sure these constructors are not stripped

    // A unique ID, using author email
    val id:String = "",
    // The author's username
    val name: String = ""

):Serializable {
    // Serializable to pass object between Fragments

    // To debug
    override fun toString(): String {
        return "id: $id, name: $name"
    }
}
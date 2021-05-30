package com.michaelhsieh.writingimprov

import java.io.Serializable

/**
 * One piece of user-submitted writing represented in
 * My Writing RecyclerView row.
 */
data class WritingItem(
    // The title of the piece of writing
    val name: String,

    val prompt: String,

    val time: String,

    val url: String,

    val writing:String
):Serializable {
    // Serializable to pass object between Fragments

    // To debug
    override fun toString(): String {
        return "name: $name, prompt: $prompt, time: $time, url: $url, writing: $writing"
    }
}
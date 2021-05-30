package com.michaelhsieh.writingimprov

import java.io.Serializable

data class WritingItem(
    val name: String,

    val prompt: String,

    val time: String,

    val thumbUrl: String
):Serializable {
    // Serializable to pass object between Fragments

    // To debug
    override fun toString(): String {
        return "name: $name, prompt: $prompt, time: $time, thumb url: $thumbUrl"
    }
}
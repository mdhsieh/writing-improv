package com.michaelhsieh.writingimprov.httprequest

import retrofit2.Call
import retrofit2.http.GET

interface JsonRandomWordsAPI {
    // Returning a list here because
    // response is an array containing a single JSON object
    @GET("word")
    fun getRandomWord(): Call<List<RandomWord>>
}
package com.michaelhsieh.writingimprov.httprequest

import retrofit2.Call
import retrofit2.http.GET

interface JsonUnsplashApi {

    @GET("photos/random")
    fun getRandomImage(): Call<List<UnsplashImage>>

}
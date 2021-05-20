package com.michaelhsieh.writingimprov.httprequest

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface JsonUnsplashApi {

    // client ID is developer access key
    @GET("photos/random")
    fun getRandomImage(@Query("client_id") id:String): Call<UnsplashImage>

}
package com.michaelhsieh.writingimprov.httprequest

import com.google.gson.JsonObject

data class UnsplashImage (
    val id:String,

    val urls:JsonObject,

    val width:Double,

    val height:Double
    )

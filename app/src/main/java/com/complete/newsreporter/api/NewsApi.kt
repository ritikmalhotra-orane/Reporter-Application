package com.complete.newsreporter.api

import com.complete.newsreporter.model.NewsResponse
import com.complete.newsreporter.modelX.SosResponse
import com.complete.newsreporter.utils.Constants.Companion.API_KEY
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface NewsApi {

    @GET("v2/top-headlines")
    suspend fun getBreakingNews(
        @Query("country")
        countryCode : String = "in",
        @Query("page")
        page:Int = 1,
        @Query("apiKey")
        apiKey:String=API_KEY
    ): Response<NewsResponse>

    @GET("v2/everything")
    suspend fun searchForNews(
        @Query("q")
        searchQuery : String,
        @Query("page")
        page:Int = 1,
        @Query("apiKey")
        apiKey:String=API_KEY
    ): Response<NewsResponse>

    @GET("https://emergencynumberapi.com/api/country/in")
    suspend fun getNumber() : Response<SosResponse>
}
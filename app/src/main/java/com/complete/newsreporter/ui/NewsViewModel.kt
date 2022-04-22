package com.complete.newsreporter.ui

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.ConnectivityManager.*
import android.net.NetworkCapabilities.*
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.complete.newsreporter.NewsApplication
import com.complete.newsreporter.database.NewsRepository
import com.complete.newsreporter.model.Article
import com.complete.newsreporter.model.NewsResponse
import com.complete.newsreporter.modelX.SosResponse
import com.complete.newsreporter.utils.Resources
import kotlinx.coroutines.launch
import retrofit2.Response
import java.io.IOException

class NewsViewModel (app: Application,val repository: NewsRepository):AndroidViewModel(app){
    val breakingNews:MutableLiveData<Resources<NewsResponse>> = MutableLiveData()
    var searchedQuery:MutableLiveData<Resources<NewsResponse>> = MutableLiveData()
    val numberReponse:MutableLiveData<Resources<SosResponse>> = MutableLiveData()

    var breakingNewsPage = 1
    var searchedPageNumber = 1
    var breakingNewsResponse: NewsResponse? =null
    var searchNewsResponse: NewsResponse? =null
    init{
        getBreakingNews("in")
        getSearchedNews("headlines")
    }
    fun getNumber() = viewModelScope.launch {
        numberReponse.postValue(Resources.Loading())
        val numbers = repository.getNumber()
        numberReponse.postValue(handleSosReponse(numbers))
    }

    private fun handleSosReponse(response: Response<SosResponse>):Resources<SosResponse>{
        if(response.isSuccessful){
            response.body()?.let{ resultResponse->
                return Resources.Success(resultResponse)
            }
        }
        return Resources.Error(response.message())
    }

    fun getBreakingNews(countryCode:String) = viewModelScope.launch {
        breakingNews.postValue(Resources.Loading())
        safeBreakingCall(countryCode)
    }
    fun getSearchedNews(searchQuery:String) = viewModelScope.launch {
        searchedQuery.postValue(Resources.Loading())
        safeSearchCall(searchQuery)
    }
    fun getSavedNews() = repository.getAll()
    fun saveNews(article: Article) = viewModelScope.launch {
        repository.upsert(article)
    }
    fun deleteNews(article:Article) = viewModelScope.launch {
        repository.delete(article)
    }

    private fun handleBreakingNewsResponse(response:Response<NewsResponse>):Resources<NewsResponse>{
        if(response.isSuccessful){
            response.body()?.let{ resultResponse->
                if(breakingNewsResponse == null){
                    breakingNewsResponse = resultResponse
                }else{
                    breakingNewsPage++;
                    val oldList = breakingNewsResponse!!.articles
                    val newList = resultResponse.articles
                    oldList.addAll(newList)
                }

                // breakingNewsResponse ?: resultResponse = this indicates that if
                // breakingNewsRespponse is not null then it will be returned and
                // if its null then only resultResponse is returned
                return Resources.Success(breakingNewsResponse!! )
                //?: resultResponse
            }
        }
        return Resources.Error(response.message())
    }
    private fun handleSearchedNewsResponse(response:Response<NewsResponse>):Resources<NewsResponse>{
        if(response.isSuccessful){
            response.body()?.let{ resultResponse->
                return Resources.Success(searchNewsResponse ?: resultResponse)
            }
        }
        return Resources.Error(response.message())
    }

    private suspend fun safeBreakingCall(countryCode:String){
        try{
            if(hasConnection()){
                val response = repository.getBreakingNews(countryCode,breakingNewsPage)
                breakingNews.postValue(handleBreakingNewsResponse(response))
            }else{
                breakingNews.postValue(Resources.Error("No Internet Connection"))
            }
        }catch(t:Throwable){
            when(t){
                is IOException -> breakingNews.postValue(Resources.Error("Network Failed"))
                else -> breakingNews.postValue(Resources.Error("conversion error"))
            }
        }
    }
    private suspend fun safeSearchCall(searchQuery:String){

        searchedQuery.postValue(Resources.Loading())
        try{
            if(hasConnection()){
                val response = repository.getSearchQuery(searchQuery,searchedPageNumber)
                searchedQuery.postValue(handleSearchedNewsResponse(response))
            }else{
                searchedQuery.postValue(Resources.Error("No Internet Connection"))
            }
        }catch(t:Throwable){
            when(t){
                is IOException -> searchedQuery.postValue(Resources.Error("Network Failed"))
                else -> searchedQuery.postValue(Resources.Error("conversion error"))
            }
        }
    }
    private fun hasConnection():Boolean{
        val connectivityManager = getApplication<NewsApplication>().getSystemService(
            Context.CONNECTIVITY_SERVICE
        )as ConnectivityManager
        if(Build.VERSION.SDK_INT >=  Build.VERSION_CODES.M){
            val activeNetwork = connectivityManager.activeNetwork ?: return false
            val cababilities = connectivityManager.getNetworkCapabilities(activeNetwork)?:return false
            return when{
                cababilities.hasTransport(TRANSPORT_WIFI)->true
                cababilities.hasTransport(TRANSPORT_CELLULAR)->true
                cababilities.hasTransport(TRANSPORT_ETHERNET)->true
                else -> false

            }
        }else{
            connectivityManager.activeNetworkInfo?.run {
                return when(type) {

                    TYPE_WIFI->true
                    TYPE_MOBILE->true
                    TYPE_ETHERNET->true
                    else->false
                }
            }
        }
        return false
    }
}
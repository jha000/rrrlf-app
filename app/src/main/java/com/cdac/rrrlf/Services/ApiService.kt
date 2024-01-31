package com.cdac.rrrlf.Services

import com.cdac.rrrlf.Data.*
import retrofit2.Call
import retrofit2.http.GET

interface ApiService {
    @GET("state")
    suspend fun getStates(): List<State>

    @GET("district")
    suspend fun getDistricts(): List<District>

    @GET("/beneficiary")
    fun getLibraries(): Call<List<Library>>

    @GET("all")
    suspend fun getAllData(): List<AllData>

    @GET("scheme")
    fun getSchemes(): Call<List<Scheme>>

}
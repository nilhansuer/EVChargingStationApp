package com.nilhansuer.evchargingstationapp

import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface ApiService {
    @FormUrlEncoded
    @POST("receive_csv/")
    fun sendCsvData(@Field("csv_data") csvData: String): Call<String> // Change String to whatever type you expect from the response
}

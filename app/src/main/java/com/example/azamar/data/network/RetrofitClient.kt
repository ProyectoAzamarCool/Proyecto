package com.example.azamar.data.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import kotlin.jvm.java

// 🛑 IMPORTACIÓN DE AuthInterceptor YA NO DEBE EXISTIR O DEBE ESTAR COMENTADA

object RetrofitClient {

    private const val BASE_URL = "https://api-bqajzo735a-uc.a.run.app/"

    // 🛑 CLIENTE SIN INTERCEPTOR DE SEGURIDAD
    private val client = OkHttpClient.Builder()
        // .addInterceptor(AuthInterceptor()) // ¡COMENTADO TEMPORALMENTE!
        .build()

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val neonApiService: NeonApiService by lazy {
        retrofit.create(NeonApiService::class.java)
    }
}
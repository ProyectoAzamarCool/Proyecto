package com.example.azamar.data.network

import retrofit2.Response
import retrofit2.http.GET
import com.example.azamar.data.model.Abogado // Importa el modelo de Abogado
import com.example.azamar.data.model.Reglamento


interface NeonApiService {

    @GET("abogados")
    suspend fun getAbogados(): Response<List<Abogado>>

    // Endpoint PENDIENTE para el reglamento (Artículos)
    @GET("reglamentos")
    suspend fun getReglamentoActualizado(): List<Reglamento>
}
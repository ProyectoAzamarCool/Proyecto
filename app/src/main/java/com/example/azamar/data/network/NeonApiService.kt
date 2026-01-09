package com.example.azamar.data.network

import retrofit2.Response
import retrofit2.http.GET
import com.example.azamar.data.model.Abogado // Importa el modelo de Abogado
import com.example.azamar.data.model.Reglamento

// IMPORTACIÓN PENDIENTE:
// import com.example.pruebas.data.Articulo

interface NeonApiService {

    // Endpoint para obtener la lista de Abogados desde Neon
    // ¡Asegúrate de que la ruta 'api/v1/abogados' coincida con tu backend!
    @GET("abogados")
    suspend fun getAbogados(): Response<List<Abogado>>

    // Endpoint PENDIENTE para el reglamento (Artículos)
    @GET("reglamentos") // Asegúrate de que este sea el nombre de tu tabla en la API
    suspend fun getReglamentoActualizado(): List<Reglamento>
}
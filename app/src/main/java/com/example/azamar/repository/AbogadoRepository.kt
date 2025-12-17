package com.example.azamar.repository

import com.example.azamar.data.dao.AbogadoDao
import com.example.azamar.data.model.Abogado
import com.example.azamar.data.network.RetrofitClient // 💡 IMPORTACIÓN NECESARIA
import kotlinx.coroutines.flow.Flow
import java.io.IOException // 💡 IMPORTACIÓN para manejar errores de red/IO

class AbogadoRepository(private val abogadoDao: AbogadoDao) {

    // Expone el Flow directamente desde el DAO (la lista de abogados)
    // El ViewModel observará esto como la Fuente Única de Verdad.
    val allAbogados: Flow<List<Abogado>> = abogadoDao.getAllAbogados()

    /**
     * Inserta la lista de abogados. Se utiliza para la precarga o la sincronización.
     */
    suspend fun insertAll(abogados: List<Abogado>) {
        abogadoDao.insertAll(abogados)
    }

    /**
     * 🌟 Lógica de Sincronización Offline-First (refreshData)
     * 1. Intenta obtener los datos de la Red (Neon/Backend).
     * 2. Si es exitoso, actualiza el caché de Room.
     * 3. Si falla (sin conexión, error HTTP), el repositorio ignora el error y la UI
     * sigue mostrando el caché existente, manteniendo el modo Offline-First.
     */
    suspend fun refreshAbogados() {
        try {
            // Llama al endpoint a través de la interfaz Retrofit
            val response = RetrofitClient.neonApiService.getAbogados()

            if (response.isSuccessful) {
                val abogadosDesdeNeon = response.body()
                abogadosDesdeNeon?.let {
                    // Primero, limpiamos la tabla (para evitar duplicados y asegurar frescura)
                    abogadoDao.deleteAll()
                    // Luego, insertamos la nueva data en Room
                    abogadoDao.insertAll(it)
                    println("Sincronización de Abogados exitosa.")
                }
            } else {
                // Manejo de errores HTTP (4xx, 5xx)
                // En un proyecto real, esto podría ser un logger o un LiveData de error
                println("Error HTTP al obtener abogados: ${response.code()}")
            }
        } catch (e: IOException) {
            // Manejo de errores de red (Ej. no hay conexión o timeout)
            // No hacemos nada, simplemente usamos los datos cacheados
            println("Error de red/IO. No se pudo sincronizar. Usando datos locales. Error: ${e.message}")
        } catch (e: Exception) {
            // Cualquier otra excepción (Ej. Parsing)
            println("Error desconocido durante la sincronización: ${e.message}")
        }
    }
}
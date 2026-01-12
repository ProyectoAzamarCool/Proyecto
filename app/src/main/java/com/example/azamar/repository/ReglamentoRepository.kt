package com.example.azamar.repository

import androidx.lifecycle.LiveData
import com.example.azamar.data.dao.ReglamentoDao
import com.example.azamar.data.model.Reglamento
import com.example.azamar.data.network.NeonApiService

class ReglamentoRepository(
    private val apiService: NeonApiService,
    private val reglamentoDao: ReglamentoDao
) {
    val allReglamentos: LiveData<List<Reglamento>> = reglamentoDao.getAllReglamentos()

    suspend fun refrescarReglamento() {
        try {
            val response = apiService.getReglamentoActualizado()
            if (response.isNotEmpty()) {
                reglamentoDao.eliminarTodo()
                reglamentoDao.insertarReglamentos(response)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun buscarParaChatbot(pregunta: String): String {
        val resultados = reglamentoDao.buscarReglamentoParaChatbot(pregunta)
        return if (resultados.isNotEmpty()) {
            resultados.joinToString("\n\n") {
                "Art. ${it.numero_articulo}: ${it.texto_completo}"
            }
        } else {
            ""
        }
    }
}
package com.example.azamar.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.azamar.repository.ReglamentoRepository
import kotlinx.coroutines.launch

class ReglamentoViewModel(private val repository: ReglamentoRepository) : ViewModel() {

    // Bloque de inicialización: se ejecuta en cuanto se crea el ViewModel
    init {
        // Al abrir el chat, intentamos actualizar el reglamento del celular
        sincronizarReglamento()
    }

    /**
     * Busca en la base de datos local (Room) los artículos que coincidan con la duda del usuario.
     * @param pregunta La consulta del usuario (ej: "multa por exceso de velocidad")
     * @param onResult Callback que devuelve un String con los artículos encontrados
     */
    fun obtenerContextoLegal(pregunta: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            // Buscamos en el repositorio (que a su vez busca en el DAO local)
            val contexto = repository.buscarParaChatbot(pregunta)

            // Si no encontró nada, podemos devolver un mensaje por defecto
            if (contexto.isEmpty()) {
                onResult("No se encontraron artículos específicos en el reglamento local para esta consulta.")
            } else {
                onResult(contexto)
            }
        }
    }

    /**
     * Conecta con la API de Neon, descarga el reglamento y lo guarda en Room.
     * Gracias al 'fallbackToDestructiveMigration' en tu Database, si hay cambios,
     * la base se actualizará sin problemas.
     */
    fun sincronizarReglamento() {
        viewModelScope.launch {
            try {
                repository.refrescarReglamento()
            } catch (e: Exception) {
                // Si no hay internet, no pasa nada, el usuario usará la versión que ya tiene guardada
                e.printStackTrace()
            }
        }
    }
}
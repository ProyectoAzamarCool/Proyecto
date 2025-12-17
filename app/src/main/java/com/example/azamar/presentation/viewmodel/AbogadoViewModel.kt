package com.example.azamar.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.azamar.data.model.Abogado
import com.example.azamar.repository.AbogadoRepository
import kotlinx.coroutines.launch

class AbogadoViewModel(private val repository: AbogadoRepository) : ViewModel() {

    // LiveData que la UI observará. Sigue tomando datos de Room (Single Source of Truth).
    val allAbogados = repository.allAbogados.asLiveData()

    // 💡 NUEVO BLOQUE: Llama a la sincronización al crear el ViewModel
    init {
        // Al iniciar, intentamos sincronizar. Si no hay conexión, usa la caché.
        refreshData()
    }

    /**
     * 🌟 NUEVA FUNCIÓN: Inicia el proceso de sincronización (Network -> Room).
     * Esto se ejecuta en un hilo seguro (viewModelScope).
     */
    fun refreshData() {
        viewModelScope.launch {
            repository.refreshAbogados()
        }
    }

    /**
     * Función para insertar abogados. Solo se mantendrá por si es necesaria en pruebas o utilidades.
     */
    fun insert(abogados: List<Abogado>) = viewModelScope.launch {
        repository.insertAll(abogados)
    }
}

// -----------------------------------------------------------------------------
// CLASE FACTORY: Se mantiene sin cambios.
// -----------------------------------------------------------------------------
class AbogadoViewModelFactory(private val repository: AbogadoRepository) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AbogadoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AbogadoViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
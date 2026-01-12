package com.example.azamar.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.azamar.data.model.Reglamento

@Dao
interface ReglamentoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarReglamentos(reglamentos: List<Reglamento>)

    @Query("SELECT * FROM reglamentos_tabla")
    fun getAllReglamentos(): LiveData<List<Reglamento>>

    // Búsqueda optimizada para alimentar al Chatbot
    @Query("SELECT * FROM reglamentos_tabla WHERE texto_completo LIKE '%' || :query || '%' OR tags_busqueda LIKE '%' || :query || '%'")
    suspend fun buscarReglamentoParaChatbot(query: String): List<Reglamento>

    @Query("DELETE FROM reglamentos_tabla")
    suspend fun eliminarTodo()
}
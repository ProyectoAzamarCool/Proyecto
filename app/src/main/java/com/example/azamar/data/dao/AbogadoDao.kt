package com.example.azamar.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.azamar.data.model.Abogado
import kotlinx.coroutines.flow.Flow

@Dao
interface AbogadoDao {

    // Obtener todos los abogados, ordenados por nombre. Flow permite observar cambios.
    @Query("SELECT * FROM abogados_tabla ORDER BY nombre ASC")
    fun getAllAbogados(): Flow<List<Abogado>>

    // Insertar una lista completa. Si el ID existe, lo reemplaza (actualiza).
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(abogados: List<Abogado>)

    // Borra toda la tabla (útil para resincronizar datos).
    @Query("DELETE FROM abogados_tabla")
    suspend fun deleteAll()
}
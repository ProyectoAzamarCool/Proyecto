package com.example.azamar.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reglamentos_tabla")
data class Reglamento(
    @PrimaryKey
    val numero_articulo: String,
    val tipo_norma: String,
    val texto_completo: String,
    val tags_busqueda: List<String>, // Se manejará con Converter
    val sancion: SancionInfo?         // Se manejará con Converter
)

data class SancionInfo(
    val monto_uma: String?,
    val puntos_licencia: Int?,
    val corralon: Boolean?
)
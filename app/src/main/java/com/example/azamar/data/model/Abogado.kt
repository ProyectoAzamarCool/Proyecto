package com.example.azamar.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName // 💡 Importación requerida

@Entity(tableName = "abogados_tabla")
data class Abogado(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val nombre: String,
    val especialidad: String,

    // 🌟 Mapeo de la API al Modelo: "telefono_contacto" -> "telefono"
    @SerializedName("telefono_contacto")
    val telefono: String,

    // 🌟 Mapeo de la API al Modelo: "email_contacto" -> "correo"
    @SerializedName("email_contacto")
    val correo: String,

    // 💡 Hacemos los campos ausentes de la API anulables (nullable) y les damos un valor por defecto.
    val fotoUrl: String? = null,
    val latitud: Double? = 0.0,
    val longitud: Double? = 0.0,
    val distanciaKm: Double? = null
)
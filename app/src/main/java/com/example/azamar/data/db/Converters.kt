package com.example.azamar.data.db

import androidx.room.TypeConverter
import com.example.azamar.data.model.SancionInfo
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()

    // Para la lista de Tags (JSON)
    @TypeConverter
    fun fromStringList(value: String?): List<String> {
        if (value == null) return emptyList()
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun fromList(list: List<String>?): String {
        return gson.toJson(list ?: emptyList<String>())
    }

    // Para el objeto Sancion (JSON)
    @TypeConverter
    fun fromSancionJson(json: String?): SancionInfo? {
        if (json == null) return null
        return gson.fromJson(json, SancionInfo::class.java)
    }

    @TypeConverter
    fun toSancionJson(sancion: SancionInfo?): String {
        return gson.toJson(sancion)
    }
}
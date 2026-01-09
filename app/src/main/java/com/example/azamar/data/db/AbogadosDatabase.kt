package com.example.azamar.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.azamar.data.dao.AbogadoDao
import com.example.azamar.data.dao.ReglamentoDao
import com.example.azamar.data.model.Abogado
import com.example.azamar.data.model.Reglamento

// 1. Subimos la versión a 2 y agregamos Reglamento::class
@Database(entities = [Abogado::class, Reglamento::class], version = 1, exportSchema = false)
// 2. Registramos los convertidores para manejar los campos JSON de la tabla
@TypeConverters(Converters::class)
abstract class AbogadosDatabase : RoomDatabase() {

    abstract fun abogadoDao(): AbogadoDao
    // 3. Agregamos el acceso al nuevo DAO
    abstract fun reglamentoDao(): ReglamentoDao

    companion object {
        @Volatile
        private var INSTANCE: AbogadosDatabase? = null

        fun getDatabase(context: Context): AbogadosDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AbogadosDatabase::class.java,
                    "Asistente_DB"
                )
                    // 4. Agregamos esta línea para evitar errores al subir de versión si no quieres manejar migraciones complejas aún
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
package com.example.azamar.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.azamar.data.dao.AbogadoDao
import com.example.azamar.data.model.Abogado

@Database(entities = [Abogado::class], version = 1, exportSchema = false)
abstract class AbogadosDatabase : RoomDatabase() {

    abstract fun abogadoDao(): AbogadoDao

    companion object {
        @Volatile
        private var INSTANCE: AbogadosDatabase? = null

        fun getDatabase(context: Context): AbogadosDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AbogadosDatabase::class.java,
                    "abogados_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
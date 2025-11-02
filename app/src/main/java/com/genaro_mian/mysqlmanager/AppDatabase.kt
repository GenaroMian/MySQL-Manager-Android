package com.genaro_mian.mysqlmanager // (Seu pacote)

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// @Database diz ao Room que este é o "armário" principal.
@Database(entities = [ConexaoSalva::class], version = 1)
abstract class AppDatabase : RoomDatabase() {

    // Informa ao banco qual DAO ele "possui"
    abstract fun conexaoDao(): ConexaoDao

    // O 'companion object' é um padrão para garantir que só exista
    // UMA instância do banco de dados aberta por vez no app (Singleton).
    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            // Se a instância já existe, retorna ela.
            return INSTANCE ?: synchronized(this) {
                // Se não existe, cria o banco.
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mysql_manager_database" // Nome do arquivo do banco no celular
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
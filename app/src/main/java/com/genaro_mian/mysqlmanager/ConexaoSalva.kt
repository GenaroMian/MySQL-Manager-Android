package com.genaro_mian.mysqlmanager // (Seu pacote)

import androidx.room.Entity
import androidx.room.PrimaryKey

// A anotação @Entity diz ao Room que esta classe é uma tabela no banco.
@Entity(tableName = "conexoes_salvas")
data class ConexaoSalva(

    // @PrimaryKey(autoGenerate = true) diz que o 'id' é a chave única
    // e o Room deve gerá-la sozinho (1, 2, 3...)
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0, // 0 é o valor padrão para um novo item

    val alias: String,
    val url: String,
    val port: String,
    val dbName: String,
    val user: String,
    val pass: String // Vamos usar 'pass' em vez de 'password',
    // pois 'password' às vezes é uma palavra reservada
)
package com.genaro_mian.mysqlmanager // (Seu pacote)

// 1. A classe que você moveu do DataViewScreen.kt
data class QueryResult(
    val columns: List<String> = emptyList(),
    val rows: List<List<String>> = emptyList()
)

// 2. A nova classe para os resultados do Terminal
// (Sealed class é ótima para representar estados diferentes)
sealed class SqlResult {
    // Estado inicial, nada aconteceu
    object Idle : SqlResult()

    // Para o "loading"
    object Loading : SqlResult()

    // Para SELECTs (retorna a nossa QueryResult)
    data class SuccessSelect(val data: QueryResult) : SqlResult()

    // Para INSERT, UPDATE, DELETE (retorna o nº de linhas)
    data class SuccessUpdate(val rowsAffected: Int) : SqlResult()

    // Para erros
    data class Error(val message: String) : SqlResult()
}
package com.genaro_mian.mysqlmanager // (Seu pacote)

// ... (imports do Dao, Insert, Query, Flow) ...
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ConexaoDao {

    // ... (sua função 'inserir' e 'getTodasConexoes' continuam aqui) ...
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(conexao: ConexaoSalva)

    @Query("SELECT * FROM conexoes_salvas ORDER BY alias ASC")
    fun getTodasConexoes(): Flow<List<ConexaoSalva>>

    @Query("DELETE FROM conexoes_salvas WHERE id = :idDaConexao")
    suspend fun deletarPeloId(idDaConexao: Int)


    // **ADICIONE ESTA NOVA FUNÇÃO:**
    // Busca uma conexão específica pelo ID.
    // Não usamos 'Flow' porque só queremos o dado uma vez.
    @Query("SELECT * FROM conexoes_salvas WHERE id = :idDaConexao LIMIT 1")
    suspend fun getConexaoPeloId(idDaConexao: Int): ConexaoSalva? // '?' = pode não encontrar
}
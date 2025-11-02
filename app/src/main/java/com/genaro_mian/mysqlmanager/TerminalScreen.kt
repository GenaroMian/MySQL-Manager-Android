package com.genaro_mian.mysqlmanager // (Seu pacote)

import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(
    navController: NavController,
    conexaoId: Int,
    dbName: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val conexaoDao = AppDatabase.getDatabase(context).conexaoDao()

    // Estados da tela
    var sqlQuery by remember { mutableStateOf("") } // Onde o usuário digita
    var resultState by remember { mutableStateOf<SqlResult>(SqlResult.Idle) }
    var conexaoSalva by remember { mutableStateOf<ConexaoSalva?>(null) }

    // Busca a conexão (apenas para o título)
    LaunchedEffect(key1 = conexaoId) {
        conexaoSalva = withContext(Dispatchers.IO) {
            conexaoDao.getConexaoPeloId(conexaoId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Terminal (${conexaoSalva?.alias} / $dbName)") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Voltar",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            )
        },
        // Botão "Executar" (Run)
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (sqlQuery.isNotBlank()) {
                        // Inicia a Coroutine para executar a query
                        scope.launch {
                            resultState = SqlResult.Loading // Mostra o loading

                            val conexao = withContext(Dispatchers.IO) {
                                conexaoDao.getConexaoPeloId(conexaoId)
                            }

                            if (conexao == null) {
                                resultState = SqlResult.Error("Conexão não encontrada.")
                            } else {
                                // Chama a nossa nova função de lógica
                                resultState = executeGenericQuery(conexao, dbName, sqlQuery)
                            }
                        }
                    } else {
                        Toast.makeText(context, "Digite uma query SQL.", Toast.LENGTH_SHORT).show()
                    }
                },
                // Muda o ícone se estiver carregando
                content = {
                    if (resultState is SqlResult.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Icon(Icons.Default.PlayArrow, "Executar")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(8.dp)
        ) {
            // 1. A Caixa de Texto para a Query
            OutlinedTextField(
                value = sqlQuery,
                onValueChange = { sqlQuery = it },
                label = { Text("Query SQL") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp), // Altura maior
                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace)
            )

            Spacer(modifier = Modifier.height(8.dp))
            Divider(thickness = 2.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // 2. A Área de Resultados
            Text("Resultados:", style = MaterialTheme.typography.titleMedium)

            // O 'when' decide o que mostrar baseado no 'resultState'
            when (val result = resultState) {
                is SqlResult.Idle -> {
                    Text("Digite um comando SQL e clique em 'Executar'.")
                }
                is SqlResult.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                }
                is SqlResult.Error -> {
                    Text(result.message, color = MaterialTheme.colorScheme.error)
                }
                is SqlResult.SuccessUpdate -> {
                    Text("Sucesso! ${result.rowsAffected} linhas afetadas.", color = Color(0xFF006400)) // Verde Escuro
                }
                is SqlResult.SuccessSelect -> {
                    // Reutilizamos a UI da DataViewScreen
                    DataTable(result = result.data)
                }
            }
        }
    }
}

// **NOVA LÓGICA DE EXECUÇÃO (Avançada)**
// Esta função usa 'statement.execute()' para rodar qualquer SQL
private suspend fun executeGenericQuery(
    conexao: ConexaoSalva,
    dbName: String,
    sql: String
): SqlResult {
    return withContext(Dispatchers.IO) {
        var connection: Connection? = null
        try {
            connection = connectToMySQL(
                url = conexao.url,
                port = conexao.port,
                dbName = dbName, // Conecta ao DB específico
                user = conexao.user,
                pass = conexao.pass
            )

            val statement = connection.createStatement()

            // A MÁGICA: statement.execute()
            // Retorna 'true' se for um SELECT (tem ResultSet)
            // Retorna 'false' se for INSERT/UPDATE/DELETE (tem updateCount)
            val hasResultSet = statement.execute(sql)

            if (hasResultSet) {
                // É UM SELECT
                val resultSet = statement.resultSet
                val metaData = resultSet.metaData
                val columnCount = metaData.columnCount
                val columnNames = (1..columnCount).map { metaData.getColumnName(it) }

                val rows = mutableListOf<List<String>>()
                while (resultSet.next()) {
                    val row = (1..columnCount).map { resultSet.getString(it) ?: "NULL" }
                    rows.add(row)
                }
                SqlResult.SuccessSelect(QueryResult(columns = columnNames, rows = rows))

            } else {
                // É UM INSERT, UPDATE, DELETE, CREATE, etc.
                val rowsAffected = statement.updateCount
                SqlResult.SuccessUpdate(rowsAffected)
            }

        } catch (e: Exception) {
            e.printStackTrace()
            SqlResult.Error(e.message ?: "Erro desconhecido")
        } finally {
            connection?.close()
        }
    }
}

// **A UI DA TABELA (Copiada da DataViewScreen)**
// (Idealmente, isto também iria para um ficheiro partilhado)
@Composable
private fun DataTable(result: QueryResult) {
    Box(modifier = Modifier
        .fillMaxSize()
        .horizontalScroll(rememberScrollState())) {

        LazyColumn(modifier = Modifier.fillMaxSize()) {

            // Item 1: O Cabeçalho (Colunas)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    result.columns.forEach { columnName ->
                        Text(
                            text = columnName,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.widthIn(min = 100.dp)
                        )
                    }
                }
                Divider(color = Color.Black, thickness = 2.dp)
            }

            // Itens 2...N: As Linhas de Dados
            items(result.rows) { rowData ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowData.forEach { cellData ->
                        Text(
                            text = cellData,
                            modifier = Modifier.widthIn(min = 100.dp)
                        )
                    }
                }
                Divider()
            }
        }
    }
}


// **Função de conexão (A mesma das outras telas)**
private fun connectToMySQL(url: String, port: String, dbName: String, user: String, pass: String): Connection {
    Class.forName("com.mysql.jdbc.Driver")
    val dbSegment = if (dbName.isNotBlank()) "/$dbName" else ""
    val connectionUrl = "jdbc:mysql://$url:$port$dbSegment"
    return DriverManager.getConnection(connectionUrl, user, pass)
}
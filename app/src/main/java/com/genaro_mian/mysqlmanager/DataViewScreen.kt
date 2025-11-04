package com.genaro_mian.mysqlmanager // (Seu pacote)

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.DriverManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataViewScreen(
    navController: NavController,
    conexaoId: Int,
    dbName: String,
    tableName: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val conexaoDao = AppDatabase.getDatabase(context).conexaoDao()

    var isLoading by remember { mutableStateOf(true) }
    var queryResult by remember { mutableStateOf<QueryResult>(QueryResult()) }
    var erro by remember { mutableStateOf<String?>(null) }
    var conexaoSalva by remember { mutableStateOf<ConexaoSalva?>(null) }

    LaunchedEffect(key1 = conexaoId, key2 = dbName, key3 = tableName) {
        scope.launch(Dispatchers.IO) {
            val conexao = conexaoDao.getConexaoPeloId(conexaoId)
            conexaoSalva = conexao

            if (conexao == null) {
                withContext(Dispatchers.Main) {
                    erro = "Erro: Conexão (ID: $conexaoId) não encontrada."
                    isLoading = false
                }
                return@launch
            }

            var connection: Connection? = null
            try {
                connection = connectToMySQL(
                    url = conexao.url,
                    port = conexao.port,
                    dbName = dbName,
                    user = conexao.user,
                    pass = conexao.pass
                )

                val statement = connection.createStatement()
                val resultSet = statement.executeQuery("SELECT * FROM `$tableName` LIMIT 100;")

                val metaData = resultSet.metaData
                val columnCount = metaData.columnCount
                val columnNames = (1..columnCount).map { metaData.getColumnName(it) }

                val rows = mutableListOf<List<String>>()
                while (resultSet.next()) {
                    val row = (1..columnCount).map {
                        resultSet.getString(it) ?: "NULL"
                    }
                    rows.add(row)
                }

                withContext(Dispatchers.Main) {
                    queryResult = QueryResult(columns = columnNames, rows = rows)
                    isLoading = false
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    erro = "Erro na query: ${e.message}"
                    isLoading = false
                }
            } finally {
                connection?.close()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${conexaoSalva?.alias ?: "..."} / $tableName") },
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    navController.navigate("terminal_screen/${conexaoId}?dbName=${dbName}")
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Computer,
                    contentDescription = "Abrir Terminal"
                )
            }
        },
        content = { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (erro != null) {
                    Text(
                        text = erro!!,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Box(modifier = Modifier
                        .fillMaxSize()
                        .horizontalScroll(rememberScrollState())) {

                        LazyColumn(modifier = Modifier.fillMaxSize()) {

                            // Cabeçalho (Colunas)
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    queryResult.columns.forEach { columnName ->
                                        Text(
                                            text = columnName,
                                            fontWeight = FontWeight.Bold,
                                            // **A CORREÇÃO ESTÁ AQUI**
                                            modifier = Modifier.width(150.dp)
                                        )
                                    }
                                }
                                Divider(color = Color.Black, thickness = 2.dp)
                            }

                            // Linhas de Dados
                            items(queryResult.rows) { rowData ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowData.forEach { cellData ->
                                        Text(
                                            text = cellData,
                                            // **E AQUI**
                                            modifier = Modifier.width(150.dp)
                                        )
                                    }
                                }
                                Divider()
                            }
                        }
                    }
                }
            }
        }
    )
}


// Função de conexão (Sem alterações)
private fun connectToMySQL(url: String, port: String, dbName: String, user: String, pass: String): Connection {
    Class.forName("com.mysql.jdbc.Driver")
    val dbSegment = if (dbName.isNotBlank()) "/$dbName" else ""
    val connectionUrl = "jdbc:mysql://$url:$port$dbSegment"
    return DriverManager.getConnection(connectionUrl, user, pass)
}
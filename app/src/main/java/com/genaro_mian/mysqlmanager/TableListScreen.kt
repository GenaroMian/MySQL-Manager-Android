package com.genaro_mian.mysqlmanager // (Seu pacote)

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Computer // <-- Import do ícone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.DriverManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableListScreen(
    navController: NavController,
    conexaoId: Int,
    dbName: String
) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val conexaoDao = AppDatabase.getDatabase(context).conexaoDao()

    // Estados da tela
    var isLoading by remember { mutableStateOf(true) }
    var tableNames by remember { mutableStateOf<List<String>>(emptyList()) }
    var erro by remember { mutableStateOf<String?>(null) }
    var conexaoSalva by remember { mutableStateOf<ConexaoSalva?>(null) }

    // LaunchedEffect (Sem alterações)
    LaunchedEffect(key1 = conexaoId, key2 = dbName) {
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
                    dbName = dbName, // Com dbName
                    user = conexao.user,
                    pass = conexao.pass
                )

                val statement = connection.createStatement()
                val resultSet = statement.executeQuery("SHOW TABLES;")

                val tables = mutableListOf<String>()
                while (resultSet.next()) {
                    tables.add(resultSet.getString(1))
                }

                withContext(Dispatchers.Main) {
                    tableNames = tables
                    isLoading = false
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    erro = "Erro de conexão: ${e.message}"
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
                title = { Text("${conexaoSalva?.alias ?: "..."} / $dbName") },
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

        // **MUDANÇA (FAB ADICIONADO)**
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    // **MUDANÇA (ROTA CORRIGIDA)**
                    // Navega para o terminal, passando o dbName (rota opcional)
                    navController.navigate("terminal_screen/${conexaoId}?dbName=${dbName}")
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Computer,
                    contentDescription = "Abrir Terminal"
                )
            }
        }

    ) { innerPadding ->
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
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(tableNames) { tableName ->
                        ListItem(
                            headlineContent = { Text(tableName) },

                            // **MUDANÇA (CORREÇÃO DO BUG)**
                            modifier = Modifier.clickable {
                                // O clique na tabela deve ir para a TELA DE DADOS
                                navController.navigate("data_view_screen/${conexaoId}/${dbName}/${tableName}")
                            }
                        )
                        Divider()
                    }
                }
            }
        }
    }
}


// Função de conexão (Sem alterações)
private fun connectToMySQL(url: String, port: String, dbName: String, user: String, pass: String): Connection {
    Class.forName("com.mysql.jdbc.Driver")
    val dbSegment = if (dbName.isNotBlank()) "/$dbName" else ""
    val connectionUrl = "jdbc:mysql://$url:$port$dbSegment"
    return DriverManager.getConnection(connectionUrl, user, pass)
}
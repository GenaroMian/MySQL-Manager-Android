package com.genaro_mian.mysqlmanager // (Seu pacote)

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Computer // <-- MUDANÇA (NOVO IMPORT)
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
import java.sql.ResultSet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatabaseListScreen(navController: NavController, conexaoId: Int) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val conexaoDao = AppDatabase.getDatabase(context).conexaoDao()

    // Estados da tela
    var isLoading by remember { mutableStateOf(true) }
    var databaseNames by remember { mutableStateOf<List<String>>(emptyList()) }
    var erro by remember { mutableStateOf<String?>(null) }
    var conexaoSalva by remember { mutableStateOf<ConexaoSalva?>(null) }

    // LaunchedEffect (Sem alterações)
    LaunchedEffect(key1 = conexaoId) {
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
                // Conecta sem dbName (correto)
                connection = connectToMySQL(
                    url = conexao.url,
                    port = conexao.port,
                    dbName = "",
                    user = conexao.user,
                    pass = conexao.pass
                )

                val statement = connection.createStatement()
                val resultSet = statement.executeQuery("SHOW DATABASES;")

                val dbs = mutableListOf<String>()
                while (resultSet.next()) {
                    dbs.add(resultSet.getString(1))
                }

                withContext(Dispatchers.Main) {
                    databaseNames = dbs
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
                title = { Text(conexaoSalva?.alias ?: "Carregando...") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack, "Voltar",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            )
        },

        // **MUDANÇA (NOVO FAB ADICIONADO)**
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    // Navega para o terminal, sem passar o dbName
                    // (Isto funciona por causa da Rota 6 que atualizamos)
                    navController.navigate("terminal_screen/${conexaoId}")
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
                    items(databaseNames) { dbName ->
                        ListItem(
                            headlineContent = { Text(dbName) },
                            modifier = Modifier.clickable {
                                navController.navigate("table_list_screen/${conexaoId}/$dbName")
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
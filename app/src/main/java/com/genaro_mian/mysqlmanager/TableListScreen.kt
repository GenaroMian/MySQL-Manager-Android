package com.genaro_mian.mysqlmanager // (Seu pacote)

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Computer // <-- Import do ícone
import androidx.compose.material.icons.filled.Storage
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

    var isLoading by remember { mutableStateOf(true) }
    var tableNames by remember { mutableStateOf<List<String>>(emptyList()) }
    var erro by remember { mutableStateOf<String?>(null) }
    var conexaoSalva by remember { mutableStateOf<ConexaoSalva?>(null) }

    // 🔹 Carrega as tabelas da conexão
    LaunchedEffect(conexaoId, dbName) {
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
                connection = connectToMySQL(conexao.url, conexao.port, dbName, conexao.user, conexao.pass)
                val statement = connection.createStatement()
                val resultSet = statement.executeQuery("SHOW TABLES;")

                val tables = mutableListOf<String>()
                while (resultSet.next()) tables.add(resultSet.getString(1))

                withContext(Dispatchers.Main) {
                    tableNames = tables
                    isLoading = false
                }
            } catch (e: Exception) {
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
                title = {
                    Column {
                        Text(conexaoSalva?.alias ?: "Conexão", style = MaterialTheme.typography.titleLarge)
                        Text(
                            dbName,
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { navController.navigate("terminal_screen/${conexaoId}?dbName=${dbName}") },
                icon = { Icon(Icons.Default.Computer, contentDescription = "Abrir Terminal") },
                text = { Text("Terminal SQL") },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 12.dp)
        ) {
            when {
                isLoading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Carregando tabelas...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                erro != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Computer, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(erro!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyLarge)
                    }
                }

                tableNames.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(72.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Nenhuma tabela encontrada", style = MaterialTheme.typography.titleMedium)
                        Text("Esse banco ainda não possui tabelas.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(tableNames) { tableName ->
                            ElevatedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        navController.navigate("data_view_screen/${conexaoId}/${dbName}/${tableName}")
                                    },
                                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Storage,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .size(32.dp)
                                            .padding(end = 12.dp)
                                    )
                                    Column {
                                        Text(tableName, style = MaterialTheme.typography.titleMedium)
                                        Text("Clique para visualizar dados", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// 🔹 Função de conexão (inalterada)
private fun connectToMySQL(url: String, port: String, dbName: String, user: String, pass: String): Connection {
    Class.forName("com.mysql.jdbc.Driver")
    val dbSegment = if (dbName.isNotBlank()) "/$dbName" else ""
    val connectionUrl = "jdbc:mysql://$url:$port$dbSegment"
    return DriverManager.getConnection(connectionUrl, user, pass)
}

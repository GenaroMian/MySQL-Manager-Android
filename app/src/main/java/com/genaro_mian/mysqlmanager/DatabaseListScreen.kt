package com.genaro_mian.mysqlmanager // (Seu pacote)

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Computer
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
import java.sql.ResultSet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatabaseListScreen(navController: NavController, conexaoId: Int) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val conexaoDao = AppDatabase.getDatabase(context).conexaoDao()

    var isLoading by remember { mutableStateOf(true) }
    var databaseNames by remember { mutableStateOf<List<String>>(emptyList()) }
    var erro by remember { mutableStateOf<String?>(null) }
    var conexaoSalva by remember { mutableStateOf<ConexaoSalva?>(null) }

    // Carregar bancos da conexão
    LaunchedEffect(conexaoId) {
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
                connection = connectToMySQL(conexao.url, conexao.port, "", conexao.user, conexao.pass)
                val result = connection.createStatement().executeQuery("SHOW DATABASES;")

                val allDbs = mutableListOf<String>()
                while (result.next()) allDbs.add(result.getString(1))

                val systemDbs = setOf("information_schema", "mysql", "performance_schema", "sys", "sakila", "world")
                val filtered = allDbs.filterNot { it.lowercase() in systemDbs }

                withContext(Dispatchers.Main) {
                    databaseNames = filtered
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
                        Text(conexaoSalva?.alias ?: "Conexão MySQL", style = MaterialTheme.typography.titleLarge)
                        if (conexaoSalva != null)
                            Text(
                                "${conexaoSalva!!.user}@${conexaoSalva!!.url}:${conexaoSalva!!.port}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                                )
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
                onClick = { navController.navigate("terminal_screen/${conexaoId}") },
                icon = { Icon(Icons.Default.Computer, contentDescription = null) },
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
                        Text("Carregando bancos de dados...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                erro != null -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Computer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            erro!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                databaseNames.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Nenhum banco de dados encontrado", style = MaterialTheme.typography.titleMedium)
                        Text("Crie um novo banco pelo terminal SQL.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(databaseNames) { dbName ->
                            ElevatedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        navController.navigate("table_list_screen/${conexaoId}/$dbName")
                                    },
                                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .fillMaxWidth(),
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
                                        Text(dbName, style = MaterialTheme.typography.titleMedium)
                                        Text("Clique para listar tabelas", color = MaterialTheme.colorScheme.onSurfaceVariant)
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

// --- Função de conexão (inalterada)
private fun connectToMySQL(url: String, port: String, dbName: String, user: String, pass: String): Connection {
    Class.forName("com.mysql.jdbc.Driver")
    val dbSegment = if (dbName.isNotBlank()) "/$dbName" else ""
    val connectionUrl = "jdbc:mysql://$url:$port$dbSegment"
    return DriverManager.getConnection(connectionUrl, user, pass)
}

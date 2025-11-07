package com.genaro_mian.mysqlmanager // (Seu pacote)

import android.app.Application
import android.content.Intent // <-- NOVO IMPORT
import android.net.Uri // <-- NOVO IMPORT
import android.os.Bundle
import android.widget.Toast // (Já existia no DataViewScreen, mas bom ter aqui)
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info // <-- NOVO IMPORT
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.genaro_mian.mysqlmanager.ui.theme.BlueGradient
import com.genaro_mian.mysqlmanager.ui.theme.MySqlManagerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MySqlManagerTheme {
                val navController = rememberNavController()

                // O NavHost (mapa) está correto, com todas as 6 rotas
                NavHost(
                    navController = navController,
                    startDestination = "main_screen"
                ) {

                    // Rota 1: Tela Principal
                    composable("main_screen") {
                        MainScreen(navController = navController)
                    }

                    // Rota 2: Nova Conexão (com ID opcional)
                    composable(
                        route = "new_connection_screen?id={conexaoId}",
                        arguments = listOf(
                            navArgument("conexaoId") {
                                type = NavType.IntType
                                defaultValue = 0
                            }
                        )
                    ) { backStackEntry ->
                        val id = backStackEntry.arguments?.getInt("conexaoId") ?: 0
                        NovaConexaoScreen(navController = navController, conexaoId = id)
                    }

                    // Rota 3: Lista de Bancos
                    composable(
                        route = "database_list_screen/{conexaoId}",
                        arguments = listOf(navArgument("conexaoId") { type = NavType.IntType })
                    ) { backStackEntry ->
                        val id = backStackEntry.arguments?.getInt("conexaoId")
                        if (id != null) {
                            DatabaseListScreen(navController = navController, conexaoId = id)
                        } else {
                            navController.popBackStack()
                        }
                    }

                    // Rota 4: Lista de Tabelas
                    composable(
                        route = "table_list_screen/{conexaoId}/{dbName}",
                        arguments = listOf(
                            navArgument("conexaoId") { type = NavType.IntType },
                            navArgument("dbName") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val id = backStackEntry.arguments?.getInt("conexaoId")
                        val nomeDoBanco = backStackEntry.arguments?.getString("dbName")
                        if (id != null && nomeDoBanco != null) {
                            TableListScreen(
                                navController = navController,
                                conexaoId = id,
                                dbName = nomeDoBanco
                            )
                        } else {
                            navController.popBackStack()
                        }
                    }

                    // Rota 5: Visualização de Dados
                    composable(
                        route = "data_view_screen/{conexaoId}/{dbName}/{tableName}",
                        arguments = listOf(
                            navArgument("conexaoId") { type = NavType.IntType },
                            navArgument("dbName") { type = NavType.StringType },
                            navArgument("tableName") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val id = backStackEntry.arguments?.getInt("conexaoId")
                        val nomeDoBanco = backStackEntry.arguments?.getString("dbName")
                        val nomeDaTabela = backStackEntry.arguments?.getString("tableName")
                        if (id != null && nomeDoBanco != null && nomeDaTabela != null) {
                            DataViewScreen(
                                navController = navController,
                                conexaoId = id,
                                dbName = nomeDoBanco,
                                tableName = nomeDaTabela
                            )
                        } else {
                            navController.popBackStack()
                        }
                    }

                    // Rota 6: O Terminal (dbName opcional)
                    composable(
                        route = "terminal_screen/{conexaoId}?dbName={dbName}",
                        arguments = listOf(
                            navArgument("conexaoId") { type = NavType.IntType },
                            navArgument("dbName") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            }
                        )
                    ) { backStackEntry ->
                        val id = backStackEntry.arguments?.getInt("conexaoId")
                        val nomeDoBanco = backStackEntry.arguments?.getString("dbName") ?: ""

                        if (id != null) {
                            TerminalScreen(
                                navController = navController,
                                conexaoId = id,
                                dbName = nomeDoBanco
                            )
                        } else {
                            navController.popBackStack()
                        }
                    }
                }
            }
        }
    }
}

// --- TELA PRINCIPAL (Com Menu de Privacidade) ---
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainScreen(navController: NavController) {
    val context = LocalContext.current
    val viewModel: MainViewModel = viewModel(
        factory = MainViewModelFactory(context.applicationContext as Application)
    )

    val conexoes by viewModel.conexoes.collectAsState(initial = emptyList())
    var showMenu by remember { mutableStateOf(false) }
    var showDropdownMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var conexaoSelecionada by remember { mutableStateOf<ConexaoSalva?>(null) }

    val privacyPolicyUrl =
        "https://docs.google.com/document/d/1E2d-PvxIGVy9KTmB8gmpifxxYBQt2QcbWzuyWmtrBIw/edit?usp=sharing"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "MySQL Manager",
                        style = MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.onPrimary)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.background(BlueGradient),
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Menu",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Política de Privacidade") },
                            leadingIcon = { Icon(Icons.Default.Info, null) },
                            onClick = {
                                showMenu = false
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(privacyPolicyUrl))
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    Toast.makeText(
                                        context,
                                        "Não foi possível abrir o link",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { navController.navigate("new_connection_screen") },
                text = { Text("Nova conexão") },
                icon = { Icon(Icons.Default.Add, null) },
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
                modifier = Modifier.padding(12.dp)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (conexoes.isEmpty()) {
                // Estado vazio
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(72.dp)
                    )
                    Text(
                        "Nenhuma conexão salva",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Toque em “Nova conexão” para criar uma.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp)
                ) {
                    items(conexoes, key = { it.id }) { conexao ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + slideInVertically(),
                            exit = fadeOut()
                        ) {
                            ElevatedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .combinedClickable(
                                        onClick = {
                                            navController.navigate("database_list_screen/${conexao.id}")
                                        },
                                        onLongClick = {
                                            conexaoSelecionada = conexao
                                            showDropdownMenu = true
                                        }
                                    ),
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Ícone DB redimensionado
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_database_3d),
                                        contentDescription = "Banco de Dados",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .padding(end = 12.dp)
                                    )



                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = conexao.alias,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${conexao.user}@${conexao.url}",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        DropdownMenu(
                            expanded = showDropdownMenu && conexaoSelecionada?.id == conexao.id,
                            onDismissRequest = { showDropdownMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Editar conexão") },
                                onClick = {
                                    showDropdownMenu = false
                                    navController.navigate("new_connection_screen?id=${conexao.id}")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Excluir") },
                                onClick = {
                                    showDropdownMenu = false
                                    showDeleteDialog = true
                                }
                            )
                        }
                    }

                }
            }

            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    icon = { Icon(Icons.Default.Info, null) },
                    title = { Text("Excluir Conexão") },
                    text = {
                        Text("Tem certeza que deseja excluir \"${conexaoSelecionada?.alias}\"?")
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                conexaoSelecionada?.let { viewModel.deletarConexao(it) }
                                showDeleteDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Excluir")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteDialog = false }) {
                            Text("Cancelar")
                        }
                    }
                )
            }
        }
    }
}



// Preview (Sem alterações)
@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MySqlManagerTheme {
        MainScreen(navController = rememberNavController())
    }
}
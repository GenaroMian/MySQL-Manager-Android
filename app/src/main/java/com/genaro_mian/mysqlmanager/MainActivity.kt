package com.genaro_mian.mysqlmanager // (Seu pacote)

import android.app.Application
import android.content.Intent // <-- NOVO IMPORT
import android.net.Uri // <-- NOVO IMPORT
import android.os.Bundle
import android.widget.Toast // (Já existia no DataViewScreen, mas bom ter aqui)
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info // <-- NOVO IMPORT
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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

    // 1. Pegar o ViewModel
    val context = LocalContext.current
    val viewModel: MainViewModel = viewModel(
        factory = MainViewModelFactory(context.applicationContext as Application)
    )

    // 2. Pegar a lista de conexões do ViewModel
    val conexoes by viewModel.conexoes.collectAsState(initial = emptyList())

    // 3. Estados para os Menus e Diálogos
    var showMenu by remember { mutableStateOf(false) } // <-- PARA O MENU DA TOPBAR
    var showDropdownMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var conexaoSelecionada by remember { mutableStateOf<ConexaoSalva?>(null) }

    // 4. O seu link
    val privacyPolicyUrl = "https://docs.google.com/document/d/1E2d-PvxIGVy9KTmB8gmpifxxYBQt2QcbWzuyWmtrBIw/edit?usp=sharing"


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MySQL Manager") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                // **MUDANÇA AQUI: LÓGICA DO MENU DA TOPBAR**
                actions = {
                    // 1. O Botão (Três Pontos)
                    IconButton(onClick = { showMenu = true }) { // <-- Abre o menu
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Menu",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }

                    // 2. O Menu Flutuante (ancorado ao IconButton)
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        // 3. O Item da Política de Privacidade
                        DropdownMenuItem(
                            text = { Text("Política de Privacidade") },
                            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                            onClick = {
                                showMenu = false // Fecha o menu
                                // Cria e lança o Intent para abrir o browser
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(privacyPolicyUrl))
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // Se o usuário não tiver um browser (raro)
                                    Toast.makeText(context, "Não foi possível abrir o link", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                        // (Pode adicionar mais itens aqui, como "Sobre")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    navController.navigate("new_connection_screen")
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Adicionar Conexão"
                )
            }
        }
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // LazyColumn (sem alterações)
            if (conexoes.isEmpty()) {
                Text(text = "Nenhuma conexão salva.", modifier = Modifier.padding(16.dp))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(conexoes) { conexao ->
                        Box {
                            ListItem(
                                headlineContent = { Text(conexao.alias) },
                                supportingContent = { Text("${conexao.user}@${conexao.url}") },
                                modifier = Modifier.combinedClickable(
                                    onClick = {
                                        navController.navigate("database_list_screen/${conexao.id}")
                                    },
                                    onLongClick = {
                                        conexaoSelecionada = conexao
                                        showDropdownMenu = true
                                    }
                                )
                            )

                            // DropdownMenu do "Editar/Excluir" (sem alterações)
                            DropdownMenu(
                                expanded = showDropdownMenu && conexaoSelecionada?.id == conexao.id,
                                onDismissRequest = { showDropdownMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Editar") },
                                    onClick = {
                                        showDropdownMenu = false
                                        navController.navigate(
                                            "new_connection_screen?id=${conexao.id}"
                                        )
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
                        Divider()
                    }
                }
            }

            // AlertDialog do "Excluir" (sem alterações)
            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = {
                        showDeleteDialog = false
                    },
                    title = { Text("Excluir Conexão") },
                    text = { Text("Tem certeza que quer excluir \"${conexaoSelecionada?.alias}\"?") },

                    confirmButton = {
                        Button(
                            onClick = {
                                conexaoSelecionada?.let {
                                    viewModel.deletarConexao(it)
                                }
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
                        TextButton(
                            onClick = {
                                showDeleteDialog = false
                            }
                        ) {
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
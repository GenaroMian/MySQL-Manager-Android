package com.genaro_mian.mysqlmanager // (Seu pacote)

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.DriverManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovaConexaoScreen(
    navController: NavController,
    conexaoId: Int // <-- 1. RECEBENDO O ID DA NAVEGAÇÃO
) {

    // --- Estados para os campos de texto ---
    var alias by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("3306") }
    var dbName by remember { mutableStateOf("") }
    var user by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // --- Ferramentas ---
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val conexaoDao = AppDatabase.getDatabase(context).conexaoDao()

    // --- Lógica de Edição ---
    val isEditing = conexaoId != 0
    val screenTitle = if (isEditing) "Editar Conexão" else "Nova Conexão"

    // --- 2. LÓGICA DE CARREGAMENTO (SE ESTIVER EDITANDO) ---
    LaunchedEffect(key1 = conexaoId) {
        if (isEditing) {
            // Mostra um "loading" rápido enquanto busca
            isLoading = true
            // Busca os dados no banco Room (fora da thread principal)
            val conexaoSalva = withContext(Dispatchers.IO) {
                conexaoDao.getConexaoPeloId(conexaoId)
            }

            // Preenche os campos de texto
            if (conexaoSalva != null) {
                alias = conexaoSalva.alias
                url = conexaoSalva.url
                port = conexaoSalva.port
                dbName = conexaoSalva.dbName
                user = conexaoSalva.user
                password = conexaoSalva.pass // Usando 'pass'
            }
            isLoading = false // Esconde o loading
        }
    }

    // --- Lógica de Validação (A mesma de antes) ---
    val isFormValid = alias.isNotBlank() &&
            url.isNotBlank() &&
            port.isNotBlank() &&
            user.isNotBlank() &&
            password.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(screenTitle) }, // Título dinâmico
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
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            // --- Campos de Texto (mesmos de antes) ---
            OutlinedTextField(value = alias, onValueChange = { alias = it }, label = { Text("Apelido (Alias)") }, modifier = Modifier.fillMaxWidth(), readOnly = isLoading)
            OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("URL (IP ou Domínio)") }, modifier = Modifier.fillMaxWidth(), readOnly = isLoading)
            OutlinedTextField(value = port, onValueChange = { port = it }, label = { Text("Porta") }, modifier = Modifier.fillMaxWidth(), readOnly = isLoading)
            OutlinedTextField(value = dbName, onValueChange = { dbName = it }, label = { Text("Nome do Banco (Opcional)") }, modifier = Modifier.fillMaxWidth(), readOnly = isLoading)
            OutlinedTextField(value = user, onValueChange = { user = it }, label = { Text("Usuário") }, modifier = Modifier.fillMaxWidth(), readOnly = isLoading)
            OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Senha") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), readOnly = isLoading)

            Spacer(modifier = Modifier.height(16.dp))

            // --- Botão TESTAR (mesmo de antes) ---
            Button(
                onClick = {
                    isLoading = true
                    scope.launch(Dispatchers.IO) {
                        try {
                            val connection = connectToMySQL(url, port, dbName, user, password)
                            connection.close()
                            withContext(Dispatchers.Main) {
                                isLoading = false
                                Toast.makeText(context, "Sucesso! Conexão OK.", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            withContext(Dispatchers.Main) {
                                isLoading = false
                                Toast.makeText(context, "Erro: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = isFormValid && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("TESTAR CONEXÃO")
                }
            }

            // --- 3. LÓGICA DE SALVAR/ATUALIZAR ---
            Button(
                onClick = {
                    // Cria o objeto para salvar
                    val conexaoParaSalvar = ConexaoSalva(
                        // Se estiver editando, passa o ID original.
                        // Se for novo, passa 0 (e o Room gera um novo ID).
                        id = if (isEditing) conexaoId else 0,
                        alias = alias,
                        url = url,
                        port = port,
                        dbName = dbName,
                        user = user,
                        pass = password
                    )

                    scope.launch(Dispatchers.IO) {
                        // O DAO vai fazer "REPLACE" (Update) se o ID já existir
                        // ou "INSERT" se o ID for 0.
                        conexaoDao.inserir(conexaoParaSalvar)

                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Conexão salva!", Toast.LENGTH_SHORT).show()
                            navController.popBackStack() // Volta para a MainScreen
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                ),
                enabled = isFormValid && !isLoading
            ) {
                Text("SALVAR")
            }
        }
    }
}


// --- Função de Conexão (A mesma de antes) ---
private fun connectToMySQL(url: String, port: String, dbName: String, user: String, pass: String): Connection {
    Class.forName("com.mysql.jdbc.Driver")
    // Lógica para 'dbName' opcional
    val dbSegment = if (dbName.isNotBlank()) "/$dbName" else ""
    val connectionUrl = "jdbc:mysql://$url:$port$dbSegment"
    return DriverManager.getConnection(connectionUrl, user, pass)
}
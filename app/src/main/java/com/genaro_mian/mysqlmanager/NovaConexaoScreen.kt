package com.genaro_mian.mysqlmanager // (Seu pacote)

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
    conexaoId: Int
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val conexaoDao = AppDatabase.getDatabase(context).conexaoDao()

    var alias by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("3306") }
    var dbName by remember { mutableStateOf("") }
    var user by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val isEditing = conexaoId != 0
    val screenTitle = if (isEditing) "Editar Conexão" else "Nova Conexão"

    // --- Carregar conexão existente ---
    LaunchedEffect(conexaoId) {
        if (isEditing) {
            isLoading = true
            val conexaoSalva = withContext(Dispatchers.IO) {
                conexaoDao.getConexaoPeloId(conexaoId)
            }
            conexaoSalva?.let {
                alias = it.alias
                url = it.url
                port = it.port
                dbName = it.dbName
                user = it.user
                password = it.pass
            }
            isLoading = false
        }
    }

    val isFormValid = alias.isNotBlank() && url.isNotBlank() && port.isNotBlank() && user.isNotBlank() && password.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        screenTitle,
                        style = MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.onPrimary)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            Text(
                "Configuração de Conexão",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Campos de texto com ícones e labels
                    StyledInputField(value = alias, onValueChange = { alias = it }, label = "Apelido", leadingIcon = Icons.Default.Storage)
                    StyledInputField(value = url, onValueChange = { url = it }, label = "Endereço IP ou Domínio", leadingIcon = Icons.Default.Cloud)
                    StyledInputField(value = port, onValueChange = { port = it }, label = "Porta", leadingIcon = Icons.Default.Settings)
                    StyledInputField(value = dbName, onValueChange = { dbName = it }, label = "Nome do Banco (opcional)", leadingIcon = Icons.Default.Folder)
                    StyledInputField(value = user, onValueChange = { user = it }, label = "Usuário", leadingIcon = Icons.Default.Person)
                    StyledInputField(
                        value = password,
                        onValueChange = { password = it },
                        label = "Senha",
                        isPassword = true,
                        leadingIcon = Icons.Default.Lock
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Botão de teste de conexão
            Button(
                onClick = {
                    isLoading = true
                    scope.launch(Dispatchers.IO) {
                        try {
                            val connection = connectToMySQL(url, port, dbName, user, password)
                            connection.close()
                            withContext(Dispatchers.Main) {
                                isLoading = false
                                Toast.makeText(context, "✅ Conexão bem-sucedida!", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                isLoading = false
                                Toast.makeText(context, "Erro: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = isFormValid && !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("TESTAR CONEXÃO")
                }
            }

            // Botão de salvar
            Button(
                onClick = {
                    val conexaoParaSalvar = ConexaoSalva(
                        id = if (isEditing) conexaoId else 0,
                        alias = alias,
                        url = url,
                        port = port,
                        dbName = dbName,
                        user = user,
                        pass = password
                    )
                    scope.launch(Dispatchers.IO) {
                        conexaoDao.inserir(conexaoParaSalvar)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "💾 Conexão salva com sucesso!", Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = isFormValid && !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("SALVAR CONEXÃO")
            }
        }
    }
}

// 🔹 Campo reutilizável estilizado
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StyledInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: ImageVector,
    isPassword: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        leadingIcon = { Icon(leadingIcon, null, tint = MaterialTheme.colorScheme.primary) },
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            cursorColor = MaterialTheme.colorScheme.primary
        )
    )
}



// --- Função de Conexão (A mesma de antes) ---
private fun connectToMySQL(url: String, port: String, dbName: String, user: String, pass: String): Connection {
    Class.forName("com.mysql.jdbc.Driver")
    // Lógica para 'dbName' opcional
    val dbSegment = if (dbName.isNotBlank()) "/$dbName" else ""
    val connectionUrl = "jdbc:mysql://$url:$port$dbSegment"
    return DriverManager.getConnection(connectionUrl, user, pass)
}
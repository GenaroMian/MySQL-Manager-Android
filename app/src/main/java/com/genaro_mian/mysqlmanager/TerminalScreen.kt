package com.genaro_mian.mysqlmanager // (Seu pacote)

import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.util.regex.Pattern

// --- LÓGICA DO SYNTAX HIGHLIGHTER (Sem alterações) ---
private val colorKeyword = Color(0xFF0033B1)
private val colorString = Color(0xFF067D17)
private val colorNumber = Color(0xFF1A5AF3)
private val colorComment = Color(0xFF808080)
private val styleKeyword = SpanStyle(color = colorKeyword, fontWeight = FontWeight.Bold)
private val styleString = SpanStyle(color = colorString)
private val styleNumber = SpanStyle(color = colorNumber)
private val styleComment = SpanStyle(color = colorComment)
private val styleNormal = SpanStyle(color = Color.Black)
private val keywords = setOf(
    "SELECT", "FROM", "WHERE", "INSERT", "INTO", "VALUES", "UPDATE", "SET", "DELETE",
    "CREATE", "TABLE", "DATABASE", "DROP", "ALTER", "ADD", "PRIMARY", "KEY", "FOREIGN",
    "INT", "VARCHAR", "TEXT", "DATE", "DATETIME", "CHAR", "NOT", "NULL", "AUTO_INCREMENT",
    "AND", "OR", "ON", "JOIN", "INNER", "LEFT", "RIGHT", "GROUP", "BY", "ORDER", "ASC", "DESC",
    "LIMIT", "SHOW", "TABLES", "DATABASES", "USE", "GRANT", "ALL", "PRIVILEGES", "WITH", "OPTION"
)
private val sqlPattern = Pattern.compile(
    "(?i)(${keywords.joinToString("|")})|" + // Grupo 1
            "('[^']*'|\"[^\"]*\")|" +              // Grupo 2
            "(\\b\\d+\\b)|" +                      // Grupo 3
            "(--[^\n]*)"                          // Grupo 4
)

private fun sqlSyntaxHighlighterWithUppercase(): VisualTransformation {
    return VisualTransformation { text ->
        val annotatedString = buildAnnotatedString {
            val matcher = sqlPattern.matcher(text.text)
            var lastIndex = 0
            while (matcher.find()) {
                val start = matcher.start()
                val end = matcher.end()
                if (start > lastIndex) {
                    withStyle(styleNormal) {
                        append(text.text.substring(lastIndex, start))
                    }
                }
                when {
                    matcher.group(1) != null -> {
                        withStyle(styleKeyword) {
                            append(matcher.group(1).uppercase())
                        }
                    }
                    matcher.group(2) != null -> {
                        withStyle(styleString) { append(matcher.group(2)) }
                    }
                    matcher.group(3) != null -> {
                        withStyle(styleNumber) { append(matcher.group(3)) }
                    }
                    matcher.group(4) != null -> {
                        withStyle(styleComment) { append(matcher.group(4)) }
                    }
                }
                lastIndex = end
            }
            if (lastIndex < text.text.length) {
                withStyle(styleNormal) {
                    append(text.text.substring(lastIndex))
                }
            }
        }
        TransformedText(annotatedString, OffsetMapping.Identity)
    }
}


// --- A TELA DO TERMINAL (Com Correção de "Colar") ---
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

    var sqlQuery by remember { mutableStateOf(TextFieldValue("")) }

    var conexaoSalva by remember { mutableStateOf<ConexaoSalva?>(null) }
    var lastSqlResult by remember { mutableStateOf<SqlResult>(SqlResult.Idle) }
    var resultData by remember { mutableStateOf(QueryResult()) }
    var errorMessage by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }
    var showResultSheet by remember { mutableStateOf(false) }
    var showErrorSheet by remember { mutableStateOf(false) }


    LaunchedEffect(key1 = conexaoId) {
        conexaoSalva = withContext(Dispatchers.IO) {
            conexaoDao.getConexaoPeloId(conexaoId)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Terminal (${conexaoSalva?.alias} / $dbName)") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Voltar", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                actions = {
                    if (lastSqlResult is SqlResult.Error) {
                        IconButton(onClick = { showErrorSheet = true }) {
                            Icon(Icons.Default.Error, "Mostrar Erro", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (sqlQuery.text.isNotBlank()) {
                        scope.launch {
                            lastSqlResult = SqlResult.Loading
                            showErrorSheet = false
                            showResultSheet = false

                            val conexao = withContext(Dispatchers.IO) {
                                conexaoDao.getConexaoPeloId(conexaoId)
                            }
                            if (conexao == null) {
                                lastSqlResult = SqlResult.Error("Conexão não encontrada.")
                            } else {
                                val result = executeGenericQuery(conexao, dbName, sqlQuery.text)
                                lastSqlResult = result

                                when (result) {
                                    is SqlResult.SuccessSelect -> {
                                        resultData = result.data
                                        showResultSheet = true
                                        sqlQuery = TextFieldValue("")
                                    }
                                    is SqlResult.SuccessUpdate -> {
                                        snackbarHostState.showSnackbar(
                                            "Sucesso! ${result.rowsAffected} linhas afetadas."
                                        )
                                        sqlQuery = TextFieldValue("")
                                    }
                                    is SqlResult.Error -> {
                                        errorMessage = result.message
                                        showErrorSheet = true
                                    }
                                    else -> {}
                                }
                            }
                        }
                    } else {
                        Toast.makeText(context, "Digite uma query SQL.", Toast.LENGTH_SHORT).show()
                    }
                },
                content = {
                    if (lastSqlResult is SqlResult.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Icon(Icons.Default.PlayArrow, "Executar")
                    }
                }
            )
        }
    ) { innerPadding ->

        // Editor com Números de Linha
        val lineCount = remember(sqlQuery.text) {
            sqlQuery.text.count { it == '\n' } + 1
        }

        val editorTextStyle = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = 16.sp
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. A CALHA (GUTTER) DE NÚMEROS DE LINHA
            Text(
                text = (1..lineCount).joinToString("\n"),
                modifier = Modifier
                    .fillMaxHeight()
                    .width(40.dp)
                    .padding(top = 16.dp, start = 8.dp),
                textAlign = TextAlign.End,
                style = editorTextStyle.copy(color = Color.Gray)
            )

            // 2. O EDITOR DE TEXTO REAL
            BasicTextField(
                value = sqlQuery,

                // **MUDANÇA AQUI: LÓGICA DE "COLAR" CORRIGIDA**
                onValueChange = { newValue ->
                    val oldText = sqlQuery.text
                    val newText = newValue.text

                    // **A CORREÇÃO (GUARDA):**
                    // Se a mudança NÃO foi de 1 carácter (é uma colagem ou deleção),
                    // apenas aceite o novo valor e não faça a "magia".
                    if (newText.length != oldText.length + 1) {
                        sqlQuery = newValue
                        return@BasicTextField
                    }

                    // Se chegámos aqui, foi uma digitação de 1 carácter.
                    // Podemos fazer a nossa "magia" de auto-completar:

                    val typedCharIndex = newValue.selection.start - 1

                    // Caso 1: Auto-completar parênteses
                    if (typedCharIndex >= 0) {
                        val typedChar = newText[typedCharIndex]
                        if (typedChar == '(') {
                            val textBefore = newText.substring(0, newValue.selection.start)
                            val textAfter = newText.substring(newValue.selection.start)
                            val finalText = textBefore + ")" + textAfter
                            sqlQuery = newValue.copy(
                                text = finalText,
                                selection = TextRange(newValue.selection.start)
                            )
                            return@BasicTextField
                        }
                    }

                    // Caso 2: Auto-indentar com "Enter"
                    if (newText.length > oldText.length && newText.count { it == '\n' } > oldText.count { it == '\n' }) {
                        val enterIndex = newValue.selection.start - 1
                        if (enterIndex >= 0) {
                            val lineStartIndex = oldText.lastIndexOf('\n', enterIndex - 1) + 1
                            val lineText = oldText.substring(lineStartIndex, enterIndex)
                            val charBeforeEnter = lineText.trimEnd().lastOrNull()

                            val textBefore = newText.substring(0, newValue.selection.start)
                            val textAfter = newText.substring(newValue.selection.start)

                            // 2a: Indentação "Sanduíche"
                            if (charBeforeEnter == '(') {
                                val indent = "    "
                                val finalText = textBefore + indent + "\n" + textAfter
                                val newCursorPos = newValue.selection.start + indent.length

                                sqlQuery = newValue.copy(
                                    text = finalText,
                                    selection = TextRange(newCursorPos)
                                )
                                return@BasicTextField
                            }

                            // 2b: Indentação "Persistente"
                            val currentIndentation = lineText.takeWhile { it.isWhitespace() }
                            if (currentIndentation.isNotEmpty()) {
                                val finalText = textBefore + currentIndentation + textAfter
                                val newCursorPos = newValue.selection.start + currentIndentation.length

                                sqlQuery = newValue.copy(
                                    text = finalText,
                                    selection = TextRange(newCursorPos)
                                )
                                return@BasicTextField
                            }
                        }
                    }

                    // Caso 3: Padrão (apenas aceita a mudança)
                    sqlQuery = newValue
                },

                visualTransformation = sqlSyntaxHighlighterWithUppercase(),
                textStyle = editorTextStyle.copy(color = Color.Black),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp, end = 16.dp, start = 8.dp)
            )
        }

        // Gavetas de Resultados (Sem alterações)
        if (showResultSheet) {
            ModalBottomSheet(onDismissRequest = { showResultSheet = false }) {
                Text("Resultados do SELECT", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
                DataTable(result = resultData)
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
        if (showErrorSheet) {
            ModalBottomSheet(onDismissRequest = { showErrorSheet = false }) {
                Text("Erro na Query", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error)
                Text(errorMessage, modifier = Modifier.padding(16.dp))
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

// --- LÓGICA DE EXECUÇÃO (Sem alterações) ---
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
                dbName = dbName,
                user = conexao.user,
                pass = conexao.pass
            )
            val statement = connection.createStatement()
            val hasResultSet = statement.execute(sql)
            if (hasResultSet) {
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

// --- UI DA TABELA (Sem alterações) ---
@Composable
private fun DataTable(result: QueryResult) {
    Box(modifier = Modifier
        .fillMaxSize()
        .horizontalScroll(rememberScrollState())) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    result.columns.forEach { columnName ->
                        Text(
                            text = columnName,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(150.dp)
                        )
                    }
                }
                Divider(color = Color.Black, thickness = 2.dp)
            }
            items(result.rows) { rowData ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowData.forEach { cellData ->
                        Text(
                            text = cellData,
                            modifier = Modifier.width(150.dp)
                        )
                    }
                }
                Divider()
            }
        }
    }
}


// --- FUNÇÃO DE CONEXÃO (Sem alterações) ---
private fun connectToMySQL(url: String, port: String, dbName: String, user: String, pass: String): Connection {
    Class.forName("com.mysql.jdbc.Driver")
    val dbSegment = if (dbName.isNotBlank()) "/$dbName" else ""
    val connectionUrl = "jdbc:mysql://$url:$port$dbSegment"
    return DriverManager.getConnection(connectionUrl, user, pass)
}
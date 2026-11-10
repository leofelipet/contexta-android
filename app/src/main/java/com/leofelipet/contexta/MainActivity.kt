package com.leofelipet.contexta

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private sealed interface Screen {
    data object Setup : Screen
    data object Inbox : Screen
    data class Thread(val id: String, val title: String) : Screen
}

class MainActivity : ComponentActivity() {
    private val api = Api()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val prefs = Prefs(this)
        setContent {
            MaterialTheme {
                var screen by remember {
                    mutableStateOf<Screen>(if (prefs.configured()) Screen.Inbox else Screen.Setup)
                }
                when (val current = screen) {
                    Screen.Setup -> SetupScreen(api, prefs) { screen = Screen.Inbox }
                    Screen.Inbox -> InboxScreen(
                        api, prefs,
                        onOpen = { screen = Screen.Thread(it.id, it.displayTitle) },
                        onLogout = { prefs.clear(); screen = Screen.Setup },
                        onUnauthorized = { prefs.clear(); screen = Screen.Setup },
                        installApk = ::installApk,
                    )
                    is Screen.Thread -> ThreadScreen(
                        api, prefs, current.id, current.title,
                        onBack = { screen = Screen.Inbox },
                        onUnauthorized = { prefs.clear(); screen = Screen.Setup },
                    )
                }
            }
        }
    }

    private fun installApk(file: File) {
        if (Build.VERSION.SDK_INT >= 26 && !packageManager.canRequestPackageInstalls()) {
            startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName")))
            return
        }
        val uri = FileProvider.getUriForFile(this, "$packageName.files", file)
        startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
        )
    }
}

@Composable
private fun SetupScreen(api: Api, prefs: Prefs, onReady: () -> Unit) {
    var url by remember { mutableStateOf(prefs.baseUrl) }
    var token by remember { mutableStateOf(prefs.token) }
    var error by remember { mutableStateOf<String?>(null) }
    var pending by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun save() {
        if (pending) return
        pending = true
        error = null
        scope.launch {
            try {
                val base = normalizeBaseUrl(url)
                withContext(Dispatchers.IO) { api.conversations(base, token.trim(), query = "") }
                prefs.baseUrl = base
                prefs.token = token.trim()
                onReady()
            } catch (_: Unauthorized) {
                error = "Token recusado. Use o API_BEARER_TOKEN do backend Go."
            } catch (_: Exception) {
                error = "Não conectou. Cole a URL do Go (não a do site) e o token."
            } finally {
                pending = false
            }
        }
    }

    Column(
        Modifier.fillMaxSize().imePadding().padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Contexta", style = MaterialTheme.typography.headlineMedium)
        Text("URL do backend Go e o token da API.", Modifier.padding(top = 8.dp, bottom = 24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            url, { url = it }, label = { Text("URL do backend") },
            placeholder = { Text("https://seu-servidor") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            token, { token = it }, label = { Text("API_BEARER_TOKEN") },
            singleLine = true, visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { save() }),
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        )
        if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp))
        Button(onClick = { save() }, enabled = !pending && url.isNotBlank() && token.isNotBlank(), modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) {
            Text(if (pending) "Conectando..." else "Entrar")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InboxScreen(
    api: Api,
    prefs: Prefs,
    onOpen: (Conversation) -> Unit,
    onLogout: () -> Unit,
    onUnauthorized: () -> Unit,
    installApk: (File) -> Unit,
) {
    var items by remember { mutableStateOf(listOf<Conversation>()) }
    var cursor by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var update by remember { mutableStateOf<AndroidUpdate?>(null) }
    var downloading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    fun fail(e: Exception) {
        if (e is Unauthorized) onUnauthorized() else error = "Não foi possível carregar as conversas."
    }

    suspend fun fetch(reset: Boolean) {
        val page = withContext(Dispatchers.IO) {
            api.conversations(prefs.baseUrl, prefs.token, cursor = if (reset) null else cursor, query = query)
        }
        items = if (reset) page.data else items + page.data.filter { next -> items.none { it.id == next.id } }
        cursor = page.nextCursor
    }

    fun reload() {
        scope.launch {
            loading = true
            error = null
            try {
                fetch(true)
            } catch (e: Exception) {
                fail(e)
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(prefs.baseUrl, prefs.token) { reload() }

    DisposableEffect(prefs.baseUrl, prefs.token) {
        val source = api.events(prefs.baseUrl, prefs.token) { reload() }
        onDispose { source.cancel() }
    }

    LaunchedEffect(prefs.baseUrl, prefs.token) {
        update = try {
            withContext(Dispatchers.IO) { api.update(prefs.baseUrl, prefs.token) }
        } catch (_: Exception) {
            null
        }
    }

    val remote = update
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Conversas") },
                actions = {
                    TextButton(onClick = { reload() }) { Text("Atualizar") }
                    TextButton(onClick = onLogout) { Text("Sair") }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                query, { query = it },
                label = { Text("Buscar") }, singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { reload() }),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
            if (loading && items.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
                    items(items, key = { it.id }) { conversation ->
                        Column(Modifier.fillMaxWidth().clickable { onOpen(conversation) }.padding(horizontal = 16.dp, vertical = 12.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(conversation.displayTitle, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                Text(formatTimestamp(conversation.lastMessageAt ?: conversation.lastMessage?.timestamp.orEmpty()), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(conversation.lastMessage?.displayText().orEmpty(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                        }
                    }
                    if (cursor != null) {
                        item {
                            TextButton(onClick = {
                                scope.launch {
                                    try {
                                        fetch(false)
                                    } catch (e: Exception) {
                                        fail(e)
                                    }
                                }
                            }, modifier = Modifier.fillMaxWidth()) { Text("Mais") }
                        }
                    }
                }
            }
        }
    }

    if (remote != null && needsUpdate(BuildConfig.VERSION_CODE, remote.versionCode) && !downloading) {
        AlertDialog(
            onDismissRequest = { update = null },
            title = { Text("Atualização ${remote.versionName.ifBlank { remote.versionCode.toString() }}") },
            text = { Text(remote.notes.ifBlank { "Há uma versão nova do app." }) },
            confirmButton = {
                TextButton(onClick = {
                    downloading = true
                    scope.launch {
                        try {
                            val file = File(context.cacheDir, "updates/app.apk")
                            withContext(Dispatchers.IO) { api.downloadApk(prefs.baseUrl, prefs.token, file) }
                            installApk(file)
                        } catch (_: Exception) {
                            error = "Não foi possível baixar a atualização."
                        } finally {
                            downloading = false
                            update = null
                        }
                    }
                }) { Text("Atualizar") }
            },
            dismissButton = { TextButton(onClick = { update = null }) { Text("Depois") } },
        )
    }
    if (downloading) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Baixando") },
            text = { LinearProgressIndicator(Modifier.fillMaxWidth()) },
            confirmButton = {},
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThreadScreen(
    api: Api,
    prefs: Prefs,
    conversationId: String,
    title: String,
    onBack: () -> Unit,
    onUnauthorized: () -> Unit,
) {
    var items by remember { mutableStateOf(listOf<Message>()) }
    var cursor by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    fun fail(e: Exception) {
        if (e is Unauthorized) onUnauthorized() else error = "Não foi possível carregar as mensagens."
    }

    suspend fun fetch(reset: Boolean) {
        val page = withContext(Dispatchers.IO) {
            api.messages(prefs.baseUrl, prefs.token, conversationId, cursor = if (reset) null else cursor)
        }
        items = if (reset) page.data else items + page.data.filter { next -> items.none { it.id == next.id } }
        cursor = page.nextCursor
    }

    fun reload() {
        scope.launch {
            loading = true
            error = null
            try {
                fetch(true)
            } catch (e: Exception) {
                fail(e)
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(conversationId) { reload() }
    DisposableEffect(conversationId, prefs.baseUrl, prefs.token) {
        val source = api.events(prefs.baseUrl, prefs.token) { reload() }
        onDispose { source.cancel() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = { TextButton(onClick = onBack) { Text("Voltar") } },
            )
        },
    ) { padding ->
        if (loading && items.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                reverseLayout = true,
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(items, key = { it.id }) { message ->
                    val inbound = message.direction != "outbound"
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (inbound) Arrangement.Start else Arrangement.End) {
                        Column(
                            Modifier.widthIn(max = 320.dp)
                                .background(
                                    if (inbound) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primaryContainer,
                                    RoundedCornerShape(16.dp),
                                )
                                .padding(12.dp),
                        ) {
                            if (inbound && !message.sender.isNullOrBlank()) {
                                Text(message.sender, style = MaterialTheme.typography.labelMedium)
                            }
                            Text(message.displayText())
                            Text(formatTimestamp(message.timestamp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if (cursor != null) {
                    item {
                        TextButton(onClick = {
                            scope.launch {
                                try {
                                    fetch(false)
                                } catch (e: Exception) {
                                    fail(e)
                                }
                            }
                        }, modifier = Modifier.fillMaxWidth()) { Text("Mais antigas") }
                    }
                }
                if (error != null) item { Text(error!!, color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

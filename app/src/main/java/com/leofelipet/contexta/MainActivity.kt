package com.leofelipet.contexta

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private sealed interface Screen {
    data object Setup : Screen
    data object Help : Screen
    data object Inbox : Screen
    data class Thread(val id: String, val title: String) : Screen
}

class MainActivity : ComponentActivity() {
    private val api = Api()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true
        val prefs = Prefs(this)
        setContent {
            ContextaTheme {
                var screen by remember {
                    mutableStateOf<Screen>(if (prefs.configured()) Screen.Inbox else Screen.Setup)
                }
                when (val current = screen) {
                    Screen.Setup -> SetupScreen(
                        api, prefs, ::installApk,
                        onHelp = { screen = Screen.Help },
                    ) { screen = Screen.Inbox }
                    Screen.Help -> HelpScreen(onBack = { screen = Screen.Setup })
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
private fun SetupScreen(
    api: Api,
    prefs: Prefs,
    installApk: (File) -> Unit,
    onHelp: () -> Unit,
    onReady: () -> Unit,
) {
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
                error = "Essa chave não foi aceita. Confira se copiou inteira."
            } catch (_: Exception) {
                error = "Não foi possível conectar. Confira o endereço e tente de novo."
            } finally {
                pending = false
            }
        }
    }

    Box(Modifier.fillMaxSize().background(Paper)) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
        ) {
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 20.dp),
            ) {
                Image(
                    painterResource(R.drawable.ic_launcher),
                    contentDescription = "Contexta",
                    modifier = Modifier.size(72.dp).clip(RoundedCornerShape(18.dp)),
                )
                Text("Boas-vindas", color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, modifier = Modifier.padding(top = 20.dp))
                Text(
                    "Para começar, informe o endereço do seu Contexta e a chave de acesso.",
                    color = Muted,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
                )
                OutlinedTextField(
                    url, { url = it },
                    label = { Text("Endereço") },
                    placeholder = { Text("https://") },
                    singleLine = true,
                    colors = contextaFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    token, { token = it },
                    label = { Text("Chave de acesso") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    colors = contextaFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { save() }),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, fontSize = 14.sp, modifier = Modifier.padding(top = 12.dp))
                }
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = { save() },
                    enabled = !pending && url.isNotBlank() && token.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Brand,
                        contentColor = Color.White,
                        disabledContainerColor = Brand.copy(alpha = 0.35f),
                        disabledContentColor = Color.White,
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) {
                    Text(if (pending) "Conectando..." else "Entrar", fontWeight = FontWeight.SemiBold)
                }
                TextButton(onClick = onHelp, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    Text("Onde encontro esses dados?", color = Brand)
                }
            }
            Text(
                BuildConfig.VERSION_NAME,
                color = Muted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            )
        }
        GitHubUpdate(api, installApk)
    }
}

@Composable
private fun HelpScreen(onBack: () -> Unit) {
    val steps = listOf(
        "Você precisa de duas coisas: o endereço do Contexta e a chave de acesso.",
        "O endereço começa com https://. É o servidor, não a página de login do painel no computador.",
        "A chave de acesso é um texto longo. Quem instalou o Contexta encontra no servidor, no arquivo .env, na linha API_BEARER_TOKEN — copie o que vem depois do =.",
        "Cole o endereço no primeiro campo e a chave no segundo.",
        "Toque em Entrar. Se der erro, confira se a chave foi copiada inteira, sem espaço a mais.",
    )
    Column(
        Modifier
            .fillMaxSize()
            .background(Paper)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        TextButton(onClick = onBack, modifier = Modifier.padding(start = 8.dp, top = 4.dp)) {
            Text("Voltar", color = Brand, fontWeight = FontWeight.SemiBold)
        }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
        ) {
            Text("Onde encontrar os dados", color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 24.sp)
            Spacer(Modifier.height(20.dp))
            steps.forEachIndexed { index, step ->
                Row(Modifier.padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(BrandSoft),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("${index + 1}", color = Brand, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Text(step, color = Ink, fontSize = 15.sp, lineHeight = 22.sp, modifier = Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Voltar", color = Brand, fontWeight = FontWeight.SemiBold)
            }
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
    val scope = rememberCoroutineScope()

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

    Scaffold(
        containerColor = Paper,
        topBar = {
            TopAppBar(
                title = { Text("Conversas", fontWeight = FontWeight.SemiBold, color = Ink) },
                actions = {
                    TextButton(onClick = { reload() }) { Text("Atualizar", color = Brand, fontWeight = FontWeight.SemiBold) }
                    TextButton(onClick = onLogout) { Text("Sair", color = Muted, fontWeight = FontWeight.SemiBold) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite, titleContentColor = Ink),
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                query, { query = it },
                label = { Text("Buscar") },
                singleLine = true,
                colors = contextaFieldColors(),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { reload() }),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
            HorizontalDivider(color = Line)
            if (loading && items.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Brand) }
            } else {
                if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
                LazyColumn(Modifier.fillMaxSize().background(SurfaceWhite), contentPadding = PaddingValues(bottom = 24.dp)) {
                    items(items, key = { it.id }) { conversation ->
                        Column(Modifier.fillMaxWidth().clickable { onOpen(conversation) }) {
                            Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(conversation.displayTitle, color = Ink, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f).padding(end = 12.dp))
                                    Text(
                                        formatTimestamp(conversation.lastMessageAt ?: conversation.lastMessage?.timestamp.orEmpty()),
                                        color = Muted,
                                        fontSize = 12.sp,
                                    )
                                }
                                Text(
                                    conversation.lastMessage?.displayText().orEmpty(),
                                    color = Muted,
                                    fontSize = 14.sp,
                                    maxLines = 2,
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                            }
                            HorizontalDivider(color = Line)
                        }
                    }
                    if (cursor != null) {
                        item {
                            TextButton(
                                onClick = {
                                    scope.launch {
                                        try {
                                            fetch(false)
                                        } catch (e: Exception) {
                                            fail(e)
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text("Mais", color = Brand, fontWeight = FontWeight.SemiBold) }
                        }
                    }
                }
            }
        }
    }
    GitHubUpdate(api, installApk)
}

@Composable
private fun GitHubUpdate(api: Api, installApk: (File) -> Unit) {
    var remote by remember { mutableStateOf<GithubRelease?>(null) }
    var downloading by remember { mutableStateOf(false) }
    var dismissed by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        remote = try {
            withContext(Dispatchers.IO) { api.githubLatest() }
        } catch (_: Exception) {
            null
        }
    }

    val release = remote
    val apkUrl = release?.apkUrl
    if (!dismissed && !downloading && release != null && apkUrl != null && needsUpdate(BuildConfig.VERSION_NAME, release.versionName)) {
        AlertDialog(
            onDismissRequest = { dismissed = true },
            title = { Text("Atualização ${release.versionName}", color = Ink) },
            text = { Text(release.body.ifBlank { "Há uma versão nova do app." }, color = Muted) },
            confirmButton = {
                TextButton(onClick = {
                    downloading = true
                    scope.launch {
                        try {
                            val file = File(context.cacheDir, "updates/app.apk")
                            withContext(Dispatchers.IO) { api.downloadUrl(apkUrl, file) }
                            installApk(file)
                        } catch (_: Exception) {
                            dismissed = true
                        } finally {
                            downloading = false
                        }
                    }
                }) { Text("Atualizar", color = Brand, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = { TextButton(onClick = { dismissed = true }) { Text("Depois", color = Muted) } },
            containerColor = SurfaceWhite,
        )
    }
    if (downloading) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Baixando", color = Ink) },
            text = { LinearProgressIndicator(Modifier.fillMaxWidth(), color = Brand, trackColor = BrandSoft) },
            confirmButton = {},
            containerColor = SurfaceWhite,
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
        containerColor = Chat,
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.SemiBold, color = Ink, maxLines = 1) },
                navigationIcon = { TextButton(onClick = onBack) { Text("Voltar", color = Brand, fontWeight = FontWeight.SemiBold) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite, titleContentColor = Ink),
            )
        },
    ) { padding ->
        if (loading && items.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Brand) }
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
                            Modifier
                                .widthIn(max = 320.dp)
                                .background(if (inbound) SurfaceWhite else OutboundBubble, RoundedCornerShape(16.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                        ) {
                            if (inbound && !message.sender.isNullOrBlank()) {
                                Text(message.sender, color = Brand, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                            }
                            Text(message.displayText(), color = Ink, fontSize = 14.sp, lineHeight = 20.sp)
                            Text(formatTimestamp(message.timestamp), color = Muted, fontSize = 10.sp, modifier = Modifier.align(Alignment.End).padding(top = 4.dp))
                        }
                    }
                }
                if (cursor != null) {
                    item {
                        TextButton(
                            onClick = {
                                scope.launch {
                                    try {
                                        fetch(false)
                                    } catch (e: Exception) {
                                        fail(e)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Mais antigas", color = Brand, fontWeight = FontWeight.SemiBold) }
                    }
                }
                if (error != null) item { Text(error!!, color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

package com.leofelipet.contexta

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private enum class HomeTab(val label: String, val icon: ImageVector) {
    Conversas("Conversas", Icons.Filled.MailOutline),
    Contatos("Contatos", Icons.Filled.Person),
    Tarefas("Tarefas", Icons.Filled.CheckCircle),
    Busca("Busca", Icons.Filled.Search),
}

private sealed interface Screen {
    data object Setup : Screen
    data object Help : Screen
    data object Inbox : Screen
    data object More : Screen
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
                var cache by remember { mutableStateOf(SessionCache()) }
                var stack by remember {
                    mutableStateOf(listOf<Screen>(if (prefs.configured()) Screen.Inbox else Screen.Setup))
                }
                var tab by remember { mutableStateOf(HomeTab.Conversas) }
                fun back() {
                    if (stack.size > 1) stack = stack.dropLast(1)
                }
                fun leave() {
                    prefs.clear()
                    cache = SessionCache()
                    tab = HomeTab.Conversas
                    stack = listOf(Screen.Setup)
                }
                BackHandler(enabled = stack.size > 1) { back() }
                when (val current = stack.last()) {
                    Screen.Setup -> SetupScreen(
                        api, prefs, ::installApk,
                        onHelp = { stack = stack + Screen.Help },
                    ) { stack = listOf(Screen.Inbox) }
                    Screen.Help -> HelpScreen(onBack = ::back)
                    Screen.Inbox -> HomeScreen(
                        api, prefs, cache, tab,
                        onTab = { tab = it },
                        onOpen = { stack = stack + Screen.Thread(it.id, it.displayTitle) },
                        onUnauthorized = ::leave,
                        onMore = { stack = stack + Screen.More },
                        installApk = ::installApk,
                    )
                    Screen.More -> MoreScreen(onBack = ::back, onLogout = ::leave)
                    is Screen.Thread -> ThreadScreen(
                        api, prefs, cache, current.id, current.title,
                        onBack = ::back,
                        onUnauthorized = ::leave,
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
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painterResource(R.drawable.ic_launcher),
                    contentDescription = "Contexta",
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)),
                )
                Text(
                    "Boas-vindas",
                    color = Ink,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 28.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 20.dp),
                )
                Text(
                    "Para começar, informe o endereço do seu Contexta e a chave de acesso.",
                    color = Muted,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    textAlign = TextAlign.Center,
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
                    Text(
                        error!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    )
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
        IconButton(onClick = onBack, modifier = Modifier.padding(start = 4.dp, top = 4.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = Ink)
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
        }
    }
}

@Composable
private fun HomeScreen(
    api: Api,
    prefs: Prefs,
    cache: SessionCache,
    tab: HomeTab,
    onTab: (HomeTab) -> Unit,
    onOpen: (Conversation) -> Unit,
    onUnauthorized: () -> Unit,
    onMore: () -> Unit,
    installApk: (File) -> Unit,
) {
    BackHandler(enabled = tab != HomeTab.Conversas) { onTab(HomeTab.Conversas) }
    Column(Modifier.fillMaxSize().background(Paper)) {
        Box(Modifier.weight(1f)) {
            when (tab) {
                HomeTab.Conversas -> InboxScreen(api, prefs, cache, onOpen, onUnauthorized, onMore)
                else -> ComingSoonScreen(tab, onMore)
            }
        }
        HomeBottomBar(tab, onTab)
    }
    GitHubUpdate(api, installApk)
}

@Composable
private fun HomeBottomBar(tab: HomeTab, onTab: (HomeTab) -> Unit) {
    Column(Modifier.background(SurfaceWhite)) {
        HorizontalDivider(color = Line)
        NavigationBar(containerColor = SurfaceWhite, contentColor = Muted, tonalElevation = 0.dp) {
            HomeTab.entries.forEach { item ->
                NavigationBarItem(
                    selected = tab == item,
                    onClick = { onTab(item) },
                    icon = { Icon(item.icon, contentDescription = item.label) },
                    label = { Text(item.label) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Brand,
                        selectedTextColor = Brand,
                        indicatorColor = BrandSoft,
                        unselectedIconColor = Muted,
                        unselectedTextColor = Muted,
                    ),
                )
            }
        }
    }
}

@Composable
private fun ComingSoonScreen(tab: HomeTab, onMore: () -> Unit) {
    Scaffold(
        containerColor = Paper,
        contentWindowInsets = WindowInsets(0, 0, 0, 0), // above HomeBottomBar; default insets leave a blank strip
        topBar = {
            ContextaBar(tab.label) {
                IconButton(onClick = onMore) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Mais", tint = Ink)
                }
            }
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.size(64.dp).clip(CircleShape).background(BrandSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(tab.icon, contentDescription = null, tint = Brand, modifier = Modifier.size(28.dp))
            }
            Text(tab.label, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, modifier = Modifier.padding(top = 16.dp))
            Text(
                "Em breve neste app.\nPor enquanto use o painel web.",
                color = Muted,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

private data class MoreItem(val label: String, val icon: ImageVector)

private val moreItems = listOf(
    MoreItem("Visão geral", Icons.Filled.Home),
    MoreItem("Empresas", Icons.Filled.AccountBox),
    MoreItem("Memórias", Icons.Filled.Star),
    MoreItem("Manutenção", Icons.Filled.Build),
    MoreItem("Integrações", Icons.Filled.Share),
    MoreItem("Sistema", Icons.Filled.Settings),
)

@Composable
private fun MoreScreen(onBack: () -> Unit, onLogout: () -> Unit) {
    Scaffold(
        containerColor = Paper,
        topBar = { ContextaBar("Mais", onBack) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).background(SurfaceWhite)) {
            Text(
                "Painel",
                color = Muted,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
            )
            moreItems.forEach { item ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        Modifier.size(36.dp).clip(CircleShape).background(BrandSoft),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(item.icon, contentDescription = null, tint = Brand, modifier = Modifier.size(18.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(item.label, color = Ink, fontWeight = FontWeight.SemiBold)
                        Text("Em breve", color = Muted, fontSize = 12.sp)
                    }
                }
                HorizontalDivider(Modifier.padding(start = 64.dp), color = Line)
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onLogout)
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = Muted)
                Text("Sair", color = Ink, fontWeight = FontWeight.SemiBold)
            }
            Text(
                "Versão ${BuildConfig.VERSION_NAME}",
                color = Muted,
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun InboxScreen(
    api: Api,
    prefs: Prefs,
    cache: SessionCache,
    onOpen: (Conversation) -> Unit,
    onUnauthorized: () -> Unit,
    onMore: () -> Unit,
) {
    var items by remember { mutableStateOf(cache.conversations) }
    var cursor by remember { mutableStateOf(cache.conversationCursor) }
    var query by remember { mutableStateOf(cache.conversationQuery) }
    var type by remember { mutableStateOf(cache.conversationType) }
    var blockedConversations by remember { mutableStateOf(cache.blockedConversationIds) }
    var blockedContacts by remember { mutableStateOf(cache.blockedContactIds) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(!cache.conversationsReady) }
    var loadingMore by remember { mutableStateOf(false) }
    val paging = remember { booleanArrayOf(false) }
    val ticket = remember { intArrayOf(0) }
    val hasRows = remember { booleanArrayOf(cache.conversations.isNotEmpty()) }
    val scope = rememberCoroutineScope()
    val listState = cache.conversationListState

    fun fail(e: Exception) {
        if (e is Unauthorized) onUnauthorized() else error = "Não foi possível carregar as conversas."
    }

    fun loadDenylist() {
        scope.launch {
            try {
                val page = withContext(Dispatchers.IO) { api.denylist(prefs.baseUrl, prefs.token) }
                val sets = denylistSets(page.data)
                cache.blockedConversationIds = sets.first
                cache.blockedContactIds = sets.second
                blockedConversations = sets.first
                blockedContacts = sets.second
            } catch (e: Unauthorized) {
                onUnauthorized()
            } catch (_: Exception) {
            }
        }
    }

    fun loadMore() {
        val next = cache.conversationCursor ?: return
        if (paging[0]) return
        paging[0] = true
        loadingMore = true
        val mine = ticket[0]
        val q = cache.conversationQuery
        val kind = cache.conversationType
        scope.launch {
            try {
                val page = withContext(Dispatchers.IO) {
                    api.conversations(prefs.baseUrl, prefs.token, cursor = next, query = q, type = kind)
                }
                if (mine != ticket[0]) return@launch
                if (page.data.isEmpty()) {
                    cache.conversationCursor = null
                    cursor = null
                    return@launch
                }
                val merged = cache.conversations + page.data.filter { row -> cache.conversations.none { it.id == row.id } }
                cache.conversations = merged
                cache.conversationCursor = page.nextCursor
                items = merged
                cursor = page.nextCursor
                hasRows[0] = merged.isNotEmpty()
            } catch (e: Exception) {
                if (mine == ticket[0]) fail(e)
            } finally {
                if (mine == ticket[0]) {
                    paging[0] = false
                    loadingMore = false
                }
            }
        }
    }

    fun reload(q: String, kind: String) {
        val mine = ++ticket[0]
        paging[0] = true
        if (kind != cache.conversationType) {
            items = emptyList()
            cursor = null
            hasRows[0] = false
            loading = true
        }
        scope.launch {
            if (!hasRows[0]) loading = true
            error = null
            try {
                val page = withContext(Dispatchers.IO) {
                    api.conversations(prefs.baseUrl, prefs.token, query = q, type = kind)
                }
                if (mine != ticket[0]) return@launch
                val sameFilter = q == cache.conversationQuery && kind == cache.conversationType
                val hadMore = sameFilter && cache.conversations.size > page.data.size
                val freshIds = page.data.map { it.id }.toSet()
                val merged = if (hadMore) {
                    page.data + cache.conversations.filter { it.id !in freshIds }
                } else {
                    page.data
                }
                if (!sameFilter) listState.scrollToItem(0)
                cache.conversationQuery = q
                cache.conversationType = kind
                cache.conversations = merged
                if (!hadMore) cache.conversationCursor = page.nextCursor
                cache.conversationsReady = true
                items = merged
                cursor = cache.conversationCursor
                hasRows[0] = merged.isNotEmpty()
            } catch (e: Exception) {
                if (mine == ticket[0]) fail(e)
            } finally {
                if (mine == ticket[0]) {
                    paging[0] = false
                    loading = false
                    val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
                    if (shouldPrefetch(last, cache.conversations.size) && cache.conversationCursor != null) loadMore()
                }
            }
        }
    }

    LaunchedEffect(query, type) {
        val warm = cache.conversationsReady && cache.conversationQuery == query && cache.conversationType == type
        if (!warm && query.isNotEmpty() && type == cache.conversationType) delay(300)
        reload(query, type)
    }

    LaunchedEffect(prefs.baseUrl, prefs.token) { loadDenylist() }

    LaunchedEffect(listState) {
        snapshotFlow {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            Triple(last, items.size, cursor)
        }.collect { (last, count, next) ->
            if (next != null && shouldPrefetch(last, count)) loadMore()
        }
    }

    DisposableEffect(prefs.baseUrl, prefs.token) {
        val source = api.events(prefs.baseUrl, prefs.token) { reload(cache.conversationQuery, cache.conversationType) }
        onDispose { source.cancel() }
    }

    Scaffold(
        containerColor = Paper,
        contentWindowInsets = WindowInsets(0, 0, 0, 0), // above HomeBottomBar; default insets leave a blank strip
        topBar = {
            ContextaBar("Conversas") {
                IconButton(onClick = { reload(query, type); loadDenylist() }) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Atualizar", tint = Ink)
                }
                IconButton(onClick = onMore) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Mais", tint = Ink)
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).background(SurfaceWhite)) {
            OutlinedTextField(
                query,
                { query = it },
                placeholder = { Text("Buscar conversa") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Muted) },
                trailingIcon = if (query.isBlank()) {
                    null
                } else {
                    {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = "Limpar", tint = Muted)
                        }
                    }
                },
                singleLine = true,
                colors = contextaFieldColors(),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
            TypePills(type) { type = it }
            when {
                loading && items.isEmpty() -> ConversationSkeleton()
                items.isEmpty() -> EmptyHint(
                    Icons.Filled.MailOutline,
                    error ?: if (query.isBlank() && type.isBlank()) "Nenhuma conversa ainda" else "Nenhuma conversa encontrada",
                )
                else -> LazyColumn(Modifier.fillMaxSize(), state = listState) {
                    if (error != null) item { Text(error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp)) }
                    items(items, key = { it.id }) { conversation ->
                        ConversationRow(
                            conversation,
                            blocked = conversation.isBlocked(blockedConversations, blockedContacts),
                        ) { onOpen(conversation) }
                        HorizontalDivider(Modifier.padding(start = 72.dp), color = Line)
                    }
                    if (loadingMore) item { ConversationSkeleton(rows = 2) }
                }
            }
        }
    }
}

private val typePills = listOf("" to "Todas", "direct" to "Diretas", "group" to "Grupos")

@Composable
private fun TypePills(selected: String, onSelect: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        typePills.forEach { (value, label) ->
            val on = selected == value
            Text(
                label,
                color = if (on) Brand else Muted,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (on) BrandSoft else Paper)
                    .clickable { onSelect(value) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun ConversationRow(conversation: Conversation, blocked: Boolean, onOpen: () -> Unit) {
    val whenLabel = formatListTime(conversation.lastMessageAt ?: conversation.lastMessage?.timestamp.orEmpty())
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        InitialAvatar(conversation.displayTitle)
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    conversation.displayTitle,
                    color = Ink,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                )
                if (whenLabel.isNotBlank()) Text(whenLabel, color = Muted, fontSize = 12.sp)
            }
            Row(
                Modifier.padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    conversation.lastMessage?.displayText().orEmpty().ifBlank { "Sem mensagens" },
                    color = Muted,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (conversation.isGroup) StatusChip("Grupo", Color(0xFFF1F5F9), Color(0xFF475569))
                if (blocked) StatusChip("Bloqueada", Color(0xFFFFFBEB), Color(0xFF92400E), Icons.Filled.Warning)
            }
        }
    }
}

@Composable
private fun StatusChip(label: String, background: Color, color: Color, icon: ImageVector? = null) {
    Row(
        Modifier.clip(RoundedCornerShape(20.dp)).background(background).padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(10.dp))
        Text(label, color = color, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
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

@Composable
private fun ThreadScreen(
    api: Api,
    prefs: Prefs,
    cache: SessionCache,
    conversationId: String,
    title: String,
    onBack: () -> Unit,
    onUnauthorized: () -> Unit,
) {
    val thread = remember(conversationId) { cache.threads.getOrPut(conversationId) { ThreadCache() } }
    var items by remember(conversationId) { mutableStateOf(thread.items) }
    var cursor by remember(conversationId) { mutableStateOf(thread.cursor) }
    var detail by remember(conversationId) { mutableStateOf(thread.detail) }
    var companies by remember { mutableStateOf(cache.companies) }
    var blocked by remember(conversationId) {
        mutableStateOf(conversationId in cache.blockedConversationIds)
    }
    var error by remember(conversationId) { mutableStateOf<String?>(null) }
    var loading by remember(conversationId) { mutableStateOf(thread.items.isEmpty()) }
    var loadingMore by remember(conversationId) { mutableStateOf(false) }
    var confirmBlock by remember(conversationId) { mutableStateOf(false) }
    var pickCompany by remember(conversationId) { mutableStateOf(false) }
    var contact by remember(conversationId) { mutableStateOf<Contact?>(null) }
    var showContact by remember(conversationId) { mutableStateOf(false) }
    val paging = remember(conversationId) { booleanArrayOf(false) }
    val ticket = remember(conversationId) { intArrayOf(0) }
    val hasRows = remember(conversationId) { booleanArrayOf(thread.items.isNotEmpty()) }
    val scope = rememberCoroutineScope()
    val listState = thread.listState
    val heading = detail?.displayTitle ?: title

    fun fail(e: Exception) {
        if (e is Unauthorized) onUnauthorized() else error = "Não foi possível carregar as mensagens."
    }

    fun markBlocked() {
        cache.blockedConversationIds = cache.blockedConversationIds + conversationId
        blocked = true
    }

    fun loadHeader() {
        scope.launch {
            try {
                val conv = withContext(Dispatchers.IO) { api.conversation(prefs.baseUrl, prefs.token, conversationId) }
                thread.detail = conv
                detail = conv
                blocked = conv.isBlocked(cache.blockedConversationIds, cache.blockedContactIds)
            } catch (e: Unauthorized) {
                onUnauthorized()
            } catch (_: Exception) {
            }
        }
        if (cache.companies.isNotEmpty()) return
        scope.launch {
            try {
                val page = withContext(Dispatchers.IO) { api.companies(prefs.baseUrl, prefs.token) }
                cache.companies = page.data
                companies = page.data
            } catch (_: Exception) {
            }
        }
    }

    fun setCompany(nextId: String) {
        val current = detail?.companyId.orEmpty()
        if (nextId == current) {
            pickCompany = false
            return
        }
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    if (nextId.isNotBlank()) api.attachConversation(prefs.baseUrl, prefs.token, nextId, conversationId)
                    else if (current.isNotBlank()) api.detachConversation(prefs.baseUrl, prefs.token, current, conversationId)
                }
                val name = companies.firstOrNull { it.id == nextId }?.name.orEmpty()
                val updated = (detail ?: Conversation(id = conversationId, title = heading)).copy(companyId = nextId, companyName = name)
                thread.detail = updated
                detail = updated
                pickCompany = false
            } catch (e: Unauthorized) {
                onUnauthorized()
            } catch (_: Exception) {
            }
        }
    }

    fun block() {
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    api.addDenylist(prefs.baseUrl, prefs.token, "conversation", conversationId, "Bloqueada pelo app")
                }
                markBlocked()
                confirmBlock = false
            } catch (e: Unauthorized) {
                onUnauthorized()
            } catch (_: Exception) {
                confirmBlock = false
            }
        }
    }

    fun openContact() {
        val id = detail?.contactId.orEmpty()
        if (id.isBlank()) return
        showContact = true
        if (contact?.id == id) return
        scope.launch {
            try {
                contact = withContext(Dispatchers.IO) { api.contact(prefs.baseUrl, prefs.token, id) }
            } catch (e: Unauthorized) {
                onUnauthorized()
            } catch (_: Exception) {
                showContact = false
            }
        }
    }

    fun loadOlder() {
        val next = thread.cursor ?: return
        if (paging[0]) return
        paging[0] = true
        loadingMore = true
        val mine = ticket[0]
        scope.launch {
            try {
                val page = withContext(Dispatchers.IO) {
                    api.messages(prefs.baseUrl, prefs.token, conversationId, cursor = next)
                }
                if (mine != ticket[0]) return@launch
                if (page.data.isEmpty()) {
                    cursor = null
                    thread.cursor = null
                    return@launch
                }
                val merged = thread.items + page.data.filter { row -> thread.items.none { it.id == row.id } }
                items = merged
                cursor = page.nextCursor
                thread.items = merged
                thread.cursor = page.nextCursor
                hasRows[0] = merged.isNotEmpty()
            } catch (e: Exception) {
                if (mine == ticket[0]) fail(e)
            } finally {
                if (mine == ticket[0]) {
                    paging[0] = false
                    loadingMore = false
                }
            }
        }
    }

    fun reload() {
        val mine = ++ticket[0]
        paging[0] = true
        scope.launch {
            if (!hasRows[0]) loading = true
            error = null
            val atBottom = listState.firstVisibleItemIndex == 0
            try {
                val page = withContext(Dispatchers.IO) {
                    api.messages(prefs.baseUrl, prefs.token, conversationId)
                }
                if (mine != ticket[0]) return@launch
                val freshIds = page.data.map { it.id }.toSet()
                val hadOlder = thread.items.any { old -> old.id !in freshIds }
                val merged = page.data + thread.items.filter { it.id !in freshIds }
                items = merged
                if (!hadOlder) cursor = page.nextCursor
                thread.items = merged
                thread.cursor = cursor
                hasRows[0] = merged.isNotEmpty()
                if (atBottom) listState.scrollToItem(0)
            } catch (e: Exception) {
                if (mine == ticket[0]) fail(e)
            } finally {
                if (mine == ticket[0]) {
                    paging[0] = false
                    loading = false
                    val max = listState.layoutInfo.visibleItemsInfo.maxOfOrNull { it.index } ?: -1
                    if (thread.cursor != null && shouldLoadOlder(max, thread.items.size)) loadOlder()
                }
            }
        }
    }

    LaunchedEffect(conversationId) {
        loadHeader()
        reload()
    }
    LaunchedEffect(listState, conversationId) {
        snapshotFlow {
            val max = listState.layoutInfo.visibleItemsInfo.maxOfOrNull { it.index } ?: -1
            Triple(max, items.size, cursor)
        }.collect { (max, count, next) ->
            if (next != null && shouldLoadOlder(max, count)) loadOlder()
        }
    }
    DisposableEffect(conversationId, prefs.baseUrl, prefs.token) {
        val source = api.events(prefs.baseUrl, prefs.token) { reload() }
        onDispose { source.cancel() }
    }

    val context = LocalContext.current
    val stickyLabel by remember(items) {
        derivedStateOf {
            val top = listState.layoutInfo.visibleItemsInfo
                .map { it.index }
                .filter { it < items.size }
                .maxOrNull() ?: return@derivedStateOf null
            dayLabel(items[top].timestamp)
        }
    }
    val stickyVisible = showStickyDay(stickyLabel, nearLatestMessages(listState.firstVisibleItemIndex))
    var draft by remember(conversationId) { mutableStateOf("") }
    Scaffold(
        containerColor = Chat,
        topBar = {
            Column(Modifier.background(SurfaceWhite)) {
                ContextaBar(
                    heading,
                    onBack,
                    subtitle = when {
                        blocked -> "Bloqueada"
                        detail?.isGroup == true -> "Grupo"
                        else -> null
                    },
                ) {
                    if (blocked) {
                        StatusChip("Bloqueada", Color(0xFFFFFBEB), Color(0xFF92400E), Icons.Filled.Warning)
                    } else {
                        IconButton(onClick = { confirmBlock = true }) {
                            Icon(Icons.Filled.Warning, contentDescription = "Bloquear conversa", tint = Muted)
                        }
                    }
                    if (!detail?.contactId.isNullOrBlank()) {
                        IconButton(onClick = { openContact() }) {
                            Icon(Icons.Filled.Info, contentDescription = "Ver contato", tint = Ink)
                        }
                    }
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { pickCompany = true }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Filled.AccountBox, contentDescription = null, tint = Brand, modifier = Modifier.size(18.dp))
                    Text("Empresa", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        detail?.companyName?.ifBlank { "Nenhuma" } ?: "Nenhuma",
                        color = Ink,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = Muted)
                }
                HorizontalDivider(color = Line)
            }
        },
        bottomBar = { ComposerPlaceholder(draft) { draft = it } },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            Image(
                painterResource(R.drawable.chat_bg),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            when {
                loading && items.isEmpty() -> MessageSkeleton()
                items.isEmpty() -> EmptyHint(Icons.Filled.MailOutline, error ?: "Ainda não há mensagens")
                else -> {
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        state = listState,
                        reverseLayout = true,
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                    itemsIndexed(items, key = { _, message -> message.id }) { index, message ->
                        val older = items.getOrNull(index + 1)?.timestamp
                        val label = dayLabel(message.timestamp)
                        Column(Modifier.fillMaxWidth()) {
                            if (showDayChip(message.timestamp, older) && !(stickyVisible && label == stickyLabel)) {
                                DayChip(label)
                            }
                            Bubble(message)
                        }
                    }
                    if (loadingMore) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                Bone(Modifier.fillMaxWidth(0.4f).height(36.dp).clip(RoundedCornerShape(16.dp)))
                            }
                        }
                    }
                    if (error != null) item { Text(error!!, color = MaterialTheme.colorScheme.error) }
                    }
                    if (stickyVisible && !stickyLabel.isNullOrBlank()) {
                        DayChip(stickyLabel!!, Modifier.align(Alignment.TopCenter).padding(top = 8.dp).zIndex(1f))
                    }
                }
            }
        }
    }
    if (confirmBlock) {
        AlertDialog(
            onDismissRequest = { confirmBlock = false },
            title = { Text("Bloquear conversa?", color = Ink) },
            text = {
                Text(
                    "Novas mensagens de “$heading” deixarão de ser salvas. O histórico existente permanece.",
                    color = Muted,
                )
            },
            confirmButton = {
                TextButton(onClick = { block() }) { Text("Bloquear", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = { TextButton(onClick = { confirmBlock = false }) { Text("Cancelar", color = Muted) } },
            containerColor = SurfaceWhite,
        )
    }
    if (pickCompany) {
        AlertDialog(
            onDismissRequest = { pickCompany = false },
            title = { Text("Empresa", color = Ink) },
            text = {
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    item {
                        CompanyChoice("Nenhuma", selected = detail?.companyId.isNullOrBlank()) { setCompany("") }
                    }
                    items(companies, key = { it.id }) { company ->
                        CompanyChoice(company.name, selected = company.id == detail?.companyId) { setCompany(company.id) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { pickCompany = false }) { Text("Fechar", color = Muted) } },
            containerColor = SurfaceWhite,
        )
    }
    if (showContact) {
        val person = contact
        AlertDialog(
            onDismissRequest = { showContact = false },
            title = { Text(person?.displayName ?: "Contato", color = Ink) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (person == null) Text("Carregando…", color = Muted)
                    else {
                        if (person.phone.isNotBlank()) {
                            TextButton(
                                onClick = {
                                    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${person.phone}")))
                                },
                                contentPadding = PaddingValues(0.dp),
                            ) {
                                Icon(Icons.Filled.Phone, contentDescription = null, tint = Brand, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.size(8.dp))
                                Text(person.phone, color = Brand, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        if (person.companyName.isNotBlank()) Text(person.companyName, color = Muted, fontSize = 14.sp)
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showContact = false }) { Text("Fechar", color = Muted) } },
            containerColor = SurfaceWhite,
        )
    }
}

@Composable
private fun CompanyChoice(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        color = if (selected) Brand else Ink,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
    )
}

@Composable
private fun Bubble(message: Message) {
    val inbound = message.direction != "outbound"
    val audio = message.audioLabel()
    val shape = RoundedCornerShape(
        topStart = if (inbound) 4.dp else 16.dp,
        topEnd = if (inbound) 16.dp else 4.dp,
        bottomEnd = 16.dp,
        bottomStart = 16.dp,
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (inbound) Arrangement.Start else Arrangement.End) {
        Column(
            Modifier
                .widthIn(max = 300.dp)
                .clip(shape)
                .background(if (inbound) SurfaceWhite else OutboundBubble)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            if (inbound && !message.sender.isNullOrBlank()) {
                Text(message.sender, color = Brand, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            }
            if (audio != null) {
                Text(audio, color = Brand, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, modifier = Modifier.padding(bottom = 2.dp))
            }
            Text(message.displayText(), color = Ink, fontSize = 15.sp, lineHeight = 21.sp)
            Row(Modifier.align(Alignment.End).padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (!inbound && !message.status.isNullOrBlank()) {
                    Text(message.status, color = Muted, fontSize = 11.sp)
                }
                Text(formatClock(message.timestamp), color = Muted, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun ComposerPlaceholder(value: String, onValueChange: (String) -> Unit) {
    Column(Modifier.background(SurfaceWhite).navigationBarsPadding()) {
        HorizontalDivider(color = Line)
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            OutlinedTextField(
                value,
                onValueChange,
                placeholder = { Text("Mensagem") },
                singleLine = true,
                colors = contextaFieldColors(),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = {}, enabled = false) {
                Icon(Icons.Filled.Send, contentDescription = "Enviar", tint = Line)
            }
        }
    }
}

@Composable
private fun DayChip(label: String, modifier: Modifier = Modifier) {
    if (label.isBlank()) return
    Box(modifier.fillMaxWidth().padding(bottom = 8.dp), contentAlignment = Alignment.Center) {
        Text(
            label,
            color = Ink,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceWhite.copy(alpha = 0.92f))
                .padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun InitialAvatar(name: String) {
    val letter = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    Box(Modifier.size(44.dp).clip(CircleShape).background(BrandSoft), contentAlignment = Alignment.Center) {
        Text(letter, color = Brand, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    }
}

@Composable
private fun EmptyHint(icon: ImageVector, message: String) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = Brand, modifier = Modifier.size(28.dp))
        Text(message, color = Muted, fontSize = 15.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable
private fun ConversationSkeleton(rows: Int = 8) {
    Column(Modifier.fillMaxWidth().background(SurfaceWhite)) {
        repeat(rows) {
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Bone(Modifier.size(44.dp).clip(CircleShape))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Bone(Modifier.fillMaxWidth(0.42f).height(14.dp).clip(RoundedCornerShape(6.dp)))
                    Bone(Modifier.fillMaxWidth(0.78f).height(12.dp).clip(RoundedCornerShape(6.dp)))
                }
            }
        }
    }
}

@Composable
private fun MessageSkeleton() {
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Bottom),
    ) {
        Bone(Modifier.fillMaxWidth(0.62f).height(52.dp).clip(RoundedCornerShape(16.dp)))
        Bone(Modifier.align(Alignment.End).fillMaxWidth(0.48f).height(40.dp).clip(RoundedCornerShape(16.dp)))
        Bone(Modifier.fillMaxWidth(0.7f).height(64.dp).clip(RoundedCornerShape(16.dp)))
    }
}

@Composable
private fun Bone(modifier: Modifier) {
    val transition = rememberInfiniteTransition(label = "bone")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 700), RepeatMode.Reverse),
        label = "alpha",
    )
    Box(modifier.background(Line.copy(alpha = alpha)))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContextaBar(
    title: String,
    onBack: (() -> Unit)? = null,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = {
            Column {
                Text(title, fontWeight = FontWeight.SemiBold, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (!subtitle.isNullOrBlank()) {
                    Text(subtitle, color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = Ink)
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite, titleContentColor = Ink),
    )
}

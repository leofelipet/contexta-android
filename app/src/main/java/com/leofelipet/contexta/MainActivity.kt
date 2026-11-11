package com.leofelipet.contexta

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import java.time.LocalDate
import java.time.LocalDateTime

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
    data class ContactDetail(val id: String, val title: String) : Screen
    data class TaskForm(val id: String?) : Screen
    data object ReminderForm : Screen
    data object Companies : Screen
    data class CompanyDetail(val id: String, val title: String) : Screen
    data class CompanyForm(val id: String?) : Screen
    data object Memories : Screen
    data class MemoryDetail(val id: String, val title: String) : Screen
    data object MemoryForm : Screen
    data object Dashboard : Screen
    data object Maintenance : Screen
    data object Integrations : Screen
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
                        onOpen = { id, title -> stack = stack + Screen.Thread(id, title) },
                        onOpenContact = { stack = stack + Screen.ContactDetail(it.id, it.displayName) },
                        onOpenTask = { stack = stack + Screen.TaskForm(it) },
                        onOpenReminder = { stack = stack + Screen.ReminderForm },
                        onUnauthorized = ::leave,
                        onMore = { stack = stack + Screen.More },
                        installApk = ::installApk,
                    )
                    Screen.More -> MoreScreen(
                        onBack = ::back,
                        onLogout = ::leave,
                        onDashboard = { stack = stack + Screen.Dashboard },
                        onCompanies = { stack = stack + Screen.Companies },
                        onMemories = { stack = stack + Screen.Memories },
                        onMaintenance = { stack = stack + Screen.Maintenance },
                        onIntegrations = { stack = stack + Screen.Integrations },
                    )
                    Screen.Integrations -> IntegrationsScreen(
                        api, prefs,
                        onBack = ::back,
                        onUnauthorized = ::leave,
                    )
                    Screen.Dashboard -> DashboardScreen(
                        api, prefs,
                        onBack = ::back,
                        onOpenConversation = { id, title -> stack = stack + Screen.Thread(id, title) },
                        onUnauthorized = ::leave,
                    )
                    Screen.Maintenance -> MaintenanceScreen(
                        api, prefs, cache,
                        onBack = ::back,
                        onUnauthorized = ::leave,
                    )
                    Screen.Companies -> CompaniesScreen(
                        api, prefs, cache,
                        onBack = ::back,
                        onOpen = { stack = stack + Screen.CompanyDetail(it.id, it.name) },
                        onCreate = { stack = stack + Screen.CompanyForm(null) },
                        onUnauthorized = ::leave,
                    )
                    is Screen.CompanyDetail -> CompanyDetailScreen(
                        api, prefs, cache, current.id, current.title,
                        onBack = ::back,
                        onEdit = { stack = stack + Screen.CompanyForm(current.id) },
                        onOpenConversation = { id, title -> stack = stack + Screen.Thread(id, title) },
                        onOpenContact = { stack = stack + Screen.ContactDetail(it.id, it.displayName) },
                        onOpenTask = { stack = stack + Screen.TaskForm(it) },
                        onUnauthorized = ::leave,
                    )
                    is Screen.CompanyForm -> CompanyFormScreen(
                        api, prefs, cache, current.id,
                        onBack = ::back,
                        onUnauthorized = ::leave,
                    )
                    Screen.Memories -> MemoriesScreen(
                        api, prefs, cache,
                        onBack = ::back,
                        onOpen = { stack = stack + Screen.MemoryDetail(it.id, it.headline) },
                        onCreate = { stack = stack + Screen.MemoryForm },
                        onUnauthorized = ::leave,
                    )
                    is Screen.MemoryDetail -> MemoryDetailScreen(
                        api, prefs, cache, current.id, current.title,
                        onBack = ::back,
                        onUnauthorized = ::leave,
                    )
                    Screen.MemoryForm -> MemoryFormScreen(
                        api, prefs, cache,
                        onBack = ::back,
                        onUnauthorized = ::leave,
                    )
                    is Screen.TaskForm -> TaskFormScreen(
                        api, prefs, cache, current.id,
                        onBack = ::back,
                        onUnauthorized = ::leave,
                    )
                    Screen.ReminderForm -> ReminderFormScreen(
                        api, prefs, cache,
                        onBack = ::back,
                        onUnauthorized = ::leave,
                    )
                    is Screen.ContactDetail -> ContactDetailScreen(
                        api, prefs, cache, current.id, current.title,
                        onBack = ::back,
                        onOpen = { id, title -> stack = stack + Screen.Thread(id, title) },
                        onUnauthorized = ::leave,
                    )
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
    onOpen: (String, String) -> Unit,
    onOpenContact: (Contact) -> Unit,
    onOpenTask: (String?) -> Unit,
    onOpenReminder: () -> Unit,
    onUnauthorized: () -> Unit,
    onMore: () -> Unit,
    installApk: (File) -> Unit,
) {
    BackHandler(enabled = tab != HomeTab.Conversas) { onTab(HomeTab.Conversas) }
    Column(Modifier.fillMaxSize().background(Paper)) {
        Box(Modifier.weight(1f)) {
            when (tab) {
                HomeTab.Conversas -> InboxScreen(api, prefs, cache, onOpen, onUnauthorized, onMore)
                HomeTab.Contatos -> ContactsScreen(api, prefs, cache, onOpenContact, onUnauthorized, onMore)
                HomeTab.Busca -> SearchScreen(api, prefs, cache, onOpen, onUnauthorized, onMore)
                HomeTab.Tarefas -> TasksScreen(api, prefs, cache, onOpenTask, onOpenReminder, onUnauthorized, onMore)
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

private val directionPills = listOf("" to "Todas", "inbound" to "Recebidas", "outbound" to "Enviadas")
private val messageTypePills = listOf(
    "" to "Todos",
    "text" to "Texto",
    "audio" to "Áudio",
    "image" to "Imagem",
    "document" to "Documento",
)

@Composable
private fun ContactsScreen(
    api: Api,
    prefs: Prefs,
    cache: SessionCache,
    onOpenContact: (Contact) -> Unit,
    onUnauthorized: () -> Unit,
    onMore: () -> Unit,
) {
    var items by remember { mutableStateOf(cache.contacts) }
    var cursor by remember { mutableStateOf(cache.contactCursor) }
    var query by remember { mutableStateOf(cache.contactQuery) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(!cache.contactsReady) }
    var loadingMore by remember { mutableStateOf(false) }
    val paging = remember { booleanArrayOf(false) }
    val ticket = remember { intArrayOf(0) }
    val hasRows = remember { booleanArrayOf(cache.contacts.isNotEmpty()) }
    val scope = rememberCoroutineScope()
    val listState = cache.contactListState

    fun fail(e: Exception) {
        if (e is Unauthorized) onUnauthorized() else error = "Não foi possível carregar os contatos."
    }

    fun loadMore() {
        val next = cache.contactCursor ?: return
        if (paging[0]) return
        paging[0] = true
        loadingMore = true
        val mine = ticket[0]
        val q = cache.contactQuery
        scope.launch {
            try {
                val page = withContext(Dispatchers.IO) {
                    api.contacts(prefs.baseUrl, prefs.token, cursor = next, query = q)
                }
                if (mine != ticket[0]) return@launch
                if (page.data.isEmpty()) {
                    cache.contactCursor = null
                    cursor = null
                    return@launch
                }
                val merged = cache.contacts + page.data.filter { row -> cache.contacts.none { it.id == row.id } }
                cache.contacts = merged
                cache.contactCursor = page.nextCursor
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

    fun reload(q: String) {
        val mine = ++ticket[0]
        paging[0] = true
        scope.launch {
            if (!hasRows[0]) loading = true
            error = null
            try {
                val page = withContext(Dispatchers.IO) { api.contacts(prefs.baseUrl, prefs.token, query = q) }
                if (mine != ticket[0]) return@launch
                val same = q == cache.contactQuery
                val hadMore = same && cache.contacts.size > page.data.size
                val freshIds = page.data.map { it.id }.toSet()
                val merged = if (hadMore) page.data + cache.contacts.filter { it.id !in freshIds } else page.data
                if (!same) listState.scrollToItem(0)
                cache.contactQuery = q
                cache.contacts = merged
                if (!hadMore) cache.contactCursor = page.nextCursor
                cache.contactsReady = true
                items = merged
                cursor = cache.contactCursor
                hasRows[0] = merged.isNotEmpty()
            } catch (e: Exception) {
                if (mine == ticket[0]) fail(e)
            } finally {
                if (mine == ticket[0]) {
                    paging[0] = false
                    loading = false
                    val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
                    if (shouldPrefetch(last, cache.contacts.size) && cache.contactCursor != null) loadMore()
                }
            }
        }
    }

    LaunchedEffect(query) {
        val warm = cache.contactsReady && cache.contactQuery == query
        if (!warm && query.isNotEmpty()) delay(300)
        reload(query)
    }
    LaunchedEffect(listState) {
        snapshotFlow {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            Triple(last, items.size, cursor)
        }.collect { (last, count, next) ->
            if (next != null && shouldPrefetch(last, count)) loadMore()
        }
    }

    Scaffold(
        containerColor = Paper,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            ContextaBar("Contatos") {
                IconButton(onClick = onMore) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Mais", tint = Ink)
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).background(SurfaceWhite)) {
            SearchField(query, { query = it }, "Nome ou telefone")
            when {
                loading && items.isEmpty() -> ConversationSkeleton()
                items.isEmpty() -> EmptyHint(
                    Icons.Filled.Person,
                    error ?: if (query.isBlank()) "Nenhum contato ainda" else "Nenhum contato encontrado",
                )
                else -> LazyColumn(Modifier.fillMaxSize(), state = listState) {
                    if (error != null) item { Text(error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp)) }
                    items(items, key = { it.id }) { contact ->
                        ContactRow(contact) { onOpenContact(contact) }
                        HorizontalDivider(Modifier.padding(start = 72.dp), color = Line)
                    }
                    if (loadingMore) item { ConversationSkeleton(rows = 2) }
                }
            }
        }
    }
}

@Composable
private fun ContactRow(contact: Contact, onOpen: () -> Unit) {
    val subtitle = listOf(contact.phone.ifBlank { "Telefone não informado" }, contact.companyName.ifBlank { "" })
        .filter { it.isNotBlank() }
        .joinToString(" · ")
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        InitialAvatar(contact.displayName)
        Column(Modifier.weight(1f)) {
            Text(contact.displayName, color = Ink, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = Muted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
private fun ContactDetailScreen(
    api: Api,
    prefs: Prefs,
    cache: SessionCache,
    contactId: String,
    title: String,
    onBack: () -> Unit,
    onOpen: (String, String) -> Unit,
    onUnauthorized: () -> Unit,
) {
    var contact by remember(contactId) { mutableStateOf(cache.contacts.firstOrNull { it.id == contactId }) }
    var chats by remember(contactId) { mutableStateOf(listOf<Conversation>()) }
    var error by remember(contactId) { mutableStateOf<String?>(null) }
    var loading by remember(contactId) { mutableStateOf(true) }
    val context = LocalContext.current
    val heading = contact?.displayName ?: title

    LaunchedEffect(contactId) {
        loading = true
        error = null
        try {
            val person = withContext(Dispatchers.IO) { api.contact(prefs.baseUrl, prefs.token, contactId) }
            contact = person
            chats = withContext(Dispatchers.IO) {
                api.conversations(prefs.baseUrl, prefs.token, contactId = contactId).data
            }
        } catch (e: Unauthorized) {
            onUnauthorized()
        } catch (_: Exception) {
            error = "Não foi possível carregar o contato."
        } finally {
            loading = false
        }
    }

    Scaffold(
        containerColor = Paper,
        topBar = { ContextaBar(heading, onBack) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).background(SurfaceWhite)) {
            if (loading && contact == null) {
                ConversationSkeleton(rows = 4)
            } else {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    InitialAvatar(heading)
                    Text(heading, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
                    val phone = contact?.phone.orEmpty()
                    if (phone.isNotBlank()) {
                        TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))) }) {
                            Icon(Icons.Filled.Phone, contentDescription = null, tint = Brand, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.size(8.dp))
                            Text(phone, color = Brand, fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        Text("Telefone não informado", color = Muted, fontSize = 14.sp)
                    }
                    if (!contact?.companyName.isNullOrBlank()) {
                        Text(contact!!.companyName, color = Muted, fontSize = 14.sp)
                    }
                }
                HorizontalDivider(color = Line)
                Text(
                    "Conversas",
                    color = Muted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
                )
                when {
                    error != null -> Text(error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
                    chats.isEmpty() -> Text("Nenhuma conversa com este contato.", color = Muted, modifier = Modifier.padding(16.dp))
                    else -> chats.forEach { conversation ->
                        ConversationRow(conversation, blocked = conversation.isBlocked(cache.blockedConversationIds, cache.blockedContactIds)) {
                            onOpen(conversation.id, conversation.displayTitle)
                        }
                        HorizontalDivider(Modifier.padding(start = 72.dp), color = Line)
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchScreen(
    api: Api,
    prefs: Prefs,
    cache: SessionCache,
    onOpen: (String, String) -> Unit,
    onUnauthorized: () -> Unit,
    onMore: () -> Unit,
) {
    var items by remember { mutableStateOf(cache.searchResults) }
    var cursor by remember { mutableStateOf(cache.searchCursor) }
    var query by remember { mutableStateOf(cache.searchQuery) }
    var direction by remember { mutableStateOf(cache.searchDirection) }
    var type by remember { mutableStateOf(cache.searchType) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(!cache.searchReady) }
    var loadingMore by remember { mutableStateOf(false) }
    val paging = remember { booleanArrayOf(false) }
    val ticket = remember { intArrayOf(0) }
    val hasRows = remember { booleanArrayOf(cache.searchResults.isNotEmpty()) }
    val scope = rememberCoroutineScope()
    val listState = cache.searchListState

    fun fail(e: Exception) {
        if (e is Unauthorized) onUnauthorized() else error = "Não foi possível buscar mensagens."
    }

    fun loadMore() {
        val next = cache.searchCursor ?: return
        if (paging[0]) return
        paging[0] = true
        loadingMore = true
        val mine = ticket[0]
        val q = cache.searchQuery
        val dir = cache.searchDirection
        val kind = cache.searchType
        scope.launch {
            try {
                val page = withContext(Dispatchers.IO) {
                    api.searchMessages(prefs.baseUrl, prefs.token, cursor = next, query = q, direction = dir, type = kind)
                }
                if (mine != ticket[0]) return@launch
                if (page.data.isEmpty()) {
                    cache.searchCursor = null
                    cursor = null
                    return@launch
                }
                val merged = cache.searchResults + page.data.filter { row -> cache.searchResults.none { it.id == row.id } }
                cache.searchResults = merged
                cache.searchCursor = page.nextCursor
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

    fun reload(q: String, dir: String, kind: String) {
        val mine = ++ticket[0]
        paging[0] = true
        val same = q == cache.searchQuery && dir == cache.searchDirection && kind == cache.searchType
        if (dir != cache.searchDirection || kind != cache.searchType) {
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
                    api.searchMessages(prefs.baseUrl, prefs.token, query = q, direction = dir, type = kind)
                }
                if (mine != ticket[0]) return@launch
                val hadMore = same && cache.searchResults.size > page.data.size
                val freshIds = page.data.map { it.id }.toSet()
                val merged = if (hadMore) page.data + cache.searchResults.filter { it.id !in freshIds } else page.data
                if (!same) listState.scrollToItem(0)
                cache.searchQuery = q
                cache.searchDirection = dir
                cache.searchType = kind
                cache.searchResults = merged
                if (!hadMore) cache.searchCursor = page.nextCursor
                cache.searchReady = true
                items = merged
                cursor = cache.searchCursor
                hasRows[0] = merged.isNotEmpty()
            } catch (e: Exception) {
                if (mine == ticket[0]) fail(e)
            } finally {
                if (mine == ticket[0]) {
                    paging[0] = false
                    loading = false
                    val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
                    if (shouldPrefetch(last, cache.searchResults.size) && cache.searchCursor != null) loadMore()
                }
            }
        }
    }

    LaunchedEffect(query, direction, type) {
        val warm = cache.searchReady && cache.searchQuery == query && cache.searchDirection == direction && cache.searchType == type
        if (!warm && query.isNotEmpty() && direction == cache.searchDirection && type == cache.searchType) delay(300)
        reload(query, direction, type)
    }
    LaunchedEffect(listState) {
        snapshotFlow {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            Triple(last, items.size, cursor)
        }.collect { (last, count, next) ->
            if (next != null && shouldPrefetch(last, count)) loadMore()
        }
    }

    Scaffold(
        containerColor = Paper,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            ContextaBar("Busca") {
                IconButton(onClick = onMore) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Mais", tint = Ink)
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).background(SurfaceWhite)) {
            SearchField(query, { query = it }, "Texto da mensagem")
            TypePills(direction, directionPills) { direction = it }
            TypePills(type, messageTypePills) { type = it }
            when {
                loading && items.isEmpty() -> ConversationSkeleton()
                items.isEmpty() -> EmptyHint(
                    Icons.Filled.Search,
                    error ?: "Nenhuma mensagem encontrada",
                )
                else -> LazyColumn(Modifier.fillMaxSize(), state = listState) {
                    if (error != null) item { Text(error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp)) }
                    items(items, key = { it.id }) { message ->
                        SearchRow(message) {
                            if (message.conversationId.isNotBlank()) {
                                onOpen(message.conversationId, message.sender?.ifBlank { "Conversa" } ?: "Conversa")
                            }
                        }
                        HorizontalDivider(color = Line)
                    }
                    if (loadingMore) item { ConversationSkeleton(rows = 2) }
                }
            }
        }
    }
}

@Composable
private fun SearchRow(message: Message, onOpen: () -> Unit) {
    val preview = message.displayText()
    Row(
        Modifier.fillMaxWidth().clickable(enabled = message.conversationId.isNotBlank(), onClick = onOpen).padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(36.dp).clip(CircleShape).background(if (message.inbound) Paper else BrandSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (message.inbound) Icons.Filled.MailOutline else Icons.Filled.Send,
                contentDescription = null,
                tint = if (message.inbound) Muted else Brand,
                modifier = Modifier.size(16.dp),
            )
        }
        Column(Modifier.weight(1f)) {
            Text(preview, color = Ink, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(
                    if (message.isAudio) "Áudio" else null,
                    message.sender?.trim()?.ifBlank { null },
                    if (message.inbound) "Recebida" else "Enviada",
                    formatListTime(message.timestamp).ifBlank { null },
                ).joinToString(" · "),
                color = Muted,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun SearchField(value: String, onValueChange: (String) -> Unit, placeholder: String) {
    OutlinedTextField(
        value,
        onValueChange,
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Muted) },
        trailingIcon = if (value.isBlank()) {
            null
        } else {
            {
                IconButton(onClick = { onValueChange("") }) {
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
}

private val taskSectionPills = listOf("tarefas" to "Tarefas", "schedules" to "Recorrentes", "reminders" to "Lembretes")
private val taskFilterPills = listOf("" to "Abertas", "all" to "Todas", "overdue" to "Atrasadas")
private val taskStatusPills = listOf(
    "pending" to "Pendente",
    "in_progress" to "Andamento",
    "blocked" to "Bloqueada",
    "done" to "Concluída",
    "cancelled" to "Cancelada",
)

@Composable
private fun TasksScreen(
    api: Api,
    prefs: Prefs,
    cache: SessionCache,
    onOpenTask: (String?) -> Unit,
    onOpenReminder: () -> Unit,
    onUnauthorized: () -> Unit,
    onMore: () -> Unit,
) {
    var section by remember { mutableStateOf(cache.taskSection) }
    var items by remember { mutableStateOf(cache.tasks) }
    var cursor by remember { mutableStateOf(cache.taskCursor) }
    var query by remember { mutableStateOf(cache.taskQuery) }
    var filter by remember { mutableStateOf(cache.taskFilter) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(!cache.tasksReady) }
    var loadingMore by remember { mutableStateOf(false) }
    val paging = remember { booleanArrayOf(false) }
    val ticket = remember { intArrayOf(0) }
    val hasRows = remember { booleanArrayOf(cache.tasks.isNotEmpty()) }
    val scope = rememberCoroutineScope()
    val listState = cache.taskListState

    fun fail(e: Exception) {
        if (e is Unauthorized) onUnauthorized() else error = "Não foi possível carregar as tarefas."
    }

    fun loadMore() {
        val next = cache.taskCursor ?: return
        if (paging[0]) return
        paging[0] = true
        loadingMore = true
        val mine = ticket[0]
        val q = cache.taskQuery
        val kind = cache.taskFilter
        scope.launch {
            try {
                val page = withContext(Dispatchers.IO) {
                    api.tasks(
                        prefs.baseUrl, prefs.token, cursor = next, query = q,
                        openOnly = kind.isEmpty(), overdue = kind == "overdue",
                    )
                }
                if (mine != ticket[0]) return@launch
                if (page.data.isEmpty()) {
                    cache.taskCursor = null
                    cursor = null
                    return@launch
                }
                val merged = cache.tasks + page.data.filter { row -> cache.tasks.none { it.id == row.id } }
                cache.tasks = merged
                cache.taskCursor = page.nextCursor
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
        if (kind != cache.taskFilter) {
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
                    api.tasks(
                        prefs.baseUrl, prefs.token, query = q,
                        openOnly = kind.isEmpty(), overdue = kind == "overdue",
                    )
                }
                if (mine != ticket[0]) return@launch
                val same = q == cache.taskQuery && kind == cache.taskFilter
                val hadMore = same && cache.tasks.size > page.data.size
                val freshIds = page.data.map { it.id }.toSet()
                val merged = if (hadMore) page.data + cache.tasks.filter { it.id !in freshIds } else page.data
                if (!same) listState.scrollToItem(0)
                cache.taskQuery = q
                cache.taskFilter = kind
                cache.tasks = merged
                if (!hadMore) cache.taskCursor = page.nextCursor
                cache.tasksReady = true
                items = merged
                cursor = cache.taskCursor
                hasRows[0] = merged.isNotEmpty()
            } catch (e: Exception) {
                if (mine == ticket[0]) fail(e)
            } finally {
                if (mine == ticket[0]) {
                    paging[0] = false
                    loading = false
                    val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
                    if (shouldPrefetch(last, cache.tasks.size) && cache.taskCursor != null) loadMore()
                }
            }
        }
    }

    fun complete(task: Task) {
        if (task.closed) return
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    api.updateTask(prefs.baseUrl, prefs.token, task.id, UpdateTaskBody(status = "done"))
                }
                val keep = filter == "all"
                val next = if (keep) items.map { if (it.id == task.id) it.copy(status = "done") else it } else items.filter { it.id != task.id }
                items = next
                cache.tasks = next
                hasRows[0] = next.isNotEmpty()
            } catch (e: Unauthorized) {
                onUnauthorized()
            } catch (_: Exception) {
            }
        }
    }

    LaunchedEffect(query, filter) {
        val warm = cache.tasksReady && cache.taskQuery == query && cache.taskFilter == filter
        if (!warm && query.isNotEmpty() && filter == cache.taskFilter) delay(300)
        reload(query, filter)
    }
    LaunchedEffect(listState) {
        snapshotFlow {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            Triple(last, items.size, cursor)
        }.collect { (last, count, next) ->
            if (next != null && shouldPrefetch(last, count)) loadMore()
        }
    }

    Scaffold(
        containerColor = Paper,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            ContextaBar(
                when (section) {
                    "schedules" -> "Recorrentes"
                    "reminders" -> "Lembretes"
                    else -> "Tarefas"
                },
            ) {
                if (section == "tarefas") {
                    IconButton(onClick = { onOpenTask(null) }) {
                        Icon(Icons.Filled.Add, contentDescription = "Nova tarefa", tint = Ink)
                    }
                }
                if (section == "reminders") {
                    IconButton(onClick = onOpenReminder) {
                        Icon(Icons.Filled.Add, contentDescription = "Novo lembrete", tint = Ink)
                    }
                }
                IconButton(onClick = onMore) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Mais", tint = Ink)
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).background(SurfaceWhite)) {
            TypePills(section, taskSectionPills) {
                section = it
                cache.taskSection = it
            }
            if (section == "schedules") {
                SchedulesPane(api, prefs, cache, onUnauthorized)
                return@Column
            }
            if (section == "reminders") {
                RemindersPane(api, prefs, cache, onUnauthorized)
                return@Column
            }
            SearchField(query, { query = it }, "Buscar tarefa")
            TypePills(filter, taskFilterPills) { filter = it }
            when {
                loading && items.isEmpty() -> ConversationSkeleton()
                items.isEmpty() -> EmptyHint(
                    Icons.Filled.CheckCircle,
                    error ?: if (query.isBlank()) "Nenhuma tarefa" else "Nenhuma tarefa encontrada",
                )
                else -> LazyColumn(Modifier.fillMaxSize(), state = listState) {
                    if (error != null) item { Text(error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp)) }
                    items(items, key = { it.id }) { task ->
                        TaskRow(task, onComplete = { complete(task) }) { onOpenTask(task.id) }
                        HorizontalDivider(Modifier.padding(start = 52.dp), color = Line)
                    }
                    if (loadingMore) item { ConversationSkeleton(rows = 2) }
                }
            }
        }
    }
}

@Composable
private fun SchedulesPane(api: Api, prefs: Prefs, cache: SessionCache, onUnauthorized: () -> Unit) {
    var items by remember { mutableStateOf(cache.schedules) }
    var cursor by remember { mutableStateOf(cache.scheduleCursor) }
    var query by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(!cache.schedulesReady) }
    var loadingMore by remember { mutableStateOf(false) }
    var confirmId by remember { mutableStateOf<String?>(null) }
    val paging = remember { booleanArrayOf(false) }
    val ticket = remember { intArrayOf(0) }
    val scope = rememberCoroutineScope()
    val listState = cache.scheduleListState

    fun fail(e: Exception) {
        if (e is Unauthorized) onUnauthorized() else error = "Não foi possível carregar as recorrências."
    }

    fun loadMore() {
        val next = cache.scheduleCursor ?: return
        if (paging[0]) return
        paging[0] = true
        loadingMore = true
        val mine = ticket[0]
        scope.launch {
            try {
                val page = withContext(Dispatchers.IO) {
                    api.schedules(prefs.baseUrl, prefs.token, cursor = next, query = query)
                }
                if (mine != ticket[0]) return@launch
                if (page.data.isEmpty()) {
                    cache.scheduleCursor = null
                    cursor = null
                    return@launch
                }
                val merged = cache.schedules + page.data.filter { row -> cache.schedules.none { it.id == row.id } }
                cache.schedules = merged
                cache.scheduleCursor = page.nextCursor
                items = merged
                cursor = page.nextCursor
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

    fun reload(q: String) {
        val mine = ++ticket[0]
        paging[0] = true
        scope.launch {
            if (cache.schedules.isEmpty()) loading = true
            error = null
            try {
                val page = withContext(Dispatchers.IO) { api.schedules(prefs.baseUrl, prefs.token, query = q) }
                if (mine != ticket[0]) return@launch
                cache.schedules = page.data
                cache.scheduleCursor = page.nextCursor
                cache.schedulesReady = true
                items = page.data
                cursor = page.nextCursor
            } catch (e: Exception) {
                if (mine == ticket[0]) fail(e)
            } finally {
                if (mine == ticket[0]) {
                    paging[0] = false
                    loading = false
                }
            }
        }
    }

    fun toggle(schedule: Schedule) {
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    api.setScheduleEnabled(prefs.baseUrl, prefs.token, schedule.id, !schedule.enabled)
                }
                val next = items.map { if (it.id == schedule.id) it.copy(enabled = !schedule.enabled) else it }
                items = next
                cache.schedules = next
            } catch (e: Unauthorized) {
                onUnauthorized()
            } catch (_: Exception) {
            }
        }
    }

    fun remove(id: String) {
        scope.launch {
            try {
                withContext(Dispatchers.IO) { api.deleteSchedule(prefs.baseUrl, prefs.token, id) }
                val next = items.filter { it.id != id }
                items = next
                cache.schedules = next
                confirmId = null
            } catch (e: Unauthorized) {
                onUnauthorized()
            } catch (_: Exception) {
                confirmId = null
            }
        }
    }

    LaunchedEffect(query) {
        if (query.isNotEmpty()) delay(300)
        reload(query)
    }
    LaunchedEffect(listState) {
        snapshotFlow {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            Triple(last, items.size, cursor)
        }.collect { (last, count, next) ->
            if (next != null && shouldPrefetch(last, count)) loadMore()
        }
    }

    SearchField(query, { query = it }, "Buscar recorrência")
    when {
        loading && items.isEmpty() -> ConversationSkeleton()
        items.isEmpty() -> EmptyHint(Icons.Filled.DateRange, error ?: "Nenhuma recorrência")
        else -> LazyColumn(Modifier.fillMaxSize(), state = listState) {
            if (error != null) item { Text(error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp)) }
            items(items, key = { it.id }) { schedule ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(schedule.title.ifBlank { "Recorrência" }, color = Ink, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(
                            listOfNotNull(
                                schedule.cron.ifBlank { null },
                                schedule.nextRunAt?.let { "próxima ${formatListTime(it)}" },
                                schedule.companyName.ifBlank { null },
                            ).joinToString(" · "),
                            color = Muted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    Switch(
                        checked = schedule.enabled,
                        onCheckedChange = { toggle(schedule) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Brand),
                    )
                    IconButton(onClick = { confirmId = schedule.id }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Apagar", tint = Muted)
                    }
                }
                HorizontalDivider(color = Line)
            }
            if (loadingMore) item { ConversationSkeleton(rows = 2) }
        }
    }
    confirmId?.let { id ->
        AlertDialog(
            onDismissRequest = { confirmId = null },
            title = { Text("Apagar recorrência?", color = Ink) },
            text = { Text("As tarefas já criadas são mantidas.", color = Muted) },
            confirmButton = {
                TextButton(onClick = { remove(id) }) { Text("Apagar", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = { TextButton(onClick = { confirmId = null }) { Text("Cancelar", color = Muted) } },
            containerColor = SurfaceWhite,
        )
    }
}

@Composable
private fun RemindersPane(api: Api, prefs: Prefs, cache: SessionCache, onUnauthorized: () -> Unit) {
    var items by remember { mutableStateOf(cache.reminders) }
    var cursor by remember { mutableStateOf(cache.reminderCursor) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(!cache.remindersReady) }
    var loadingMore by remember { mutableStateOf(false) }
    var confirmId by remember { mutableStateOf<String?>(null) }
    val paging = remember { booleanArrayOf(false) }
    val ticket = remember { intArrayOf(0) }
    val scope = rememberCoroutineScope()
    val listState = cache.reminderListState

    fun fail(e: Exception) {
        if (e is Unauthorized) onUnauthorized() else error = "Não foi possível carregar os lembretes."
    }

    fun loadMore() {
        val next = cache.reminderCursor ?: return
        if (paging[0]) return
        paging[0] = true
        loadingMore = true
        val mine = ticket[0]
        scope.launch {
            try {
                val page = withContext(Dispatchers.IO) { api.reminders(prefs.baseUrl, prefs.token, cursor = next) }
                if (mine != ticket[0]) return@launch
                if (page.data.isEmpty()) {
                    cache.reminderCursor = null
                    cursor = null
                    return@launch
                }
                val merged = cache.reminders + page.data.filter { row -> cache.reminders.none { it.id == row.id } }
                cache.reminders = merged
                cache.reminderCursor = page.nextCursor
                items = merged
                cursor = page.nextCursor
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
            if (cache.reminders.isEmpty()) loading = true
            error = null
            try {
                val page = withContext(Dispatchers.IO) { api.reminders(prefs.baseUrl, prefs.token) }
                if (mine != ticket[0]) return@launch
                cache.reminders = page.data
                cache.reminderCursor = page.nextCursor
                cache.remindersReady = true
                items = page.data
                cursor = page.nextCursor
            } catch (e: Exception) {
                if (mine == ticket[0]) fail(e)
            } finally {
                if (mine == ticket[0]) {
                    paging[0] = false
                    loading = false
                }
            }
        }
    }

    fun remove(id: String) {
        scope.launch {
            try {
                withContext(Dispatchers.IO) { api.deleteReminder(prefs.baseUrl, prefs.token, id) }
                val next = items.filter { it.id != id }
                items = next
                cache.reminders = next
                confirmId = null
            } catch (e: Unauthorized) {
                onUnauthorized()
            } catch (_: Exception) {
                confirmId = null
            }
        }
    }

    LaunchedEffect(Unit) { reload() }
    LaunchedEffect(listState) {
        snapshotFlow {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            Triple(last, items.size, cursor)
        }.collect { (last, count, next) ->
            if (next != null && shouldPrefetch(last, count)) loadMore()
        }
    }

    when {
        loading && items.isEmpty() -> ConversationSkeleton()
        items.isEmpty() -> EmptyHint(Icons.Filled.Notifications, error ?: "Nenhum lembrete")
        else -> LazyColumn(Modifier.fillMaxSize(), state = listState) {
            if (error != null) item { Text(error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp)) }
            items(items, key = { it.id }) { reminder ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            reminder.title.ifBlank { reminder.message }.ifBlank { "Lembrete" },
                            color = Ink,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            listOf(
                                reminderStatusLabel(reminder.status),
                                formatListTime(reminder.sendAt).ifBlank { reminder.sendAt },
                            ).joinToString(" · "),
                            color = Muted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    IconButton(onClick = { confirmId = reminder.id }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Apagar", tint = Muted)
                    }
                }
                HorizontalDivider(color = Line)
            }
            if (loadingMore) item { ConversationSkeleton(rows = 2) }
        }
    }
    confirmId?.let { id ->
        AlertDialog(
            onDismissRequest = { confirmId = null },
            title = { Text("Apagar lembrete?", color = Ink) },
            text = { Text("Essa ação não pode ser desfeita.", color = Muted) },
            confirmButton = {
                TextButton(onClick = { remove(id) }) { Text("Apagar", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = { TextButton(onClick = { confirmId = null }) { Text("Cancelar", color = Muted) } },
            containerColor = SurfaceWhite,
        )
    }
}

@Composable
private fun ReminderFormScreen(
    api: Api,
    prefs: Prefs,
    cache: SessionCache,
    onBack: () -> Unit,
    onUnauthorized: () -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var sendAt by remember { mutableStateOf(defaultReminderSendAt()) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    fun pickWhen() {
        val start = runCatching { LocalDateTime.parse(sendAt) }.getOrNull() ?: LocalDateTime.now().plusMinutes(5)
        DatePickerDialog(
            context,
            { _, year, month, day ->
                TimePickerDialog(
                    context,
                    { _, hour, minute ->
                        sendAt = "%04d-%02d-%02dT%02d:%02d".format(year, month + 1, day, hour, minute)
                    },
                    start.hour,
                    start.minute,
                    true,
                ).show()
            },
            start.year,
            start.monthValue - 1,
            start.dayOfMonth,
        ).show()
    }

    fun save() {
        val text = message.trim()
        if (text.isBlank() || saving) return
        saving = true
        error = null
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    api.createReminder(
                        prefs.baseUrl, prefs.token,
                        CreateReminderBody(title = title.trim(), message = text, sendAt = sendAt),
                    )
                }
                cache.invalidateReminders()
                onBack()
            } catch (e: Unauthorized) {
                onUnauthorized()
            } catch (_: Exception) {
                error = "Não foi possível criar o lembrete. Confira o horário e o Pushover."
            } finally {
                saving = false
            }
        }
    }

    Scaffold(
        containerColor = Paper,
        topBar = { ContextaBar("Novo lembrete", onBack) },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                title, { title = it },
                label = { Text("Título (opcional)") },
                singleLine = true,
                colors = contextaFieldColors(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                message, { message = it },
                label = { Text("Mensagem") },
                colors = contextaFieldColors(),
                shape = RoundedCornerShape(12.dp),
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { pickWhen() }.padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Filled.DateRange, contentDescription = null, tint = Brand, modifier = Modifier.size(18.dp))
                Text(sendAt.replace('T', ' '), color = Ink, modifier = Modifier.weight(1f))
            }
            if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
            Button(
                onClick = { save() },
                enabled = !saving && message.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Brand, contentColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                Text(if (saving) "Salvando..." else "Agendar", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun TaskRow(task: Task, onComplete: () -> Unit, onOpen: () -> Unit) {
    val overdue = task.isOverdue()
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconButton(onClick = onComplete, enabled = !task.closed, modifier = Modifier.size(28.dp)) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = if (task.closed) "Concluída" else "Marcar como concluída",
                tint = if (task.status == "done") Brand else if (overdue) Color(0xFFB91C1C) else Line,
            )
        }
        Column(Modifier.weight(1f)) {
            Text(task.title.ifBlank { "Tarefa" }, color = Ink, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                StatusChip(
                    taskStatusLabel(task.status),
                    if (task.status == "done") BrandSoft else Paper,
                    if (task.status == "done") Brand else Muted,
                )
                if (overdue) StatusChip("Atrasada", Color(0xFFFFFBEB), Color(0xFF92400E))
                val due = task.dueAt?.let { formatListTime(it) }.orEmpty()
                if (due.isNotBlank()) Text(due, color = Muted, fontSize = 12.sp)
            }
            if (task.companyName.isNotBlank()) {
                Text(task.companyName, color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}

@Composable
private fun TaskFormScreen(
    api: Api,
    prefs: Prefs,
    cache: SessionCache,
    taskId: String?,
    onBack: () -> Unit,
    onUnauthorized: () -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("pending") }
    var companyId by remember { mutableStateOf("") }
    var dueAt by remember { mutableStateOf("") }
    var companies by remember { mutableStateOf(cache.companies) }
    var pickCompany by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(taskId != null) }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val editing = taskId != null

    fun fail(e: Exception) {
        if (e is Unauthorized) onUnauthorized() else error = "Não foi possível salvar a tarefa."
    }

    LaunchedEffect(taskId) {
        if (cache.companies.isEmpty()) {
            try {
                val page = withContext(Dispatchers.IO) { api.companies(prefs.baseUrl, prefs.token) }
                cache.companies = page.data
                companies = page.data
            } catch (_: Exception) {
            }
        } else {
            companies = cache.companies
        }
        if (taskId == null) {
            loading = false
            return@LaunchedEffect
        }
        loading = true
        try {
            val task = withContext(Dispatchers.IO) { api.task(prefs.baseUrl, prefs.token, taskId) }
            title = task.title
            description = task.description
            status = task.status
            companyId = task.companyId
            dueAt = task.dueAt?.let { localDateOf(it)?.toString() }.orEmpty()
        } catch (e: Unauthorized) {
            onUnauthorized()
        } catch (_: Exception) {
            error = "Não foi possível carregar a tarefa."
        } finally {
            loading = false
        }
    }

    fun save() {
        val name = title.trim()
        if (name.isBlank() || saving) return
        saving = true
        error = null
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    if (taskId == null) {
                        api.createTask(
                            prefs.baseUrl, prefs.token,
                            CreateTaskBody(
                                title = name,
                                description = description.trim(),
                                companyId = companyId,
                                status = status,
                                dueAt = dueAt.ifBlank { null },
                            ),
                        )
                    } else {
                        api.updateTask(
                            prefs.baseUrl, prefs.token, taskId,
                            UpdateTaskBody(
                                title = name,
                                description = description.trim(),
                                companyId = companyId,
                                status = status,
                                dueAt = dueAt,
                            ),
                        )
                    }
                }
                cache.invalidateTasks()
                onBack()
            } catch (e: Exception) {
                fail(e)
            } finally {
                saving = false
            }
        }
    }

    fun remove() {
        val id = taskId ?: return
        saving = true
        scope.launch {
            try {
                withContext(Dispatchers.IO) { api.deleteTask(prefs.baseUrl, prefs.token, id) }
                cache.invalidateTasks()
                onBack()
            } catch (e: Exception) {
                fail(e)
            } finally {
                saving = false
                confirmDelete = false
            }
        }
    }

    val companyName = companies.firstOrNull { it.id == companyId }?.name ?: "Nenhuma"
    Scaffold(
        containerColor = Paper,
        topBar = {
            ContextaBar(if (editing) "Editar tarefa" else "Nova tarefa", onBack) {
                if (editing) {
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Apagar", tint = Muted)
                    }
                }
            }
        },
    ) { padding ->
        if (loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                ConversationSkeleton(rows = 5)
            }
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    title, { title = it },
                    label = { Text("Título") },
                    singleLine = true,
                    colors = contextaFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    description, { description = it },
                    label = { Text("Descrição") },
                    colors = contextaFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Status", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                TypePills(status, taskStatusPills) { status = it }
                Text("Empresa", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { pickCompany = true }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Filled.AccountBox, contentDescription = null, tint = Brand, modifier = Modifier.size(18.dp))
                    Text(companyName, color = Ink, modifier = Modifier.weight(1f))
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = Muted)
                }
                Text("Prazo", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            val start = localDateOf(dueAt) ?: LocalDate.now()
                            DatePickerDialog(
                                context,
                                { _, year, month, day -> dueAt = "%04d-%02d-%02d".format(year, month + 1, day) },
                                start.year,
                                start.monthValue - 1,
                                start.dayOfMonth,
                            ).show()
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Filled.DateRange, contentDescription = null, tint = Brand, modifier = Modifier.size(18.dp))
                    Text(dueAt.ifBlank { "Sem prazo" }, color = if (dueAt.isBlank()) Muted else Ink, modifier = Modifier.weight(1f))
                    if (dueAt.isNotBlank()) {
                        IconButton(onClick = { dueAt = "" }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = "Limpar prazo", tint = Muted)
                        }
                    }
                }
                if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
                Button(
                    onClick = { save() },
                    enabled = !saving && title.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Brand, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) {
                    Text(if (saving) "Salvando..." else "Salvar", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
    if (pickCompany) {
        AlertDialog(
            onDismissRequest = { pickCompany = false },
            title = { Text("Empresa", color = Ink) },
            text = {
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    item { CompanyChoice("Nenhuma", selected = companyId.isBlank()) { companyId = ""; pickCompany = false } }
                    items(companies, key = { it.id }) { company ->
                        CompanyChoice(company.name, selected = company.id == companyId) { companyId = company.id; pickCompany = false }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { pickCompany = false }) { Text("Fechar", color = Muted) } },
            containerColor = SurfaceWhite,
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Apagar tarefa?", color = Ink) },
            text = { Text("Essa ação não pode ser desfeita.", color = Muted) },
            confirmButton = {
                TextButton(onClick = { remove() }) { Text("Apagar", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancelar", color = Muted) } },
            containerColor = SurfaceWhite,
        )
    }
}

@Composable
private fun CompaniesScreen(
    api: Api,
    prefs: Prefs,
    cache: SessionCache,
    onBack: () -> Unit,
    onOpen: (Company) -> Unit,
    onCreate: () -> Unit,
    onUnauthorized: () -> Unit,
) {
    var items by remember { mutableStateOf(cache.companyList) }
    var cursor by remember { mutableStateOf(cache.companyCursor) }
    var query by remember { mutableStateOf(cache.companyQuery) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(!cache.companiesReady) }
    var loadingMore by remember { mutableStateOf(false) }
    val paging = remember { booleanArrayOf(false) }
    val ticket = remember { intArrayOf(0) }
    val scope = rememberCoroutineScope()
    val listState = cache.companyListState

    fun fail(e: Exception) {
        if (e is Unauthorized) onUnauthorized() else error = "Não foi possível carregar as empresas."
    }

    fun loadMore() {
        val next = cache.companyCursor ?: return
        if (paging[0]) return
        paging[0] = true
        loadingMore = true
        val mine = ticket[0]
        val q = cache.companyQuery
        scope.launch {
            try {
                val page = withContext(Dispatchers.IO) {
                    api.companies(prefs.baseUrl, prefs.token, cursor = next, query = q, limit = 20)
                }
                if (mine != ticket[0]) return@launch
                if (page.data.isEmpty()) {
                    cache.companyCursor = null
                    cursor = null
                    return@launch
                }
                val merged = cache.companyList + page.data.filter { row -> cache.companyList.none { it.id == row.id } }
                cache.companyList = merged
                cache.companyCursor = page.nextCursor
                items = merged
                cursor = page.nextCursor
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

    fun reload(q: String) {
        val mine = ++ticket[0]
        paging[0] = true
        scope.launch {
            if (cache.companyList.isEmpty()) loading = true
            error = null
            try {
                val page = withContext(Dispatchers.IO) {
                    api.companies(prefs.baseUrl, prefs.token, query = q, limit = 20)
                }
                if (mine != ticket[0]) return@launch
                cache.companyQuery = q
                cache.companyList = page.data
                cache.companyCursor = page.nextCursor
                cache.companiesReady = true
                items = page.data
                cursor = page.nextCursor
            } catch (e: Exception) {
                if (mine == ticket[0]) fail(e)
            } finally {
                if (mine == ticket[0]) {
                    paging[0] = false
                    loading = false
                }
            }
        }
    }

    LaunchedEffect(query) {
        if (query.isNotEmpty()) delay(300)
        reload(query)
    }
    LaunchedEffect(listState) {
        snapshotFlow {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            Triple(last, items.size, cursor)
        }.collect { (last, count, next) ->
            if (next != null && shouldPrefetch(last, count)) loadMore()
        }
    }

    Scaffold(
        containerColor = Paper,
        topBar = {
            ContextaBar("Empresas", onBack) {
                IconButton(onClick = onCreate) {
                    Icon(Icons.Filled.Add, contentDescription = "Nova empresa", tint = Ink)
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).background(SurfaceWhite)) {
            SearchField(query, { query = it }, "Buscar empresa")
            when {
                loading && items.isEmpty() -> ConversationSkeleton()
                items.isEmpty() -> EmptyHint(Icons.Filled.AccountBox, error ?: "Nenhuma empresa")
                else -> LazyColumn(Modifier.fillMaxSize(), state = listState) {
                    if (error != null) item { Text(error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp)) }
                    items(items, key = { it.id }) { company ->
                        Row(
                            Modifier.fillMaxWidth().clickable { onOpen(company) }.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(company.name, color = Ink, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "${company.openTaskCount} abertas · ${company.contactCount} contatos",
                                    color = Muted,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                            }
                        }
                        HorizontalDivider(color = Line)
                    }
                    if (loadingMore) item { ConversationSkeleton(rows = 2) }
                }
            }
        }
    }
}

@Composable
private fun CompanyDetailScreen(
    api: Api,
    prefs: Prefs,
    cache: SessionCache,
    companyId: String,
    title: String,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onOpenConversation: (String, String) -> Unit,
    onOpenContact: (Contact) -> Unit,
    onOpenTask: (String?) -> Unit,
    onUnauthorized: () -> Unit,
) {
    var company by remember(companyId) { mutableStateOf(cache.companyList.firstOrNull { it.id == companyId }) }
    var chats by remember(companyId) { mutableStateOf(listOf<Conversation>()) }
    var people by remember(companyId) { mutableStateOf(listOf<Contact>()) }
    var jobs by remember(companyId) { mutableStateOf(listOf<Task>()) }
    var error by remember(companyId) { mutableStateOf<String?>(null) }
    var loading by remember(companyId) { mutableStateOf(true) }
    val heading = company?.name?.ifBlank { title } ?: title

    LaunchedEffect(companyId) {
        loading = true
        error = null
        try {
            company = withContext(Dispatchers.IO) { api.company(prefs.baseUrl, prefs.token, companyId) }
            chats = withContext(Dispatchers.IO) { api.conversations(prefs.baseUrl, prefs.token, companyId = companyId).data }
            people = withContext(Dispatchers.IO) { api.contacts(prefs.baseUrl, prefs.token, companyId = companyId).data }
            jobs = withContext(Dispatchers.IO) { api.tasks(prefs.baseUrl, prefs.token, openOnly = true, companyId = companyId).data }
        } catch (e: Unauthorized) {
            onUnauthorized()
        } catch (_: Exception) {
            error = "Não foi possível carregar a empresa."
        } finally {
            loading = false
        }
    }

    Scaffold(
        containerColor = Paper,
        topBar = {
            ContextaBar(heading, onBack) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Create, contentDescription = "Editar", tint = Ink)
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).background(SurfaceWhite)) {
            if (loading && company == null) ConversationSkeleton(rows = 4)
            else {
                if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
                if (!company?.notes.isNullOrBlank()) {
                    Text(company!!.notes, color = Muted, modifier = Modifier.padding(16.dp))
                }
                Text("Tarefas abertas", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp))
                if (jobs.isEmpty()) Text("Nenhuma", color = Muted, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                jobs.forEach { task ->
                    TaskRow(task, onComplete = {}) { onOpenTask(task.id) }
                    HorizontalDivider(Modifier.padding(start = 52.dp), color = Line)
                }
                Text("Conversas", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp))
                if (chats.isEmpty()) Text("Nenhuma", color = Muted, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                chats.forEach { conversation ->
                    ConversationRow(conversation, blocked = conversation.isBlocked(cache.blockedConversationIds, cache.blockedContactIds)) {
                        onOpenConversation(conversation.id, conversation.displayTitle)
                    }
                    HorizontalDivider(Modifier.padding(start = 72.dp), color = Line)
                }
                Text("Contatos", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp))
                if (people.isEmpty()) Text("Nenhum", color = Muted, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                people.forEach { contact ->
                    ContactRow(contact) { onOpenContact(contact) }
                    HorizontalDivider(Modifier.padding(start = 72.dp), color = Line)
                }
            }
        }
    }
}

@Composable
private fun CompanyFormScreen(
    api: Api,
    prefs: Prefs,
    cache: SessionCache,
    companyId: String?,
    onBack: () -> Unit,
    onUnauthorized: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(companyId != null) }
    var saving by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val editing = companyId != null

    LaunchedEffect(companyId) {
        if (companyId == null) {
            loading = false
            return@LaunchedEffect
        }
        try {
            val company = withContext(Dispatchers.IO) { api.company(prefs.baseUrl, prefs.token, companyId) }
            name = company.name
            notes = company.notes
        } catch (e: Unauthorized) {
            onUnauthorized()
        } catch (_: Exception) {
            error = "Não foi possível carregar a empresa."
        } finally {
            loading = false
        }
    }

    fun save() {
        val title = name.trim()
        if (title.isBlank() || saving) return
        saving = true
        error = null
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    if (companyId == null) api.createCompany(prefs.baseUrl, prefs.token, CreateCompanyBody(title, notes.trim()))
                    else api.updateCompany(prefs.baseUrl, prefs.token, companyId, UpdateCompanyBody(title, notes.trim()))
                }
                cache.invalidateCompanies()
                onBack()
            } catch (e: Unauthorized) {
                onUnauthorized()
            } catch (_: Exception) {
                error = "Não foi possível salvar a empresa."
            } finally {
                saving = false
            }
        }
    }

    fun remove() {
        val id = companyId ?: return
        saving = true
        scope.launch {
            try {
                withContext(Dispatchers.IO) { api.deleteCompany(prefs.baseUrl, prefs.token, id) }
                cache.invalidateCompanies()
                onBack()
            } catch (e: Unauthorized) {
                onUnauthorized()
            } catch (_: Exception) {
                error = "Não foi possível apagar."
            } finally {
                saving = false
                confirmDelete = false
            }
        }
    }

    Scaffold(
        containerColor = Paper,
        topBar = {
            ContextaBar(if (editing) "Editar empresa" else "Nova empresa", onBack) {
                if (editing) {
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Apagar", tint = Muted)
                    }
                }
            }
        },
    ) { padding ->
        if (loading) ConversationSkeleton(rows = 3)
        else Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(name, { name = it }, label = { Text("Nome") }, singleLine = true, colors = contextaFieldColors(), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(notes, { notes = it }, label = { Text("Notas") }, colors = contextaFieldColors(), shape = RoundedCornerShape(12.dp), minLines = 3, modifier = Modifier.fillMaxWidth())
            if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
            Button(
                onClick = { save() },
                enabled = !saving && name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Brand, contentColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                Text(if (saving) "Salvando..." else "Salvar", fontWeight = FontWeight.SemiBold)
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Apagar empresa?", color = Ink) },
            text = { Text("Tarefas e contatos vinculados são mantidos, sem empresa.", color = Muted) },
            confirmButton = {
                TextButton(onClick = { remove() }) { Text("Apagar", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancelar", color = Muted) } },
            containerColor = SurfaceWhite,
        )
    }
}

@Composable
private fun MemoriesScreen(
    api: Api,
    prefs: Prefs,
    cache: SessionCache,
    onBack: () -> Unit,
    onOpen: (Memory) -> Unit,
    onCreate: () -> Unit,
    onUnauthorized: () -> Unit,
) {
    var items by remember { mutableStateOf(cache.memories) }
    var cursor by remember { mutableStateOf(cache.memoryCursor) }
    var query by remember { mutableStateOf(cache.memoryQuery) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(!cache.memoriesReady) }
    var loadingMore by remember { mutableStateOf(false) }
    val paging = remember { booleanArrayOf(false) }
    val ticket = remember { intArrayOf(0) }
    val scope = rememberCoroutineScope()
    val listState = cache.memoryListState

    fun fail(e: Exception) {
        if (e is Unauthorized) onUnauthorized() else error = "Não foi possível carregar as memórias."
    }

    fun loadMore() {
        val next = cache.memoryCursor ?: return
        if (paging[0]) return
        paging[0] = true
        loadingMore = true
        val mine = ticket[0]
        val q = cache.memoryQuery
        scope.launch {
            try {
                val page = withContext(Dispatchers.IO) { api.memories(prefs.baseUrl, prefs.token, cursor = next, query = q) }
                if (mine != ticket[0]) return@launch
                if (page.data.isEmpty()) {
                    cache.memoryCursor = null
                    cursor = null
                    return@launch
                }
                val merged = cache.memories + page.data.filter { row -> cache.memories.none { it.id == row.id } }
                cache.memories = merged
                cache.memoryCursor = page.nextCursor
                items = merged
                cursor = page.nextCursor
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

    fun reload(q: String) {
        val mine = ++ticket[0]
        paging[0] = true
        scope.launch {
            if (cache.memories.isEmpty()) loading = true
            error = null
            try {
                val page = withContext(Dispatchers.IO) { api.memories(prefs.baseUrl, prefs.token, query = q) }
                if (mine != ticket[0]) return@launch
                cache.memoryQuery = q
                cache.memories = page.data
                cache.memoryCursor = page.nextCursor
                cache.memoriesReady = true
                items = page.data
                cursor = page.nextCursor
            } catch (e: Exception) {
                if (mine == ticket[0]) fail(e)
            } finally {
                if (mine == ticket[0]) {
                    paging[0] = false
                    loading = false
                }
            }
        }
    }

    LaunchedEffect(query) {
        if (query.isNotEmpty()) delay(300)
        reload(query)
    }
    LaunchedEffect(listState) {
        snapshotFlow {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            Triple(last, items.size, cursor)
        }.collect { (last, count, next) ->
            if (next != null && shouldPrefetch(last, count)) loadMore()
        }
    }

    Scaffold(
        containerColor = Paper,
        topBar = {
            ContextaBar("Memórias", onBack) {
                IconButton(onClick = onCreate) {
                    Icon(Icons.Filled.Add, contentDescription = "Nova memória", tint = Ink)
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).background(SurfaceWhite)) {
            SearchField(query, { query = it }, "Buscar memórias")
            when {
                loading && items.isEmpty() -> ConversationSkeleton()
                items.isEmpty() -> EmptyHint(Icons.Filled.Star, error ?: "Nenhuma memória")
                else -> LazyColumn(Modifier.fillMaxSize(), state = listState) {
                    if (error != null) item { Text(error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp)) }
                    items(items, key = { it.id }) { memory ->
                        Column(Modifier.fillMaxWidth().clickable { onOpen(memory) }.padding(horizontal = 16.dp, vertical = 12.dp)) {
                            Text(memory.headline, color = Ink, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(
                                listOfNotNull(
                                    if (memory.source == "message") "Mensagem" else "Nota",
                                    memory.companyName.ifBlank { null },
                                ).joinToString(" · "),
                                color = Muted,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        HorizontalDivider(color = Line)
                    }
                    if (loadingMore) item { ConversationSkeleton(rows = 2) }
                }
            }
        }
    }
}

@Composable
private fun MemoryDetailScreen(
    api: Api,
    prefs: Prefs,
    cache: SessionCache,
    memoryId: String,
    title: String,
    onBack: () -> Unit,
    onUnauthorized: () -> Unit,
) {
    var memory by remember(memoryId) { mutableStateOf(cache.memories.firstOrNull { it.id == memoryId }) }
    var error by remember(memoryId) { mutableStateOf<String?>(null) }
    var loading by remember(memoryId) { mutableStateOf(memory == null) }
    var confirmDelete by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val heading = memory?.headline ?: title

    LaunchedEffect(memoryId) {
        try {
            memory = withContext(Dispatchers.IO) { api.memory(prefs.baseUrl, prefs.token, memoryId) }
        } catch (e: Unauthorized) {
            onUnauthorized()
        } catch (_: Exception) {
            if (memory == null) error = "Não foi possível carregar a memória."
        } finally {
            loading = false
        }
    }

    fun remove() {
        scope.launch {
            try {
                withContext(Dispatchers.IO) { api.deleteMemory(prefs.baseUrl, prefs.token, memoryId) }
                cache.invalidateMemories()
                onBack()
            } catch (e: Unauthorized) {
                onUnauthorized()
            } catch (_: Exception) {
                confirmDelete = false
            }
        }
    }

    Scaffold(
        containerColor = Paper,
        topBar = {
            ContextaBar(heading, onBack) {
                IconButton(onClick = { confirmDelete = true }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Apagar", tint = Muted)
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)) {
            if (loading) ConversationSkeleton(rows = 3)
            else {
                if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
                Text(memory?.content ?: "", color = Ink, fontSize = 16.sp, lineHeight = 22.sp)
                val meta = listOfNotNull(memory?.companyName?.ifBlank { null }, memory?.contactName?.ifBlank { null })
                if (meta.isNotEmpty()) Text(meta.joinToString(" · "), color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 16.dp))
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Apagar memória?", color = Ink) },
            text = { Text("Essa ação não pode ser desfeita.", color = Muted) },
            confirmButton = {
                TextButton(onClick = { remove() }) { Text("Apagar", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancelar", color = Muted) } },
            containerColor = SurfaceWhite,
        )
    }
}

@Composable
private fun MemoryFormScreen(
    api: Api,
    prefs: Prefs,
    cache: SessionCache,
    onBack: () -> Unit,
    onUnauthorized: () -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var companyId by remember { mutableStateOf("") }
    var companies by remember { mutableStateOf(cache.companies) }
    var pickCompany by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        if (companies.isNotEmpty()) return@LaunchedEffect
        try {
            val page = withContext(Dispatchers.IO) { api.companies(prefs.baseUrl, prefs.token) }
            cache.companies = page.data
            companies = page.data
        } catch (_: Exception) {
        }
    }

    fun save() {
        val body = content.trim()
        if (body.isBlank() || saving) return
        saving = true
        error = null
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    api.createMemory(
                        prefs.baseUrl, prefs.token,
                        CreateMemoryBody(title = title.trim(), content = body, companyId = companyId),
                    )
                }
                cache.invalidateMemories()
                onBack()
            } catch (e: Unauthorized) {
                onUnauthorized()
            } catch (_: Exception) {
                error = "Não foi possível criar a memória."
            } finally {
                saving = false
            }
        }
    }

    val companyName = companies.firstOrNull { it.id == companyId }?.name ?: "Nenhuma"
    Scaffold(
        containerColor = Paper,
        topBar = { ContextaBar("Nova memória", onBack) },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(title, { title = it }, label = { Text("Título (opcional)") }, singleLine = true, colors = contextaFieldColors(), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(content, { content = it }, label = { Text("Conteúdo") }, colors = contextaFieldColors(), shape = RoundedCornerShape(12.dp), minLines = 5, modifier = Modifier.fillMaxWidth())
            Text("Empresa", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { pickCompany = true }.padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Filled.AccountBox, contentDescription = null, tint = Brand, modifier = Modifier.size(18.dp))
                Text(companyName, color = Ink, modifier = Modifier.weight(1f))
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = Muted)
            }
            if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
            Button(
                onClick = { save() },
                enabled = !saving && content.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Brand, contentColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                Text(if (saving) "Salvando..." else "Salvar", fontWeight = FontWeight.SemiBold)
            }
        }
    }
    if (pickCompany) {
        AlertDialog(
            onDismissRequest = { pickCompany = false },
            title = { Text("Empresa", color = Ink) },
            text = {
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    item { CompanyChoice("Nenhuma", selected = companyId.isBlank()) { companyId = ""; pickCompany = false } }
                    items(companies, key = { it.id }) { company ->
                        CompanyChoice(company.name, selected = company.id == companyId) { companyId = company.id; pickCompany = false }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { pickCompany = false }) { Text("Fechar", color = Muted) } },
            containerColor = SurfaceWhite,
        )
    }
}

private val maintenancePills = listOf("denylist" to "Deny list", "cleanup" to "Limpeza")
private val staleDayPills = listOf("30" to "30 dias", "90" to "90 dias", "180" to "180 dias")

@Composable
private fun DashboardScreen(
    api: Api,
    prefs: Prefs,
    onBack: () -> Unit,
    onOpenConversation: (String, String) -> Unit,
    onUnauthorized: () -> Unit,
) {
    var data by remember { mutableStateOf<Dashboard?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        loading = true
        error = null
        try {
            data = withContext(Dispatchers.IO) { api.dashboard(prefs.baseUrl, prefs.token) }
        } catch (e: Unauthorized) {
            onUnauthorized()
        } catch (_: Exception) {
            error = "Não foi possível carregar a visão geral."
        } finally {
            loading = false
        }
    }

    val dash = data
    val online = dash?.let { whatsappOnline(it.uazapiStatus) } == true
    Scaffold(
        containerColor = Paper,
        topBar = { ContextaBar("Visão geral", onBack) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).background(Paper)) {
            if (loading && dash == null) ConversationSkeleton(rows = 6)
            else if (error != null && dash == null) Text(error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
            else if (dash != null) {
                Text(
                    if (online) "WhatsApp conectado" else "WhatsApp: ${dash.uazapiStatus.ifBlank { "indisponível" }}",
                    color = if (online) Brand else Color(0xFF92400E),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
                Row(Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard("Mensagens", dash.messages, "${dash.messagesLast7d} nos últimos 7 dias", Modifier.weight(1f))
                    StatCard("Recebidas", dash.messagesInbound, "entrantes", Modifier.weight(1f))
                }
                Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard("Enviadas", dash.messagesOutbound, "saídas", Modifier.weight(1f))
                    StatCard("Ativas (7d)", dash.activeConversations7d, "${dash.conversations} no total", Modifier.weight(1f))
                }
                Row(Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard("Contatos", dash.contacts, "", Modifier.weight(1f))
                    StatCard("Grupos", dash.groups, "${dash.directs} diretas", Modifier.weight(1f))
                }
                Text("Conversas mais ativas", color = Ink, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp))
                if (dash.topConversations.isEmpty()) {
                    Text("Sem dados ainda.", color = Muted, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                } else {
                    dash.topConversations.forEach { item ->
                        Row(
                            Modifier.fillMaxWidth().clickable { onOpenConversation(item.id, item.title.ifBlank { "Conversa" }) }.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(item.title.ifBlank { "Conversa" }, color = Ink, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(if (item.type == "group") "Grupo" else "Direta", color = Muted, fontSize = 12.sp)
                            }
                            Text(item.messageCount.toString(), color = Brand, fontWeight = FontWeight.SemiBold)
                        }
                        HorizontalDivider(color = Line)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: Int, hint: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceWhite)
            .padding(16.dp),
    ) {
        Text(label, color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Text(value.toString(), color = Ink, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
        if (hint.isNotBlank()) Text(hint, color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun MaintenanceScreen(
    api: Api,
    prefs: Prefs,
    cache: SessionCache,
    onBack: () -> Unit,
    onUnauthorized: () -> Unit,
) {
    var section by remember { mutableStateOf("denylist") }
    Scaffold(
        containerColor = Paper,
        topBar = { ContextaBar("Manutenção", onBack) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).background(SurfaceWhite)) {
            TypePills(section, maintenancePills) { section = it }
            if (section == "cleanup") CleanupPane(api, prefs, cache, onUnauthorized)
            else DenylistPane(api, prefs, cache, onUnauthorized)
        }
    }
}

@Composable
private fun DenylistPane(api: Api, prefs: Prefs, cache: SessionCache, onUnauthorized: () -> Unit) {
    var items by remember { mutableStateOf(listOf<DenylistEntry>()) }
    var cursor by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var loadingMore by remember { mutableStateOf(false) }
    var confirmId by remember { mutableStateOf<String?>(null) }
    val paging = remember { booleanArrayOf(false) }
    val ticket = remember { intArrayOf(0) }
    val scope = rememberCoroutineScope()

    fun fail(e: Exception) {
        if (e is Unauthorized) onUnauthorized() else error = "Não foi possível carregar a deny list."
    }

    fun applySets(entries: List<DenylistEntry>) {
        val sets = denylistSets(entries)
        cache.blockedConversationIds = sets.first
        cache.blockedContactIds = sets.second
    }

    fun loadMore() {
        val next = cursor ?: return
        if (paging[0]) return
        paging[0] = true
        loadingMore = true
        val mine = ticket[0]
        scope.launch {
            try {
                val page = withContext(Dispatchers.IO) { api.denylist(prefs.baseUrl, prefs.token, cursor = next, limit = 20) }
                if (mine != ticket[0]) return@launch
                if (page.data.isEmpty()) {
                    cursor = null
                    return@launch
                }
                items = items + page.data.filter { row -> items.none { it.id == row.id } }
                cursor = page.nextCursor
                applySets(items)
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
            loading = true
            error = null
            try {
                val page = withContext(Dispatchers.IO) { api.denylist(prefs.baseUrl, prefs.token, limit = 20) }
                if (mine != ticket[0]) return@launch
                items = page.data
                cursor = page.nextCursor
                applySets(page.data)
            } catch (e: Exception) {
                if (mine == ticket[0]) fail(e)
            } finally {
                if (mine == ticket[0]) {
                    paging[0] = false
                    loading = false
                }
            }
        }
    }

    fun remove(id: String) {
        scope.launch {
            try {
                withContext(Dispatchers.IO) { api.removeDenylist(prefs.baseUrl, prefs.token, id) }
                items = items.filter { it.id != id }
                applySets(items)
                confirmId = null
            } catch (e: Unauthorized) {
                onUnauthorized()
            } catch (_: Exception) {
                confirmId = null
            }
        }
    }

    LaunchedEffect(Unit) { reload() }

    when {
        loading && items.isEmpty() -> ConversationSkeleton()
        items.isEmpty() -> EmptyHint(Icons.Filled.Warning, error ?: "Nada bloqueado")
        else -> LazyColumn(Modifier.fillMaxSize()) {
            if (error != null) item { Text(error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp)) }
            items(items, key = { it.id }) { entry ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(entry.headline, color = Ink, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(entry.kindLabel, color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                    IconButton(onClick = { confirmId = entry.id }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Remover", tint = Muted)
                    }
                }
                HorizontalDivider(color = Line)
            }
            if (cursor != null) {
                item {
                    TextButton(
                        onClick = { loadMore() },
                        enabled = !loadingMore,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(if (loadingMore) "Carregando..." else "Mais", color = Brand, fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }
    confirmId?.let { id ->
        AlertDialog(
            onDismissRequest = { confirmId = null },
            title = { Text("Remover da deny list?", color = Ink) },
            text = { Text("Novas mensagens voltam a ser salvas.", color = Muted) },
            confirmButton = {
                TextButton(onClick = { remove(id) }) { Text("Remover", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = { TextButton(onClick = { confirmId = null }) { Text("Cancelar", color = Muted) } },
            containerColor = SurfaceWhite,
        )
    }
}

@Composable
private fun CleanupPane(api: Api, prefs: Prefs, cache: SessionCache, onUnauthorized: () -> Unit) {
    var days by remember { mutableStateOf("30") }
    var items by remember { mutableStateOf(listOf<StaleConversation>()) }
    var cursor by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var confirmId by remember { mutableStateOf<String?>(null) }
    val ticket = remember { intArrayOf(0) }
    val scope = rememberCoroutineScope()

    fun fail(e: Exception) {
        if (e is Unauthorized) onUnauthorized() else error = "Não foi possível carregar a limpeza."
    }

    fun reload(selected: String) {
        val mine = ++ticket[0]
        scope.launch {
            loading = true
            error = null
            try {
                val page = withContext(Dispatchers.IO) {
                    api.staleConversations(prefs.baseUrl, prefs.token, days = selected.toIntOrNull() ?: 30)
                }
                if (mine != ticket[0]) return@launch
                items = page.data
                cursor = page.nextCursor
            } catch (e: Exception) {
                if (mine == ticket[0]) fail(e)
            } finally {
                if (mine == ticket[0]) loading = false
            }
        }
    }

    fun loadMore() {
        val next = cursor ?: return
        val mine = ticket[0]
        scope.launch {
            try {
                val page = withContext(Dispatchers.IO) {
                    api.staleConversations(prefs.baseUrl, prefs.token, days = days.toIntOrNull() ?: 30, cursor = next)
                }
                if (mine != ticket[0]) return@launch
                if (page.data.isEmpty()) {
                    cursor = null
                    return@launch
                }
                items = items + page.data.filter { row -> items.none { it.id == row.id } }
                cursor = page.nextCursor
            } catch (e: Exception) {
                fail(e)
            }
        }
    }

    fun remove(id: String) {
        scope.launch {
            try {
                withContext(Dispatchers.IO) { api.deleteConversation(prefs.baseUrl, prefs.token, id) }
                items = items.filter { it.id != id }
                cache.invalidateConversations()
                confirmId = null
            } catch (e: Unauthorized) {
                onUnauthorized()
            } catch (_: Exception) {
                confirmId = null
            }
        }
    }

    LaunchedEffect(days) { reload(days) }

    TypePills(days, staleDayPills) { days = it }
    when {
        loading && items.isEmpty() -> ConversationSkeleton()
        items.isEmpty() -> EmptyHint(Icons.Filled.Delete, error ?: "Nada para limpar")
        else -> LazyColumn(Modifier.fillMaxSize()) {
            if (error != null) item { Text(error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp)) }
            items(items, key = { it.id }) { conversation ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(conversation.displayTitle, color = Ink, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            listOfNotNull(
                                if (conversation.blocked) "Bloqueada" else null,
                                "${conversation.inactiveDays} dias",
                                "${conversation.messageCount} msgs",
                            ).joinToString(" · "),
                            color = Muted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    IconButton(onClick = { confirmId = conversation.id }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Apagar", tint = Color(0xFFB91C1C))
                    }
                }
                HorizontalDivider(color = Line)
            }
            if (cursor != null) {
                item {
                    TextButton(onClick = { loadMore() }, modifier = Modifier.fillMaxWidth()) {
                        Text("Mais", color = Brand, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
    confirmId?.let { id ->
        AlertDialog(
            onDismissRequest = { confirmId = null },
            title = { Text("Apagar conversa?", color = Ink) },
            text = { Text("Apaga a conversa e todas as mensagens. Não pode ser desfeita.", color = Muted) },
            confirmButton = {
                TextButton(onClick = { remove(id) }) { Text("Apagar", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = { TextButton(onClick = { confirmId = null }) { Text("Cancelar", color = Muted) } },
            containerColor = SurfaceWhite,
        )
    }
}

private val integrationPills = listOf(
    "whatsapp" to "WhatsApp",
    "mcp" to "MCP",
    "email" to "E-mail",
    "pushover" to "Pushover",
)

@Composable
private fun IntegrationsScreen(
    api: Api,
    prefs: Prefs,
    onBack: () -> Unit,
    onUnauthorized: () -> Unit,
) {
    var section by remember { mutableStateOf("whatsapp") }
    Scaffold(
        containerColor = Paper,
        topBar = { ContextaBar("Integrações", onBack) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).background(SurfaceWhite)) {
            TypePills(section, integrationPills) { section = it }
            when (section) {
                "mcp" -> McpPane(api, prefs, onUnauthorized)
                "email" -> EmailPane(api, prefs, onUnauthorized)
                "pushover" -> PushoverPane(api, prefs, onUnauthorized)
                else -> WhatsappPane(api, prefs, onUnauthorized)
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = Muted, fontSize = 14.sp)
        Text(value.ifBlank { "—" }, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(start = 12.dp), maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun WhatsappPane(api: Api, prefs: Prefs, onUnauthorized: () -> Unit) {
    var data by remember { mutableStateOf<UazapiIntegration?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        loading = true
        try {
            data = withContext(Dispatchers.IO) { api.uazapi(prefs.baseUrl, prefs.token) }
        } catch (e: Unauthorized) {
            onUnauthorized()
        } catch (_: Exception) {
            error = "Não foi possível carregar o WhatsApp."
        } finally {
            loading = false
        }
    }
    val wa = data
    when {
        loading && wa == null -> ConversationSkeleton(rows = 5)
        error != null && wa == null -> Text(error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
        wa != null -> Column(Modifier.verticalScroll(rememberScrollState())) {
            val ok = wa.connection.connected && wa.connection.loggedIn
            Text(
                if (ok) "Conectada" else "Desconectada",
                color = if (ok) Brand else Color(0xFFB91C1C),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(16.dp),
            )
            InfoRow("Perfil", wa.instance.profileName.ifBlank { wa.instance.name })
            HorizontalDivider(color = Line)
            InfoRow("Estado", wa.instance.status)
            HorizontalDivider(color = Line)
            InfoRow("Sessão", if (wa.connection.loggedIn) "Autenticada" else "Não autenticada")
            HorizontalDivider(color = Line)
            InfoRow("Webhook", if (wa.webhook.enabled) "Ativo" else if (wa.webhook.configured) "Configurado" else "Não configurado")
            HorizontalDivider(color = Line)
            InfoRow("Eventos", wa.webhook.events.size.toString())
            HorizontalDivider(color = Line)
            InfoRow("Verificado", formatListTime(wa.checkedAt).ifBlank { wa.checkedAt })
        }
    }
}

@Composable
private fun McpPane(api: Api, prefs: Prefs, onUnauthorized: () -> Unit) {
    var data by remember { mutableStateOf<McpStatus?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        loading = true
        try {
            data = withContext(Dispatchers.IO) { api.mcpStatus(prefs.baseUrl, prefs.token) }
        } catch (e: Unauthorized) {
            onUnauthorized()
        } catch (_: Exception) {
            error = "Não foi possível carregar o MCP."
        } finally {
            loading = false
        }
    }
    val mcp = data
    when {
        loading && mcp == null -> ConversationSkeleton(rows = 6)
        error != null && mcp == null -> Text(error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
        mcp != null -> LazyColumn(Modifier.fillMaxSize()) {
            item {
                Text(
                    if (mcp.enabled) "Habilitado" else "Desabilitado",
                    color = if (mcp.enabled) Brand else Muted,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(16.dp),
                )
                InfoRow("Endpoint", mcp.endpoint)
                HorizontalDivider(color = Line)
                InfoRow("Auth", mcp.authentication)
                HorizontalDivider(color = Line)
                InfoRow("Último acesso", mcp.lastAccessAt?.let { formatListTime(it).ifBlank { it } } ?: "—")
                Text("Ferramentas", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp))
            }
            if (mcp.tools.isEmpty()) item { Text("Nenhuma ferramenta.", color = Muted, modifier = Modifier.padding(16.dp)) }
            items(mcp.tools, key = { it }) { tool ->
                Text(tool, color = Ink, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                HorizontalDivider(color = Line)
            }
        }
    }
}

@Composable
private fun EmailPane(api: Api, prefs: Prefs, onUnauthorized: () -> Unit) {
    var items by remember { mutableStateOf(listOf<EmailAccount>()) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var testingId by remember { mutableStateOf<String?>(null) }
    var testNote by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        loading = true
        try {
            items = withContext(Dispatchers.IO) { api.emailAccounts(prefs.baseUrl, prefs.token).data }
        } catch (e: Unauthorized) {
            onUnauthorized()
        } catch (_: Exception) {
            error = "Não foi possível carregar as contas de e-mail."
        } finally {
            loading = false
        }
    }
    fun test(id: String) {
        testingId = id
        testNote = null
        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) { api.testEmailAccount(prefs.baseUrl, prefs.token, id) }
                testNote = when {
                    result.imapOk && result.smtpOk -> "IMAP e SMTP ok."
                    else -> listOfNotNull(
                        if (!result.imapOk) "IMAP: ${result.imapError ?: "falhou"}" else null,
                        if (!result.smtpOk) "SMTP: ${result.smtpError ?: "falhou"}" else null,
                    ).joinToString(" · ")
                }
            } catch (e: Unauthorized) {
                onUnauthorized()
            } catch (_: Exception) {
                testNote = "Não foi possível testar."
            } finally {
                testingId = null
            }
        }
    }
    when {
        loading && items.isEmpty() -> ConversationSkeleton()
        items.isEmpty() -> EmptyHint(Icons.Filled.MailOutline, error ?: "Nenhuma conta de e-mail")
        else -> LazyColumn(Modifier.fillMaxSize()) {
            if (error != null) item { Text(error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp)) }
            if (testNote != null) item { Text(testNote!!, color = Brand, modifier = Modifier.padding(16.dp)) }
            items(items, key = { it.id }) { account ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(account.name.ifBlank { account.address }, color = Ink, fontWeight = FontWeight.SemiBold)
                        Text(
                            listOfNotNull(account.address.ifBlank { null }, if (account.enabled) "Ativa" else "Desativada").joinToString(" · "),
                            color = Muted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    TextButton(onClick = { test(account.id) }, enabled = testingId == null) {
                        Text(if (testingId == account.id) "Testando..." else "Testar", color = Brand, fontWeight = FontWeight.SemiBold)
                    }
                }
                HorizontalDivider(color = Line)
            }
        }
    }
}

@Composable
private fun PushoverPane(api: Api, prefs: Prefs, onUnauthorized: () -> Unit) {
    var status by remember { mutableStateOf<PushoverStatus?>(null) }
    var title by remember { mutableStateOf("Contexta") }
    var message by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var note by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var sending by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        loading = true
        try {
            status = withContext(Dispatchers.IO) { api.pushoverStatus(prefs.baseUrl, prefs.token) }
        } catch (e: Unauthorized) {
            onUnauthorized()
        } catch (_: Exception) {
            error = "Não foi possível carregar o Pushover."
        } finally {
            loading = false
        }
    }
    val ready = status?.configured == true && status?.enabled == true
    fun send() {
        val text = message.trim()
        if (text.isBlank() || sending || !ready) return
        sending = true
        error = null
        note = null
        scope.launch {
            try {
                withContext(Dispatchers.IO) { api.sendPushover(prefs.baseUrl, prefs.token, title.trim().ifBlank { "Contexta" }, text) }
                note = "Notificação enviada."
                message = ""
            } catch (e: Unauthorized) {
                onUnauthorized()
            } catch (_: Exception) {
                error = "Não foi possível enviar."
            } finally {
                sending = false
            }
        }
    }
    when {
        loading && status == null -> ConversationSkeleton(rows = 4)
        else -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val label = when {
                status?.configured == true && status?.enabled == true -> "Ativa"
                status?.configured == true -> "Desativada"
                else -> "Não configurada"
            }
            Text(label, color = if (ready) Brand else Color(0xFF92400E), fontWeight = FontWeight.SemiBold)
            if (!ready) Text("Configure o Pushover no painel web para enviar daqui.", color = Muted, fontSize = 13.sp)
            OutlinedTextField(title, { title = it }, label = { Text("Título") }, singleLine = true, colors = contextaFieldColors(), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(message, { message = it }, label = { Text("Mensagem") }, colors = contextaFieldColors(), shape = RoundedCornerShape(12.dp), minLines = 4, modifier = Modifier.fillMaxWidth())
            if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
            if (note != null) Text(note!!, color = Brand)
            Button(
                onClick = { send() },
                enabled = ready && !sending && message.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Brand, contentColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                Text(if (sending) "Enviando..." else "Enviar teste", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

private data class MoreItem(val label: String, val icon: ImageVector, val key: String? = null)

private val moreItems = listOf(
    MoreItem("Visão geral", Icons.Filled.Home, "dashboard"),
    MoreItem("Empresas", Icons.Filled.AccountBox, "companies"),
    MoreItem("Memórias", Icons.Filled.Star, "memories"),
    MoreItem("Manutenção", Icons.Filled.Build, "maintenance"),
    MoreItem("Integrações", Icons.Filled.Share, "integrations"),
    MoreItem("Sistema", Icons.Filled.Settings),
)

@Composable
private fun MoreScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onDashboard: () -> Unit,
    onCompanies: () -> Unit,
    onMemories: () -> Unit,
    onMaintenance: () -> Unit,
    onIntegrations: () -> Unit,
) {
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
                val open = when (item.key) {
                    "dashboard" -> onDashboard
                    "companies" -> onCompanies
                    "memories" -> onMemories
                    "maintenance" -> onMaintenance
                    "integrations" -> onIntegrations
                    else -> null
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .then(if (open != null) Modifier.clickable(onClick = open) else Modifier)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
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
                        Text(if (open == null) "Em breve" else "Abrir", color = Muted, fontSize = 12.sp)
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
    onOpen: (String, String) -> Unit,
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
                        ) { onOpen(conversation.id, conversation.displayTitle) }
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
private fun TypePills(
    selected: String,
    options: List<Pair<String, String>> = typePills,
    onSelect: (String) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { (value, label) ->
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

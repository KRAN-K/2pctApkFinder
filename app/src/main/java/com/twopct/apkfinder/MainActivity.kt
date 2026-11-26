package com.twopct.apkfinder

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme(colorScheme = darkColorScheme()) { Surface(Modifier.fillMaxSize()) { Screen() } } }
    }
}

val categories = mapOf(
    "Browsers" to "browser", "Video Players" to "video player", "Music" to "music player",
    "Games" to "game", "Emulators" to "emulator", "Launchers" to "launcher",
    "File Managers" to "file manager", "Keyboards" to "keyboard", "Messaging" to "messenger",
    "Maps & Navigation" to "maps", "Camera & Photos" to "camera", "Notes" to "notes",
    "VPN & Privacy" to "vpn", "Android TV" to "android tv", "Ebook Readers" to "ebook reader",
    "Tools" to "tools", "Security" to "security", "Terminal" to "terminal"
)

@Composable
fun Screen() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var openSourceOnly by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var results by remember { mutableStateOf(listOf<ApkResult>()) }

    fun open(url: String) = ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    fun search() {
        if (query.isBlank()) return
        loading = true; error = null
        scope.launch {
            val r = withContext(Dispatchers.IO) {
                runCatching {
                    Sources.fdroid(query) + if (openSourceOnly) emptyList() else Sources.apkMirror(query)
                }
            }
            results = r.getOrDefault(emptyList())
            error = r.exceptionOrNull()?.message ?: if (results.isEmpty()) "No results" else null
            loading = false
        }
    }

    Column(Modifier.padding(16.dp).statusBarsPadding()) {
        Text("2% APK Finder", style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().padding(vertical = 8.dp),
            label = { Text("App name or package") }, singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { search() }))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(openSourceOnly, { openSourceOnly = it })
            Text("Open-source / community builds only")
        }
        Button(onClick = { search() }, Modifier.fillMaxWidth()) { Text("Search") }
        var menuOpen by remember { mutableStateOf(false) }
        var selectedCat by remember { mutableStateOf("") }
        Box(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            OutlinedButton(onClick = { menuOpen = true }, Modifier.fillMaxWidth()) {
                Text(if (selectedCat.isEmpty()) "Categories  ▾" else "$selectedCat  ▾")
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                categories.forEach { (label, term) ->
                    DropdownMenuItem(text = { Text(label) }, onClick = {
                        menuOpen = false; selectedCat = label; query = term; search()
                    })
                }
            }
        }
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        LazyColumn {
            items(results) { r ->
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(r.title, style = MaterialTheme.typography.titleMedium)
                        Text(r.source, style = MaterialTheme.typography.labelSmall)
                        if (r.summary.isNotBlank()) Text(r.summary)
                        Row {
                            TextButton({ open(r.pageUrl) }) { Text("Open page") }
                            r.directUrl?.let { TextButton({ open(it) }) { Text("Download APK") } }
                        }
                    }
                }
            }
        }
    }
}

package info.plateaukao.einkbro.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.lifecycleScope
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.database.UserScript
import info.plateaukao.einkbro.userscript.UserScriptManager
import info.plateaukao.einkbro.view.EBToast
import info.plateaukao.einkbro.view.compose.MyTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.android.ext.android.inject

class UserScriptListActivity : ComponentActivity() {
    private val userScriptManager: UserScriptManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Only the URL crosses the Intent boundary: multi-MB script bodies overflow the
        // 1MB Binder transaction limit (TransactionTooLargeException), and fetching here
        // lets the screen show a progress indicator instead of stalling in the browser.
        val installUrl = intent?.getStringExtra(EXTRA_INSTALL_URL)

        setContent {
            val scripts = remember { mutableStateListOf<UserScript>() }
            var editing by remember { mutableStateOf<UserScript?>(null) }
            var showEditor by remember { mutableStateOf(false) }
            var editorSourceUrl by remember { mutableStateOf<String?>(null) }

            fun refresh() {
                lifecycleScope.launch {
                    val list = withContext(Dispatchers.IO) {
                        userScriptManager.reload()
                        userScriptManager.scripts.map { it.script }
                    }
                    scripts.clear()
                    scripts.addAll(list)
                }
            }

            var fetchingUrl by remember { mutableStateOf(installUrl) }

            remember {
                refresh()
                // If launched with a script to install (from a .user.js URL), fetch it and
                // open the editor; fetchingUrl drives the progress indicator meanwhile.
                if (installUrl != null) {
                    lifecycleScope.launch {
                        val fetched = withContext(Dispatchers.IO) { fetchScript(installUrl) }
                        fetchingUrl = null
                        if (fetched != null) {
                            editing = UserScript(name = "", code = fetched, sourceUrl = installUrl)
                            editorSourceUrl = installUrl
                            showEditor = true
                        } else {
                            EBToast.show(this@UserScriptListActivity, R.string.toast_load_error)
                        }
                    }
                }
                true
            }

            MyTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(stringResource(R.string.setting_title_userscripts)) },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                                }
                            },
                            actions = {
                                IconButton(onClick = {
                                    editing = null
                                    editorSourceUrl = null
                                    showEditor = true
                                }) {
                                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.userscript_add))
                                }
                            },
                        )
                    },
                ) { padding ->
                    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                        fetchingUrl?.let { url ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.width(24.dp).height(24.dp),
                                    strokeWidth = 2.dp,
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    url,
                                    maxLines = 2,
                                    color = MaterialTheme.colors.onSurface,
                                )
                            }
                        }
                        if (scripts.isEmpty() && fetchingUrl == null) {
                            Text(
                                stringResource(R.string.userscript_empty),
                                modifier = Modifier.padding(24.dp),
                                color = MaterialTheme.colors.onSurface,
                            )
                        }
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(scripts, key = { it.id }) { script ->
                                ScriptRow(
                                    script = script,
                                    onToggle = { enabled ->
                                        lifecycleScope.launch {
                                            withContext(Dispatchers.IO) {
                                                userScriptManager.setEnabled(script.id, enabled)
                                            }
                                            refresh()
                                        }
                                    },
                                    onEdit = {
                                        editing = script
                                        editorSourceUrl = script.sourceUrl
                                        showEditor = true
                                    },
                                    onDelete = {
                                        lifecycleScope.launch {
                                            withContext(Dispatchers.IO) {
                                                userScriptManager.delete(script.id)
                                            }
                                            refresh()
                                        }
                                    },
                                )
                                Divider()
                            }
                        }
                    }
                }

                if (showEditor) {
                    ScriptEditorDialog(
                        initial = editing,
                        onDismiss = { showEditor = false },
                        onSaveCode = { code ->
                            showEditor = false
                            lifecycleScope.launch {
                                withContext(Dispatchers.IO) {
                                    val current = editing
                                    if (current != null && current.id != 0L) {
                                        userScriptManager.update(current.copy(code = code))
                                    } else {
                                        userScriptManager.add(code, editorSourceUrl)
                                    }
                                }
                                refresh()
                            }
                        },
                        onFetchUrl = { url, onResult ->
                            lifecycleScope.launch {
                                val fetched = withContext(Dispatchers.IO) { fetchScript(url) }
                                if (fetched != null) {
                                    editorSourceUrl = url
                                    onResult(fetched)
                                }
                            }
                        },
                    )
                }
            }
        }
    }

    private fun fetchScript(url: String): String? = try {
        OkHttpClient().newCall(Request.Builder().url(url).build()).execute().use { resp ->
            resp.body?.string()
        }
    } catch (e: Exception) {
        null
    }

    companion object {
        private const val EXTRA_INSTALL_URL = "installUrl"

        fun createIntent(context: Context): Intent =
            Intent(context, UserScriptListActivity::class.java)

        fun createInstallIntent(context: Context, installUrl: String): Intent =
            Intent(context, UserScriptListActivity::class.java).apply {
                putExtra(EXTRA_INSTALL_URL, installUrl)
            }
    }
}

@Composable
private fun ScriptRow(
    script: UserScript,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Switch(checked = script.enabled, onCheckedChange = onToggle)
        Spacer(Modifier.width(12.dp))
        Text(
            script.name.ifBlank { "(unnamed)" },
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colors.onSurface,
        )
        IconButton(onClick = onEdit) {
            Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.menu_edit))
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.menu_delete))
        }
    }
}

private const val EDITOR_DISPLAY_LIMIT = 10_000

@Composable
private fun ScriptEditorDialog(
    initial: UserScript?,
    onDismiss: () -> Unit,
    onSaveCode: (String) -> Unit,
    onFetchUrl: (String, (String) -> Unit) -> Unit,
) {
    var code by remember { mutableStateOf(initial?.code.orEmpty()) }
    var url by remember { mutableStateOf("") }
    // TextField cannot handle multi-MB scripts (composition stalls the UI thread
    // indefinitely), so beyond this size show a truncated read-only preview; `code`
    // keeps the full body and is what gets saved.
    val tooLargeToEdit = code.length > EDITOR_DISPLAY_LIMIT

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colors.surface)
                .padding(16.dp),
        ) {
            Text(
                stringResource(R.string.setting_title_userscripts),
                style = MaterialTheme.typography.h6,
                color = MaterialTheme.colors.onSurface,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextField(
                    value = url,
                    onValueChange = { url = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text(stringResource(R.string.userscript_install_from_url)) },
                )
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = { if (url.isNotBlank()) onFetchUrl(url) { code = it } }) {
                    Text(stringResource(R.string.userscript_fetch))
                }
            }
            Spacer(Modifier.height(8.dp))
            TextField(
                value = if (tooLargeToEdit) code.take(EDITOR_DISPLAY_LIMIT) + "\n..." else code,
                onValueChange = { if (!tooLargeToEdit) code = it },
                readOnly = tooLargeToEdit,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp, max = 360.dp),
                label = { Text(stringResource(R.string.userscript_code)) },
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = { if (code.isNotBlank()) onSaveCode(code) }) {
                    Text(stringResource(android.R.string.ok))
                }
            }
        }
    }
}

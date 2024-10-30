package info.plateaukao.einkbro.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.view.compose.MyTheme
import io.github.edsuns.adfilter.AdFilter
import io.github.edsuns.adfilter.DownloadState
import io.github.edsuns.adfilter.Filter
import io.github.edsuns.adfilter.FilterViewModel
import java.text.SimpleDateFormat
import java.util.*

class AdBlockSettingActivity : ComponentActivity() {

    private val viewModel: FilterViewModel = AdFilter.get().viewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MyTheme {
                SettingsScreen(viewModel = viewModel) {
                    finish()
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(viewModel: FilterViewModel, onCloseAction: () -> Unit = {}) {
    val filters = remember { mutableStateOf(viewModel.filters.value ?: emptyMap()) }
    val isEnabled = remember { mutableStateOf(viewModel.isEnabled.value) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = { (context as ComponentActivity).onBackPressed() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = { filters.value.keys.forEach { viewModel.download(it) } }) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "update")
                    }
                    IconButton(onClick = { onCloseAction() }) {
                        Icon(Icons.Outlined.Close, contentDescription = stringResource(android.R.string.cancel))
                    }
//                    IconButton(onClick = { showDialog(context, viewModel) }) {
//                        Icon(Icons.Outlined.Add, contentDescription = "add filter")
//                    }
                }
            )
        },
        content = { paddingValues ->
            Column(modifier = Modifier.padding(paddingValues)) {
                Switch(
                    checked = isEnabled.value!!,
                    onCheckedChange = { viewModel.isEnabled.value = it }
                )
                if (isEnabled.value!!) {
                    val filterList = viewModel.filters.value?.values?.toList() ?: emptyList()
                    LazyColumn {
                        items(filterList.size) { index ->
                            val filter = filterList[index]
                            FilterRow(filter, SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH)) {
                                viewModel.download(it.id)
                            }
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun FilterRow(
    filter: Filter,
    dateFormatter: SimpleDateFormat,
    onClick: (Filter) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onClick(filter) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (filter.name.isBlank()) filter.url else filter.name,
                style = MaterialTheme.typography.subtitle1,
                maxLines = 1,
            )
            Text(
                text = filter.url,
                style = MaterialTheme.typography.body2,
                maxLines = 1,
                color = Color.Gray
            )
            Row {
                Text(
                    modifier = Modifier.weight(1f),
                    text = when (filter.downloadState) {
                        DownloadState.ENQUEUED -> "waiting"
                        DownloadState.DOWNLOADING -> "downloading"
                        DownloadState.INSTALLING -> "installing"
                        DownloadState.FAILED -> "failed"
                        DownloadState.CANCELLED -> "cancelled"
                        else -> {
                            if (filter.hasDownloaded())
                                dateFormatter.format(Date(filter.updateTime))
                            else "not downloaded"
                        }
                    },
                    style = MaterialTheme.typography.caption,
                    color = Color.Gray
                )
                if (filter.hasDownloaded()) {
                    Text(
                        text = "filter count: ${filter.filtersCount}",
                        style = MaterialTheme.typography.caption,
                        color = Color.Gray
                    )
                }
            }
        }
        Switch(
            checked = filter.isEnabled,
            onCheckedChange = null, // You can handle this change outside
            enabled = filter.filtersCount > 0
        )
    }
}


//@Composable
//fun showDialog(context: Context, viewModel: FilterViewModel) {
//    var textState = remember { mutableStateOf("") }
//
//    val dialog = AlertDialog.Builder(context)
//        .setTitle(R.string.add_filter)
//        .setNegativeButton(android.R.string.cancel, Unit)
//        .setPositiveButton(android.R.string.ok) { _, _ ->
//            val url = textState.value
//            if (url.isNotBlank() && URLUtil.isNetworkUrl(url)) {
//                val filter = viewModel.addFilter("", url)
//                viewModel.download(filter.id)
//            } else {
//                Toast.makeText(context, R.string.invalid_url, Toast.LENGTH_SHORT).show()
//            }
//        }
//        .create()
//
//    dialog.setView(
//        Column(
//            modifier = Modifier
//                .padding(16.dp)
//                .fillMaxWidth()
//        ) {
//            BasicTextField(
//                value = textState.value,
//                onValueChange = { textState.value = it },
//                modifier = Modifier.fillMaxWidth()
//            )
//        }
//    )
//    dialog.show()
//}

package info.plateaukao.einkbro.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import org.koin.androidx.viewmodel.ext.android.viewModel as koinViewModel
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.database.SavedPage
import info.plateaukao.einkbro.unit.IntentUnit
import info.plateaukao.einkbro.view.compose.MyTheme
import info.plateaukao.einkbro.viewmodel.SavedPageViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class SavedPagesActivity : ComponentActivity() {
    private val viewModel: SavedPageViewModel by koinViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    stringResource(R.string.saved_pages),
                                    color = MaterialTheme.colors.onPrimary
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(
                                        tint = MaterialTheme.colors.onPrimary,
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = stringResource(R.string.back)
                                    )
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    SavedPagesList(
                        modifier = Modifier.padding(innerPadding),
                        viewModel = viewModel,
                        onPageClick = { savedPage ->
                            val file = File(savedPage.filePath)
                            if (file.exists()) {
                                IntentUnit.launchUrl(
                                    this@SavedPagesActivity,
                                    "file://${savedPage.filePath}"
                                )
                                finish()
                            }
                        },
                        onPageDelete = { savedPage ->
                            viewModel.deleteSavedPage(savedPage)
                        }
                    )
                }
            }
        }
    }

    companion object {
        fun createIntent(context: Context) = Intent(context, SavedPagesActivity::class.java)
    }
}

@Composable
fun SavedPagesList(
    modifier: Modifier = Modifier,
    viewModel: SavedPageViewModel,
    onPageClick: (SavedPage) -> Unit,
    onPageDelete: (SavedPage) -> Unit,
) {
    val savedPages by viewModel.getAllSavedPages().collectAsState(emptyList())

    if (savedPages.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.no_saved_pages),
                color = MaterialTheme.colors.onBackground,
            )
        }
    } else {
        LazyColumn(
            modifier = modifier.padding(10.dp),
        ) {
            items(savedPages.size) { index ->
                val savedPage = savedPages[index]
                SavedPageItem(
                    savedPage = savedPage,
                    onClick = { onPageClick(savedPage) },
                    onDelete = { onPageDelete(savedPage) },
                )
                if (index < savedPages.lastIndex) Divider(thickness = 1.dp)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SavedPageItem(
    savedPage: SavedPage,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onDelete,
            )
            .padding(vertical = 10.dp)
    ) {
        Text(
            text = savedPage.title,
            color = MaterialTheme.colors.onBackground,
            style = MaterialTheme.typography.body1,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = savedPage.url,
                color = MaterialTheme.colors.onBackground.copy(alpha = 0.6f),
                style = MaterialTheme.typography.caption,
                maxLines = 1,
            )
            Text(
                modifier = Modifier.padding(start = 8.dp),
                text = dateFormatter.format(savedPage.savedAt),
                style = MaterialTheme.typography.caption.copy(
                    color = MaterialTheme.colors.onBackground.copy(alpha = 0.6f),
                ),
            )
            Icon(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(24.dp)
                    .clickable { onDelete() },
                imageVector = ImageVector.vectorResource(id = R.drawable.icon_delete),
                contentDescription = "delete",
                tint = MaterialTheme.colors.onBackground,
            )
        }
    }
}

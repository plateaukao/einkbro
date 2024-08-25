package info.plateaukao.einkbro.activity

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.database.Article
import info.plateaukao.einkbro.database.Highlight
import info.plateaukao.einkbro.unit.BackupUnit
import info.plateaukao.einkbro.unit.BrowserUnit
import info.plateaukao.einkbro.unit.IntentUnit
import info.plateaukao.einkbro.unit.ShareUtil
import info.plateaukao.einkbro.view.NinjaToast
import info.plateaukao.einkbro.view.compose.MyTheme
import info.plateaukao.einkbro.viewmodel.HighlightViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import java.text.SimpleDateFormat
import java.util.Locale

class HighlightsActivity : ComponentActivity(), KoinComponent {
    private val highlightViewModel: HighlightViewModel by viewModels()
    private val backupUnit: BackupUnit by lazy { BackupUnit(this) }

    private lateinit var exportHighlightsLauncher: ActivityResultLauncher<Intent>

    private var highlightsRoute = HighlightsRoute.RouteArticles
    private fun showFileChooser(highlightsRoute: HighlightsRoute) {
        this.highlightsRoute = highlightsRoute
        BrowserUnit.createFilePicker(exportHighlightsLauncher, "highlights.html")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        exportHighlightsLauncher =
            IntentUnit.createResultLauncher(this) {
                it.data?.data?.let { uri -> exportHighlights(uri) }
            }


        setContent {
            val navController: NavHostController = rememberNavController()

            MyTheme {
                val backStackEntry = navController.currentBackStackEntryAsState()
                val currentScreen = HighlightsRoute.valueOf(
                    backStackEntry.value?.destination?.route?.split("/")?.first()
                        ?: HighlightsRoute.RouteArticles.name
                )

                Scaffold(
                    topBar = {
                        HighlightsBar(
                            currentScreen = currentScreen,
                            onClick = { showFileChooser(currentScreen) },
                            navigateUp = {
                                if (navController.previousBackStackEntry != null) navController.navigateUp()
                                else finish()
                            }
                        )
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = HighlightsRoute.RouteArticles.name,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(HighlightsRoute.RouteArticles.name) {
                            ArticlesScreen(navController, highlightViewModel) { article ->
                                highlightViewModel.launchUrl(this@HighlightsActivity, article.url)
                            }
                        }
                        composable("${HighlightsRoute.RouteHighlights.name}/{articleId}") { backStackEntry ->
                            HighlightsScreen(backStackEntry.arguments?.getString("articleId")
                                ?.toInt() ?: 0,
                                modifier = Modifier.padding(10.dp),
                                highlightViewModel,
                                deleteHighlight = { highlightViewModel.deleteHighlight(it) }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun exportHighlights(uri: Uri) {
        highlightViewModel.viewModelScope.launch(Dispatchers.IO) {
            val data = if (highlightsRoute == HighlightsRoute.RouteArticles) {
                highlightViewModel.dumpArticlesHighlightsAsHtml()
            } else {
                highlightViewModel.dumpSingleArticleHighlights(highlightsRoute.articleId)
            }
            backupUnit.exportDataToFileUri(uri, data)

            withContext(Dispatchers.Main) {
                NinjaToast.show(this@HighlightsActivity, R.string.toast_backup_successful)
                IntentUnit.showFile(this@HighlightsActivity, uri)
            }
        }
    }

    companion object {
        fun createIntent(context: Context) = Intent(
            context,
            HighlightsActivity::class.java
        )
    }
}

enum class HighlightsRoute(@StringRes val articleId: Int) {
    RouteArticles(R.string.articles),
    RouteHighlights(R.string.highlights),
}

@Composable
fun ArticlesScreen(
    navHostController: NavHostController,
    highlightViewModel: HighlightViewModel,
    onLinkClick: (Article) -> Unit,
) {
    val articles by highlightViewModel.getAllArticles().collectAsState(emptyList())
    LazyColumn(
        modifier = Modifier.padding(10.dp),
        reverseLayout = true
    ) {
        items(articles.size) { index ->
            val article = articles[index]
            ArticleItem(
                modifier = Modifier.padding(vertical = 10.dp),
                navHostController = navHostController,
                article = article,
                onLinkClick = { onLinkClick(article) },
                deleteArticle = {
                    highlightViewModel.deleteArticle(article.id)
                }
            )
            if (index < articles.lastIndex) Divider(thickness = 1.dp)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ArticleItem(
    modifier: Modifier,
    navHostController: NavHostController,
    article: Article,
    onLinkClick: () -> Unit = {},
    deleteArticle: (Article) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    navHostController.navigate("${HighlightsRoute.RouteHighlights.name}/${article.id}")
                },
                onLongClick = { deleteArticle(article) }
            )
    ) {
        Text(
            modifier = modifier,
            text = article.title,
            color = MaterialTheme.colors.onBackground,
        )
        Row(
            modifier = Modifier
                .align(Alignment.End)
                .clickable { onLinkClick() },
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier
                    .padding(end = 5.dp)
                    .size(23.dp),
                painter = painterResource(id = R.drawable.icon_exit),
                contentDescription = "link",
                tint = MaterialTheme.colors.onBackground,
            )
            Text(
                modifier = Modifier.padding(end = 15.dp),
                text = SimpleDateFormat("MM-dd", Locale.getDefault()).format(article.date),
                style = MaterialTheme.typography.caption.copy(
                    color = MaterialTheme.colors.onBackground,
                )
            )
            Icon(
                modifier = Modifier
                    .size(23.dp)
                    .clickable { deleteArticle(article) },
                painter = painterResource(id = R.drawable.icon_delete),
                contentDescription = "delete",
                tint = MaterialTheme.colors.onBackground,
            )
        }
    }
}

@Composable
fun HighlightsScreen(
    articleId: Int,
    modifier: Modifier = Modifier,
    highlightViewModel: HighlightViewModel,
    deleteHighlight: (Highlight) -> Unit,
) {

    val highlights by highlightViewModel.getHighlightsForArticle(articleId)
        .collectAsState(emptyList())

    var articleName by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val article = highlightViewModel.getArticle(articleId)
        articleName = article?.title.orEmpty()
    }

    LazyColumn(
        modifier = modifier.padding(10.dp),
    ) {
        item {
            Text(
                modifier = Modifier.padding(vertical = 10.dp),
                text = articleName,
                style = MaterialTheme.typography.h6.copy(
                    color = MaterialTheme.colors.onBackground,
                )
            )
        }
        items(highlights.size) { index ->
            HighlightItem(
                highlight = highlights[index],
                deleteHighlight = deleteHighlight,
            )
            if (index < highlights.lastIndex) Divider(thickness = 1.dp)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HighlightItem(
    highlight: Highlight,
    deleteHighlight: (Highlight) -> Unit,
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .padding(vertical = 5.dp)
            .clickable {
                ShareUtil.copyToClipboard(context, highlight.content)
            }
    ) {
        Text(
            modifier = Modifier.padding(vertical = 10.dp),
            text = highlight.content,
            color = MaterialTheme.colors.onBackground,
        )
        Row(
            modifier = Modifier.align(Alignment.End),
            horizontalArrangement = Arrangement.End,
        ) {
            Icon(
                modifier = Modifier
                    .size(24.dp)
                    .clickable {
                        ShareUtil.copyToClipboard(context, highlight.content)
                    },
                painter = painterResource(id = R.drawable.ic_copy),
                contentDescription = "copy",
                tint = MaterialTheme.colors.onBackground,
            )
            Spacer(modifier = Modifier.size(10.dp))
            Icon(
                modifier = Modifier
                    .size(24.dp)
                    .clickable {
                        deleteHighlight(highlight)
                    },
                painter = painterResource(id = R.drawable.icon_delete),
                contentDescription = "delete",
                tint = MaterialTheme.colors.onBackground,
            )
        }
    }
}

@Composable
fun HighlightsBar(
    currentScreen: HighlightsRoute,
    onClick: () -> Unit,
    navigateUp: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(
                stringResource(currentScreen.articleId),
                color = MaterialTheme.colors.onPrimary
            )
        },
        navigationIcon = {
            IconButton(onClick = navigateUp) {
                Icon(
                    tint = MaterialTheme.colors.onPrimary,
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back)
                )
            }
        },
        actions = {
            IconButton(onClick = onClick) {
                Icon(
                    tint = MaterialTheme.colors.onPrimary,
                    painter = painterResource(id = R.drawable.icon_export),
                    contentDescription = ""
                )
            }
        }
    )
}

@Preview
@Composable
fun PreviewArticleItem() {
    MyTheme {
        ArticleItem(
            modifier = Modifier,
            article = Article(
                title = "Hello",
                url = "123",
                date = System.currentTimeMillis(),
                tags = ""
            ),
            navHostController = rememberNavController(),
            deleteArticle = {}
        )
    }
}

@Preview
@Composable
fun PreviewHighlightItem() {
    MyTheme {
        HighlightItem(
            highlight = Highlight(
                articleId = 1,
                content = "Hello",
            ),
            deleteHighlight = {}
        )
    }
}

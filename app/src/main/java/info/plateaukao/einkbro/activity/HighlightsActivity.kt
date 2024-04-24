package info.plateaukao.einkbro.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.unit.BackupUnit
import info.plateaukao.einkbro.view.compose.MyTheme
import info.plateaukao.einkbro.view.dialog.compose.HorizontalSeparator
import info.plateaukao.einkbro.viewmodel.HighlightViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.text.SimpleDateFormat
import java.util.Locale

class HighlightsActivity : ComponentActivity(), KoinComponent {
    private val config: ConfigManager by inject()
    private val highlightViewModel: HighlightViewModel by viewModels()
    private val backupUnit: BackupUnit by lazy { BackupUnit(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        setContent {
            val navController: NavHostController = rememberNavController()

            var showDialog by remember { mutableStateOf(false) }

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
                            onClick = { exportHighlights(currentScreen) },
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
                                deleteHighlight = { it -> highlightViewModel.deleteHighlight(it) }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun exportHighlights(currentScreen: HighlightsRoute) {
        highlightViewModel.viewModelScope.launch(Dispatchers.IO) {
            if (currentScreen == HighlightsRoute.RouteArticles) {
                var data = ""
                highlightViewModel.getAllArticlesAsync().forEach { article ->
                    val articleTitle = highlightViewModel.getArticle(article.id)?.title ?: ""
                    val highlights = highlightViewModel.getHighlightsForArticleAsync(article.id)
                    data += "<h1>$articleTitle</h1><br/><hr/><br/>"
                    data += highlights.joinToString("<br/><br/>") { it.content }
                    data += "<br/><br/>"
                }
                backupUnit.exportHighlights(data)
            } else {
                val articleId = currentScreen.titleId
                val articleTitle = highlightViewModel.getArticle(articleId)?.title ?: ""
                val highlights = highlightViewModel.getHighlightsForArticleAsync(articleId)
                var data = "<h1>$articleTitle</h1><hr><br/>"
                data += highlights.joinToString("<br/><br/>") { it.content }
                backupUnit.exportHighlights(data, articleTitle)
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

enum class HighlightsRoute(@StringRes val titleId: Int) {
    RouteArticles(R.string.articles),
    RouteHighlights(R.string.highlights),
}

@Composable
fun ArticlesScreen(
    navHostController: NavHostController,
    highlightViewModel: HighlightViewModel,
    onLinkClick: (Article) -> Unit
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
    deleteArticle: (Article) -> Unit
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
            text = article.title
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onLinkClick() },
            horizontalArrangement = Arrangement.End
        ) {
            Icon(
                modifier = Modifier.size(18.dp),
                painter = painterResource(id = R.drawable.icon_exit),
                contentDescription = "link",
                tint = Color.Gray
            )
            Text(
                text = SimpleDateFormat("MMM dd", Locale.getDefault()).format(article.date),
                style = MaterialTheme.typography.caption.copy(
                    color = Color.Gray
                )
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
        articleName = article?.title ?: ""
    }

    LazyColumn(
        modifier = modifier.padding(10.dp),
    ) {
        item {
            Text(
                text = articleName,
                style = MaterialTheme.typography.h6
            )
        }
        item {
            HorizontalSeparator()
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
    deleteHighlight: (Highlight) -> Unit
) {
    Text(
        modifier = Modifier
            .padding(vertical = 10.dp)
            .combinedClickable(
                onClick = {},
                onLongClick = { deleteHighlight(highlight) }
            ),
        text = highlight.content
    )
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
                stringResource(currentScreen.titleId),
                color = MaterialTheme.colors.onPrimary
            )
        },
        navigationIcon = {
            IconButton(onClick = navigateUp) {
                Icon(
                    tint = MaterialTheme.colors.onPrimary,
                    imageVector = Icons.Filled.ArrowBack,
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

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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.database.Article
import info.plateaukao.einkbro.database.Highlight
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.view.compose.MyTheme
import info.plateaukao.einkbro.viewmodel.HighlightViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.text.SimpleDateFormat
import java.util.Locale

class HighlightsActivity : ComponentActivity(), KoinComponent {
    private val config: ConfigManager by inject()
    private val highlightViewModel: HighlightViewModel by viewModels()

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
                                deleteHighlight = {}
                            )
                        }
                    }
                }
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
    Column (
        modifier = modifier.combinedClickable(
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
                .align(Alignment.End)
                .clickable { onLinkClick() }
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
    val highlights by highlightViewModel.getHighlightsForArticle(articleId).collectAsState(emptyList())
    LazyColumn(
        modifier = modifier.padding(10.dp),
    ) {
        items(highlights.size) { index ->
            HighlightItem(
                highlight = highlights[index],
                deleteHighlight = deleteHighlight,
            )
            if (index < highlights.lastIndex) Divider(thickness = 1.dp)
        }
    }
}

@Composable
fun HighlightItem(
    highlight: Highlight,
    deleteHighlight: (Highlight) -> Unit
) {
    Text(
        modifier = Modifier.padding(vertical = 10.dp),
        text = highlight.content
    )
}

@Composable
fun HighlightsBar(
    currentScreen: HighlightsRoute,
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
        }
    )
}

@Preview
@Composable
fun previewArticleItem() {
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
fun previewHighlightItem() {
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

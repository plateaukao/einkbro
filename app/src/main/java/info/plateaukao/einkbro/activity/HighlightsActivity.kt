package info.plateaukao.einkbro.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.database.Article
import info.plateaukao.einkbro.database.BookmarkManager
import info.plateaukao.einkbro.database.Highlight
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.view.compose.MyTheme
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class HighlightsActivity : ComponentActivity(), KoinComponent {
    private val config: ConfigManager by inject()
    private val bookmarkManager: BookmarkManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        setContent {
            val navController: NavHostController = rememberNavController()

            val actionList = remember { mutableStateOf(config.gptActionList) }
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
                            ArticlesScreen(navController, bookmarkManager)
                        }
                        composable("${HighlightsRoute.RouteHighlights.name}/{articleId}") { backStackEntry ->
                            HighlightsScreen(backStackEntry.arguments?.getString("articleId")
                                ?.toInt() ?: 0,
                                modifier = Modifier.padding(10.dp),
                                bookmarkManager,
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
    bookmarkManager: BookmarkManager
) {
    val articles by bookmarkManager.getAllArticles().collectAsState(emptyList())
    LazyColumn(
        modifier = Modifier.padding(10.dp),
        reverseLayout = true
    ) {
        items(articles.size) { index ->
            val article = articles[index]
            ArticleItem(
                modifier = Modifier.padding(bottom = 10.dp)
                .clickable {
                    navHostController.navigate("${HighlightsRoute.RouteHighlights.name}/${articles[index].id}")
                },
                article = article,
                deleteArticle = {} // { a -> bookmarkManager.deleteArticle(a) }
            )
        }
    }
}

@Composable
fun ArticleItem(
    modifier: Modifier,
    article: Article,
    deleteArticle: (Article) -> Unit
) {
    Text(
        modifier = modifier,
        text = article.title
    )
}

@Composable
fun HighlightsScreen(
    articleId: Int,
    modifier: Modifier = Modifier,
    bookmarkManager: BookmarkManager,
    deleteHighlight: (Highlight) -> Unit,
) {
    val highlights by bookmarkManager.getHighlightsForArticle(articleId).collectAsState(emptyList())
    LazyColumn(
        modifier = modifier.padding(10.dp),
    ) {
        items(highlights.size) { index ->
            HighlightItem(
                highlight = highlights[index],
                deleteHighlight = deleteHighlight,
            )
        }
    }
}

@Composable
fun HighlightItem(
    highlight: Highlight,
    deleteHighlight: (Highlight) -> Unit
) {
    Text(
        modifier = Modifier.padding(bottom = 10.dp),
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

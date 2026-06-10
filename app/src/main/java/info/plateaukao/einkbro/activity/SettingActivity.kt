package info.plateaukao.einkbro.activity

import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NO_ANIMATION
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import android.view.WindowInsets
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.annotation.StringRes
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.TopAppBar
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.activity.SettingRoute.About
import info.plateaukao.einkbro.activity.SettingRoute.Backup
import info.plateaukao.einkbro.activity.SettingRoute.Behavior
import info.plateaukao.einkbro.activity.SettingRoute.ChatGPT
import info.plateaukao.einkbro.activity.SettingRoute.DataControl
import info.plateaukao.einkbro.activity.SettingRoute.Gesture
import info.plateaukao.einkbro.activity.SettingRoute.Main
import info.plateaukao.einkbro.activity.SettingRoute.Misc
import info.plateaukao.einkbro.activity.SettingRoute.Search
import info.plateaukao.einkbro.activity.SettingRoute.StartControl
import info.plateaukao.einkbro.activity.SettingRoute.Toolbar
import info.plateaukao.einkbro.activity.SettingRoute.Ui
import info.plateaukao.einkbro.activity.SettingRoute.UserAgent
import info.plateaukao.einkbro.activity.SettingRoute.valueOf
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.setting.DividerSettingItem
import info.plateaukao.einkbro.setting.GesturePickerScreen
import info.plateaukao.einkbro.setting.SettingItemInterface
import info.plateaukao.einkbro.setting.SearchSettingScreen
import info.plateaukao.einkbro.setting.SettingScreen
import info.plateaukao.einkbro.setting.screens.BackupOps
import info.plateaukao.einkbro.setting.screens.SettingScreenDeps
import info.plateaukao.einkbro.setting.screens.buildAboutSettingItems
import info.plateaukao.einkbro.setting.screens.buildBackupSettingItems
import info.plateaukao.einkbro.setting.screens.buildBehaviorSettingItems
import info.plateaukao.einkbro.setting.screens.buildChatGptSettingItems
import info.plateaukao.einkbro.setting.screens.buildClearDataSettingItems
import info.plateaukao.einkbro.setting.screens.buildGestureSettingItems
import info.plateaukao.einkbro.setting.screens.buildMainSettingItems
import info.plateaukao.einkbro.setting.screens.buildMiscSettingItems
import info.plateaukao.einkbro.setting.screens.buildSearchSettingItems
import info.plateaukao.einkbro.setting.screens.buildStartSettingItems
import info.plateaukao.einkbro.setting.screens.buildToolbarSettingItems
import info.plateaukao.einkbro.setting.screens.buildUiSettingItems
import info.plateaukao.einkbro.setting.screens.buildUserAgentSettingItems
import info.plateaukao.einkbro.unit.BackupCategory
import info.plateaukao.einkbro.unit.BackupUnit
import info.plateaukao.einkbro.unit.disablePendingTransitions
import info.plateaukao.einkbro.unit.LocaleManager
import info.plateaukao.einkbro.unit.ShareUtil
import info.plateaukao.einkbro.view.compose.MyTheme
import info.plateaukao.einkbro.view.dialog.DialogManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject

class SettingActivity : FragmentActivity(), BackupOps {
    private val config: ConfigManager by inject()
    private val dialogManager: DialogManager by lazy { DialogManager(this) }
    private val backupUnit: BackupUnit by lazy { BackupUnit(this) }

    private var pendingBackupCategories: Set<BackupCategory> = emptySet()

    private val exportBookmarksLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val uri: Uri = result.data?.data ?: return@registerForActivityResult
            backupUnit.exportBookmarks(uri)
        }

    private val importBookmarksLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val uri: Uri = result.data?.data ?: return@registerForActivityResult
            backupUnit.importBookmarks(uri)
        }

    private val exportBackupLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val uri: Uri = result.data?.data ?: return@registerForActivityResult
            val categories = pendingBackupCategories
            pendingBackupCategories = emptySet()
            if (categories.isNotEmpty()) {
                lifecycleScope.launch { backupUnit.backupData(this@SettingActivity, uri, categories) }
            }
        }

    private val importBackupLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val uri: Uri = result.data?.data ?: return@registerForActivityResult
            val available = backupUnit.getAvailableCategories(this, uri)
            if (available == null) {
                if (backupUnit.restoreLegacyBackupData(this, uri)) {
                    dialogManager.showRestartConfirmDialog()
                }
            } else {
                dialogManager.showRestoreCategoryDialog(available) { selected ->
                    lifecycleScope.launch {
                        if (backupUnit.restoreBackupData(this@SettingActivity, uri, selected)) {
                            dialogManager.showRestartConfirmDialog()
                        }
                    }
                }
            }
        }


    @OptIn(ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        val routeName = intent.getStringExtra(KEY_ROUTE) ?: Main.name
        setContent {
            val navController: NavHostController = rememberNavController()
            MyTheme {

                val backStackEntry = navController.currentBackStackEntryAsState()
                val currentScreen = valueOf(backStackEntry.value?.destination?.route ?: Main.name)
                var isSearching by remember { mutableStateOf(false) }
                var searchQuery by remember { mutableStateOf("") }

                Scaffold(
                    modifier = Modifier.semantics {
                        testTagsAsResourceId = true
                    },
                    topBar = {
                        if (isSearching) {
                            SearchSettingBar(
                                query = searchQuery,
                                onQueryChange = { searchQuery = it },
                                onClose = { isSearching = false; searchQuery = "" }
                            )
                        } else {
                            SettingBar(
                                currentScreen = currentScreen,
                                navigateUp = {
                                    if (navController.previousBackStackEntry != null) navController.navigateUp()
                                    else finish()
                                },
                                close = { finish() },
                                onSearch = { isSearching = true }
                            )
                        }
                    }
                ) { innerPadding ->
                    if (isSearching) {
                        SearchSettingScreen(
                            query = searchQuery,
                            allSettings = allSearchableSettings,
                            navController = navController,
                            dialogManager = dialogManager,
                            linkAction = this@SettingActivity::handleLink,
                            modifier = Modifier.padding(innerPadding),
                        )
                    } else NavHost(
                        navController = navController,
                        startDestination = routeName,
                        modifier = Modifier.padding(innerPadding),
                        enterTransition = { fadeIn(animationSpec = tween(1)) },
                        exitTransition = { fadeOut(animationSpec = tween(1)) },
                    ) {
                        val action = this@SettingActivity::handleLink
                        composable(Main.name) {
                            SettingScreen(navController, mainSettings, dialogManager, action, 2)
                        }
                        composable(Ui.name) {
                            SettingScreen(navController, uiSettingItems, dialogManager, action, 1)
                        }
                        composable(Toolbar.name) {
                            SettingScreen(navController, toolbarSettingItems, dialogManager, action, 1)
                        }
                        composable(Behavior.name) {
                            SettingScreen(navController, behaviorSettingItems, dialogManager, action, 1)
                        }
                        composable(Gesture.name) {
                            SettingScreen(navController, gestureSettingItems, dialogManager, action, 2)
                        }
                        composable(SettingRoute.GesturePicker.name) {
                            GesturePickerScreen(navController)
                        }
                        composable(Backup.name) {
                            SettingScreen(navController, dataSettingItems, dialogManager, action, 1)
                        }
                        composable(StartControl.name) {
                            SettingScreen(navController, startSettingItems, dialogManager, action, 1)
                        }
                        composable(DataControl.name) {
                            SettingScreen(navController, clearDataSettingItems, dialogManager, action, 1)
                        }
                        composable(UserAgent.name) {
                            SettingScreen(navController, userAgentSettingItems, dialogManager, action, 1)
                        }
                        composable(Misc.name) {
                            SettingScreen(navController, miscSettingItems, dialogManager, action, 1)
                        }
                        composable(ChatGPT.name) {
                            SettingScreen(navController, chatGptSettingItems, dialogManager, action, 1)
                        }
                        composable(Search.name) {
                            SettingScreen(navController, searchSettingItems, dialogManager, action, 1)
                        }
                        composable(About.name) {
                            SettingScreen(navController, buildAboutSettingItems(deps), dialogManager, action, 2)
                        }
                    }
                }
            }
        }

        if (config.ui.hideStatusbar) {
            hideStatusBar()
        }
    }

    override fun onResume() {
        super.onResume()
        disablePendingTransitions()
    }

    override fun attachBaseContext(newBase: Context) {
        if (config.uiLocaleLanguage.isNotEmpty()) {
            super.attachBaseContext(
                LocaleManager.setLocale(newBase, config.uiLocaleLanguage)
            )
        } else {
            super.attachBaseContext(newBase)
        }
    }

    override fun exportAppData() {
        dialogManager.showBackupCategoryDialog { categories ->
            pendingBackupCategories = categories
            dialogManager.showBackupFilePicker(exportBackupLauncher)
        }
    }

    override fun importAppData() {
        dialogManager.showImportBackupFilePicker(importBackupLauncher)
    }

    override fun shareAppData() {
        dialogManager.showBackupCategoryDialog { categories ->
            shareAppData(categories)
        }
    }

    override fun exportBookmarks() {
        dialogManager.showBookmarkFilePicker(exportBookmarksLauncher)
    }

    override fun importBookmarks() {
        dialogManager.showImportBookmarkFilePicker(importBookmarksLauncher)
    }

    private fun shareAppData(categories: Set<BackupCategory>) {
        lifecycleScope.launch(Dispatchers.IO) {
            val tempFile = backupUnit.backupToTempFile(categories) ?: return@launch
            withContext(Dispatchers.Main) {
                ShareUtil.startServingFile(lifecycleScope, tempFile)
                dialogManager.showOkCancelDialog(
                    title = getString(R.string.setting_title_share_appData),
                    message = getString(R.string.share_broadcasting),
                    okAction = { ShareUtil.stopBroadcast(); tempFile.delete() },
                    showNegativeButton = false,
                )
            }
        }
    }

    override fun receiveAppData() {
        val tempFile = java.io.File(cacheDir, "backup_receive.zip")
        val dialog = dialogManager.showOkCancelDialog(
            title = getString(R.string.setting_title_receive_appData),
            message = getString(R.string.share_waiting),
            okAction = { ShareUtil.stopBroadcast() },
            showNegativeButton = false,
        )

        ShareUtil.startReceivingFile(lifecycleScope, tempFile, onConnected = {
            dialog.findViewById<android.widget.TextView>(android.R.id.message)?.text =
                getString(R.string.share_receiving)
        }) { file ->
            dialog.dismiss()
            val available = backupUnit.getAvailableCategories(file)
            if (available != null) {
                dialogManager.showRestoreCategoryDialog(available) { selected ->
                    lifecycleScope.launch {
                        if (backupUnit.restoreBackupData(file, selected)) {
                            dialogManager.showRestartConfirmDialog()
                        }
                        file.delete()
                    }
                }
            } else {
                file.delete()
            }
        }
    }

    private fun handleLink(url: String) {
        startActivity(
            Intent(this, BrowserActivity::class.java).apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, url)
            }
        )
        finish()
        disablePendingTransitions()
    }

    private val allSearchableSettings: List<Pair<Int, SettingItemInterface>> by lazy {
        listOf(
            Ui.titleId to uiSettingItems,
            Toolbar.titleId to toolbarSettingItems,
            Behavior.titleId to behaviorSettingItems,
            Gesture.titleId to gestureSettingItems,
            Search.titleId to searchSettingItems,
            Backup.titleId to dataSettingItems,
            DataControl.titleId to clearDataSettingItems,
            StartControl.titleId to startSettingItems,
            Misc.titleId to miscSettingItems,
            ChatGPT.titleId to chatGptSettingItems,
            UserAgent.titleId to userAgentSettingItems,
        ).flatMap { (categoryResId, items) ->
            items.filter { it !is DividerSettingItem }
                .map { categoryResId to it }
        }
    }

    private val deps = SettingScreenDeps(this, config, lifecycleScope, this)

    private val mainSettings = buildMainSettingItems()
    private val uiSettingItems = buildUiSettingItems(deps)
    private val behaviorSettingItems = buildBehaviorSettingItems(deps)
    private val toolbarSettingItems = buildToolbarSettingItems(deps)
    private val gestureSettingItems = buildGestureSettingItems(deps)
    private val searchSettingItems = buildSearchSettingItems(deps)
    private val dataSettingItems = buildBackupSettingItems(deps)
    private val clearDataSettingItems = buildClearDataSettingItems(deps)
    private val miscSettingItems = buildMiscSettingItems(deps)
    private val userAgentSettingItems = buildUserAgentSettingItems(deps)
    private val chatGptSettingItems = buildChatGptSettingItems(deps)
    private val startSettingItems = buildStartSettingItems(deps)

    @Suppress("DEPRECATION")
    private fun hideStatusBar() {
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.systemBars())
            window.setDecorFitsSystemWindows(false)
        }
    }

    companion object {
        private const val KEY_ROUTE = "route"

        // create an intent to navigate to desired setting screen route
        fun createIntent(context: Context, route: SettingRoute): Intent {
            return Intent(context, SettingActivity::class.java).apply {
                addFlags(FLAG_ACTIVITY_NO_ANIMATION)
                putExtra(KEY_ROUTE, route.name)
            }
        }
    }
}

enum class SettingRoute(@StringRes val titleId: Int) {
    Main(R.string.settings),
    Ui(R.string.setting_title_ui),
    Toolbar(R.string.setting_title_toolbar),
    Behavior(R.string.setting_title_behavior),
    Gesture(R.string.setting_gestures),
    Backup(R.string.setting_title_data),
    StartControl(R.string.setting_title_start_control),
    DataControl(R.string.setting_title_clear_control),
    UserAgent(R.string.setting_title_userAgent),
    Search(R.string.setting_title_search),
    About(R.string.title_about),
    ChatGPT(R.string.setting_title_chat_gpt),
    Misc(R.string.misc),
    GesturePicker(R.string.setting_gestures);
}

@Composable
fun SettingBar(
    currentScreen: SettingRoute,
    navigateUp: () -> Unit,
    close: () -> Unit,
    onSearch: () -> Unit,
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
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back)
                )
            }
        },
        actions = {
            IconButton(onClick = onSearch) {
                Icon(
                    tint = MaterialTheme.colors.onPrimary,
                    imageVector = Icons.Filled.Search,
                    contentDescription = stringResource(R.string.search_hint)
                )
            }
            if (currentScreen != SettingRoute.Main) {
                IconButton(onClick = close) {
                    Icon(
                        tint = MaterialTheme.colors.onPrimary,
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.back)
                    )
                }
            }
        }
    )
}

@Composable
fun SearchSettingBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    TopAppBar(
        title = {
            TextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = {
                    Text(
                        stringResource(R.string.search_settings_hint),
                        color = MaterialTheme.colors.onPrimary.copy(alpha = 0.6f)
                    )
                },
                singleLine = true,
                colors = TextFieldDefaults.textFieldColors(
                    textColor = MaterialTheme.colors.onPrimary,
                    cursorColor = MaterialTheme.colors.onPrimary,
                    backgroundColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
            )
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
        },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(
                    tint = MaterialTheme.colors.onPrimary,
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back)
                )
            }
        },
    )
}

@file:Suppress("DEPRECATION")

package info.plateaukao.einkbro

import android.Manifest
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Looper
import android.preference.PreferenceManager
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import info.plateaukao.einkbro.activity.BrowserActivity
import info.plateaukao.einkbro.activity.SettingActivity
import info.plateaukao.einkbro.browser.AdBlock
import info.plateaukao.einkbro.browser.Cookie
import info.plateaukao.einkbro.browser.Javascript
import info.plateaukao.einkbro.database.BookmarkManager
import info.plateaukao.einkbro.database.RecordRepository
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.search.suggestion.SearchSuggestionViewModel
import info.plateaukao.einkbro.data.remote.InstapaperRepository
import info.plateaukao.einkbro.service.TtsManager
import info.plateaukao.einkbro.service.TtsNotificationManager
import info.plateaukao.einkbro.unit.LocaleManager
import info.plateaukao.einkbro.util.WebViewUtil
import io.github.edsuns.adfilter.AdFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import info.plateaukao.einkbro.viewmodel.ActionModeMenuViewModel
import info.plateaukao.einkbro.viewmodel.ExternalSearchViewModel
import info.plateaukao.einkbro.viewmodel.GptQueryViewModel
import info.plateaukao.einkbro.viewmodel.HighlightViewModel
import info.plateaukao.einkbro.viewmodel.InstapaperViewModel
import info.plateaukao.einkbro.viewmodel.RemoteConnViewModel
import info.plateaukao.einkbro.viewmodel.SavedPageViewModel
import info.plateaukao.einkbro.viewmodel.TranslationViewModel
import info.plateaukao.einkbro.viewmodel.TtsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.GlobalContext.startKoin
import org.koin.dsl.module
import timber.log.Timber

class EinkBroApplication : Application() {

    private val sp: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(applicationContext)
    }

    private val config: ConfigManager by lazy {
        ConfigManager(applicationContext, sp)
    }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val ttsManager: TtsManager = TtsManager(this, appScope)

    private val myModule = module {
        single<CoroutineScope> { appScope }
        single { config }
        single { sp }
        single { BookmarkManager(androidContext()) }
        single { RecordRepository() }
        single { AdBlock(androidContext()) }
        single { Javascript(androidContext()) }
        single { Cookie(androidContext()) }
        single { ttsManager }
        single { TtsNotificationManager(androidContext()) }
        single { InstapaperRepository() }
        single { SearchSuggestionViewModel() }
        viewModel { TranslationViewModel(get(), get()) }
        viewModel { TtsViewModel(get(), get(), get()) }
        viewModel { ActionModeMenuViewModel(get()) }
        viewModel { ExternalSearchViewModel(get()) }
        viewModel { RemoteConnViewModel(get()) }
        viewModel { InstapaperViewModel(get()) }
        viewModel { GptQueryViewModel(get()) }
        viewModel { HighlightViewModel(get()) }
        viewModel { SavedPageViewModel(get()) }
    }

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@EinkBroApplication)
            modules(myModule)
        }

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

        if (config.uiLocaleLanguage.isNotEmpty()) {
            LocaleManager.setLocale(this, config.uiLocaleLanguage)
        }

        instance = this

        setupAdBlock()
    }

    private fun setupAdBlock() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        val filter = AdFilter.create(this)
        filter.setEnabled(config.adBlock)
        if (config.adBlock) {
            appScope.launch {
                filter.viewModel.workToFilterMap.collect { notifyDownloading(it.isEmpty()) }
            }
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        ttsManager.release()
    }


    private var isDownloading = false
    private val channelId = "DOWNLOAD"
    private val notificationId = 1

    private fun notifyDownloading(finished: Boolean) {
        if (isDownloading != finished) {// only accept valid event
            return
        }

        val clazz = if (finished) BrowserActivity::class.java else SettingActivity::class.java
        val intent = Intent(this, clazz).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(this, channelId).apply {
            setContentTitle("Adblock Filter Download")
            setContentIntent(pendingIntent)
            setDefaults(NotificationCompat.DEFAULT_ALL)
            setVibrate(longArrayOf(0L))
            setSound(null)
            priority = NotificationCompat.PRIORITY_HIGH
        }
        if (finished) {
            isDownloading = false
            builder.apply {
                //setContentText(getString(R.string.download_complete))
                setContentText("Download Complete")
                setSmallIcon(android.R.drawable.stat_sys_download_done)
                setProgress(0, 0, false)
                setOngoing(false)
            }
        } else {
            isDownloading = true
            builder.apply {
                //setContentText(getString(R.string.download_in_progress))
                setContentText("Download in Progress")
                setSmallIcon(android.R.drawable.stat_sys_download)
                setProgress(0, 0, true)
                setOngoing(true)// make the notification unable to be cleared
            }
        }
        NotificationManagerCompat.from(this).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val name: CharSequence = "Filter Download"
                val description: String = "Filter Download Description"
                val importance = NotificationManager.IMPORTANCE_HIGH
                val channel = NotificationChannel(channelId, name, importance)
                channel.setSound(null, null)
                channel.description = description

                // Add the channel
                createNotificationChannel(channel)
            }
            // Check if notification permission is granted
            if (ContextCompat.checkSelfPermission(
                    this@EinkBroApplication,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val notification = builder.build()
                cancel(notificationId)
                notify(notificationId, notification)
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ActivityCompat.requestPermissions(
                        (this@EinkBroApplication as BrowserActivity),
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        1
                    )
                }
            }
        }
    }

    // This snippet borrow from mihon
    // https://github.com/mihonapp/mihon/blob/81871a34694c8e408d907731292b7266c5b993cc/app/src/main/java/eu/kanade/tachiyomi/App.kt#L231
    override fun getPackageName(): String {
        try {
            // Override the value passed as X-Requested-With in WebView requests
            val stackTrace = Looper.getMainLooper().thread.stackTrace
            val isChromiumCall = stackTrace.any { trace ->
                trace.className.lowercase() in setOf("org.chromium.base.buildinfo", "org.chromium.base.apkinfo") &&
                        trace.methodName.lowercase() in setOf("getall", "getpackagename", "<init>")
            }

            if (isChromiumCall) return WebViewUtil.spoofedPackageName(applicationContext)
        } catch (_: Exception) {
        }

        return super.getPackageName()
    }

    companion object {
        lateinit var instance: EinkBroApplication
            private set
    }
}

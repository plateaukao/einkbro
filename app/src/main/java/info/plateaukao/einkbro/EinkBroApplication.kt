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
import info.plateaukao.einkbro.database.RecordDb
import info.plateaukao.einkbro.pocket.PocketNetwork
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.service.TtsManager
import info.plateaukao.einkbro.unit.LocaleManager
import io.github.edsuns.adfilter.AdFilter
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.startKoin
import org.koin.dsl.module
import timber.log.Timber

@Suppress("DEPRECATION")
class EinkBroApplication : Application() {

    private val sp: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(applicationContext)
    }

    private val config: ConfigManager by lazy {
        ConfigManager(applicationContext, sp)
    }

    private val ttsManager: TtsManager = TtsManager(this)

    private val myModule = module {
        single { config }
        single { sp }
        single { BookmarkManager(androidContext()) }
        single { RecordDb(androidContext()) }
        single { AdBlock(androidContext()) }
        single { Javascript(androidContext()) }
        single { Cookie(androidContext()) }
        single { ttsManager }
        single { PocketNetwork() }
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
//        Thread.setDefaultUncaughtExceptionHandler(
//            CustomExceptionHandler(Thread.getDefaultUncaughtExceptionHandler())
//        )
    }

    private fun setupAdBlock() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        val filter = AdFilter.create(this)
        filter.setEnabled(config.adBlock)
        val viewModel = filter.viewModel
        GlobalScope.launch {
            viewModel.workToFilterMap.collect { notifyDownloading(it.isEmpty()) }
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

    companion object {
        lateinit var instance: EinkBroApplication
            private set
    }
}

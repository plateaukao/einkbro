package info.plateaukao.einkbro

import android.app.Application
import android.content.SharedPreferences
import android.preference.PreferenceManager
import androidx.appcompat.app.AppCompatDelegate
import com.tencent.smtt.sdk.QbSdk
import com.tencent.smtt.sdk.QbSdk.PreInitCallback
import info.plateaukao.einkbro.browser.AdBlock
import info.plateaukao.einkbro.browser.AdBlockV2
import info.plateaukao.einkbro.browser.Cookie
import info.plateaukao.einkbro.browser.Javascript
import info.plateaukao.einkbro.database.BookmarkManager
import info.plateaukao.einkbro.database.RecordDb
import info.plateaukao.einkbro.pocket.PocketNetwork
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.service.TtsManager
import info.plateaukao.einkbro.unit.LocaleManager
import info.plateaukao.einkbro.util.CustomExceptionHandler
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.startKoin
import org.koin.dsl.module

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
        single { AdBlockV2(androidContext()) }
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

        initX5()

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

        if (config.uiLocaleLanguage.isNotEmpty()) {
            LocaleManager.setLocale(this, config.uiLocaleLanguage)
        }

        instance = this

        Thread.setDefaultUncaughtExceptionHandler(
            CustomExceptionHandler(Thread.getDefaultUncaughtExceptionHandler())
        )
    }

    override fun onTerminate() {
        super.onTerminate()
        ttsManager.release()
    }

    private fun initX5() {
        QbSdk.initX5Environment(this, object : PreInitCallback {
            override fun onCoreInitFinished() {
            }

            override fun onViewInitFinished(p0: Boolean) {
            }
        })
    }

    companion object {
        lateinit var instance: EinkBroApplication
            private set
    }
}

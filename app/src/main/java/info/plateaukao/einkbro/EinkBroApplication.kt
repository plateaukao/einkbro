package info.plateaukao.einkbro

import android.app.Application
import android.content.SharedPreferences
import android.preference.PreferenceManager
import androidx.appcompat.app.AppCompatDelegate
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
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.startKoin
import org.koin.dsl.module

class EinkBroApplication : Application() {

    private val sp: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(applicationContext)
    }

    private val config: ConfigManager by lazy {
        ConfigManager(applicationContext, sp)
    }

    private val ttsManager: TtsManager by lazy {
        TtsManager(applicationContext)
    }

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

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

        LocaleManager.setLocale(this, config.localeLanguage.languageCode)
    }

    override fun onTerminate() {
        super.onTerminate()
        ttsManager.release()
    }
}

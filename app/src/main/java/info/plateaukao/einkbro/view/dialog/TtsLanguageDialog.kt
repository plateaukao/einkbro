package info.plateaukao.einkbro.view.dialog

import android.content.Context
import androidx.appcompat.app.AlertDialog
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.preference.ConfigManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Locale

class TtsLanguageDialog(val context: Context) : KoinComponent {
    private val config: ConfigManager by inject()

    fun show(locales: List<Locale>) {
        val availableLocales = locales.sortedBy { it.displayName }
        val availableLocalDisplayNames = availableLocales.map { it.displayName }.toTypedArray()

        AlertDialog.Builder(context, R.style.TouchAreaDialog).apply {
            setTitle("Read in Which Language")
            setSingleChoiceItems(
                availableLocalDisplayNames, availableLocales.indexOf(config.ttsLocale)
            ) { dialog, selectedIndex ->
                val locale = availableLocales[selectedIndex]
                config.ttsLocale = locale
                dialog.dismiss()
            }
        }.create().show()
    }
}

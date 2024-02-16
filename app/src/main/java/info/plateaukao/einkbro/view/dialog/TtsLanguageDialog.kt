package info.plateaukao.einkbro.view.dialog

import android.content.Context
import androidx.appcompat.app.AlertDialog
import info.plateaukao.einkbro.R
import org.koin.core.component.KoinComponent
import java.util.Locale

class TtsLanguageDialog(val context: Context) : KoinComponent {

    fun show(locales: List<Locale>, action: (Locale) -> Unit) {
        val availableLocales = locales.sortedBy { it.displayName }
        val availableLocalDisplayNames = availableLocales.map { it.displayName }.toTypedArray()

        AlertDialog.Builder(context, R.style.TouchAreaDialog).apply {
            setTitle("Read in Which Language")
            setSingleChoiceItems(availableLocalDisplayNames, -1) { dialog, selectedIndex ->
                action(availableLocales[selectedIndex])
                dialog.dismiss()
            }
        }.create().show()
    }
}

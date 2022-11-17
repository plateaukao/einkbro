package info.plateaukao.einkbro.view.dialog

import android.content.Context
import androidx.appcompat.app.AlertDialog
import info.plateaukao.einkbro.R
import org.koin.core.component.KoinComponent
import java.util.Locale

class TtsLanguageDialog (val context: Context): KoinComponent {

    private val localeList = listOf(
        Locale.TRADITIONAL_CHINESE,
        Locale.SIMPLIFIED_CHINESE,
        Locale.JAPANESE,
        Locale.KOREAN,
        Locale.ENGLISH
    )

    fun show(action: (Locale) -> Unit) {
        val locales = localeList.map { it.displayName }.toTypedArray()

        AlertDialog.Builder(context, R.style.TouchAreaDialog).apply {
            setTitle("Read in Which Language")
            setSingleChoiceItems(locales, -1) { dialog, selectedIndex ->
                action(localeList[selectedIndex])
                dialog.dismiss()
            }
        }.create().show()
    }
}

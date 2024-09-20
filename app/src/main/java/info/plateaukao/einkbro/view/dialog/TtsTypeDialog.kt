package info.plateaukao.einkbro.view.dialog

import android.content.Context
import androidx.appcompat.app.AlertDialog
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.viewmodel.TtsType
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class TtsTypeDialog(val context: Context) : KoinComponent {
    private val config: ConfigManager by inject()

    fun show(action: (TtsType) -> Unit) {
        val types = TtsType.entries
        AlertDialog.Builder(context, R.style.TouchAreaDialog).apply {
            setTitle("Read by Which Engine")
            setSingleChoiceItems(
                types.map { it.name }.toTypedArray(),
                config.ttsType.ordinal
            ) { dialog, selectedIndex ->
                val newType = types[selectedIndex]
                config.ttsType = newType
                action(newType)
                dialog.dismiss()
            }
        }.create().show()
    }
}

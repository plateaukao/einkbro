package info.plateaukao.einkbro.view.dialog

import android.content.Context
import androidx.appcompat.app.AlertDialog
import icu.xmc.edgettslib.entity.VoiceItem
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.unit.HelperUnit
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ETtsVoiceDialog(val context: Context) : KoinComponent {
    private val config: ConfigManager by inject()
    private val voices: List<VoiceItem> = Json.decodeFromString(
        HelperUnit.getStringFromAsset("eVoiceList.json")
    )

    fun show(action: (VoiceItem) -> Unit) {
        AlertDialog.Builder(context, R.style.TouchAreaDialog).apply {
            setTitle("Read in Which Language")
            setSingleChoiceItems(
                voices.map { it.FriendlyName }.toTypedArray(),
                voices.indexOfFirst { it.Name == config.ettsVoice.Name }
            ) { dialog, selectedIndex ->
                val newVoice = voices[selectedIndex]
                config.ettsVoice = newVoice
                action(newVoice)
                dialog.dismiss()
            }
        }.create().show()
    }
}

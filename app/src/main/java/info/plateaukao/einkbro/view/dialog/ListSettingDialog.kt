package info.plateaukao.einkbro.view.dialog

import android.content.Context
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.unit.ViewUnit.dp
import org.koin.core.component.KoinComponent
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ListSettingWithNameDialog(
    private val context: Context,
    private val titleId: Int,
    private val names: List<String>,
    private val defaultValue: Int
) : KoinComponent {
    suspend fun show() = suspendCoroutine<Int?> { continuation ->

        AlertDialog.Builder(context, R.style.TouchAreaDialog).apply {
            setTitle(context.resources.getString(titleId))
            setSingleChoiceItems(
                names.toTypedArray(),
                defaultValue
            ) { dialog, selectedIndex ->
                dialog.dismiss()
                continuation.resume(selectedIndex)
            }
        }.create().also {
            it.show()
            it.window?.setLayout(300.dp(context), ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }
}

class ListSettingDialog(
    private val context: Context,
    private val titleId: Int,
    private val nameResIds: List<Int>,
    private val defaultValue: Int
) {
    suspend fun show(): Int? {
        val names = nameResIds.map { context.resources.getString(it) }
        return ListSettingWithNameDialog(context, titleId, names, defaultValue).show()
    }
}


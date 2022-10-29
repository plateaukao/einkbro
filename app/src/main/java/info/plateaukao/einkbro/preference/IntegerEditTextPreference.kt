package info.plateaukao.einkbro.preference

import android.content.Context
import android.util.AttributeSet
import androidx.preference.EditTextPreference

class IntegerEditTextPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : EditTextPreference(context, attrs, defStyle) {

    override fun getPersistedString(defaultReturnValue: String?): String {
        val defaultAsInt: Int = try {
            defaultReturnValue?.toInt() ?: 80
        } catch (e: NumberFormatException) {
            // No default is set
            80
        }
        val intValue: Int = getPersistedInt(defaultAsInt)
        return intValue.toString()
    }

    override fun persistString(value: String): Boolean {
        return try {
            persistInt(value.toInt())
        } catch (e: NumberFormatException) {
            false
        }
    }
}
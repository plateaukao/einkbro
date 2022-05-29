package de.baumann.browser.preference

import android.content.Context
import android.util.AttributeSet
import androidx.preference.EditTextPreference

class IntegerEditTextPreference : EditTextPreference {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

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
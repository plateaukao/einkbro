package de.baumann.browser.view

import android.content.Context
import android.widget.Toast

object NinjaToast {
    @JvmStatic
    fun show(context: Context?, stringResId: Int) {
        Toast.makeText(context, stringResId, Toast.LENGTH_LONG).show()
    }

    @JvmStatic
    fun show(context: Context?, text: String?) {
        Toast.makeText(context, text, Toast.LENGTH_LONG).show()
    }

    @JvmStatic
    fun showShort(context: Context?, text: String?) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
    }
}
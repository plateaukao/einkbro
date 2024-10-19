package info.plateaukao.einkbro.view

import android.content.Context
import android.widget.Toast

object EBToast {
    @JvmStatic
    fun show(context: Context?, stringResId: Int) {
        Toast.makeText(context, stringResId, Toast.LENGTH_SHORT).show()
    }

    @JvmStatic
    fun show(context: Context?, text: String?) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
    }

    @JvmStatic
    fun showShort(context: Context?, stringResId: Int) {
        Toast.makeText(context, stringResId, Toast.LENGTH_SHORT).show()
    }

    @JvmStatic
    fun showShort(context: Context?, text: String?) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
    }
}
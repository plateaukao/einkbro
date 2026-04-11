package info.plateaukao.einkbro.browser

import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.MotionEvent

class EBClickHandler(
    private val onLongPress: (Message, MotionEvent?) -> Unit,
) : Handler(Looper.getMainLooper()) {

    var currentMotionEvent: MotionEvent? = null

    override fun handleMessage(message: Message) {
        super.handleMessage(message)
        onLongPress(message, currentMotionEvent)
        currentMotionEvent?.recycle()
        currentMotionEvent = null
    }
}

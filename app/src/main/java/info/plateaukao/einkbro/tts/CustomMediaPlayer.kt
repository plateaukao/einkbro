package info.plateaukao.einkbro.tts

import android.media.MediaPlayer

class CustomMediaPlayer : MediaPlayer() {

    private var onResetListener: () -> Unit = {}

    fun setOnResetListener(listener: () -> Unit) {
        this.onResetListener = listener
    }

    override fun reset() {
        super.reset()
        onResetListener()
    }
}

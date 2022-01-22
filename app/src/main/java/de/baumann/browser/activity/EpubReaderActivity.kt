package de.baumann.browser.activity

import android.content.Intent
import de.baumann.browser.epub.EpubReaderView
import de.baumann.browser.view.NinjaWebView

class EpubReaderActivity: BrowserActivity() {


    override fun dispatchIntent(intent: Intent) {
        val epubLocation = intent.extras?.getString("epub_location")
        if (epubLocation != null) {
            addAlbum(url = "about:blank")
            runOnUiThread {
                with(ninjaWebView as EpubReaderView) {
                    OpenEpubFile(epubLocation)
                    GotoPosition(0, 0F)
                }
            }
        } else {
            super.dispatchIntent(intent)
        }
    }

    override fun createNinjaWebView(): NinjaWebView = EpubReaderView(this, this)
}
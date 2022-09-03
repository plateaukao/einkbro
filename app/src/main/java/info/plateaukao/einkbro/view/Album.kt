package info.plateaukao.einkbro.view

import android.content.Context
import android.graphics.Bitmap
import android.view.LayoutInflater
import androidx.core.graphics.drawable.toBitmap
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.databinding.AlbumBinding
import info.plateaukao.einkbro.browser.AlbumController
import info.plateaukao.einkbro.browser.BrowserController

class Album(
    private val albumController: AlbumController,
    private var browserController: BrowserController?
) {
    var isLoaded = false

    var albumTitle: String = ""

    var bitmap: Bitmap? = null

    var isActivated = false

    fun show() {
        browserController?.showAlbum(albumController)
    }

    fun remove() {
        browserController?.removeAlbum(albumController)
    }

    fun getUrl(): String = albumController.albumUrl

    fun setAlbumCover(bitmap: Bitmap?) {
        this.bitmap = bitmap
    }

    fun getAlbumBitmap(): Bitmap? = bitmap

    fun activate() {
        isActivated = true
    }

    fun deactivate() {
        isActivated = false
    }
}
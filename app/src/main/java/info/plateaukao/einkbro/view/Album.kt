package info.plateaukao.einkbro.view

import android.graphics.Bitmap
import info.plateaukao.einkbro.browser.AlbumController
import info.plateaukao.einkbro.browser.BrowserController

data class Album(
    private val albumController: AlbumController,
    private var browserController: BrowserController?
) {
    var isLoaded = false

    var albumTitle: String = ""

    var bitmap: Bitmap? = null

    var isActivated = false

    fun showOrJumpToTop() {
        val controller = browserController ?: return
        if (controller.isCurrentAlbum(albumController)) {
            if (controller.isAtTop()) {
                controller.refreshAction()
            } else {
                controller.jumpToTop()
            }
        } else {
            controller.showAlbum(albumController)
        }
    }

    fun remove() {
        browserController?.removeAlbum(albumController)
    }

    fun getUrl(): String = albumController.albumUrl

    fun setAlbumCover(bitmap: Bitmap?) {
        this.bitmap = bitmap
        browserController?.onUpdateAlbum(this)
    }

    fun getAlbumBitmap(): Bitmap? = bitmap

    fun activate() {
        isActivated = true
    }

    fun deactivate() {
        isActivated = false
    }
}
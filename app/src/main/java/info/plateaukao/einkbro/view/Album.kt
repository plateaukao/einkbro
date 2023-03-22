package info.plateaukao.einkbro.view

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import info.plateaukao.einkbro.browser.AlbumController
import info.plateaukao.einkbro.browser.BrowserController

data class Album(
    private val albumController: AlbumController,
    private var browserController: BrowserController?
) {
    var isLoaded = false

    var albumTitle: String = ""

    var bitmap: Bitmap? = null

    var stateOfBitmap by mutableStateOf(bitmap)

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
        stateOfBitmap = bitmap
    }

    fun activate() {
        isActivated = true
    }

    fun deactivate() {
        isActivated = false
    }
}
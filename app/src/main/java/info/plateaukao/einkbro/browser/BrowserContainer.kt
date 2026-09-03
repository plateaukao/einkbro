package info.plateaukao.einkbro.browser

import android.view.ViewGroup
import info.plateaukao.einkbro.view.EBWebView
import java.util.*

class BrowserContainer {
    private val list: MutableList<AlbumController> = LinkedList()
    operator fun get(index: Int): AlbumController {
        return list[index]
    }

    fun add(controller: AlbumController) = list.add(controller)

    fun add(controller: AlbumController, index: Int) = list.add(index, controller)

    fun remove(controller: AlbumController) {
        (controller as? EBWebView)?.let { destroyWebView(it) }
        list.remove(controller)
    }

    /** Swaps a lazily restored controller for its materialized WebView in place. */
    fun replace(old: AlbumController, new: AlbumController) {
        val index = list.indexOf(old)
        if (index >= 0) list[index] = new else list.add(new)
    }

    fun indexOf(controller: AlbumController?): Int = list.indexOf(controller)

    fun list(): List<AlbumController> = list

    fun size(): Int = list.size

    fun isEmpty(): Boolean = list.isEmpty()

    fun clear() {
        for (albumController in list) {
            (albumController as? EBWebView)?.let { destroyWebView(it) }
        }
        list.clear()
    }

    // WebView.destroy() requires the view to be detached first; leaving it in
    // the tree also kept every closed tab pinned by its parent.
    private fun destroyWebView(webView: EBWebView) {
        (webView.parent as? ViewGroup)?.removeView(webView)
        webView.destroy()
    }

    fun pauseAll() = list.forEach { it.pauseWebView() }

    fun resumeAll() = list.forEach { it.resumeWebView() }
}

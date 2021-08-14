package de.baumann.browser.browser

import kotlin.jvm.Synchronized
import de.baumann.browser.view.NinjaWebView
import java.util.*

class BrowserContainer {
    private val list: MutableList<AlbumController> = LinkedList()
    operator fun get(index: Int): AlbumController {
        return list[index]
    }

    @Synchronized
    fun add(controller: AlbumController) = list.add(controller)

    @Synchronized
    fun add(controller: AlbumController, index: Int) = list.add(index, controller)

    @Synchronized
    fun remove(controller: AlbumController) {
        (controller as NinjaWebView).destroy()
        list.remove(controller)
    }

    fun indexOf(controller: AlbumController?): Int = list.indexOf(controller)

    fun list(): List<AlbumController> = list

    fun size(): Int = list.size

    @Synchronized
    fun clear() {
        for (albumController in list) {
            (albumController as NinjaWebView).destroy()
        }
        list.clear()
    }
}
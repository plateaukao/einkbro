package info.plateaukao.einkbro.browser

import info.plateaukao.einkbro.view.Album

interface AlbumController {
    val album: Album
    var albumTitle: String
    fun activate()
    fun deactivate()
    val albumUrl: String
    fun pauseWebView()
    fun resumeWebView()
}
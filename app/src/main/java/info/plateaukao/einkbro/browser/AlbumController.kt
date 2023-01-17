package info.plateaukao.einkbro.browser

import info.plateaukao.einkbro.view.Album

interface AlbumController {
    val album: Album
    var albumTitle: String
    val albumUrl: String
    var initAlbumUrl: String
    fun activate()
    fun deactivate()

    fun pauseWebView()
    fun resumeWebView()
}
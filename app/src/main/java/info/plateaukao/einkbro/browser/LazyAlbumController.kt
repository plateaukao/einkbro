package info.plateaukao.einkbro.browser

import info.plateaukao.einkbro.view.Album

/**
 * A restored-but-never-opened tab. It shows its saved title in the tab list
 * but owns no WebView; TabManager creates the real EBWebView on first
 * activation and adopts this controller's [Album] so the tab keeps its
 * identity in the list. This keeps a cold start with N saved tabs from
 * constructing N Chromium WebViews before the first frame.
 */
class LazyAlbumController(
    title: String,
    url: String,
    albumCallback: AlbumCallback?,
) : AlbumController {
    override val album: Album = Album(this, albumCallback)

    override var albumTitle: String = title
        set(value) {
            field = value
            album.albumTitle = value
        }

    // No page is loaded yet; consumers fall back to initAlbumUrl.
    override val albumUrl: String = ""
    override var initAlbumUrl: String = url

    override var isTranslatePage: Boolean = false
    override var isAIPage: Boolean = false

    init {
        album.albumTitle = title
    }

    override fun activate() = album.activate()
    override fun deactivate() = album.deactivate()
    override fun pauseWebView() {}
    override fun resumeWebView() {}
}

package de.baumann.browser.epub

interface EpubReaderListener {
    fun onPageChangeListener(ChapterNumber: Int, PageNumber: Int, ProgressStart: Float, ProgressEnd: Float)
    fun onChapterChangeListener(ChapterNumber: Int)
    fun onTextSelectionModeChangeListner(mode: Boolean?)
    fun onLinkClicked(url: String?)
    fun onBookStartReached()
    fun onBookEndReached()
    fun onSingleTap()
}


package info.plateaukao.einkbro.epub

interface EpubReaderListener {
    fun onPageChangeListener(ChapterNumber: Int, PageNumber: Int, ProgressStart: Float, ProgressEnd: Float)
    fun onChapterChangeListener(ChapterNumber: Int)
    fun onTextSelectionModeChangeListener(mode: Boolean?)
    fun onLinkClicked(url: String?)
    fun onBookStartReached()
    fun onBookEndReached()
    fun onSingleTap()
}


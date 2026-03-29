package info.plateaukao.einkbro.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.lifecycleScope
import info.plateaukao.einkbro.epub.EpubReaderListener
import info.plateaukao.einkbro.epub.EpubReaderView
import info.plateaukao.einkbro.unit.BrowserUnit
import info.plateaukao.einkbro.view.EBToast
import info.plateaukao.einkbro.view.EBWebView
import info.plateaukao.einkbro.view.dialog.compose.TocDialogFragment
import info.plateaukao.einkbro.view.dialog.compose.TocItem
import kotlinx.coroutines.launch

class EpubReaderActivity: BrowserActivity() {
    override var shouldRunClearService: Boolean = false
    private lateinit var epubReader: EpubReaderView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initUI()
    }

    private fun initUI() {
        composeToolbarViewController.setEpubReaderMode()
        composeToolbarViewController.showTabbar(false)
        hideOverview()
    }

    override fun showTocDialog() {
        if (epubReader.isEinkBroEpub) {
            showEditableTocDialog()
        } else {
            epubReader.showTocDialog()
        }
    }

    private fun showEditableTocDialog() {
        val chapters = epubReader.epubChapters
        val tocItems = chapters.mapIndexed { index, chapter ->
            TocItem(title = chapter.title, originalIndex = index)
        }
        TocDialogFragment(
            chapters = tocItems,
            isEditable = true,
            onNavigate = { originalIndex -> epubReader.navigateToChapter(originalIndex) },
            onTocChanged = { newItems ->
                epubReader.applyTocChanges(newItems.map { it.originalIndex })
            },
        ).show(supportFragmentManager, "TocDialog")
    }
    override fun dispatchIntent(intent: Intent) {
        when (intent.action) {
            Intent.ACTION_VIEW -> {
                val epubUri = intent.data ?: return
                val shouldGotoLastChapter = intent.getBooleanExtra(ARG_TO_LAST_CHAPTER, false)

                addAlbum(
                    url = BrowserUnit.URL_ABOUT_BLANK,
                    enablePreloadWebView = false
                ) // so that it won't miss the preload webview
                lifecycleScope.launch {
                    try {
                        with(ebWebView as EpubReaderView) {
                            openEpubFile(epubUri)
                            if (shouldGotoLastChapter) {
                                gotoLastChapter()
                            } else {
                                gotoFirstChapter()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("EpubReaderActivity", "Failed to open epub", e)
                        // If permission denied, try re-opening via file picker
                        if (e is SecurityException || e.cause is SecurityException) {
                            EBToast.show(this@EpubReaderActivity, "Permission denied. Please select the file again.")
                            showOpenEpubFilePicker()
                        } else {
                            EBToast.show(this@EpubReaderActivity, "Failed to open epub: ${e.message}")
                        }
                    }
                }
            }
            ACTION_READ_ALOUD -> {
                readArticle()
            }
            else -> {
                super.dispatchIntent(intent)
            }
        }
    }

    override fun addHistory(title: String, url: String) {
        // don't need it, since it's not normal web page
    }

    override fun createebWebView(): EBWebView {
        epubReader = EpubReaderView(this, this)
        ebWebView = epubReader

        epubReader.setEpubReaderListener(object : EpubReaderListener {
            override fun onTextSelectionModeChangeListener(mode: Boolean?) {
                /*
                if (mode!!) {
                    bottom_contextual_bar.setVisibility(View.VISIBLE)
                } else {
                    bottom_contextual_bar.setVisibility(View.GONE)
                }
                 */
            }

            override fun onPageChangeListener(ChapterNumber: Int, PageNumber: Int, ProgressStart: Float, ProgressEnd: Float) {
                Log.d("EpubReader", "PageChange: Chapter:$ChapterNumber PageNumber:$PageNumber")
            }

            override fun onChapterChangeListener(ChapterNumber: Int) {
                Log.d("EpubReader", "ChapterChange$ChapterNumber ")
            }

            override fun onLinkClicked(url: String?) {
                Log.d("EpubReader", "LinkClicked:$url ")
            }

            override fun onBookStartReached() {
                //Use this method to go to previous book
                //When user slides previous when opened the first page of the book
                Log.d("EpubReader", "StartReached")
            }

            override fun onBookEndReached() {
                //Use this method to go to next book
                //When user slides next when opened the last page of the book
                Log.d("EpubReader", "EndReached")
            }

            override fun onSingleTap() {
                Log.d("EpubReader", "PageTapped")
            }
        })

        return epubReader
    }

    companion object {
        const val ARG_TO_LAST_CHAPTER = "arg_to_last_chapter"
    }
}
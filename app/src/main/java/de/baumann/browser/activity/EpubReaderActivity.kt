package de.baumann.browser.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View.GONE
import androidx.lifecycle.lifecycleScope
import de.baumann.browser.Ninja.R
import de.baumann.browser.epub.EpubReaderListener
import de.baumann.browser.epub.EpubReaderView
import de.baumann.browser.unit.BrowserUnit
import de.baumann.browser.view.NinjaWebView
import kotlinx.coroutines.launch

class EpubReaderActivity: BrowserActivity() {
    private lateinit var epubReader: EpubReaderView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initUI()
    }

    private fun initUI() {
        with (binding.omniboxTabcount) {
            text = ""
            setBackgroundResource(R.drawable.ic_toc)
            setOnClickListener { epubReader.showTocDialog() }
        }
        binding.omniboxBack.visibility = GONE
        binding.toolbarInputUrl.visibility = GONE
        binding.omniboxInput.onItemClickListener = null
    }

    override fun dispatchIntent(intent: Intent) {
        val epubUri= intent.data ?: return
        addAlbum(url = BrowserUnit.URL_ABOUT_BLANK, enablePreloadWebView = false) // so that it won't miss the preload webview
        lifecycleScope.launch {
            with(ninjaWebView as EpubReaderView) {
                openEpubFile(epubUri)
                GotoPosition(0, 0F)
            }
        }
    }

    override fun addHistory(url: String) {
        // don't need it, since it's not normal web page
    }

    override fun createNinjaWebView(): NinjaWebView {
        epubReader = EpubReaderView(this, this)
        ninjaWebView = epubReader

        epubReader.setEpubReaderListener(object : EpubReaderListener {
            override fun onTextSelectionModeChangeListner(mode: Boolean?) {
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
}
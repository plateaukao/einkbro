package de.baumann.browser.view

import android.content.Context
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnLongClickListener
import de.baumann.browser.Ninja.R
import de.baumann.browser.Ninja.databinding.AlbumBinding
import de.baumann.browser.browser.AlbumController
import de.baumann.browser.browser.BrowserController

internal class Album(
    context: Context,
    private val albumController: AlbumController,
    private var browserController: BrowserController?
) {
    private val binding: AlbumBinding = AlbumBinding.inflate(LayoutInflater.from(context))

    val albumView: View
        get() = binding.root
    var albumTitle: String
        get() = binding.albumTitle.text.toString()
        set(value) {
            binding.albumTitle.text = value
        }

    init {
        binding.root.setOnClickListener {
            browserController?.showAlbum(albumController)
            browserController?.hideOverview()
        }
        binding.root.setOnLongClickListener(OnLongClickListener {
            browserController?.removeAlbum(albumController)
            true
        })

        binding.albumTitle.text = context.getString(R.string.app_name)
        binding.albumClose.setOnClickListener {
            browserController?.removeAlbum(
                albumController
            )
        }
    }

    fun setAlbumCover(bitmap: Bitmap?) = binding.albumCover.setImageBitmap(bitmap)

    fun setBrowserController(browserController: BrowserController?) {
        this.browserController = browserController
    }

    fun activate() { binding.root.isSelected = true }

    fun deactivate() { binding.root.isSelected = false }
}
package de.baumann.browser.view

import android.content.Context
import android.graphics.Bitmap
import android.view.LayoutInflater
import androidx.core.graphics.drawable.toBitmap
import de.baumann.browser.Ninja.R
import de.baumann.browser.Ninja.databinding.AlbumBinding
import de.baumann.browser.browser.AlbumController
import de.baumann.browser.browser.BrowserController

class Album(
    context: Context,
    private val albumController: AlbumController,
    private var browserController: BrowserController?
) {
    private val binding: AlbumBinding = AlbumBinding.inflate(LayoutInflater.from(context))

    var isLoaded = false

    var albumTitle: String
        get() = binding.albumTitle.text.toString()
        set(value) {
            binding.albumTitle.text = value
        }

    init {
        binding.root.setOnClickListener {
        }
        binding.root.setOnLongClickListener {
            true
        }

        binding.albumTitle.text = context.getString(R.string.app_name)
        binding.albumClose.setOnClickListener {
            browserController?.removeAlbum(
                albumController
            )
        }
    }

    fun show() {
        browserController?.showAlbum(albumController)
    }

    fun remove() {
        browserController?.removeAlbum(albumController)
    }

    fun getUrl(): String = albumController.albumUrl

    fun setAlbumCover(bitmap: Bitmap?) =
        binding.albumCover.setImageBitmap(bitmap)

    fun getAlbumBitmap(): Bitmap? = binding.albumCover.drawable?.toBitmap()

    fun isActivated() = binding.root.isSelected

    fun activate() { binding.root.isSelected = true }

    fun deactivate() { binding.root.isSelected = false }
}
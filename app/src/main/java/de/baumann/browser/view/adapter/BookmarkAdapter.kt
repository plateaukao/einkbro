package de.baumann.browser.view.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import de.baumann.browser.Ninja.R
import de.baumann.browser.database.Bookmark
import de.baumann.browser.database.BookmarkManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class BookmarkAdapter(
    private val onItemClick: (Bookmark) -> Unit,
    private val onTabIconClick: (Bookmark) -> Unit,
    private val onItemLongClick: (Bookmark) -> Unit,
) : ListAdapter<Bookmark, BookmarkAdapter.BookmarkViewHolder>(DiffCallback), KoinComponent {
    private val bookmarkManager: BookmarkManager by inject()

    class BookmarkViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.record_item_title)
        val tabView: ImageView = view.findViewById(R.id.icon_tab)
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): BookmarkViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.list_item_bookmark, viewGroup, false)

        return BookmarkViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: BookmarkViewHolder, position: Int) {
        val bookmark = getItem(viewHolder.adapterPosition)
        viewHolder.textView.text = bookmark.title

        setupIcon(viewHolder, bookmark)
        setupItem(viewHolder, bookmark)
    }

    private fun setupItem(viewHolder: BookmarkViewHolder, bookmark: Bookmark) {
        with(viewHolder.itemView) {
            setOnClickListener { onItemClick(bookmark) }
            setOnLongClickListener {
                onItemLongClick(bookmark)
                true
            }
        }
    }

    private fun setupIcon(viewHolder: BookmarkViewHolder, bookmark: Bookmark) {
        if (bookmark.isDirectory) {
            viewHolder.tabView.setOnClickListener { onItemClick(bookmark) }
            viewHolder.tabView.setImageResource(R.drawable.ic_folder)
        } else {
            viewHolder.tabView.setOnClickListener { onTabIconClick(bookmark) }
            viewHolder.tabView.setImageResource(R.drawable.icon_plus)

            bookmarkManager.findFaviconBy(bookmark.url)?.getBitmap()?.let {
                viewHolder.tabView.setImageBitmap(it)
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<Bookmark>() {
            override fun areItemsTheSame(oldItem: Bookmark, newItem: Bookmark): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Bookmark, newItem: Bookmark): Boolean {
                return oldItem == newItem
            }
        }
    }
}
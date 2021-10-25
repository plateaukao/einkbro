package de.baumann.browser.view.adapter

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

class BookmarkAdapter(
    private val onItemClick: (Bookmark) -> Unit,
    private val onTabIconClick: (Bookmark) -> Unit,
    private val onItemLongClick: (Bookmark) -> Unit,
) : ListAdapter<Bookmark, BookmarkAdapter.BookmarkViewHolder>(DiffCallback) {

    class BookmarkViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.record_item_title)
        val iconView : ImageView = view.findViewById(R.id.ib_icon)
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
        if (bookmark.isDirectory) {
            viewHolder.iconView.setImageResource(R.drawable.ic_folder)
            viewHolder.tabView.visibility = View.INVISIBLE
        } else {
            viewHolder.iconView.setImageResource(R.drawable.circle_red_big)
            viewHolder.tabView.visibility = View.VISIBLE
        }

        with(viewHolder.itemView) {
            setOnClickListener { onItemClick(bookmark) }
            setOnLongClickListener {
                onItemLongClick(bookmark)
                true
            }
        }
        viewHolder.tabView.setOnClickListener {
            onTabIconClick(bookmark)
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
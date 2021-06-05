package de.baumann.browser.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import de.baumann.browser.Ninja.R
import de.baumann.browser.database.Bookmark

class BookmarkAdapter(
    private val data: MutableList<Bookmark>,
    private val onItemClick: (Bookmark) -> Unit,
    private val onTabIconClick: (Bookmark) -> Unit,
    private val onItemLongClick: (Bookmark) -> Unit,
) : RecyclerView.Adapter<BookmarkAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.record_item_title)
        val iconView : ImageView = view.findViewById(R.id.ib_icon)
        val tabView: ImageView = view.findViewById(R.id.icon_tab)
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.list_item_bookmark, viewGroup, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val bookmark = data[position]
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

    override fun getItemCount() = data.size

    fun remove(bookmark: Bookmark) {
        data.remove(bookmark)
        notifyDataSetChanged()
    }
}
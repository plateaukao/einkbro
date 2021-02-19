package de.baumann.browser.view

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import de.baumann.browser.Ninja.R
import de.baumann.browser.unit.BrowserUnit

class GridAdapter(private val context: Context, private val list: List<GridItem>) : BaseAdapter() {
    private class Holder(val title: TextView, val cover: ImageView)

    override fun getView(position: Int, convertView: View, parent: ViewGroup): View {
        val holder: Holder
        var view = convertView
        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.grid_item, parent, false)
            holder = Holder(
                    title = view.findViewById(R.id.grid_item_title),
                    cover = view.findViewById(R.id.grid_item_cover)
            )
            view.tag = holder
        } else {
            holder = view.tag as Holder
        }
        val item = list[position]
        holder.title.text = item.title
        holder.cover.setImageBitmap(BrowserUnit.file2Bitmap(context, item.filename))
        return view
    }

    override fun getCount(): Int = list.size

    override fun getItem(index: Int): Any = list[index]

    override fun getItemId(index: Int): Long = index.toLong()
}
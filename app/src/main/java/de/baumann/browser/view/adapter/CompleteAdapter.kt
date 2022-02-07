package de.baumann.browser.view.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import de.baumann.browser.Ninja.R
import de.baumann.browser.database.Record
import de.baumann.browser.database.RecordType
import java.util.*

class CompleteAdapter(
    private val context: Context,
    private val layoutResId: Int,
    recordList: List<Record>,
    private val clickAction: (Record) -> Unit
) : BaseAdapter(), Filterable {
    private inner class CompleteFilter : Filter() {
        override fun performFiltering(prefix: CharSequence?): FilterResults {
            if (prefix != null) {
                resultList.clear()
                originalList.filter { item ->
                    item.title?.contains(
                            prefix,
                            ignoreCase = true
                    ) == true || item.url.contains(prefix, ignoreCase = true)
                }.forEach { item -> resultList.add(item) }
            }

            return FilterResults().apply {
                values = resultList
                count = resultList.size
            }
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults) {
            notifyDataSetChanged()
        }
    }

    private class Holder(
        val titleView: TextView,
        val urlView: TextView,
        val icon: ImageButton
    )

    private val originalList: MutableList<Record> = ArrayList()
    private val resultList: MutableList<Record> = ArrayList()
    private val filter = CompleteFilter()
    private fun deDuplicate(recordList: List<Record>) {
        for (record in recordList) {
            if (record.title?.isNotEmpty() == true && record.url.isNotEmpty()) {
                originalList.add(record)
            }
        }
    }

    override fun getCount(): Int = resultList.size

    override fun getFilter(): Filter = filter

    override fun getItem(position: Int): Any = resultList[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var view: View = convertView ?: LayoutInflater.from(context).inflate(layoutResId, null, false)
        val holder: Holder
        if (convertView == null) {
            holder = Holder(
                view.findViewById(R.id.complete_item_title),
                view.findViewById(R.id.complete_item_url),
                view.findViewById(R.id.ib_icon)
            )
            view.tag = holder
        } else {
            holder = view.tag as Holder
        }
        // sometimes the resultList will be refreshed, so the index will be wrong => ignore it by putting dummy Record
        val (title, url, _, type) = resultList.getOrElse(position) { Record(null, "", 0) }
        with (holder) {
            titleView.text = title
            urlView.text = url
            val resource = if (type === RecordType.Bookmark) R.drawable.icon_bookmark else R.drawable.icon_earth
            icon.setImageResource(resource)
            view.setOnClickListener { clickAction.invoke(resultList[position]) }
        }

        return view
    }

    init {
        deDuplicate(recordList)
    }
}
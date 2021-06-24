package de.baumann.browser.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import de.baumann.browser.Ninja.R
import de.baumann.browser.database.Record
import de.baumann.browser.database.RecordType
import java.text.SimpleDateFormat
import java.util.*

class RecordAdapter(
        private val records: MutableList<Record>,
        private val onItemClick: OnItemClick,
        private val onItemLongClick: OnItemLongClick,
) : RecyclerView.Adapter<RecordHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordHolder =
        RecordHolder(parent.inflate(R.layout.list_item, false))

    override fun onBindViewHolder(holder: RecordHolder, position: Int) =
        with (holder) {
            bindRecord(records[position])
            clickAction = { onItemClick(position) }
            longClickAction = { onItemLongClick(position) }
        }

    override fun getItemCount(): Int = records.size

    fun removeAt(position: Int) {
        records.removeAt(position)
        notifyDataSetChanged()
    }

    fun clear() {
        records.clear()
        notifyDataSetChanged()
    }

    fun getItemAt(position: Int): Record = records[position]
}

class RecordHolder(v: View) : RecyclerView.ViewHolder(v), View.OnClickListener, View.OnLongClickListener {
    private val title: TextView = v.findViewById(R.id.record_item_title)
    private val time: TextView = v.findViewById(R.id.record_item_time)
    private val icon: ImageButton = v.findViewById(R.id.ib_icon)

    var clickAction: (() -> Unit)? = null
    var longClickAction: (() -> Unit)? = null

    var record: Record? = null

    init {
        v.setOnClickListener(this)
        v.setOnLongClickListener(this)
    }

    override fun onClick(v: View) = clickAction?.invoke() ?: Unit

    override fun onLongClick(v: View?): Boolean {
        longClickAction?.invoke()
        return true
    }

    fun bindRecord(record: Record) {
        this.record = record
        title.text = record.title
        time.text = SimpleDateFormat("MMM dd", Locale.getDefault()).format(record.time)
        icon.setImageResource(when (record.type) {
            RecordType.Bookmark -> R.drawable.ic_bookmark
            RecordType.History -> R.drawable.icon_earth
        })
    }
}

fun ViewGroup.inflate(@LayoutRes layoutRes: Int, attachToRoot: Boolean = false): View =
    LayoutInflater.from(context).inflate(layoutRes, this, attachToRoot)

typealias OnItemClick = (position: Int) -> Unit
typealias OnItemLongClick = (position: Int) -> Unit

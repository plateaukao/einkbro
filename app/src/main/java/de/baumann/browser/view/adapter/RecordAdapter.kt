package de.baumann.browser.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import de.baumann.browser.Ninja.R
import de.baumann.browser.database.Record
import java.text.SimpleDateFormat
import java.util.*

class RecordAdapter(
        private val records: List<Record>,
        private val onItemClick: OnItemClick,
        private val onItemLongClick: OnItemClick,
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
}

class RecordHolder(
        v: View,
) : RecyclerView.ViewHolder(v), View.OnClickListener, View.OnLongClickListener {
    private var view: View = v
    private val title: TextView = v.findViewById(R.id.record_item_title)
    private val time: TextView = v.findViewById(R.id.record_item_time)

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
    }

    companion object {
        private val RECORD_KEY = "RECORD"
    }
}

fun ViewGroup.inflate(@LayoutRes layoutRes: Int, attachToRoot: Boolean = false): View {
    return LayoutInflater.from(context).inflate(layoutRes, this, attachToRoot)
}

typealias OnItemClick = (position: Int) -> Unit
typealias OnItemLongClick = (position: Int) -> Unit

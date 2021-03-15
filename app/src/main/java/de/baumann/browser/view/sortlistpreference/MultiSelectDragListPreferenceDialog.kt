package de.baumann.browser.view.sortlistpreference

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.preference.PreferenceDialogFragmentCompat
import de.baumann.browser.Ninja.R
import de.baumann.browser.view.sortlistpreference.DragSortListView.DropListener
import java.util.*

class MultiSelectDragListPreferenceDialog(
        private val preference: MultiSelectDragListPreference,
        private val shouldFinishActivityAfterDismissed: Boolean
) : PreferenceDialogFragmentCompat() {
    private lateinit var listView: DragSortListView
    private val selectedItems: BooleanArray
        private get() {
            val entries = preference.entryValues
            val entryCount = entries.size
            val values = preference.getValues()
            val result = BooleanArray(entryCount)
            for (i in 0 until entryCount) {
                result[i] = values.contains(entries[i].toString())
            }
            return result
        }
    private lateinit var iconListAdapter: ArrayAdapter<PreferenceItemInfo>
    private var mPreferenceChanged = false
    private val onDrop = DropListener { from, to ->
        if (from != to) {
            val item = iconListAdapter.getItem(from)
            iconListAdapter.remove(item)
            iconListAdapter.insert(item, to)
            listView.moveCheckState(from, to)
            mPreferenceChanged = true
            refreshNewValues()
        }
    }

    init {
        val b = Bundle()
        b.putString(ARG_KEY, preference.key)
        arguments = b
    }

    private fun refreshNewValues() {
        preference.newValues.clear()
        val n = iconListAdapter.count
        val checkedPositions = listView.checkedItemPositions
        for (i in 0 until n) {
            if (checkedPositions[i]) {
                preference.newValues.add(preference.entryValues[listOf(*preference.entries).indexOf(iconListAdapter.getItem(i))] as String)
            }
        }
    }

    override fun onPrepareDialogBuilder(builder: AlertDialog.Builder) {
        super.onPrepareDialogBuilder(builder)
        val mEntries = preference.getEntries()
        val mEntryValues = preference.entryValues
        val itemInfos = mEntries.mapIndexed { index, entry ->
            PreferenceItemInfo(
                    entry.toString(),
                    preference.entryIconIds[index]?.toString()?.toInt() ?: 0
            )
        }

        check(!(mEntries == null || mEntryValues == null)) { "MultiSelectListPreference requires an entries array and " + "an entryValues array." }

        val entries = arrayOfNulls<String>(mEntries.size)
        for (i in mEntries.indices) {
            entries[i] = mEntries[i].toString()
        }

        val selectedItems = selectedItems
        val orderedList = ArrayList<PreferenceItemInfo>()
        val n = selectedItems.size
        for (value in preference.values) {
            val index = listOf(*mEntryValues).indexOf(value)
            orderedList.add(itemInfos[index])
        }

        for (i in mEntries.indices) {
            if (!preference.values.contains(mEntryValues[i])) orderedList.add(itemInfos[i])
        }

        iconListAdapter = createItemAdapter(orderedList)
        listView = DragSortListView(context, null).apply {
            adapter = iconListAdapter
            setDropListener(onDrop)
            isDragEnabled = true
            floatAlpha = 0.8f
            choiceMode = ListView.CHOICE_MODE_MULTIPLE

            for (i in 0 until n) {
                setItemChecked(i, i < preference.values.size)
            }

            onItemClickListener = OnItemClickListener { _, _, _, _ ->
                mPreferenceChanged = true
                refreshNewValues()
            }

            builder.setView(this)
        }

        val controller = DragSortController(listView).apply {
            setDragHandleId(R.id.drag_handle)
            isRemoveEnabled = false
            isSortEnabled = true
            setBackgroundColor(0xFFFFFF)
            dragInitMode = DragSortController.ON_DOWN
        }

        listView.setFloatViewManager(controller)
        listView.setOnTouchListener(controller)


        preference.newValues.clear()
        preference.newValues.addAll(preference.values)
    }

    private fun createItemAdapter(itemList: List<PreferenceItemInfo>): ArrayAdapter<PreferenceItemInfo> {
        return object: ArrayAdapter<PreferenceItemInfo>(requireContext(), R.layout.item_list_preference_multi_drag, R.id.text, itemList) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val item = getItem(position)
                val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_list_preference_multi_drag, parent, false) as CheckableMultiSelectLayout
                val imageView = view.findViewById<ImageView>(R.id.icon)
                val iconResId = item?.iconResId ?: 0
                if (iconResId != 0) {
                    imageView.setImageResource(iconResId)
                }
                view.findViewById<TextView>(R.id.text).text = item?.title ?: ""
                return view
            }
        }
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult && mPreferenceChanged) {
            refreshNewValues()
            val values = preference.newValues
            if (preference.callChangeListener(values) && values.size > 0) {
                preference.setValues(values)
            }
        }
        mPreferenceChanged = false

        if (shouldFinishActivityAfterDismissed) {
            activity?.finish()
        }
    }
}

private class PreferenceItemInfo(val title: String, val iconResId: Int)
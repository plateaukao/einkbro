package de.baumann.browser.view.dialog

import android.app.AlertDialog
import android.content.*
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import de.baumann.browser.Ninja.R
import de.baumann.browser.Ninja.databinding.DialogToolbarConfigBinding
import de.baumann.browser.preference.ConfigManager
import de.baumann.browser.unit.ViewUnit
import de.baumann.browser.view.sortlistpreference.CheckableMultiSelectLayout
import de.baumann.browser.view.sortlistpreference.DragSortController
import de.baumann.browser.view.sortlistpreference.DragSortListView
import de.baumann.browser.view.toolbaricons.ToolbarAction
import java.util.ArrayList

class ToolbarConfigDialog(
    private val context: Context,
) {
    private val config: ConfigManager = ConfigManager(context)

    private lateinit var dialog: AlertDialog
    private val binding: DialogToolbarConfigBinding = DialogToolbarConfigBinding.inflate(LayoutInflater.from(context))

    fun show() {
        val builder = AlertDialog.Builder(context, R.style.TouchAreaDialog).apply { setView(binding.root) }

        initViews()
        dialog = builder.create().apply {
            window?.setGravity(Gravity.BOTTOM or Gravity.RIGHT)
            window?.setBackgroundDrawableResource(R.drawable.background_with_margin)
        }
        dialog.show()
        dialog.window?.setLayout(ViewUnit.dpToPixel(context, 300).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun initViews() {
        binding.actionOk.setOnClickListener {
            config.toolbarActions = newValues
            dialog.dismiss()
        }
        binding.actionCancel.setOnClickListener {
            dialog.dismiss()
        }

        val orderedList = ArrayList<PreferenceItemInfo>()
        // put selected
        for (toolbarAction in config.toolbarActions) {
            orderedList.add(toolbarAction.toPreferenceItemInfo())
        }

        // put unselected
        for (toolbarAction in ToolbarAction.values()) {
            // translation only supports Onyx devices
            if (toolbarAction == ToolbarAction.Translation && Build.MANUFACTURER != "ONYX") {
                continue
            }

            if (!config.toolbarActions.contains(toolbarAction)) {
                orderedList.add((toolbarAction.toPreferenceItemInfo()))
            }
        }

        iconListAdapter = createItemAdapter(orderedList)
        listView = binding.dragSortListView.apply {
            adapter = iconListAdapter
            setDropListener(onDrop)
            isDragEnabled = true
            floatAlpha = 0.8f
            choiceMode = ListView.CHOICE_MODE_MULTIPLE

            for (i in config.toolbarActions.indices) {
                setItemChecked(i, true)
            }

            onItemClickListener = AdapterView.OnItemClickListener { _, _, _, _ ->
                refreshNewValues()
            }
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


        newValues.clear()
        newValues.addAll(config.toolbarActions)
    }

    private lateinit var listView: DragSortListView
    private var newValues: MutableList<ToolbarAction> = mutableListOf()

    private lateinit var iconListAdapter: ArrayAdapter<PreferenceItemInfo>
    private val onDrop = DragSortListView.DropListener { from, to ->
        if (from != to) {
            val item = iconListAdapter.getItem(from)
            iconListAdapter.remove(item)
            iconListAdapter.insert(item, to)
            listView.moveCheckState(from, to)
            refreshNewValues()
        }
    }

    private fun refreshNewValues() {
        newValues.clear()
        val n = iconListAdapter.count
        val checkedPositions = listView.checkedItemPositions
        for (i in 0 until n) {
            if (checkedPositions[i]) {
                val preferenceInfo = iconListAdapter.getItem(i) ?: continue
                newValues.add(ToolbarAction.fromOrdinal(preferenceInfo.ordinal))
            }
        }
    }

    private fun createItemAdapter(itemList: List<PreferenceItemInfo>): ArrayAdapter<PreferenceItemInfo> {
        return object: ArrayAdapter<PreferenceItemInfo>(context, R.layout.item_list_preference_multi_drag, R.id.text, itemList) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val item = getItem(position)
                val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_list_preference_multi_drag, parent, false) as CheckableMultiSelectLayout
                val imageView = view.findViewById<ImageView>(R.id.icon)
                val iconResId = item?.iconResId ?: 0
                if (iconResId != 0) {
                    imageView.setImageResource(iconResId)
                } else {
                    imageView.setImageBitmap(null)
                }
                view.findViewById<TextView>(R.id.text).text = item?.title ?: ""
                return view
            }
        }
    }
}

private class PreferenceItemInfo(val ordinal: Int, val title: String, val iconResId: Int)

private fun ToolbarAction.toPreferenceItemInfo(): PreferenceItemInfo {
    return PreferenceItemInfo(this.ordinal, this.title, this.iconResId)
}
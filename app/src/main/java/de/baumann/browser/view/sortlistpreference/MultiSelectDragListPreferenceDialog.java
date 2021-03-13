package de.baumann.browser.view.sortlistpreference;

import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.preference.PreferenceDialogFragmentCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.baumann.browser.Ninja.R;

public class MultiSelectDragListPreferenceDialog extends PreferenceDialogFragmentCompat {
    private final MultiSelectDragListPreference preference;

    private DragSortListView listView;

    public MultiSelectDragListPreferenceDialog(MultiSelectDragListPreference preference) {
        this.preference = preference;

        final Bundle b = new Bundle();
        b.putString(ARG_KEY, preference.getKey());
        setArguments(b);
    }


    private boolean[] getSelectedItems() {
        final CharSequence[] entries = preference.entryValues;
        final int entryCount = entries.length;
        final List<String> values = preference.getValues();
        boolean[] result = new boolean[entryCount];

        for (int i = 0; i < entryCount; i++) {
            result[i] = values.contains(entries[i].toString());
        }

        return result;
    }

    private ArrayAdapter<String> adapter;
    private boolean mPreferenceChanged;
    private DragSortListView.DropListener onDrop = new DragSortListView.DropListener() {
        @Override
        public void drop(int from, int to) {
            if (from != to) {
                String item = adapter.getItem(from);
                adapter.remove(item);
                adapter.insert(item, to);
                listView.moveCheckState(from, to);
                mPreferenceChanged = true;
                refreshNewValues();
            }
        }
    };

    private void refreshNewValues() {
        preference.newValues.clear();
        int n = adapter.getCount();
        SparseBooleanArray checkedPositions = listView.getCheckedItemPositions();
        for (int i = 0; i < n; i++) {
            if (checkedPositions.get(i) == true) {
                preference.newValues.add((String) preference.entryValues[Arrays.asList(preference.entries).indexOf(adapter.getItem(i))]);
            }
        }
    }


    @Override
    protected void onPrepareDialogBuilder(androidx.appcompat.app.AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);

        CharSequence[] mEntries = preference.getEntries();
        CharSequence[] mEntryValues = preference.entryValues;

        if (mEntries == null || mEntryValues == null) {
            throw new IllegalStateException("MultiSelectListPreference requires an entries array and " + "an entryValues array.");
        }

        String[] entries = new String[mEntries.length];
        for (int i = 0; i < mEntries.length; i++) {
            entries[i] = mEntries[i].toString();
        }

        boolean[] selectedItems = getSelectedItems();

        ArrayList<String> orderedList = new ArrayList<>();
        int n = selectedItems.length;

        for (String value : preference.values) {
            int index = Arrays.asList(mEntryValues).indexOf(value);
            orderedList.add(mEntries[index].toString());
        }

        for (int i = 0; i < mEntries.length; i++) {
            if (!preference.values.contains(mEntryValues[i])) orderedList.add(mEntries[i].toString());
        }

        adapter = new ArrayAdapter<>(getContext(), R.layout.item_list_preference_multi_drag, R.id.text, orderedList);
        listView = new DragSortListView(getContext(), null);
        listView.setAdapter(adapter);

        listView.setDropListener(onDrop);
        listView.setDragEnabled(true);
        listView.setFloatAlpha(0.8f);

        DragSortController controller = new DragSortController(listView);
        controller.setDragHandleId(R.id.drag_handle);
        controller.setRemoveEnabled(false);
        controller.setSortEnabled(true);
        controller.setBackgroundColor(0xFFFFFF);
        controller.setDragInitMode(DragSortController.ON_DOWN);

        listView.setFloatViewManager(controller);
        listView.setOnTouchListener(controller);


        builder.setView(listView);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                mPreferenceChanged = true;
                refreshNewValues();
            }
        });

        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        for (int i = 0; i < n; i++) {
            listView.setItemChecked(i, i < preference.values.size());
        }

        /*
         * boolean [] checkedItems = getSelectedItems();
         * builder.setMultiChoiceItems(mEntries, checkedItems,
         * new DialogInterface.OnMultiChoiceClickListener() {
         * public void onClick(DialogInterface dialog, int which, boolean
         * isChecked) {
         * if (isChecked) {
         * mPreferenceChanged |= mNewValues.add(mEntryValues[which].toString());
         * } else {
         * mPreferenceChanged |=
         * mNewValues.remove(mEntryValues[which].toString());
         * }
         * }
         * });
         */
        preference.newValues.clear();
        preference.newValues.addAll(preference.values);
    }

        @Override
        public void onDialogClosed ( boolean positiveResult)
        {
            if (positiveResult && mPreferenceChanged) {
                refreshNewValues();
                final List<String> values = preference.newValues;
                if (preference.callChangeListener(values) && values.size() > 0) {
                    preference.setValues(values);
                }
            }
            mPreferenceChanged = false;
        }
}

package org.askerov.dynamicgrid;

import android.widget.BaseAdapter;

import java.util.HashMap;
import java.util.List;

/*
 * Author: alex askerov
 * Date: 9/6/13
 * Time: 7:43 PM
 */


/**
 * Abstract adapter for {@link org.askerov.dynamicgrid.DynamicGridView} with sable items id;
 */

@SuppressWarnings("WeakerAccess")
public abstract class AbstractDynamicGridAdapter extends BaseAdapter implements DynamicGridAdapterInterface {
    public static final int INVALID_ID = -1;

    private int nextStableId = 0;
    private final HashMap<Object, Integer> mIdMap = new HashMap<>();

    @Override
    public final boolean hasStableIds() {
        return true;
    }

    protected void addStableId(Object item) {
        mIdMap.put(item, nextStableId++);
    }

    protected void addAllStableId(List<?> items) {
        for (Object item : items) {
            addStableId(item);
        }
    }

    @Override
    public final long getItemId(int position) {
        if (position < 0 || position >= mIdMap.size()) {
            return INVALID_ID;
        }
        Object item = getItem(position);
        return mIdMap.get(item);
    }

    /**
     * clear stable id map
     * should called when clear adapter data;
     */
    protected void clearStableIdMap() {
        mIdMap.clear();
    }
    protected void removeStableID(Object item) {
        mIdMap.remove(item);
    }
}

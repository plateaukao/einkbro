package org.askerov.dynamicgrid;

import android.view.View;

import java.util.List;

/**
 * Author: alex askerov
 * Date: 9/7/13
 * Time: 10:14 PM
 */
@SuppressWarnings({"WeakerAccess", "unchecked"})
public class DynamicGridUtils {

    public static void reorder(List list, int indexFrom, int indexTwo) {
        Object obj = list.remove(indexFrom);
        list.add(indexTwo, obj);
    }

    public static float getViewX(View view) {
        return Math.abs((view.getRight() - view.getLeft()) / 2);
    }

    public static float getViewY(View view) {
        return Math.abs((view.getBottom() - view.getTop()) / 2);
    }
}

package de.baumann.browser.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import de.baumann.browser.Ninja.R;

public class GridAdapter_filter extends BaseAdapter {
    private static class Holder {
        TextView title;
        ImageView icon;
    }

    private final List<GridItem_filter> list;

    private final Context context;

    public GridAdapter_filter(Context context, List<GridItem_filter> list) {
        this.context = context;
        this.list = list;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Holder holder;
        View view = convertView;

        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.list_item_bookmark, parent, false);
            holder = new Holder();
            holder.title = view.findViewById(R.id.record_item_title);
            holder.icon = view.findViewById(R.id.ib_icon);
            view.setTag(holder);
        } else {
            holder = (Holder) view.getTag();
        }

        GridItem_filter item = list.get(position);
        holder.title.setText(item.getTitle());
        holder.icon.setImageDrawable(item.getIcon());

        return view;
    }

    @Override
    public int getCount() {
        // TODO Auto-generated method stub
        return list.size();
        //return 0;
    }

    @Override
    public Object getItem(int arg0) {
        // TODO Auto-generated method stub
        return list.get(arg0);

    }

    @Override
    public long getItemId(int arg0) {
        // TODO Auto-generated method stub
        return arg0;

    }
}

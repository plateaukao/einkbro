package de.baumann.browser.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import de.baumann.browser.Ninja.R;
import de.baumann.browser.unit.BrowserUnit;

import java.util.List;

public class GridAdapter extends BaseAdapter {
    private static class Holder {
        TextView title;
        ImageView cover;
    }

    private final List<GridItem> list;

    private final Context context;

    public GridAdapter(Context context, List<GridItem> list) {
        this.context = context;
        this.list = list;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Holder holder;
        View view = convertView;

        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.grid_item, parent, false);
            holder = new Holder();
            holder.title = view.findViewById(R.id.grid_item_title);
            holder.cover = view.findViewById(R.id.grid_item_cover);
            view.setTag(holder);
        } else {
            holder = (Holder) view.getTag();
        }

        GridItem item = list.get(position);
        holder.title.setText(item.getTitle());
        holder.cover.setImageBitmap(BrowserUnit.file2Bitmap(context, item.getFilename()));

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

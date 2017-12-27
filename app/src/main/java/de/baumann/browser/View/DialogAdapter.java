package de.baumann.browser.View;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import de.baumann.browser.Ninja.R;

public class DialogAdapter extends ArrayAdapter<String> {
    private final Context context;
    private final int layoutResId;
    private final List<String> list;

    public DialogAdapter(Context context, List<String> list) {
        super(context, R.layout.dialog_text_item, list);
        this.context = context;
        this.layoutResId = R.layout.dialog_text_item;
        this.list = list;
    }

    private static class Holder {
        TextView textView;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        Holder holder;
        View view = convertView;

        if (view == null) {
            view = LayoutInflater.from(context).inflate(layoutResId, parent, false);
            holder = new Holder();
            holder.textView = view.findViewById(R.id.dialog_text_item);
            view.setTag(holder);
        } else {
            holder = (Holder) view.getTag();
        }

        holder.textView.setText(list.get(position));

        return view;
    }
}

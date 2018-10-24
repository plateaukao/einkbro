package de.baumann.browser.View;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.List;

import de.baumann.browser.Browser.Javascript;
import de.baumann.browser.Ninja.R;

public class Adapter_Javascript extends ArrayAdapter<String> {
    private final Context context;
    private final int layoutResId;
    private final List<String> list;

    public Adapter_Javascript(Context context, List<String> list){
        super(context, R.layout.whitelist_item, list);
        this.context = context;
        this.layoutResId = R.layout.whitelist_item;
        this.list = list;
    }

    private static class Holder {
        TextView domain;
        ImageButton cancel;
    }

    @NonNull
    @Override
    public View getView(final int position, View convertView, @NonNull ViewGroup parent) {
        Holder holder;
        View view = convertView;

        if (view == null) {
            view = LayoutInflater.from(context).inflate(layoutResId, parent, false);
            holder = new Holder();
            holder.domain = view.findViewById(R.id.whitelist_item_domain);
            holder.cancel = view.findViewById(R.id.whitelist_item_cancel);
            view.setTag(holder);
        } else {
            holder = (Holder) view.getTag();
        }

        holder.domain.setText(list.get(position));
        holder.cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Javascript Javascript = new Javascript(context);
                Javascript.removeDomain(list.get(position));
                list.remove(position);
                notifyDataSetChanged();
                NinjaToast.show(context, R.string.toast_delete_successful);
            }
        });

        return view;
    }
}

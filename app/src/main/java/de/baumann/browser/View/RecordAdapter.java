package de.baumann.browser.View;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import com.github.curioustechizen.ago.RelativeTimeTextView;
import de.baumann.browser.Database.Record;
import de.baumann.browser.Ninja.R;

import java.util.List;

public class RecordAdapter extends ArrayAdapter<Record> {
    private final Context context;
    private final int layoutResId;
    private final List<Record> list;

    public RecordAdapter(Context context, List<Record> list) {
        super(context, R.layout.record_item, list);
        this.context = context;
        this.layoutResId = R.layout.record_item;
        this.list = list;
    }

    private static class Holder {
        TextView title;
        RelativeTimeTextView time;
        TextView url;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        Holder holder;
        View view = convertView;

        if (view == null) {
            view = LayoutInflater.from(context).inflate(layoutResId, parent, false);
            holder = new Holder();
            holder.title = view.findViewById(R.id.record_item_title);
            holder.time = view.findViewById(R.id.record_item_time);
            holder.url = view.findViewById(R.id.record_item_url);
            view.setTag(holder);
        } else {
            holder = (Holder) view.getTag();
        }

        Record record = list.get(position);
        holder.title.setText(record.getTitle());
        holder.time.setReferenceTime(record.getTime());
        holder.url.setText(record.getURL());

        return view;
    }
}
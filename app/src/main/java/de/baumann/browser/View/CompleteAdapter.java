package de.baumann.browser.View;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.baumann.browser.Database.Record;
import de.baumann.browser.Ninja.R;

public class CompleteAdapter extends BaseAdapter implements Filterable {
    private class CompleteFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence prefix) {
            if (prefix == null) {
                return new FilterResults();
            }

            resultList.clear();
            for (CompleteItem item : originalList) {
                if (item.getTitle().contains(prefix) || item.getURL().contains(prefix)) {
                    if (item.getTitle().contains(prefix)) {
                        item.setIndex(item.getTitle().indexOf(prefix.toString()));
                    } else if (item.getURL().contains(prefix)) {
                        item.setIndex(item.getURL().indexOf(prefix.toString()));
                    }
                    resultList.add(item);
                }
            }

            Collections.sort(resultList, new Comparator<CompleteItem>() {
                @Override
                public int compare(CompleteItem first, CompleteItem second) {
                    return Integer.compare(first.getIndex(), second.getIndex());
                }
            });

            FilterResults results = new FilterResults();
            results.values = resultList;
            results.count = resultList.size();

            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            notifyDataSetChanged();
        }
    }

    private class CompleteItem {
        private final String title;
        String getTitle() {
            return title;
        }

        private final String url;
        String getURL() {
            return url;
        }

        private int index = Integer.MAX_VALUE;
        int getIndex() {
            return index;
        }
        void setIndex(int index) {
            this.index = index;
        }

        CompleteItem(String title, String url) {
            this.title = title;
            this.url = url;
        }

        @Override
        public boolean equals(Object object) {
            if (!(object instanceof CompleteItem)) {
                return false;
            }

            CompleteItem item = (CompleteItem) object;
            return item.getTitle().equals(title) && item.getURL().equals(url);
        }

        @Override
        public int hashCode() {
            if (title == null || url == null) {
                return 0;
            }

            return title.hashCode() & url.hashCode();
        }
    }

    private static class Holder {
        TextView titleView;
        TextView urlView;
    }

    private final Context context;
    private final int layoutResId;
    private final List<CompleteItem> originalList;
    private final List<CompleteItem> resultList;
    private final CompleteFilter filter = new CompleteFilter();

    public CompleteAdapter(Context context, List<Record> recordList) {
        this.context = context;
        this.layoutResId = R.layout.list_item;
        this.originalList = new ArrayList<>();
        this.resultList = new ArrayList<>();
        deDup(recordList);
    }

    private void deDup(List<Record> recordList) {
        for (Record record : recordList) {
            if (record.getTitle() != null
                    && !record.getTitle().isEmpty()
                    && record.getURL() != null
                    && !record.getURL().isEmpty()) {
                originalList.add(new CompleteItem(record.getTitle(), record.getURL()));
            }
        }

        Set<CompleteItem> set = new HashSet<>(originalList);
        originalList.clear();
        originalList.addAll(set);
    }

    @Override
    public int getCount() {
        return resultList.size();
    }

    @Override
    public Filter getFilter() {
        return filter;
    }

    @Override
    public Object getItem(int position) {
        return resultList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        Holder holder;

        if (view == null) {
            view = LayoutInflater.from(context).inflate(layoutResId, null, false);
            holder = new Holder();
            holder.titleView = view.findViewById(R.id.record_item_title);
            holder.urlView = view.findViewById(R.id.record_item_url);
            view.setTag(holder);
        } else {
            holder = (Holder) view.getTag();
        }

        CompleteItem item = resultList.get(position);
        holder.titleView.setText(item.getTitle());
        if (item.getURL() != null) {
            holder.urlView.setText(item.getURL());
        } else {
            holder.urlView.setText(item.getURL());
        }

        return view;
    }
}

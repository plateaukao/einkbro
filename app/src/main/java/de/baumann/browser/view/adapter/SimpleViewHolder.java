package de.baumann.browser.view.adapter;

import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

public class SimpleViewHolder extends RecyclerView.ViewHolder
{
    public TextView[] views;

    public SimpleViewHolder (View itemView, int[] to)
    {
        super(itemView);

        views = new TextView[to.length];
        for(int i = 0 ; i < to.length ; i++) {
            views[i] = (TextView) itemView.findViewById(to[i]);
        }
    }
}

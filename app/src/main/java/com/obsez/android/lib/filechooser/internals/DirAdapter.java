package com.obsez.android.lib.filechooser.internals;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;


import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.baumann.browser.R;

public class DirAdapter extends ArrayAdapter<File> {

    private final static SimpleDateFormat _formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault());

    private final List<File> m_entries;

    public DirAdapter(Context cxt, List<File> entries) {
        super(cxt, R.layout.li_row_textview, R.id.text1, entries);
        m_entries = entries;
    }

    // This function is called to show each view item
    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        ViewGroup rl = (ViewGroup) super.getView(position, convertView, parent);

        TextView tvName = (TextView) rl.findViewById(R.id.text1);
        TextView tvSize = (TextView) rl.findViewById(R.id.txt_size);
        TextView tvDate = (TextView) rl.findViewById(R.id.txt_date);
        ImageView ivIcon = (ImageView) rl.findViewById(R.id.icon);

        File file = m_entries.get(position);
        if (file == null) {
            tvName.setText("..");
            ivIcon.setImageResource(R.drawable.folder_upload);
        } else if (file.isDirectory()) {
            tvName.setText(m_entries.get(position).getName());
            tvSize.setText("");
            ivIcon.setImageResource(R.drawable.folder);
            //FileInfo fileInfo;
            tvDate.setText(_formatter.format(new Date(file.lastModified())));
        } else {
            tvName.setText(m_entries.get(position).getName());
            //tvSize.setText(Long.toString(file.length()));
            tvSize.setText(FileUtil.getReadableFileSize(file.length()));
            tvDate.setText(_formatter.format(new Date(file.lastModified())));
            ivIcon.setImageResource(R.drawable.file);
        }

        return rl;
    }
}


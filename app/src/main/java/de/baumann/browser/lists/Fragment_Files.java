package de.baumann.browser.lists;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.FilterQueryProvider;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import de.baumann.browser.R;
import de.baumann.browser.databases.DbAdapter_Files;
import de.baumann.browser.helper.CustomViewPager;
import de.baumann.browser.helper.helper_editText;
import de.baumann.browser.helper.helper_main;
import de.baumann.browser.helper.helper_toolbar;

import static android.content.ContentValues.TAG;
import static java.lang.String.valueOf;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class Fragment_Files extends Fragment {

    private ListView listView = null;
    private EditText editText;
    private DbAdapter_Files db;
    private SimpleCursorAdapter adapter;
    private SharedPreferences sharedPref;
    private TextView listBar;
    private Toolbar toolbar;
    private CustomViewPager viewPager;

    private int top;
    private int index;
    

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_lists, container, false);

        setHasOptionsMenu(true);

        PreferenceManager.setDefaultValues(getActivity(), R.xml.user_settings, false);
        PreferenceManager.setDefaultValues(getActivity(), R.xml.user_settings_search, false);
        sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        sharedPref.edit().putString("files_startFolder",
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath()).apply();

        toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
        editText = (EditText) getActivity().findViewById(R.id.editText);
        listBar = (TextView) getActivity().findViewById(R.id.listBar);
        listView = (ListView)rootView.findViewById(R.id.list);
        viewPager = (CustomViewPager) getActivity().findViewById(R.id.viewpager);

        //calling Notes_DbAdapter
        db = new DbAdapter_Files(getActivity());
        db.open();

        return rootView;
    }

    private void setTitle () {
        if (sharedPref.getString("sortDBF", "title").equals("title")) {
            listBar.setText(getString(R.string.app_title_downloads) + " | " + getString(R.string.sort_title));
        } else if (sharedPref.getString("sortDBF", "title").equals("file_date")) {
            listBar.setText(getString(R.string.app_title_downloads) + " | " + getString(R.string.sort_date));
        } else {
            listBar.setText(getString(R.string.app_title_downloads) + " | " + getString(R.string.sort_extension));
        }
    }

    private void isEdited () {
        index = listView.getFirstVisiblePosition();
        View v = listView.getChildAt(0);
        top = (v == null) ? 0 : (v.getTop() - listView.getPaddingTop());
    }

    private void setFilesList() {

        getActivity().deleteDatabase("files_DB_v01.db");

        String path = sharedPref.getString("files_startFolder",
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath());

        File f = new File(path);
        final File[] files = f.listFiles();

        // looping through all items <item>
        if (files.length == 0) {
            Snackbar.make(listView, R.string.toast_files, Snackbar.LENGTH_LONG).show();
        }

        for (File file : files) {

            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

            String file_Name = file.getName();
            String file_Size = getReadableFileSize(file.length());
            String file_date = formatter.format(new Date(file.lastModified()));
            String file_path = file.getAbsolutePath();

            String file_ext;
            if (file.isDirectory()) {
                file_ext = ".";
            } else {
                try {
                    file_ext = file.getAbsolutePath().substring(file.getAbsolutePath().lastIndexOf("."));
                } catch (Exception e) {
                    file_ext = file.getAbsolutePath().substring(file.getAbsolutePath().lastIndexOf("/"));
                }
            }

            db.open();
            if(db.isExist(file_Name)) {
                Log.i(TAG, "Entry exists" + file_Name);
            } else {
                db.insert(file_Name, file_Size, file_ext, file_path, file_date);
            }
        }

        try {
            db.insert("...", "", "", "", "");
        } catch (Exception e) {
            Snackbar.make(listView, R.string.toast_directory, Snackbar.LENGTH_LONG).show();
        }

        //display data
        final int layoutstyle=R.layout.list_item;
        int[] xml_id = new int[] {
                R.id.textView_title_notes,
                R.id.textView_des_notes,
                R.id.textView_create_notes
        };
        String[] column = new String[] {
                "files_title",
                "files_content",
                "files_creation"
        };
        final Cursor row = db.fetchAllData(getActivity());
        adapter = new SimpleCursorAdapter(getActivity(), layoutstyle,row,column, xml_id, 0) {
            @Override
            public View getView (final int position, View convertView, ViewGroup parent) {

                Cursor row = (Cursor) listView.getItemAtPosition(position);
                final String files_icon = row.getString(row.getColumnIndexOrThrow("files_icon"));
                final String files_attachment = row.getString(row.getColumnIndexOrThrow("files_attachment"));

                View v = super.getView(position, convertView, parent);
                final ImageView iv = (ImageView) v.findViewById(R.id.icon_notes);
                final ImageView iv2 = (ImageView) v.findViewById(R.id.icon_notes2);

                iv.setVisibility(View.VISIBLE);
                Uri uri = Uri.fromFile(new File(files_attachment));

                if (files_icon.matches("")) {
                    iv.setVisibility(View.INVISIBLE);
                    iv2.setVisibility(View.VISIBLE);
                    iv.setImageResource(R.drawable.arrow_up_dark);
                } else if (files_icon.matches("(.)")) {
                    iv.setImageResource(R.drawable.folder);
                } else if (files_icon.matches("(.m3u8|.mp3|.wma|.midi|.wav|.aac|.aif|.amp3|.weba|.ogg)")) {
                    iv.setImageResource(R.drawable.file_music);
                } else if (files_icon.matches("(.mpeg|.mp4|.webm|.qt|.3gp|.3g2|.avi|.flv|.h261|.h263|.h264|.asf|.wmv)")) {
                    iv.setImageResource(R.drawable.file_video);
                } else if(files_icon.matches("(.gif|.bmp|.tiff|.scg|.png|.jpg|.JPG|.jpeg)")) {
                    try {
                        iv2.setVisibility(View.INVISIBLE);
                        Picasso.with(getActivity()).load(uri).resize(76, 76).centerCrop().memoryPolicy(MemoryPolicy.NO_CACHE).into(iv);
                    } catch (Exception e) {
                        Log.w("Browser", "Error load thumbnail", e);
                        iv.setImageResource(R.drawable.file_image);
                    }
                } else if (files_icon.matches("(.vcs|.vcf|.css|.ics|.conf|.config|.java|.html)")) {
                    iv.setImageResource(R.drawable.file_xml);
                } else if (files_icon.matches("(.apk)")) {
                    iv.setImageResource(R.drawable.android);
                } else if (files_icon.matches("(.pdf)")) {
                    iv.setImageResource(R.drawable.file_pdf);
                } else if (files_icon.matches("(.rtf|.csv|.txt|.doc|.xls|.ppt|.docx|.pptx|.xlsx|.odt|.ods|.odp)")) {
                    iv.setImageResource(R.drawable.file_document);
                } else if (files_icon.matches("(.zip|.rar)")) {
                    iv.setImageResource(R.drawable.zip_box);
                } else {
                    iv.setImageResource(R.drawable.file);
                }

                return v;
            }
        };

        //display data by filter
        final String note_search = sharedPref.getString("filter_filesBY", "files_title");
        sharedPref.edit().putString("filter_filesBY", "files_title").apply();
        editText.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.getFilter().filter(s.toString());
            }
        });
        adapter.setFilterQueryProvider(new FilterQueryProvider() {
            public Cursor runQuery(CharSequence constraint) {
                return db.fetchDataByFilter(constraint.toString(),note_search);
            }
        });

        listView.setAdapter(adapter);
        listView.setSelectionFromTop(index, top);
        //onClick function
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterview, View view, int position, long id) {

                Cursor row2 = (Cursor) listView.getItemAtPosition(position);
                final String files_icon = row2.getString(row2.getColumnIndexOrThrow("files_icon"));
                final String files_attachment = row2.getString(row2.getColumnIndexOrThrow("files_attachment"));

                final File pathFile = new File(files_attachment);

                if(pathFile.isDirectory()) {
                    try {
                        sharedPref.edit().putString("files_startFolder", files_attachment).apply();
                        setFilesList();
                    } catch (Exception e) {
                        Snackbar.make(listView, R.string.toast_directory, Snackbar.LENGTH_LONG).show();
                    }
                } else if(files_attachment.equals("")) {
                    try {
                        final File pathActual = new File(sharedPref.getString("files_startFolder",
                                Environment.getExternalStorageDirectory().getPath()));
                        sharedPref.edit().putString("files_startFolder", pathActual.getParent()).apply();
                        setFilesList();
                    } catch (Exception e) {
                        Snackbar.make(listView, R.string.toast_directory, Snackbar.LENGTH_LONG).show();
                    }
                } else {
                    helper_main.open(files_icon, getActivity(), pathFile, listView);
                }
            }
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

                isEdited();

                Cursor row2 = (Cursor) listView.getItemAtPosition(position);
                final String files_title = row2.getString(row2.getColumnIndexOrThrow("files_title"));
                final String files_attachment = row2.getString(row2.getColumnIndexOrThrow("files_attachment"));

                final File pathFile = new File(files_attachment);

                if (pathFile.isDirectory()) {
                    Snackbar snackbar = Snackbar
                            .make(listView, R.string.bookmark_remove_confirmation, Snackbar.LENGTH_LONG)
                            .setAction(R.string.toast_yes, new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    sharedPref.edit().putString("files_startFolder", pathFile.getParent()).apply();
                                    deleteRecursive(pathFile);
                                    setFilesList();
                                }
                            });
                    snackbar.show();

                } else {
                    final CharSequence[] options = {
                            getString(R.string.choose_menu_2),
                            getString(R.string.choose_menu_3),
                            getString(R.string.choose_menu_4)};

                    final AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());
                    dialog.setPositiveButton(R.string.toast_cancel, new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int whichButton) {
                            dialog.cancel();
                        }
                    });
                    dialog.setItems(options, new DialogInterface.OnClickListener() {
                        @SuppressWarnings("ResultOfMethodCallIgnored")
                        @Override
                        public void onClick(DialogInterface dialog, int item) {

                            if (options[item].equals(getString(R.string.choose_menu_2))) {

                                if (pathFile.exists()) {
                                    Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                                    sharingIntent.setType("image/png");
                                    sharingIntent.putExtra(Intent.EXTRA_SUBJECT, files_title);
                                    sharingIntent.putExtra(Intent.EXTRA_TEXT, files_title);
                                    Uri bmpUri = Uri.fromFile(pathFile);
                                    sharingIntent.putExtra(Intent.EXTRA_STREAM, bmpUri);
                                    startActivity(Intent.createChooser(sharingIntent, (getString(R.string.app_share_file))));
                                }
                            }
                            if (options[item].equals(getString(R.string.choose_menu_4))) {

                                Snackbar snackbar = Snackbar
                                        .make(listView, R.string.bookmark_remove_confirmation, Snackbar.LENGTH_LONG)
                                        .setAction(R.string.toast_yes, new View.OnClickListener() {
                                            @Override
                                            public void onClick(View view) {
                                                pathFile.delete();
                                                setFilesList();
                                            }
                                        });
                                snackbar.show();
                            }
                            if (options[item].equals(getString(R.string.choose_menu_3))) {
                                sharedPref.edit().putString("pathFile", files_attachment).apply();
                                editText.setVisibility(View.VISIBLE);
                                helper_editText.showKeyboard(getActivity(), editText, 2, files_title, getString(R.string.bookmark_edit_title));
                            }
                        }
                    });
                    dialog.show();
                }

                return true;
            }
        });
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void deleteRecursive(File fileOrDirectory) {

        if (fileOrDirectory.isDirectory()) {
            for (File child : fileOrDirectory.listFiles()) {
                deleteRecursive(child);
            }
        }
        fileOrDirectory.delete();
    }

    private static String getReadableFileSize(long size) {
        final int BYTES_IN_KILOBYTES = 1024;
        final DecimalFormat dec = new DecimalFormat("###.#");
        final String KILOBYTES = " KB";
        final String MEGABYTES = " MB";
        final String GIGABYTES = " GB";
        float fileSize = 0;
        String suffix = KILOBYTES;

        if (size > BYTES_IN_KILOBYTES) {
            fileSize = size / BYTES_IN_KILOBYTES;
            if (fileSize > BYTES_IN_KILOBYTES) {
                fileSize = fileSize / BYTES_IN_KILOBYTES;
                if (fileSize > BYTES_IN_KILOBYTES) {
                    fileSize = fileSize / BYTES_IN_KILOBYTES;
                    suffix = GIGABYTES;
                } else {
                    suffix = MEGABYTES;
                }
            }
        }
        return valueOf(dec.format(fileSize) + suffix);
    }

    public void doBack() {
        //BackPressed in activity will call this;
        Snackbar snackbar = Snackbar
                .make(listView, getString(R.string.toast_exit), Snackbar.LENGTH_SHORT)
                .setAction(getString(R.string.toast_yes), new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        getActivity().finish();
                    }
                });
        snackbar.show();
    }

    @Override
    public void setUserVisibleHint(final boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if (isVisibleToUser) {
            AppCompatActivity appCompatActivity = (AppCompatActivity)getActivity();
            assert appCompatActivity.getSupportActionBar() != null;
            appCompatActivity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            setTitle();
            setFilesList();
            helper_toolbar.toolbarGestures(getActivity(), toolbar, viewPager, editText, listBar, "");
        } else {
            Log.i("Browser", "Browser: isVisibleToUser false");
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_file, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        super.onPrepareOptionsMenu(menu);

        if (sharedPref.getInt("keyboard", 0) == 0) {
            // normal
            menu.findItem(R.id.action_cancel).setVisible(false);
            menu.findItem(R.id.action_save_bookmark).setVisible(false);
        } else if (sharedPref.getInt("keyboard", 0) == 1){
            // filter
            menu.findItem(R.id.action_sort).setVisible(false);
            menu.findItem(R.id.action_save_bookmark).setVisible(false);
        } else {
            // filter
            menu.findItem(R.id.action_sort).setVisible(false);
            menu.findItem(R.id.action_filter).setVisible(false);
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch (item.getItemId()) {

            case R.id.filter_title:
                sharedPref.edit().putString("filter_filesBY", "files_title").apply();
                setFilesList();
                editText.setVisibility(View.VISIBLE);
                helper_editText.showKeyboard(getActivity(), editText, 1, "", getString(R.string.action_filter_title));
                return true;
            case R.id.filter_url:
                sharedPref.edit().putString("filter_filesBY", "files_icon").apply();
                setFilesList();
                editText.setVisibility(View.VISIBLE);
                helper_editText.showKeyboard(getActivity(), editText, 1, "", getString(R.string.action_filter_url));
                return true;

            case R.id.filter_today:
                DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Calendar cal = Calendar.getInstance();
                final String search = dateFormat.format(cal.getTime());
                sharedPref.edit().putString("filter_filesBY", "files_creation").apply();
                setFilesList();
                editText.setText(search);
                listBar.setText(getString(R.string.app_title_bookmarks) + " | " + getString(R.string.filter_today));
                return true;
            case R.id.filter_yesterday:
                DateFormat dateFormat2 = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Calendar cal2 = Calendar.getInstance();
                cal2.add(Calendar.DATE, -1);
                final String search2 = dateFormat2.format(cal2.getTime());
                sharedPref.edit().putString("filter_filesBY", "files_creation").apply();
                setFilesList();
                editText.setText(search2);
                listBar.setText(getString(R.string.app_title_bookmarks) + " | " + getString(R.string.filter_yesterday));
                return true;
            case R.id.filter_before:
                DateFormat dateFormat3 = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Calendar cal3 = Calendar.getInstance();
                cal3.add(Calendar.DATE, -2);
                final String search3 = dateFormat3.format(cal3.getTime());
                sharedPref.edit().putString("filter_filesBY", "files_creation").apply();
                setFilesList();
                editText.setText(search3);
                listBar.setText(getString(R.string.app_title_bookmarks) + " | " + getString(R.string.filter_before));
                return true;
            case R.id.filter_month:
                DateFormat dateFormat4 = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
                Calendar cal4 = Calendar.getInstance();
                final String search4 = dateFormat4.format(cal4.getTime());
                sharedPref.edit().putString("filter_filesBY", "files_creation").apply();
                setFilesList();
                editText.setText(search4);
                listBar.setText(getString(R.string.app_title_bookmarks) + " | " + getString(R.string.filter_month));
                return true;
            case R.id.filter_own:
                sharedPref.edit().putString("filter_filesBY", "files_creation").apply();
                setFilesList();
                editText.setVisibility(View.VISIBLE);
                helper_editText.showKeyboard(getActivity(), editText, 1, "", getString(R.string.action_filter_create));
                return true;
            case R.id.filter_clear:
                editText.setVisibility(View.GONE);
                setTitle();
                helper_editText.hideKeyboard(getActivity(), editText, 0, getString(R.string.app_title_history), getString(R.string.app_search_hint));
                setFilesList();
                return true;



            case R.id.action_save_bookmark:

                final File pathFile = new File(sharedPref.getString("pathFile", ""));

                String inputTag = editText.getText().toString().trim();

                File dir = pathFile.getParentFile();
                File to = new File(dir,inputTag);

                if(db.isExist(inputTag)){
                    Snackbar.make(listView, getString(R.string.toast_newTitle), Snackbar.LENGTH_LONG).show();
                } else {
                    pathFile.renameTo(to);
                    pathFile.delete();
                    Snackbar.make(listView, R.string.bookmark_added, Snackbar.LENGTH_SHORT).show();
                    editText.setVisibility(View.GONE);
                    setTitle();
                    helper_editText.hideKeyboard(getActivity(), editText, 0, getString(R.string.app_title_bookmarks), getString(R.string.app_search_hint));
                    setFilesList();
                }

                return true;

            case R.id.action_cancel:
                editText.setVisibility(View.GONE);
                setTitle();
                helper_editText.hideKeyboard(getActivity(), editText, 0, getString(R.string.app_title_bookmarks), getString(R.string.app_search_hint));
                setFilesList();
                return true;

            case R.id.sort_title:
                sharedPref.edit().putString("sortDBF", "title").apply();
                setFilesList();
                setTitle();
                return true;
            case R.id.sort_extension:
                sharedPref.edit().putString("sortDBF", "file_ext").apply();
                setFilesList();
                setTitle();
                return true;
            case R.id.sort_date:
                sharedPref.edit().putString("sortDBF", "file_date").apply();
                setFilesList();
                setTitle();
                return true;

            case android.R.id.home:
                viewPager.setCurrentItem(sharedPref.getInt("tab", 0));
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
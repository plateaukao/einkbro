/*
    This file is part of the Browser webview app.

    HHS Moodle WebApp is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    HHS Moodle WebApp is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with the Browser webview app.

    If not, see <http://www.gnu.org/licenses/>.
 */

package de.baumann.browser.popups;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.FilterQueryProvider;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.io.File;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

import de.baumann.browser.R;
import de.baumann.browser.databases.DbAdapter_Files;
import de.baumann.browser.helper.helper_editText;
import de.baumann.browser.helper.helper_main;

import static android.content.ContentValues.TAG;
import static java.lang.String.valueOf;

public class Popup_files extends AppCompatActivity {

    private ListView listView = null;
    private EditText editText;
    private TextView urlBar;
    private DbAdapter_Files db;
    private SimpleCursorAdapter adapter;
    private SharedPreferences sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(ContextCompat.getColor(Popup_files.this, R.color.colorThreeDark));

        setContentView(R.layout.activity_popup);
        helper_main.onStart(Popup_files.this);

        PreferenceManager.setDefaultValues(this, R.xml.user_settings, false);
        PreferenceManager.setDefaultValues(this, R.xml.user_settings_search, false);
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPref.edit().putString("files_startFolder",
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath()).apply();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        editText = (EditText) findViewById(R.id.editText);
        editText.setVisibility(View.GONE);
        editText.setHint(R.string.app_search_hint);
        editText.clearFocus();
        urlBar = (TextView) findViewById(R.id.urlBar);
        setTitle();

        listView = (ListView)findViewById(R.id.list);

        //calling Notes_DbAdapter
        db = new DbAdapter_Files(this);
        db.open();

        setFilesList();
    }


    private void setFilesList() {

        deleteDatabase("files_DB_v01.db");

        File f = new File(sharedPref.getString("files_startFolder",
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath()));
        final File[] files = f.listFiles();

        Arrays.sort(files, new Comparator<File>() {
            @Override
            public int compare(File file1, File file2) {
                if(file1.isDirectory()){
                    if (file2.isDirectory()){
                        return String.valueOf(file1.getName().toLowerCase()).compareTo(file2.getName().toLowerCase());
                    }else{
                        return -1;
                    }
                }else {
                    if (file2.isDirectory()){
                        return 1;
                    }else{
                        return String.valueOf(file1.getName().toLowerCase()).compareTo(file2.getName().toLowerCase());
                    }
                }
            }
        });

        // looping through all items <item>
        if (files.length == 0) {
            Snackbar.make(listView, R.string.toast_files, Snackbar.LENGTH_LONG).show();
        }

        for (File file : files) {

            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

            String file_Name = file.getName().substring(0,1).toUpperCase() + file.getName().substring(1);
            String file_Size = getReadableFileSize(file.length());
            String file_date = formatter.format(new Date(file.lastModified()));
            String file_path = file.getAbsolutePath();

            String file_ext;
            if (file.isDirectory()) {
                file_ext = ".";
            } else {
                file_ext = file.getAbsolutePath().substring(file.getAbsolutePath().lastIndexOf("."));
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
        final Cursor row = db.fetchAllData(this);
        adapter = new SimpleCursorAdapter(this, layoutstyle,row,column, xml_id, 0) {
            @Override
            public View getView (final int position, View convertView, ViewGroup parent) {

                Cursor row2 = (Cursor) listView.getItemAtPosition(position);
                final String files_icon = row2.getString(row2.getColumnIndexOrThrow("files_icon"));
                final String files_attachment = row2.getString(row2.getColumnIndexOrThrow("files_attachment"));
                final File pathFile = new File(files_attachment);

                View v = super.getView(position, convertView, parent);
                final ImageView iv = (ImageView) v.findViewById(R.id.icon_notes);

                iv.setVisibility(View.VISIBLE);

                if (pathFile.isDirectory()) {
                    iv.setImageResource(R.drawable.folder);
                } else {
                    switch (files_icon) {
                        case "":
                            iv.setImageResource(R.drawable.arrow_up_dark);
                            break;
                        case ".m3u8":case ".mp3":case ".wma":case ".midi":case ".wav":case ".aac":
                        case ".aif":case ".amp3":case ".weba":case ".ogg":
                            iv.setImageResource(R.drawable.file_music);
                            break;
                        case ".mpeg":case ".mp4":case ".webm":case ".qt":case ".3gp":
                        case ".3g2":case ".avi":case ".f4v":case ".flv":case ".h261":case ".h263":
                        case ".h264":case ".asf":case ".wmv":
                            try {
                                Glide.with(Popup_files.this)
                                        .load(files_attachment) // or URI/path
                                        .override(76, 76)
                                        .centerCrop()
                                        .into(iv); //imageView to set thumbnail to
                            } catch (Exception e) {
                                Log.w("HHS_Moodle", "Error load thumbnail", e);
                                iv.setImageResource(R.drawable.file_video);
                            }
                            break;
                        case ".vcs":case ".vcf":case ".css":case ".ics":case ".conf":case ".config":
                        case ".java":case ".html":
                            iv.setImageResource(R.drawable.file_xml);
                            break;
                        case ".apk":
                            iv.setImageResource(R.drawable.android);
                            break;
                        case ".pdf":
                            iv.setImageResource(R.drawable.file_pdf);
                            break;
                        case ".rtf":case ".csv":case ".txt":
                        case ".doc":case ".xls":case ".ppt":case ".docx":case ".pptx":case ".xlsx":
                        case ".odt":case ".ods":case ".odp":
                            iv.setImageResource(R.drawable.file_document);
                            break;
                        case ".zip":
                        case ".rar":
                            iv.setImageResource(R.drawable.zip_box);
                            break;
                        case ".gif":case ".bmp":case ".tiff":case ".svg":
                        case ".png":case ".jpg":case ".JPG":case ".jpeg":
                            try {
                                Glide.with(Popup_files.this)
                                        .load(files_attachment) // or URI/path
                                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                                        .skipMemoryCache(true)
                                        .override(76, 76)
                                        .centerCrop()
                                        .into(iv); //imageView to set thumbnail to
                            } catch (Exception e) {
                                Log.w("HHS_Moodle", "Error load thumbnail", e);
                                iv.setImageResource(R.drawable.file_image);
                            }
                            break;
                        default:
                            iv.setImageResource(R.drawable.file);
                            break;
                    }
                }

                if (files_attachment.isEmpty()) {
                    new Handler().postDelayed(new Runnable() {
                        public void run() {
                            iv.setImageResource(R.drawable.arrow_up_dark);
                        }
                    }, 350);
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
                    helper_main.open(files_icon, Popup_files.this, pathFile, listView);
                }
            }
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

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

                    final AlertDialog.Builder dialog = new AlertDialog.Builder(Popup_files.this);
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
                                helper_editText.showKeyboard(Popup_files.this, editText, 2, files_title, getString(R.string.bookmark_edit_title));
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

    private void setTitle () {
        if (sharedPref.getString("sortDBF", "title").equals("title")) {
            urlBar.setText(getString(R.string.app_title_downloads) + " | " + getString(R.string.sort_title));
        } else if (sharedPref.getString("sortDBF", "title").equals("file_date")) {
            urlBar.setText(getString(R.string.app_title_downloads) + " | " + getString(R.string.sort_date));
        } else {
            urlBar.setText(getString(R.string.app_title_downloads) + " | " + getString(R.string.sort_extension));
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
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
        return true; // this is important to call so that new menu is shown
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_file, menu);
        return true;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
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
                helper_editText.showKeyboard(Popup_files.this, editText, 1, "", getString(R.string.action_filter_title));
                return true;
            case R.id.filter_url:
                sharedPref.edit().putString("filter_filesBY", "files_icon").apply();
                setFilesList();
                editText.setVisibility(View.VISIBLE);
                helper_editText.showKeyboard(Popup_files.this, editText, 1, "", getString(R.string.action_filter_url));
                return true;

            case R.id.filter_today:
                DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Calendar cal = Calendar.getInstance();
                final String search = dateFormat.format(cal.getTime());
                sharedPref.edit().putString("filter_filesBY", "files_creation").apply();
                setFilesList();
                editText.setText(search);
                urlBar.setText(getString(R.string.app_title_bookmarks) + " | " + getString(R.string.filter_today));
                return true;
            case R.id.filter_yesterday:
                DateFormat dateFormat2 = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Calendar cal2 = Calendar.getInstance();
                cal2.add(Calendar.DATE, -1);
                final String search2 = dateFormat2.format(cal2.getTime());
                sharedPref.edit().putString("filter_filesBY", "files_creation").apply();
                setFilesList();
                editText.setText(search2);
                urlBar.setText(getString(R.string.app_title_bookmarks) + " | " + getString(R.string.filter_yesterday));
                return true;
            case R.id.filter_before:
                DateFormat dateFormat3 = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Calendar cal3 = Calendar.getInstance();
                cal3.add(Calendar.DATE, -2);
                final String search3 = dateFormat3.format(cal3.getTime());
                sharedPref.edit().putString("filter_filesBY", "files_creation").apply();
                setFilesList();
                editText.setText(search3);
                urlBar.setText(getString(R.string.app_title_bookmarks) + " | " + getString(R.string.filter_before));
                return true;
            case R.id.filter_month:
                DateFormat dateFormat4 = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
                Calendar cal4 = Calendar.getInstance();
                final String search4 = dateFormat4.format(cal4.getTime());
                sharedPref.edit().putString("filter_filesBY", "files_creation").apply();
                setFilesList();
                editText.setText(search4);
                urlBar.setText(getString(R.string.app_title_bookmarks) + " | " + getString(R.string.filter_month));
                return true;
            case R.id.filter_own:
                sharedPref.edit().putString("filter_filesBY", "files_creation").apply();
                setFilesList();
                editText.setVisibility(View.VISIBLE);
                helper_editText.showKeyboard(Popup_files.this, editText, 1, "", getString(R.string.action_filter_create));
                return true;
            case R.id.filter_clear:
                editText.setVisibility(View.GONE);
                setTitle();
                helper_editText.hideKeyboard(Popup_files.this, editText, 0, getString(R.string.app_title_history), getString(R.string.app_search_hint));
                setFilesList();
                return true;



            case R.id.action_save_bookmark:

                final File pathFile = new File(sharedPref.getString("pathFile", ""));

                String inputTag = editText.getText().toString().trim();

                File dir = pathFile.getParentFile();
                File to = new File(dir,inputTag);

                pathFile.renameTo(to);
                pathFile.delete();

                helper_editText.hideKeyboard(Popup_files.this, editText, 0, getString(R.string.app_title_bookmarks), getString(R.string.app_search_hint));
                setFilesList();

                Snackbar.make(listView, R.string.bookmark_added, Snackbar.LENGTH_SHORT).show();

                editText.setVisibility(View.GONE);
                setTitle();

                sharedPref.edit().putString("pathFile", "").apply();

                return true;

            case R.id.action_cancel:
                editText.setVisibility(View.GONE);
                setTitle();
                helper_editText.hideKeyboard(Popup_files.this, editText, 0, getString(R.string.app_title_bookmarks), getString(R.string.app_search_hint));
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
                sharedPref.edit().putInt("keyboard", 0).apply();
                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
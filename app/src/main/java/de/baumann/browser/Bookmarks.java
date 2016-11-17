/*
    This file is part of the Browser WebApp.

    Browser WebApp is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Browser WebApp is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with the Browser webview app.

    If not, see <http://www.gnu.org/licenses/>.
 */

package de.baumann.browser;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

import de.baumann.browser.databases.Database_Bookmarks;
import de.baumann.browser.helper.Activity_settings;
import de.baumann.browser.helper.helper_editText;
import de.baumann.browser.helper.helper_main;
import de.baumann.browser.popups.Popup_history;
import de.baumann.browser.popups.Popup_pass;

public class Bookmarks extends AppCompatActivity {

    private ListView listView = null;
    private EditText editText;
    private SharedPreferences sharedPref;
    private String subStr;
    private String wikiLang;
    private String searchEngine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_bookmarks);

        PreferenceManager.setDefaultValues(this, R.xml.user_settings, false);
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPref.edit()
                .putString("started", "")
                .putInt("keyboard", 0)
                .putString("url", "")
                .putString("seqno", "")
                .apply();
        invalidateOptionsMenu();
        sharedPref.getInt("keyboard", 0);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        editText = (EditText) findViewById(R.id.editText);
        helper_editText.editText_Touch(editText, Bookmarks.this);
        helper_editText.editText_FocusChange(editText, Bookmarks.this);

        searchEngine = sharedPref.getString("searchEngine", "https://startpage.com/do/search?query=");
        wikiLang = sharedPref.getString("wikiLang", "en");

        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ( (actionId == EditorInfo.IME_ACTION_SEARCH) || ((event.getKeyCode() == KeyEvent.KEYCODE_ENTER) && (event.getAction() == KeyEvent.ACTION_DOWN ))){
                    String text = editText.getText().toString();
                    if (text.length() > 3) {
                        subStr=text.substring(3);
                    }
                    if (text.startsWith("www")) {
                        helper_main.switchToActivity(Bookmarks.this, Browser.class, "http://" + text, true);
                    } else if (text.contains("http")) {
                        helper_main.switchToActivity(Bookmarks.this, Browser.class, text, true);
                    } else if (text.contains(".w ")) {
                        helper_main.switchToActivity(Bookmarks.this, Browser.class, "https://" + wikiLang + ".wikipedia.org/wiki/Spezial:Suche?search=" + subStr, true);
                    } else if (text.startsWith(".f ")) {
                        helper_main.switchToActivity(Bookmarks.this, Browser.class, "https://www.flickr.com/search/?advanced=1&license=2%2C3%2C4%2C5%2C6%2C9&text=" + subStr, true);
                    } else  if (text.startsWith(".m ")) {
                        helper_main.switchToActivity(Bookmarks.this, Browser.class, "https://metager.de/meta/meta.ger3?focus=web&eingabe=" + subStr, true);
                    } else if (text.startsWith(".g ")) {
                        helper_main.switchToActivity(Bookmarks.this, Browser.class, "https://github.com/search?utf8=✓&q=" + subStr, true);
                    } else  if (text.startsWith(".s ")) {
                        helper_main.switchToActivity(Bookmarks.this, Browser.class, "https://startpage.com/do/search?query=" + subStr, true);
                    } else if (text.startsWith(".G ")) {
                        helper_main.switchToActivity(Bookmarks.this, Browser.class, "https://www.google.com/search?&q=" + subStr, true);
                    } else  if (text.startsWith(".d ")) {
                        helper_main.switchToActivity(Bookmarks.this, Browser.class, "https://duckduckgo.com/?q=" + subStr, true);
                    } else  if (text.startsWith(".y ")) {
                        helper_main.switchToActivity(Bookmarks.this, Browser.class, "https://www.youtube.com/results?search_query=" + subStr, true);
                    } else {
                        helper_main.switchToActivity(Bookmarks.this, Browser.class, searchEngine + text, true);
                    }
                    return true;
                } else {
                    return false;
                }
            }
        });

        listView = (ListView)findViewById(R.id.list);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                @SuppressWarnings("unchecked")
                HashMap<String,String> map = (HashMap<String,String>)listView.getItemAtPosition(position);
                helper_main.switchToActivity(Bookmarks.this, Browser.class, map.get("url"), true);
            }
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

                @SuppressWarnings("unchecked")
                HashMap<String,String> map = (HashMap<String,String>)listView.getItemAtPosition(position);
                final String seqnoStr = map.get("seqno");
                final String title = map.get("title");
                final String url = map.get("url");

                final CharSequence[] options = {
                        getString(R.string.bookmark_edit_title),
                        getString(R.string.bookmark_remove_bookmark)};
                new AlertDialog.Builder(Bookmarks.this)
                        .setItems(options, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int item) {
                                if (options[item].equals(getString(R.string.bookmark_edit_title))) {
                                    final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(Bookmarks.this);
                                    sharedPref.edit()
                                            .putInt("keyboard", 2)
                                            .putString("url", url)
                                            .putString("seqno", seqnoStr)
                                            .apply();
                                    invalidateOptionsMenu();

                                    (new Handler()).postDelayed(new Runnable() {
                                        public void run() {
                                            editText.requestFocus();
                                            helper_main.showKeyboard(Bookmarks.this, editText);
                                            editText.setText(title);
                                        }
                                    }, 200);

                                    editText.setHint(getString(R.string.app_search_hint_bookmark));
                                    editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                                        @Override
                                        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                                            if ( (actionId == EditorInfo.IME_ACTION_SEARCH) || ((event.getKeyCode() == KeyEvent.KEYCODE_ENTER) && (event.getAction() == KeyEvent.ACTION_DOWN ))){

                                                try {

                                                    final Database_Bookmarks db = new Database_Bookmarks(Bookmarks.this);
                                                    String inputTag = editText.getText().toString().trim();

                                                    db.deleteBookmark((Integer.parseInt(seqnoStr)));
                                                    db.addBookmark(inputTag, url);
                                                    db.close();
                                                    setBookmarkList();
                                                    Snackbar.make(listView, R.string.bookmark_added, Snackbar.LENGTH_SHORT).show();
                                                    editText.setHint(getString(R.string.app_search_hint));
                                                    editText.setText("");
                                                    editText.clearFocus();
                                                    InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                                                    imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);

                                                    final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(Bookmarks.this);
                                                    sharedPref.edit()
                                                            .putInt("keyboard", 0)
                                                            .apply();
                                                    invalidateOptionsMenu();

                                                } catch (Exception e) {
                                                    e.printStackTrace();
                                                }
                                                return true;
                                            } else {
                                                return false;
                                            }
                                        }
                                    });
                                }

                                if (options[item].equals(getString(R.string.bookmark_remove_bookmark))) {
                                    try {
                                        Database_Bookmarks db = new Database_Bookmarks(Bookmarks.this);
                                        final int count = db.getRecordCount();
                                        db.close();

                                        if (count == 1) {
                                            Snackbar snackbar = Snackbar
                                                    .make(listView, R.string.bookmark_remove_cannot, Snackbar.LENGTH_LONG);
                                            snackbar.show();

                                        } else {
                                            Snackbar snackbar = Snackbar
                                                    .make(listView, R.string.bookmark_remove_confirmation, Snackbar.LENGTH_LONG)
                                                    .setAction(R.string.toast_yes, new View.OnClickListener() {
                                                        @Override
                                                        public void onClick(View view) {
                                                            try {
                                                                Database_Bookmarks db = new Database_Bookmarks(Bookmarks.this);
                                                                db.deleteBookmark(Integer.parseInt(seqnoStr));
                                                                db.close();
                                                                setBookmarkList();
                                                            } catch (PackageManager.NameNotFoundException e) {
                                                                e.printStackTrace();
                                                            }
                                                        }
                                                    });
                                            snackbar.show();
                                        }

                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }

                            }
                        }).show();

                return true;
            }
        });
        setBookmarkList();

        boolean show = sharedPref.getBoolean("help_notShow", true);

        if (show){
            final AlertDialog.Builder dialog = new AlertDialog.Builder(Bookmarks.this)
                    .setTitle(R.string.dialog_help_title)
                    .setMessage(helper_main.textSpannable(getString(R.string.dialog_help)))
                    .setPositiveButton(getString(R.string.toast_yes), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            final AlertDialog d = new AlertDialog.Builder(Bookmarks.this)
                                    .setMessage(helper_main.textSpannable(getString(R.string.help_text)))
                                    .setPositiveButton(getString(R.string.toast_yes),
                                            new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int id) {
                                                    dialog.cancel();
                                                }
                                            }).show();
                            d.show();
                        }
                    })
                    .setNegativeButton(getString(R.string.toast_notAgain), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplication());
                            dialog.cancel();
                            sharedPref.edit()
                                    .putBoolean("help_notShow", false)
                                    .apply();
                        }
                    });
            dialog.show();
        }
    }

    private void setBookmarkList() {

        ArrayList<HashMap<String,String>> mapList = new ArrayList<>();

        try {
            Database_Bookmarks db = new Database_Bookmarks(Bookmarks.this);
            ArrayList<String[]> bookmarkList = new ArrayList<>();
            db.getBookmarks(bookmarkList);
            if (bookmarkList.size() == 0) {
                db.loadInitialData();
                db.getBookmarks(bookmarkList);
            }
            db.close();

            for (String[] strAry : bookmarkList) {
                HashMap<String, String> map = new HashMap<>();
                map.put("seqno", strAry[0]);
                map.put("title", strAry[1]);
                map.put("url", strAry[2]);
                mapList.add(map);
            }

            SimpleAdapter simpleAdapter = new SimpleAdapter(
                    Bookmarks.this,
                    mapList,
                    R.layout.list_item,
                    new String[] {"title", "url"},
                    new int[] {R.id.textView_title, R.id.textView_des}
            );

            listView.setAdapter(simpleAdapter);

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MenuItem saveBookmark = menu.findItem(R.id.action_save_bookmark);
        MenuItem search = menu.findItem(R.id.action_search);
        MenuItem search2 = menu.findItem(R.id.action_search2);
        MenuItem history = menu.findItem(R.id.action_history);
        MenuItem save = menu.findItem(R.id.action_save);
        MenuItem share = menu.findItem(R.id.action_share);
        MenuItem searchSite = menu.findItem(R.id.action_searchSite);
        MenuItem downloads = menu.findItem(R.id.action_downloads);
        MenuItem settings = menu.findItem(R.id.action_settings);
        MenuItem prev = menu.findItem(R.id.action_prev);
        MenuItem next = menu.findItem(R.id.action_next);
        MenuItem cancel = menu.findItem(R.id.action_cancel);
        MenuItem pass = menu.findItem(R.id.action_pass);
        MenuItem toggle = menu.findItem(R.id.action_toggle);
        MenuItem search3 = menu.findItem(R.id.action_search3);
        MenuItem help = menu.findItem(R.id.action_help);

        if (sharedPref.getInt("keyboard", 0) == 0) { //could be button state or..?
            saveBookmark.setVisible(false);
            search.setVisible(true);
            search2.setVisible(false);
            history.setVisible(true);
            save.setVisible(false);
            share.setVisible(false);
            searchSite.setVisible(false);
            downloads.setVisible(true);
            settings.setVisible(true);
            prev.setVisible(false);
            next.setVisible(false);
            cancel.setVisible(false);
            pass.setVisible(true);
            toggle.setVisible(false);
            search3.setVisible(false);
            help.setVisible(true);
        } else if (sharedPref.getInt("keyboard", 0) == 1) {
            saveBookmark.setVisible(false);
            search.setVisible(false);
            search2.setVisible(true);
            history.setVisible(false);
            save.setVisible(false);
            share.setVisible(false);
            searchSite.setVisible(false);
            downloads.setVisible(false);
            settings.setVisible(false);
            prev.setVisible(true);
            next.setVisible(true);
            cancel.setVisible(true);
            pass.setVisible(false);
            toggle.setVisible(false);
            search3.setVisible(false);
            help.setVisible(false);
        } else if (sharedPref.getInt("keyboard", 0) == 2) {
            saveBookmark.setVisible(true);
            search.setVisible(false);
            search2.setVisible(false);
            history.setVisible(false);
            save.setVisible(false);
            share.setVisible(false);
            searchSite.setVisible(false);
            downloads.setVisible(false);
            settings.setVisible(false);
            prev.setVisible(false);
            next.setVisible(false);
            cancel.setVisible(true);
            pass.setVisible(false);
            toggle.setVisible(false);
            search3.setVisible(false);
            help.setVisible(false);
        } else if (sharedPref.getInt("keyboard", 0) == 3) {
            saveBookmark.setVisible(false);
            search.setVisible(true);
            search2.setVisible(false);
            history.setVisible(false);
            save.setVisible(false);
            share.setVisible(false);
            searchSite.setVisible(false);
            downloads.setVisible(false);
            settings.setVisible(false);
            prev.setVisible(false);
            next.setVisible(false);
            cancel.setVisible(true);
            pass.setVisible(false);
            toggle.setVisible(false);
            search3.setVisible(true);
            help.setVisible(false);
        }

        return true; // this is important to call so that new menu is shown
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_browser, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_search) {

            editText.setHint(R.string.app_search_hint);
            editText.hasFocus();
            String text = editText.getText().toString();

            if (text.length() > 3) {
                subStr=text.substring(3);
            }

            if (text.isEmpty()) {
                editText.requestFocus();
                editText.setText("");
                helper_main.showKeyboard(Bookmarks.this, editText);
                sharedPref.edit()
                        .putInt("keyboard", 3)
                        .apply();
                Bookmarks.this.invalidateOptionsMenu();
            } else {
                if (text.startsWith("www")) {
                    helper_main.switchToActivity(Bookmarks.this, Browser.class, "http://" + text, true);
                } else if (text.contains("http")) {
                    helper_main.switchToActivity(Bookmarks.this, Browser.class, text, true);
                } else if (text.contains(".w ")) {
                    helper_main.switchToActivity(Bookmarks.this, Browser.class, "https://" + wikiLang + ".wikipedia.org/wiki/Spezial:Suche?search=" + subStr, true);
                } else if (text.startsWith(".f ")) {
                    helper_main.switchToActivity(Bookmarks.this, Browser.class, "https://www.flickr.com/search/?advanced=1&license=2%2C3%2C4%2C5%2C6%2C9&text=" + subStr, true);
                } else  if (text.startsWith(".m ")) {
                    helper_main.switchToActivity(Bookmarks.this, Browser.class, "https://metager.de/meta/meta.ger3?focus=web&eingabe=" + subStr, true);
                } else if (text.startsWith(".g ")) {
                    helper_main.switchToActivity(Bookmarks.this, Browser.class, "https://github.com/search?utf8=✓&q=" + subStr, true);
                } else  if (text.startsWith(".s ")) {
                    helper_main.switchToActivity(Bookmarks.this, Browser.class, "https://startpage.com/do/search?query=" + subStr, true);
                } else if (text.startsWith(".G ")) {
                    helper_main.switchToActivity(Bookmarks.this, Browser.class, "https://www.google.com/search?&q=" + subStr, true);
                } else  if (text.startsWith(".d ")) {
                    helper_main.switchToActivity(Bookmarks.this, Browser.class, "https://duckduckgo.com/?q=" + subStr, true);
                } else  if (text.startsWith(".y ")) {
                    helper_main.switchToActivity(Bookmarks.this, Browser.class, "https://www.youtube.com/results?search_query=" + subStr, true);
                } else {
                    helper_main.switchToActivity(Bookmarks.this, Browser.class, searchEngine + text, true);
                }
            }
        }

        if (id == R.id.action_search3) {
            helper_editText.editText_searchWeb(editText, Bookmarks.this);
        }

        if (id == R.id.action_settings) {
            sharedPref.edit().putString("lastActivity", "settings").apply();
            helper_main.switchToActivity(Bookmarks.this, Activity_settings.class, "", true);
        }

        if (id == R.id.action_history) {
            helper_main.switchToActivity(Bookmarks.this, Popup_history.class, "", false);
        }

        if (id == R.id.action_downloads) {
            String startDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
            helper_main.openFilePicker(Bookmarks.this, listView, startDir);
        }

        if (id == R.id.action_pass) {
            helper_main.switchToActivity(Bookmarks.this, Popup_pass.class, "", false);
        }

        if (id == R.id.action_cancel) {
            sharedPref.edit()
                    .putInt("keyboard", 0)
                    .apply();
            invalidateOptionsMenu();
            editText.setHint(getString(R.string.app_search_hint_bookmark));
            editText.setText("");
            editText.setHint(R.string.app_search_hint);
            helper_editText.editText_Touch(editText, Bookmarks.this);
            helper_editText.editText_FocusChange(editText, Bookmarks.this);
            helper_main.hideKeyboard(Bookmarks.this, editText);
        }

        if (id == R.id.action_save_bookmark) {

            try {

                final Database_Bookmarks db = new Database_Bookmarks(Bookmarks.this);
                String inputTag = editText.getText().toString().trim();

                db.deleteBookmark((Integer.parseInt(sharedPref.getString("seqno", ""))));
                db.addBookmark(inputTag, sharedPref.getString("url", ""));
                db.close();
                setBookmarkList();
                Snackbar.make(listView, R.string.bookmark_added, Snackbar.LENGTH_SHORT).show();
                editText.setText("");
                editText.clearFocus();
                editText.setHint(getString(R.string.app_search_hint));
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);

                sharedPref.edit()
                        .putInt("keyboard", 0)
                        .putString("url", "")
                        .putString("seqno", "")
                        .apply();
                invalidateOptionsMenu();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (id == R.id.action_help) {
            final AlertDialog d = new AlertDialog.Builder(Bookmarks.this)
                    .setMessage(helper_main.textSpannable(getString(R.string.help_text)))
                    .setPositiveButton(getString(R.string.toast_yes),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            }).show();
            d.show();
        }
        return super.onOptionsItemSelected(item);
    }
}
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
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
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
import de.baumann.browser.helper.helper_editText;
import de.baumann.browser.helper.helpers;
import de.baumann.browser.popups.Popup_history;

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
                .putInt("keyboard", 0)
                .putString("url", "")
                .putString("seqno", "")
                .apply();
        invalidateOptionsMenu();
        sharedPref.getInt("keyboard", 0);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        editText = (EditText) findViewById(R.id.editText);
        editText.setHint(getString(R.string.app_search_hint));
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
                    if (text.contains("http")) {
                        helpers.switchToActivity(Bookmarks.this, Browser.class, text, true);
                    } else if (text.contains(".w ")) {
                        helpers.switchToActivity(Bookmarks.this, Browser.class, "https://" + wikiLang + ".wikipedia.org/wiki/Spezial:Suche?search=" + subStr, true);
                    } else if (text.startsWith(".f ")) {
                        helpers.switchToActivity(Bookmarks.this, Browser.class, "https://www.flickr.com/search/?advanced=1&license=2%2C3%2C4%2C5%2C6%2C9&text=" + subStr, true);
                    } else  if (text.startsWith(".m ")) {
                        helpers.switchToActivity(Bookmarks.this, Browser.class, "https://metager.de/meta/meta.ger3?focus=web&eingabe=" + subStr, true);
                    } else if (text.startsWith(".g ")) {
                        helpers.switchToActivity(Bookmarks.this, Browser.class, "https://github.com/search?utf8=✓&q=" + subStr, true);
                    } else  if (text.startsWith(".s ")) {
                        helpers.switchToActivity(Bookmarks.this, Browser.class, "https://startpage.com/do/search?query=" + subStr, true);
                    } else if (text.startsWith(".G ")) {
                        helpers.switchToActivity(Bookmarks.this, Browser.class, "https://www.google.com/search?&q=" + subStr, true);
                    } else  if (text.startsWith(".d ")) {
                        helpers.switchToActivity(Bookmarks.this, Browser.class, "https://duckduckgo.com/?q=" + subStr, true);
                    } else {
                        helpers.switchToActivity(Bookmarks.this, Browser.class, searchEngine + text, true);
                    }
                    editText.setText(text);
                    editText.clearFocus();
                    InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
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
                helpers.switchToActivity(Bookmarks.this, Browser.class, map.get("url"), true);
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
                                            helpers.showKeyboard(Bookmarks.this, editText);
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
        MenuItem clear = menu.findItem(R.id.action_clear);
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

        if (sharedPref.getInt("keyboard", 0) == 0) { //could be button state or..?
            saveBookmark.setVisible(false);
            clear.setVisible(false);
            search.setVisible(true);
            search2.setVisible(false);
            history.setVisible(true);
            save.setVisible(false);
            share.setVisible(false);
            searchSite.setVisible(false);
            downloads.setVisible(true);
            settings.setVisible(false);
            prev.setVisible(false);
            next.setVisible(false);
            cancel.setVisible(false);
        } else if (sharedPref.getInt("keyboard", 0) == 1) {
            saveBookmark.setVisible(false);
            clear.setVisible(false);
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
        } else if (sharedPref.getInt("keyboard", 0) == 2) {
            saveBookmark.setVisible(true);
            clear.setVisible(true);
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

            editText.hasFocus();
            String text = editText.getText().toString();

            if (text.length() > 3) {
                subStr=text.substring(3);
            }

            if (text.isEmpty()) {
                editText.requestFocus();
                helpers.showKeyboard(Bookmarks.this, editText);
            } else {
                if (text.contains("http")) {
                    helpers.switchToActivity(Bookmarks.this, Browser.class, text, true);
                } else if (text.contains(".w ")) {
                    helpers.switchToActivity(Bookmarks.this, Browser.class, "https://" + wikiLang + ".wikipedia.org/wiki/Spezial:Suche?search=" + subStr, true);
                } else if (text.startsWith(".f ")) {
                    helpers.switchToActivity(Bookmarks.this, Browser.class, "https://www.flickr.com/search/?advanced=1&license=2%2C3%2C4%2C5%2C6%2C9&text=" + subStr, true);
                } else  if (text.startsWith(".m ")) {
                    helpers.switchToActivity(Bookmarks.this, Browser.class, "https://metager.de/meta/meta.ger3?focus=web&eingabe=" + subStr, true);
                } else if (text.startsWith(".g ")) {
                    helpers.switchToActivity(Bookmarks.this, Browser.class, "https://github.com/search?utf8=✓&q=" + subStr, true);
                } else  if (text.startsWith(".s ")) {
                    helpers.switchToActivity(Bookmarks.this, Browser.class, "https://startpage.com/do/search?query=" + subStr, true);
                } else if (text.startsWith(".G ")) {
                    helpers.switchToActivity(Bookmarks.this, Browser.class, "https://www.google.com/search?&q=" + subStr, true);
                } else  if (text.startsWith(".d ")) {
                    helpers.switchToActivity(Bookmarks.this, Browser.class, "https://duckduckgo.com/?q=" + subStr, true);
                } else {
                    helpers.switchToActivity(Bookmarks.this, Browser.class, searchEngine + text, true);
                }
            }
        }

        if (id == R.id.action_history) {
            helpers.switchToActivity(Bookmarks.this, Popup_history.class, "", false);
        }

        if (id == R.id.action_downloads) {
            startActivity(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS));
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
            helpers.hideKeyboard(Bookmarks.this, editText);
        }

        if (id == R.id.action_clear) {
            editText.setText("");
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
        return super.onOptionsItemSelected(item);
    }
}
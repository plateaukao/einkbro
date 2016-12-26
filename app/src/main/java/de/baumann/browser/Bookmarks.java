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
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
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
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import de.baumann.browser.databases.Database_Bookmarks;
import de.baumann.browser.databases.Database_ReadLater;
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

        setContentView(R.layout.activity_bookmarks);
        helper_main.onStart(Bookmarks.this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        PreferenceManager.setDefaultValues(this, R.xml.user_settings, false);
        PreferenceManager.setDefaultValues(this, R.xml.user_settings_search, false);

        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPref.edit()
                .putString("started", "")
                .putInt("keyboard", 0)
                .putString("url", "")
                .putString("seqno", "")
                .apply();
        invalidateOptionsMenu();
        sharedPref.getInt("keyboard", 0);

        editText = (EditText) findViewById(R.id.editText);
        helper_editText.editText_Touch_Bookmark(editText, Bookmarks.this);
        helper_editText.editText_FocusChange(editText, Bookmarks.this);
        String start = getString(R.string.app_name) + "    ";
        editText.setText(start);

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
                        helper_main.switchToActivity(Bookmarks.this, Browser.class, "http://" + text, false);
                    } else if (text.contains("http")) {
                        helper_main.switchToActivity(Bookmarks.this, Browser.class, text, false);
                    } else if (text.contains(".w ")) {
                        helper_main.switchToActivity(Bookmarks.this, Browser.class, "https://" + wikiLang + ".wikipedia.org/wiki/Spezial:Suche?search=" + subStr, false);
                    } else if (text.startsWith(".f ")) {
                        helper_main.switchToActivity(Bookmarks.this, Browser.class, "https://www.flickr.com/search/?advanced=1&license=2%2C3%2C4%2C5%2C6%2C9&text=" + subStr, false);
                    } else  if (text.startsWith(".m ")) {
                        helper_main.switchToActivity(Bookmarks.this, Browser.class, "https://metager.de/meta/meta.ger3?focus=web&eingabe=" + subStr, false);
                    } else if (text.startsWith(".g ")) {
                        helper_main.switchToActivity(Bookmarks.this, Browser.class, "https://github.com/search?utf8=✓&q=" + subStr, false);
                    } else  if (text.startsWith(".s ")) {
                        if (Locale.getDefault().getLanguage().contentEquals("de")){
                            helper_main.switchToActivity(Bookmarks.this, Browser.class, "https://startpage.com/do/search?query=\" + subStr + \"&lui=deutsch&l=deutsch", false);
                        } else {
                            helper_main.switchToActivity(Bookmarks.this, Browser.class, "https://startpage.com/do/search?query=" + subStr, false);
                        }
                    } else if (text.startsWith(".G ")) {
                        if (Locale.getDefault().getLanguage().contentEquals("de")){
                            helper_main.switchToActivity(Bookmarks.this, Browser.class, "https://www.google.de/search?&q=" + subStr, false);
                        } else {
                            helper_main.switchToActivity(Bookmarks.this, Browser.class, "https://www.google.com/search?&q=" + subStr, false);
                        }
                    } else  if (text.startsWith(".d ")) {
                        if (Locale.getDefault().getLanguage().contentEquals("de")){
                            helper_main.switchToActivity(Bookmarks.this, Browser.class, "https://duckduckgo.com/?q=" + subStr + "&kl=de-de&kad=de_DE&k1=-1&kaj=m&kam=osm&kp=-1&kak=-1&kd=1&t=h_&ia=web", false);
                        } else {
                            helper_main.switchToActivity(Bookmarks.this, Browser.class, "https://duckduckgo.com/?q=" + subStr, false);
                        }
                    } else  if (text.startsWith(".y ")) {
                        if (Locale.getDefault().getLanguage().contentEquals("de")){
                            helper_main.switchToActivity(Bookmarks.this, Browser.class, "https://www.youtube.com/results?hl=de&gl=DE&search_query=" + subStr, false);
                        } else {
                            helper_main.switchToActivity(Bookmarks.this, Browser.class, "https://www.youtube.com/results?search_query=" + subStr, false);
                        }
                    } else {
                        if (searchEngine.contains("https://duckduckgo.com/?q=")) {
                            if (Locale.getDefault().getLanguage().contentEquals("de")){
                                helper_main.switchToActivity(Bookmarks.this, Browser.class, "https://duckduckgo.com/?q=" + text + "&kl=de-de&kad=de_DE&k1=-1&kaj=m&kam=osm&kp=-1&kak=-1&kd=1&t=h_&ia=web", false);
                            } else {
                                helper_main.switchToActivity(Bookmarks.this, Browser.class, "https://duckduckgo.com/?q=" + text, false);
                            }
                        } else if (searchEngine.contains("https://metager.de/meta/meta.ger3?focus=web&eingabe=")) {
                            if (Locale.getDefault().getLanguage().contentEquals("de")){
                                helper_main.switchToActivity(Bookmarks.this, Browser.class, "https://metager.de/meta/meta.ger3?focus=web&eingabe=" + text, false);
                            } else {
                                helper_main.switchToActivity(Bookmarks.this, Browser.class, "https://metager.de/meta/meta.ger3?focus=web&eingabe=" + text +"&focus=web&encoding=utf8&lang=eng", false);
                            }
                        } else if (searchEngine.contains("https://startpage.com/do/search?query=")) {
                            if (Locale.getDefault().getLanguage().contentEquals("de")){
                                helper_main.switchToActivity(Bookmarks.this, Browser.class, "https://startpage.com/do/search?query=" + text + "&lui=deutsch&l=deutsch", false);
                            } else {
                                helper_main.switchToActivity(Bookmarks.this, Browser.class, "https://startpage.com/do/search?query=" + text, false);
                            }
                        }else {
                            helper_main.switchToActivity(Bookmarks.this, Browser.class, searchEngine + text, false);
                        }
                    }
                    (new Handler()).postDelayed(new Runnable() {
                        public void run() {
                            finish();
                        }
                    }, 500);
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
                        getString(R.string.menu_share),
                        getString(R.string.menu_save),
                        getString(R.string.bookmark_remove_bookmark)};
                new AlertDialog.Builder(Bookmarks.this)
                        .setPositiveButton(R.string.toast_cancel, new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int whichButton) {
                                dialog.cancel();
                            }
                        })
                        .setItems(options, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int item) {
                                if (options[item].equals(getString(R.string.bookmark_edit_title))) {

                                    helper_editText.showKeyboard(Bookmarks.this, editText, 2, title, getString(R.string.app_search_hint_bookmark));

                                    final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(Bookmarks.this);
                                    sharedPref.edit()
                                            .putString("url", url)
                                            .putString("seqno", seqnoStr)
                                            .apply();

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
                                                    editText.clearFocus();

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
                                if (options[item].equals(getString(R.string.menu_share))) {
                                    final CharSequence[] options = {
                                            getString(R.string.menu_share_link),
                                            getString(R.string.menu_share_link_copy)};
                                    new AlertDialog.Builder(Bookmarks.this)
                                            .setPositiveButton(R.string.toast_cancel, new DialogInterface.OnClickListener() {

                                                public void onClick(DialogInterface dialog, int whichButton) {
                                                    dialog.cancel();
                                                }
                                            })
                                            .setItems(options, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int item) {
                                                    if (options[item].equals(getString(R.string.menu_share_link))) {
                                                        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                                                        sharingIntent.setType("text/plain");
                                                        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, title);
                                                        sharingIntent.putExtra(Intent.EXTRA_TEXT, url);
                                                        startActivity(Intent.createChooser(sharingIntent, (getString(R.string.app_share_link))));
                                                    }
                                                    if (options[item].equals(getString(R.string.menu_share_link_copy))) {
                                                        ClipboardManager clipboard = (ClipboardManager) Bookmarks.this.getSystemService(Context.CLIPBOARD_SERVICE);
                                                        clipboard.setPrimaryClip(ClipData.newPlainText("text", url));
                                                        Snackbar.make(listView, R.string.context_linkCopy_toast, Snackbar.LENGTH_SHORT).show();
                                                    }
                                                }
                                            }).show();
                                }
                                if (options[item].equals(getString(R.string.menu_save))) {
                                    final CharSequence[] options = {
                                            getString(R.string.menu_save_readLater),
                                            getString(R.string.menu_save_pass),
                                            getString(R.string.menu_createShortcut)};
                                    new AlertDialog.Builder(Bookmarks.this)
                                            .setPositiveButton(R.string.toast_cancel, new DialogInterface.OnClickListener() {

                                                public void onClick(DialogInterface dialog, int whichButton) {
                                                    dialog.cancel();
                                                }
                                            })
                                            .setItems(options, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int item) {
                                                    if (options[item].equals(getString(R.string.menu_save_pass))) {
                                                        helper_editText.editText_savePass(Bookmarks.this, listView, title, url);
                                                    }
                                                    if (options[item].equals(getString(R.string.menu_save_readLater))) {
                                                        try {
                                                            final Database_ReadLater db = new Database_ReadLater(Bookmarks.this);
                                                            db.addBookmark(title, url);
                                                            db.close();
                                                            Snackbar.make(listView, R.string.readLater_added, Snackbar.LENGTH_SHORT).show();
                                                        } catch (Exception e) {
                                                            e.printStackTrace();
                                                        }
                                                    }
                                                    if (options[item].equals (getString(R.string.menu_createShortcut))) {
                                                        Intent i = new Intent();
                                                        i.setAction(Intent.ACTION_VIEW);
                                                        i.setClassName(Bookmarks.this, "de.baumann.browser.Browser");
                                                        i.setData(Uri.parse(url));

                                                        Intent shortcut = new Intent();
                                                        shortcut.putExtra("android.intent.extra.shortcut.INTENT", i);
                                                        shortcut.putExtra("android.intent.extra.shortcut.NAME", "THE NAME OF SHORTCUT TO BE SHOWN");
                                                        shortcut.putExtra(Intent.EXTRA_SHORTCUT_NAME, title);
                                                        shortcut.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(Bookmarks.this.getApplicationContext(), R.mipmap.ic_launcher));
                                                        shortcut.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
                                                        Bookmarks.this.sendBroadcast(shortcut);
                                                        Snackbar.make(listView, R.string.menu_createShortcut_success, Snackbar.LENGTH_SHORT).show();
                                                    }
                                                }
                                            }).show();
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

    @Override
    public void onBackPressed() {
        helper_main.isClosed(Bookmarks.this);
        finishAffinity();
    }

    @Override
    protected void onPause() {
        super.onPause();    //To change body of overridden methods use File | Settings | File Templates.
        helper_main.isOpened(Bookmarks.this);
    }

    @Override
    protected void onResume() {
        super.onResume();    //To change body of overridden methods use File | Settings | File Templates.
        helper_main.isOpened(Bookmarks.this);
    }

    @Override
    protected void onStop() {
        super.onStop();    //To change body of overridden methods use File | Settings | File Templates.
        helper_main.isClosed(Bookmarks.this);
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
        String start = getString(R.string.app_name) + "    ";

        if (id == R.id.action_search) {

            String text = editText.getText().toString();

            if (text.length() > 3) {
                subStr=text.substring(3);
            }

            if (text.isEmpty() || text.equals(start)) {
                new Handler().postDelayed(new Runnable() {
                    public void run() {
                        helper_editText.showKeyboard(Bookmarks.this, editText, 3, "", getString(R.string.app_search_hint));
                    }
                }, 200);
            } else {
                if (text.startsWith("www")) {
                    helper_main.switchToActivity(Bookmarks.this, Browser.class, "http://" + text, false);
                } else if (text.contains("http")) {
                    helper_main.switchToActivity(Bookmarks.this, Browser.class, text, false);
                } else if (text.contains(".w ")) {
                    helper_main.switchToActivity(Bookmarks.this, Browser.class, "https://" + wikiLang + ".wikipedia.org/wiki/Spezial:Suche?search=" + subStr, false);
                } else if (text.startsWith(".f ")) {
                    helper_main.switchToActivity(Bookmarks.this, Browser.class, "https://www.flickr.com/search/?advanced=1&license=2%2C3%2C4%2C5%2C6%2C9&text=" + subStr, false);
                } else  if (text.startsWith(".m ")) {
                    helper_main.switchToActivity(Bookmarks.this, Browser.class, "https://metager.de/meta/meta.ger3?focus=web&eingabe=" + subStr, false);
                } else if (text.startsWith(".g ")) {
                    helper_main.switchToActivity(Bookmarks.this, Browser.class, "https://github.com/search?utf8=✓&q=" + subStr, false);
                } else  if (text.startsWith(".s ")) {
                    if (Locale.getDefault().getLanguage().contentEquals("de")){
                        helper_main.switchToActivity(Bookmarks.this, Browser.class, "https://startpage.com/do/search?query=\" + subStr + \"&lui=deutsch&l=deutsch", false);
                    } else {
                        helper_main.switchToActivity(Bookmarks.this, Browser.class, "https://startpage.com/do/search?query=" + subStr, false);
                    }
                } else if (text.startsWith(".G ")) {
                    if (Locale.getDefault().getLanguage().contentEquals("de")){
                        helper_main.switchToActivity(Bookmarks.this, Browser.class, "https://www.google.de/search?&q=" + subStr, false);
                    } else {
                        helper_main.switchToActivity(Bookmarks.this, Browser.class, "https://www.google.com/search?&q=" + subStr, false);
                    }
                } else  if (text.startsWith(".d ")) {
                    if (Locale.getDefault().getLanguage().contentEquals("de")){
                        helper_main.switchToActivity(Bookmarks.this, Browser.class, "https://duckduckgo.com/?q=" + subStr + "&kl=de-de&kad=de_DE&k1=-1&kaj=m&kam=osm&kp=-1&kak=-1&kd=1&t=h_&ia=web", false);
                    } else {
                        helper_main.switchToActivity(Bookmarks.this, Browser.class, "https://duckduckgo.com/?q=" + subStr, false);
                    }
                } else  if (text.startsWith(".y ")) {
                    if (Locale.getDefault().getLanguage().contentEquals("de")){
                        helper_main.switchToActivity(Bookmarks.this, Browser.class, "https://www.youtube.com/results?hl=de&gl=DE&search_query=" + subStr, false);
                    } else {
                        helper_main.switchToActivity(Bookmarks.this, Browser.class, "https://www.youtube.com/results?search_query=" + subStr, false);
                    }
                } else {
                    if (searchEngine.contains("https://duckduckgo.com/?q=")) {
                        if (Locale.getDefault().getLanguage().contentEquals("de")){
                            helper_main.switchToActivity(Bookmarks.this, Browser.class, "https://duckduckgo.com/?q=" + text + "&kl=de-de&kad=de_DE&k1=-1&kaj=m&kam=osm&kp=-1&kak=-1&kd=1&t=h_&ia=web", false);
                        } else {
                            helper_main.switchToActivity(Bookmarks.this, Browser.class, "https://duckduckgo.com/?q=" + text, false);
                        }
                    } else if (searchEngine.contains("https://metager.de/meta/meta.ger3?focus=web&eingabe=")) {
                        if (Locale.getDefault().getLanguage().contentEquals("de")){
                            helper_main.switchToActivity(Bookmarks.this, Browser.class, "https://metager.de/meta/meta.ger3?focus=web&eingabe=" + text, false);
                        } else {
                            helper_main.switchToActivity(Bookmarks.this, Browser.class, "https://metager.de/meta/meta.ger3?focus=web&eingabe=" + text +"&focus=web&encoding=utf8&lang=eng", false);
                        }
                    } else if (searchEngine.contains("https://startpage.com/do/search?query=")) {
                        if (Locale.getDefault().getLanguage().contentEquals("de")){
                            helper_main.switchToActivity(Bookmarks.this, Browser.class, "https://startpage.com/do/search?query=" + text + "&lui=deutsch&l=deutsch", false);
                        } else {
                            helper_main.switchToActivity(Bookmarks.this, Browser.class, "https://startpage.com/do/search?query=" + text, false);
                        }
                    }else {
                        helper_main.switchToActivity(Bookmarks.this, Browser.class, searchEngine + text, false);
                    }
                }
                (new Handler()).postDelayed(new Runnable() {
                    public void run() {
                        finish();
                    }
                }, 500);
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
            helper_editText.hideKeyboard(Bookmarks.this, editText, 0, start, getString(R.string.app_search_hint));
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
                editText.clearFocus();

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

    private void setBookmarkList() {
        ArrayList<HashMap<String,String>> mapList = new ArrayList<>();

        try {
            Database_Bookmarks db = new Database_Bookmarks(Bookmarks.this);
            ArrayList<String[]> bookmarkList = new ArrayList<>();
            db.getBookmarks(bookmarkList, Bookmarks.this);
            if (bookmarkList.size() == 0) {
                db.loadInitialData();
                db.getBookmarks(bookmarkList, Bookmarks.this);
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
}
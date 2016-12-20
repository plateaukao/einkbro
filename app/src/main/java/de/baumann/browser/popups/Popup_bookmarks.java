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

import android.app.Activity;
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
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.util.ArrayList;
import java.util.HashMap;

import de.baumann.browser.Browser;
import de.baumann.browser.R;
import de.baumann.browser.databases.Database_Bookmarks;
import de.baumann.browser.databases.Database_ReadLater;
import de.baumann.browser.helper.helper_editText;
import de.baumann.browser.helper.helper_main;

public class Popup_bookmarks extends Activity {

    private ListView listView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_popup);
        helper_main.onStart(Popup_bookmarks.this);

        PreferenceManager.setDefaultValues(this, R.xml.user_settings, false);
        PreferenceManager.setDefaultValues(this, R.xml.user_settings_search, false);
        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        Button button = (Button) findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar snackbar = Snackbar
                        .make(listView, R.string.toast_list, Snackbar.LENGTH_LONG)
                        .setAction(R.string.toast_yes, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                Popup_bookmarks.this.deleteDatabase("browser.db");
                                setBookmarkList();
                            }
                        });
                snackbar.show();
            }
        });

        ImageButton buttonSort = (ImageButton) findViewById(R.id.butSort);
        buttonSort.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(Popup_bookmarks.this);
                View dialogView = View.inflate(Popup_bookmarks.this, R.layout.dialog_sort, null);

                final CheckBox ch_title = (CheckBox) dialogView.findViewById(R.id.checkBoxTitle);
                final CheckBox ch_create = (CheckBox) dialogView.findViewById(R.id.checkBoxCreate);


                if (sharedPref.getString("sortBO", "title").equals("title")) {
                    ch_title.setChecked(true);
                } else {
                    ch_title.setChecked(false);
                }
                if (sharedPref.getString("sortBO", "title").equals("seqno")) {
                    ch_create.setChecked(true);
                } else {
                    ch_create.setChecked(false);
                }

                ch_title.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(CompoundButton buttonView,
                                                 boolean isChecked) {
                        if(isChecked){
                            ch_create.setChecked(false);
                            sharedPref.edit().putString("sortBO", "title").apply();
                            setBookmarkList();
                        }
                    }
                });
                ch_create.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(CompoundButton buttonView,
                                                 boolean isChecked) {
                        if(isChecked){
                            ch_title.setChecked(false);
                            sharedPref.edit().putString("sortBO", "seqno").apply();
                            setBookmarkList();
                        }
                    }
                });

                builder.setView(dialogView);
                builder.setTitle(R.string.action_sort);
                builder.setPositiveButton(R.string.toast_yes, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.dismiss();
                    }
                });

                final AlertDialog dialog2 = builder.create();
                // Display the custom alert dialog on interface
                dialog2.show();
            }
        });

        listView = (ListView)findViewById(R.id.list);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                @SuppressWarnings("unchecked")
                HashMap<String,String> map = (HashMap<String,String>)listView.getItemAtPosition(position);
                helper_main.switchToActivity(Popup_bookmarks.this, Browser.class, map.get("url"), true);
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
                new AlertDialog.Builder(Popup_bookmarks.this)
                        .setPositiveButton(R.string.toast_cancel, new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int whichButton) {
                                dialog.cancel();
                            }
                        })
                        .setItems(options, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int item) {
                                if (options[item].equals(getString(R.string.bookmark_edit_title))) {
                                    try {

                                        final Database_Bookmarks db = new Database_Bookmarks(Popup_bookmarks.this);

                                        AlertDialog.Builder builder = new AlertDialog.Builder(Popup_bookmarks.this);
                                        View dialogView = View.inflate(Popup_bookmarks.this, R.layout.dialog_edit_title, null);

                                        final EditText edit_title = (EditText) dialogView.findViewById(R.id.pass_title);

                                        builder.setView(dialogView);
                                        builder.setTitle(R.string.bookmark_edit_title);
                                        builder.setPositiveButton(R.string.toast_yes, new DialogInterface.OnClickListener() {

                                            public void onClick(DialogInterface dialog, int whichButton) {

                                                String inputTag = edit_title.getText().toString().trim();
                                                db.deleteBookmark((Integer.parseInt(seqnoStr)));
                                                db.addBookmark(inputTag, url);
                                                db.close();
                                                setBookmarkList();
                                                Snackbar.make(listView, R.string.bookmark_added, Snackbar.LENGTH_SHORT).show();
                                            }
                                        });
                                        builder.setNegativeButton(R.string.toast_cancel, new DialogInterface.OnClickListener() {

                                            public void onClick(DialogInterface dialog, int whichButton) {
                                                dialog.cancel();
                                            }
                                        });

                                        final AlertDialog dialog2 = builder.create();
                                        // Display the custom alert dialog on interface
                                        dialog2.show();
                                        helper_editText.showKeyboard(Popup_bookmarks.this, edit_title, 0, title, getString(R.string.app_search_hint_bookmark));

                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }

                                if (options[item].equals(getString(R.string.bookmark_remove_bookmark))) {
                                    try {
                                        Database_Bookmarks db = new Database_Bookmarks(Popup_bookmarks.this);
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
                                                                Database_Bookmarks db = new Database_Bookmarks(Popup_bookmarks.this);
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
                                    new AlertDialog.Builder(Popup_bookmarks.this)
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
                                                        ClipboardManager clipboard = (ClipboardManager) Popup_bookmarks.this.getSystemService(Context.CLIPBOARD_SERVICE);
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
                                    new AlertDialog.Builder(Popup_bookmarks.this)
                                            .setItems(options, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int item) {
                                                    if (options[item].equals(getString(R.string.menu_save_pass))) {
                                                        helper_editText.editText_savePass(Popup_bookmarks.this, listView, title, url);
                                                    }
                                                    if (options[item].equals(getString(R.string.menu_save_readLater))) {
                                                        try {
                                                            final Database_ReadLater db = new Database_ReadLater(Popup_bookmarks.this);
                                                            db.addBookmark(title, url);
                                                            db.close();
                                                            Snackbar.make(listView, R.string.readLater_added, Snackbar.LENGTH_SHORT).show();
                                                        } catch (Exception e) {
                                                            e.printStackTrace();
                                                        }
                                                    }
                                                    if (options[item].equals(getString(R.string.menu_createShortcut))) {
                                                        Intent i = new Intent();
                                                        i.setAction(Intent.ACTION_VIEW);
                                                        i.setClassName(Popup_bookmarks.this, "de.baumann.browser.Browser");
                                                        i.setData(Uri.parse(url));

                                                        Intent shortcut = new Intent();
                                                        shortcut.putExtra("android.intent.extra.shortcut.INTENT", i);
                                                        shortcut.putExtra("android.intent.extra.shortcut.NAME", "THE NAME OF SHORTCUT TO BE SHOWN");
                                                        shortcut.putExtra(Intent.EXTRA_SHORTCUT_NAME, title);
                                                        shortcut.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(Popup_bookmarks.this.getApplicationContext(), R.mipmap.ic_launcher));
                                                        shortcut.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
                                                        Popup_bookmarks.this.sendBroadcast(shortcut);
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
    }

    private void setBookmarkList() {

        ArrayList<HashMap<String,String>> mapList = new ArrayList<>();

        try {
            Database_Bookmarks db = new Database_Bookmarks(Popup_bookmarks.this);
            ArrayList<String[]> bookmarkList = new ArrayList<>();
            db.getBookmarks(bookmarkList, Popup_bookmarks.this);
            if (bookmarkList.size() == 0) {
                db.loadInitialData();
                db.getBookmarks(bookmarkList, Popup_bookmarks.this);
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
                    Popup_bookmarks.this,
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
    public void onBackPressed() {
        helper_main.isClosed(Popup_bookmarks.this);
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();    //To change body of overridden methods use File | Settings | File Templates.
        helper_main.isOpened(Popup_bookmarks.this);
    }

    @Override
    protected void onResume() {
        super.onResume();    //To change body of overridden methods use File | Settings | File Templates.
        helper_main.isOpened(Popup_bookmarks.this);
    }

    @Override
    protected void onStop() {
        super.onStop();    //To change body of overridden methods use File | Settings | File Templates.
        helper_main.isClosed(Popup_bookmarks.this);
    }
}
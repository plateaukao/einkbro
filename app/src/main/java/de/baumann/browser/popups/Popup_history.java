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
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.util.ArrayList;
import java.util.HashMap;

import de.baumann.browser.Browser;
import de.baumann.browser.R;
import de.baumann.browser.databases.Database_History;
import de.baumann.browser.helper.Activity_password;
import de.baumann.browser.helper.class_SecurePreferences;
import de.baumann.browser.helper.helper_main;

public class Popup_history extends Activity {

    private ListView listView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferenceManager.setDefaultValues(this, R.xml.user_settings, false);
        PreferenceManager.setDefaultValues(this, R.xml.user_settings_search, false);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        class_SecurePreferences sharedPrefSec = new class_SecurePreferences(Popup_history.this, "sharedPrefSec", "Ywn-YM.XK$b:/:&CsL8;=L,y4", true);
        String pw = sharedPrefSec.getString("protect_PW");

        if (pw != null  && pw.length() > 0) {
            if (sharedPref.getBoolean("isOpened", true)) {
                helper_main.switchToActivity(Popup_history.this, Activity_password.class, "", false);
            }
        }

        setContentView(R.layout.activity_popup);

        Button button = (Button) findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Popup_history.this.deleteDatabase("history.db");
                setBookmarkList();
            }
        });

        listView = (ListView)findViewById(R.id.list);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                @SuppressWarnings("unchecked")
                HashMap<String,String> map = (HashMap<String,String>)listView.getItemAtPosition(position);
                helper_main.switchToActivity(Popup_history.this, Browser.class, map.get("url"), true);
            }
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

                @SuppressWarnings("unchecked")
                HashMap<String,String> map = (HashMap<String,String>)listView.getItemAtPosition(position);
                final String seqnoStr = map.get("seqno");

                try {
                    Database_History db = new Database_History(Popup_history.this);
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
                                            Database_History db = new Database_History(Popup_history.this);
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

                return true;
            }
        });

        setBookmarkList();
    }

    private void setBookmarkList() {

        ArrayList<HashMap<String,String>> mapList = new ArrayList<>();

        try {
            Database_History db = new Database_History(Popup_history.this);
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
                    Popup_history.this,
                    mapList,
                    R.layout.list_item,
                    new String[] {"title", "url"},
                    new int[] {R.id.textView_title, R.id.textView_des}
            );

            listView.setAdapter(simpleAdapter);

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        listView.post(new Runnable(){
            public void run() {
                listView.setSelection(listView.getCount() - 1);
            }});
    }

    @Override
    public void onBackPressed() {
        helper_main.isClosed(Popup_history.this);
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();    //To change body of overridden methods use File | Settings | File Templates.
        helper_main.isOpened(Popup_history.this);
    }

    @Override
    protected void onResume() {
        super.onResume();    //To change body of overridden methods use File | Settings | File Templates.
        helper_main.isOpened(Popup_history.this);
    }

    @Override
    protected void onStop() {
        super.onStop();    //To change body of overridden methods use File | Settings | File Templates.
        helper_main.isClosed(Popup_history.this);
    }
}
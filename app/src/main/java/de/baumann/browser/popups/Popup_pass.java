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
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.util.ArrayList;
import java.util.HashMap;

import de.baumann.browser.R;
import de.baumann.browser.databases.Database_Pass;
import de.baumann.browser.helper.class_SecurePreferences;

public class Popup_pass extends Activity {

    private ListView listView = null;
    private class_SecurePreferences sharedPrefSec;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_popup);

        sharedPrefSec = new class_SecurePreferences(Popup_pass.this, "sharedPrefSec", "Ywn-YM.XK$b:/:&CsL8;=L,y4", true);

        Button button = (Button) findViewById(R.id.button);
        button.setVisibility(View.GONE);

        listView = (ListView)findViewById(R.id.list);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                @SuppressWarnings("unchecked")
                HashMap<String,String> map = (HashMap<String,String>)listView.getItemAtPosition(position);
                final String url = map.get("url");
                final String title = map.get("title");
                final String userName = sharedPrefSec.getString(url + "UN");
                final String userPW = sharedPrefSec.getString(url + "PW");

                android.content.Intent iMain = new android.content.Intent();
                iMain.setAction("pass");
                iMain.putExtra("url", url);
                iMain.putExtra("title", title);
                iMain.putExtra("userName", userName);
                iMain.putExtra("userPW", userPW);
                iMain.setClassName(Popup_pass.this, "de.baumann.browser.Browser");
                startActivity(iMain);

                finish();
            }
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

                @SuppressWarnings("unchecked")
                HashMap<String,String> map = (HashMap<String,String>)listView.getItemAtPosition(position);
                final String seqnoStr = map.get("seqno");

                try {
                    Database_Pass db = new Database_Pass(Popup_pass.this);
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
                                            Database_Pass db = new Database_Pass(Popup_pass.this);
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
            Database_Pass db = new Database_Pass(Popup_pass.this);
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
                map.put("userName", strAry[3]);
                map.put("userPW", strAry[4]);
                mapList.add(map);
            }

            SimpleAdapter simpleAdapter = new SimpleAdapter(
                    Popup_pass.this,
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
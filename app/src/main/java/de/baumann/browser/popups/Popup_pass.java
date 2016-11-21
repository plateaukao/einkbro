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
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.util.ArrayList;
import java.util.HashMap;

import de.baumann.browser.R;
import de.baumann.browser.databases.Database_Pass;
import de.baumann.browser.helper.Activity_password;
import de.baumann.browser.helper.class_SecurePreferences;
import de.baumann.browser.helper.helper_main;

public class Popup_pass extends Activity {

    private ListView listView = null;
    private class_SecurePreferences sharedPrefSec;
    private SharedPreferences sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferenceManager.setDefaultValues(this, R.xml.user_settings, false);
        PreferenceManager.setDefaultValues(this, R.xml.user_settings_search, false);
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPrefSec = new class_SecurePreferences(Popup_pass.this, "sharedPrefSec", "Ywn-YM.XK$b:/:&CsL8;=L,y4", true);

        String pw = sharedPrefSec.getString("protect_PW");

        if (pw != null  && pw.length() > 0) {
            if (sharedPref.getBoolean("isOpened", true)) {
                helper_main.switchToActivity(Popup_pass.this, Activity_password.class, "", false);
            }
        }

        setContentView(R.layout.activity_popup);
        helper_main.setOrientation(Popup_pass.this);

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
                final String userPW = map.get("userPW");
                final String userName = map.get("userName");
                final String title = map.get("title");
                final String url = map.get("url");

                final CharSequence[] options = {
                        getString(R.string.pass_copy),
                        getString(R.string.pass_edit),
                        getString(R.string.bookmark_remove_bookmark)};
                new AlertDialog.Builder(Popup_pass.this)
                        .setItems(options, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int item) {
                                if (options[item].equals(getString(R.string.pass_edit))) {

                                    try {

                                        final class_SecurePreferences sharedPrefSec = new class_SecurePreferences(Popup_pass.this, "sharedPrefSec", "Ywn-YM.XK$b:/:&CsL8;=L,y4", true);
                                        final Database_Pass db = new Database_Pass(Popup_pass.this);

                                        AlertDialog.Builder builder = new AlertDialog.Builder(Popup_pass.this);
                                        View dialogView = View.inflate(Popup_pass.this, R.layout.dialog_login, null);

                                        final EditText pass_title = (EditText) dialogView.findViewById(R.id.pass_title);
                                        pass_title.setText(title);
                                        final EditText pass_userName = (EditText) dialogView.findViewById(R.id.pass_userName);
                                        pass_userName.setText(userName);
                                        final EditText pass_userPW = (EditText) dialogView.findViewById(R.id.pass_userPW);
                                        pass_userPW.setText(userPW);

                                        builder.setView(dialogView);
                                        builder.setTitle(R.string.pass_edit);
                                        builder.setPositiveButton(R.string.toast_yes, new DialogInterface.OnClickListener() {

                                            public void onClick(DialogInterface dialog, int whichButton) {

                                                String input_pass_title = pass_title.getText().toString().trim();
                                                String input_pass_userName = pass_userName.getText().toString().trim();
                                                String input_pass_userPW = pass_userPW.getText().toString().trim();

                                                sharedPrefSec.put(url + "UN", input_pass_userName);
                                                sharedPrefSec.put(url + "PW", input_pass_userPW);
                                                sharedPrefSec.put(url + "TI", input_pass_title);

                                                db.deleteBookmark((Integer.parseInt(seqnoStr)));
                                                db.addBookmark(
                                                        sharedPrefSec.getString(url + "TI"),
                                                        url,
                                                        sharedPrefSec.getString(url + "UN"),
                                                        sharedPrefSec.getString(url + "PW"));
                                                db.close();
                                                setBookmarkList();
                                                Snackbar.make(listView, R.string.pass_success, Snackbar.LENGTH_SHORT).show();
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

                                        new Handler().postDelayed(new Runnable() {
                                            public void run() {
                                                helper_main.showKeyboard(Popup_pass.this,pass_title);
                                            }
                                        }, 200);

                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }

                                if (options[item].equals(getString(R.string.bookmark_remove_bookmark))) {
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
                                }

                                if (options[item].equals(getString(R.string.pass_copy))) {

                                    android.content.Intent iMain = new android.content.Intent();
                                    iMain.setAction("pass");
                                    iMain.putExtra("url", sharedPref.getString("pass_copy_url", ""));
                                    iMain.putExtra("title", sharedPref.getString("pass_copy_title", ""));
                                    iMain.putExtra("userName", userName);
                                    iMain.putExtra("userPW", userPW);
                                    iMain.setClassName(Popup_pass.this, "de.baumann.browser.Browser");
                                    startActivity(iMain);

                                    sharedPref.edit().putString("pass_copy_url", "").apply();
                                    sharedPref.edit().putString("pass_copy_title","").apply();

                                    finish();
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

    @Override
    public void onBackPressed() {
        helper_main.isClosed(Popup_pass.this);
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();    //To change body of overridden methods use File | Settings | File Templates.
        helper_main.isOpened(Popup_pass.this);
    }

    @Override
    protected void onResume() {
        super.onResume();    //To change body of overridden methods use File | Settings | File Templates.
        helper_main.isOpened(Popup_pass.this);
    }

    @Override
    protected void onStop() {
        super.onStop();    //To change body of overridden methods use File | Settings | File Templates.
        helper_main.isClosed(Popup_pass.this);
    }
}
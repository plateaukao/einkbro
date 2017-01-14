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
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.mobapphome.mahencryptorlib.MAHEncryptor;

import java.util.ArrayList;
import java.util.HashMap;

import de.baumann.browser.R;
import de.baumann.browser.databases.Database_Pass;
import de.baumann.browser.helper.class_SecurePreferences;
import de.baumann.browser.helper.helper_editText;
import de.baumann.browser.helper.helper_main;

public class Popup_pass extends Activity {

    private ListView listView = null;
    private SharedPreferences sharedPref;
    private class_SecurePreferences sharedPrefSec;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_popup);
        helper_main.onStart(Popup_pass.this);

        PreferenceManager.setDefaultValues(this, R.xml.user_settings, false);
        PreferenceManager.setDefaultValues(this, R.xml.user_settings_search, false);
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPrefSec = new class_SecurePreferences(Popup_pass.this, "sharedPrefSec", "Ywn-YM.XK$b:/:&CsL8;=L,y4", true);

        TextView listTitle = (TextView) findViewById(R.id.listTitle);
        listTitle.setText(R.string.app_title_passStorage);

        ImageButton butDel = (ImageButton) findViewById(R.id.butDel);
        butDel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar snackbar = Snackbar
                        .make(listView, R.string.toast_list, Snackbar.LENGTH_LONG)
                        .setAction(R.string.toast_yes, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                Popup_pass.this.deleteDatabase("pass.db");
                                setBookmarkList();
                            }
                        });
                snackbar.show();
            }
        });

        ImageButton butSort = (ImageButton) findViewById(R.id.butSort);
        butSort.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(Popup_pass.this);
                View dialogView = View.inflate(Popup_pass.this, R.layout.dialog_sort, null);

                builder.setView(dialogView);
                builder.setTitle(R.string.action_sort);
                builder.setPositiveButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.dismiss();
                    }
                });

                final AlertDialog dialog2 = builder.create();
                // Display the custom alert dialog on interface
                dialog2.show();

                final CheckBox ch_title = (CheckBox) dialogView.findViewById(R.id.checkBoxTitle);
                final CheckBox ch_create = (CheckBox) dialogView.findViewById(R.id.checkBoxCreate);

                if (sharedPref.getString("sortPS", "title").equals("title")) {
                    ch_title.setChecked(true);
                } else {
                    ch_title.setChecked(false);
                }
                if (sharedPref.getString("sortPS", "title").equals("seqno")) {
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
                            sharedPref.edit().putString("sortPS", "title").apply();
                            setBookmarkList();
                            dialog2.dismiss();
                        }
                    }
                });
                ch_create.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(CompoundButton buttonView,
                                                 boolean isChecked) {
                        if(isChecked){
                            ch_title.setChecked(false);
                            sharedPref.edit().putString("sortPS", "seqno").apply();
                            setBookmarkList();
                            dialog2.dismiss();
                        }
                    }
                });
            }
        });

        listView = (ListView)findViewById(R.id.list);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                @SuppressWarnings("unchecked")
                HashMap<String,String> map = (HashMap<String,String>)listView.getItemAtPosition(position);
                final String userPW = map.get("userPW");
                final String userName = map.get("userName");
                final String url = map.get("url");

                try {
                    MAHEncryptor mahEncryptor = MAHEncryptor.newInstance(sharedPrefSec.getString("saveDC"));
                    String decrypted_userName = mahEncryptor.decode(userName);
                    String decrypted_userPW = mahEncryptor.decode(userPW);
                    sharedPref.edit().putString("copyPW", decrypted_userPW).apply();
                    sharedPref.edit().putString("copyUN", decrypted_userName).apply();
                    sharedPref.edit().putString("openURL", "openLogin" + url).apply();
                    finishAffinity();

                } catch (Exception e) {
                    e.printStackTrace();
                    Snackbar.make(listView, R.string.toast_error, Snackbar.LENGTH_SHORT).show();
                }
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
                        .setPositiveButton(R.string.toast_cancel, new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int whichButton) {
                                dialog.cancel();
                            }
                        })
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
                                        final EditText pass_userName = (EditText) dialogView.findViewById(R.id.pass_userName);
                                        final EditText pass_userPW = (EditText) dialogView.findViewById(R.id.pass_userPW);


                                        final MAHEncryptor mahEncryptor = MAHEncryptor.newInstance(sharedPrefSec.getString("saveDC"));
                                        final String decrypted_userName = mahEncryptor.decode(userName);
                                        final String decrypted_userPW = mahEncryptor.decode(userPW);

                                        pass_title.setText(title);
                                        pass_userName.setText(decrypted_userName);
                                        pass_userPW.setText(decrypted_userPW);

                                        builder.setView(dialogView);
                                        builder.setTitle(R.string.pass_edit);
                                        builder.setPositiveButton(R.string.toast_yes, new DialogInterface.OnClickListener() {

                                            public void onClick(DialogInterface dialog, int whichButton) {

                                                try {
                                                    String input_pass_title = pass_title.getText().toString().trim();
                                                    String encrypted_userName = mahEncryptor.encode(pass_userName.getText().toString().trim());
                                                    String encrypted_userPW = mahEncryptor.encode(pass_userPW.getText().toString().trim());

                                                    db.deleteBookmark((Integer.parseInt(seqnoStr)));
                                                    db.addBookmark(
                                                            input_pass_title,
                                                            url,
                                                            encrypted_userName,
                                                            encrypted_userPW);
                                                    db.close();
                                                    setBookmarkList();
                                                    Snackbar.make(listView, R.string.pass_success, Snackbar.LENGTH_SHORT).show();

                                                } catch (Exception e) {
                                                    e.printStackTrace();
                                                    Snackbar.make(listView, R.string.toast_error, Snackbar.LENGTH_SHORT).show();
                                                }
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
                                        helper_editText.showKeyboard(Popup_pass.this, pass_title, 0, title, getString(R.string.app_search_hint_bookmark));

                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        Snackbar.make(listView, R.string.toast_error, Snackbar.LENGTH_SHORT).show();
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
                                                                Snackbar.make(listView, R.string.toast_error, Snackbar.LENGTH_SHORT).show();
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

                                    try {
                                        MAHEncryptor mahEncryptor = MAHEncryptor.newInstance(sharedPrefSec.getString("saveDC"));
                                        String decrypted_userName = mahEncryptor.decode(userName);
                                        String decrypted_userPW = mahEncryptor.decode(userPW);
                                        sharedPref.edit().putString("copyPW", decrypted_userPW).apply();
                                        sharedPref.edit().putString("copyUN", decrypted_userName).apply();
                                        sharedPref.edit().putString("openURL", "copyLogin").apply();
                                        finishAffinity();

                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        Snackbar.make(listView, R.string.toast_error, Snackbar.LENGTH_SHORT).show();
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
            Database_Pass db = new Database_Pass(Popup_pass.this);
            ArrayList<String[]> bookmarkList = new ArrayList<>();
            db.getBookmarks(bookmarkList, Popup_pass.this);
            if (bookmarkList.size() == 0) {
                db.loadInitialData();
                db.getBookmarks(bookmarkList, Popup_pass.this);
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
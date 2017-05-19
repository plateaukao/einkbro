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

package de.baumann.browser.lists;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.mobapphome.mahencryptorlib.MAHEncryptor;

import de.baumann.browser.R;
import de.baumann.browser.databases.DbAdapter_Pass;
import de.baumann.browser.helper.helper_editText;
import de.baumann.browser.helper.helper_main;

public class List_pass extends AppCompatActivity {

    private DbAdapter_Pass db;

    private ListView listView = null;
    private SharedPreferences sharedPref;
    private  MAHEncryptor mahEncryptor;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(ContextCompat.getColor(List_pass.this, R.color.colorThreeDark));

        setContentView(R.layout.activity_popup);
        helper_main.onStart(List_pass.this);

        PreferenceManager.setDefaultValues(this, R.xml.user_settings, false);
        PreferenceManager.setDefaultValues(this, R.xml.user_settings_search, false);
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPref.edit().putString("openURL", "").apply();

        if (sharedPref.getBoolean("isOpened", false)) {
            helper_main.checkPin(List_pass.this);
        }

        try {
            mahEncryptor = MAHEncryptor.newInstance(sharedPref.getString("saved_key", ""));
        } catch (Exception e) {
            e.printStackTrace();
            Snackbar.make(listView, R.string.toast_error, Snackbar.LENGTH_SHORT).show();
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        helper_main.toolbar (this, toolbar);

        EditText editText = (EditText) findViewById(R.id.editText);
        editText.setVisibility(View.GONE);
        editText.setHint(R.string.app_search_hint);
        editText.clearFocus();
        TextView urlBar = (TextView) findViewById(R.id.urlBar);
        urlBar.setText(R.string.app_title_passStorage);

        listView = (ListView)findViewById(R.id.list);

        //calling Notes_DbAdapter
        db = new DbAdapter_Pass(this);
        db.open();
        setFilesList();
    }

    private void setFilesList() {

        //display data
        final int layoutstyle=R.layout.list_item;
        int[] xml_id = new int[] {
                R.id.textView_title_notes,
                R.id.textView_des_notes,
                R.id.textView_create_notes
        };
        String[] column = new String[] {
                "pass_title",
                "pass_content",
                "pass_creation"
        };
        final Cursor row = db.fetchAllData(this);
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, layoutstyle, row, column, xml_id, 0);

        listView.setAdapter(adapter);
        //onClick function
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterview, View view, int position, long id) {
                Cursor row2 = (Cursor) listView.getItemAtPosition(position);
                final String pass_content = row2.getString(row2.getColumnIndexOrThrow("pass_content"));
                final String pass_icon = row2.getString(row2.getColumnIndexOrThrow("pass_icon"));
                final String pass_attachment = row2.getString(row2.getColumnIndexOrThrow("pass_attachment"));

                try {
                    String decrypted_userName = mahEncryptor.decode(pass_icon);
                    String decrypted_userPW = mahEncryptor.decode(pass_attachment);
                    sharedPref.edit().putString("copyPW", decrypted_userPW).apply();
                    sharedPref.edit().putString("copyUN", decrypted_userName).apply();
                    sharedPref.edit().putString("openURL", "openLogin" + pass_content).apply();
                    finishAffinity();

                } catch (Exception e) {
                    e.printStackTrace();
                    Snackbar.make(listView, R.string.toast_error, Snackbar.LENGTH_SHORT).show();
                }
            }
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Cursor row2 = (Cursor) listView.getItemAtPosition(position);
                final String _id = row2.getString(row2.getColumnIndexOrThrow("_id"));
                final String pass_title = row2.getString(row2.getColumnIndexOrThrow("pass_title"));
                final String pass_content = row2.getString(row2.getColumnIndexOrThrow("pass_content"));
                final String pass_icon = row2.getString(row2.getColumnIndexOrThrow("pass_icon"));
                final String pass_attachment = row2.getString(row2.getColumnIndexOrThrow("pass_attachment"));

                final CharSequence[] options = {
                        getString(R.string.pass_copy),
                        getString(R.string.pass_edit),
                        getString(R.string.bookmark_remove_bookmark)};
                new AlertDialog.Builder(List_pass.this)
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

                                        AlertDialog.Builder builder = new AlertDialog.Builder(List_pass.this);
                                        View dialogView = View.inflate(List_pass.this, R.layout.dialog_login, null);

                                        final EditText pass_titleET = (EditText) dialogView.findViewById(R.id.pass_title);
                                        final EditText pass_userNameET = (EditText) dialogView.findViewById(R.id.pass_userName);
                                        final EditText pass_userPWET = (EditText) dialogView.findViewById(R.id.pass_userPW);

                                        final String decrypted_userName = mahEncryptor.decode(pass_icon);
                                        final String decrypted_userPW = mahEncryptor.decode(pass_attachment);

                                        pass_titleET.setText(pass_title);
                                        pass_userNameET.setText(decrypted_userName);
                                        pass_userPWET.setText(decrypted_userPW);

                                        builder.setView(dialogView);
                                        builder.setTitle(R.string.pass_edit);
                                        builder.setPositiveButton(R.string.toast_yes, new DialogInterface.OnClickListener() {

                                            public void onClick(DialogInterface dialog, int whichButton) {

                                                try {
                                                    String input_pass_title = pass_titleET.getText().toString().trim();
                                                    String encrypted_userName = mahEncryptor.encode(pass_userNameET.getText().toString().trim());
                                                    String encrypted_userPW = mahEncryptor.encode(pass_userPWET.getText().toString().trim());

                                                    db.update(Integer.parseInt(_id), input_pass_title, pass_content, encrypted_userName, encrypted_userPW, helper_main.createDate());
                                                    setFilesList();
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
                                        helper_editText.showKeyboard(List_pass.this, pass_titleET, 0, pass_title, getString(R.string.app_search_hint_bookmark));

                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        Snackbar.make(listView, R.string.toast_error, Snackbar.LENGTH_SHORT).show();
                                    }
                                }

                                if (options[item].equals(getString(R.string.bookmark_remove_bookmark))) {
                                    Snackbar snackbar = Snackbar
                                            .make(listView, R.string.bookmark_remove_confirmation, Snackbar.LENGTH_LONG)
                                            .setAction(R.string.toast_yes, new View.OnClickListener() {
                                                @Override
                                                public void onClick(View view) {
                                                    db.delete(Integer.parseInt(_id));
                                                    setFilesList();
                                                }
                                            });
                                    snackbar.show();
                                }

                                if (options[item].equals(getString(R.string.pass_copy))) {

                                    try {
                                        String decrypted_userName = mahEncryptor.decode(pass_icon);
                                        String decrypted_userPW = mahEncryptor.decode(pass_attachment);
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
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        menu.findItem(R.id.action_cancel).setVisible(false);
        menu.findItem(R.id.action_sort).setVisible(false);
        menu.findItem(R.id.action_filter).setVisible(false);
        menu.findItem(R.id.action_save_bookmark).setVisible(false);

        return true; // this is important to call so that new menu is shown
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_popup, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch (item.getItemId()) {

            case android.R.id.home:
                finish();
                return true;

            case R.id.action_delete:
                Snackbar snackbar = Snackbar
                        .make(listView, R.string.toast_list, Snackbar.LENGTH_LONG)
                        .setAction(R.string.toast_yes, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                List_pass.this.deleteDatabase("pass.db");
                                recreate();
                            }
                        });
                snackbar.show();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
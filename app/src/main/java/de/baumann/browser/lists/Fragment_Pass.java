package de.baumann.browser.lists;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.mobapphome.mahencryptorlib.MAHEncryptor;

import de.baumann.browser.R;
import de.baumann.browser.databases.DbAdapter_Pass;
import de.baumann.browser.helper.CustomViewPager;
import de.baumann.browser.helper.helper_editText;
import de.baumann.browser.helper.helper_main;
import de.baumann.browser.helper.helper_toolbar;

public class Fragment_Pass extends Fragment {

    private MAHEncryptor mahEncryptor;
    private ListView listView = null;
    private EditText editText;
    private DbAdapter_Pass db;
    private SharedPreferences sharedPref;
    private TextView listBar;
    private Toolbar toolbar;
    private CustomViewPager viewPager;

    private int top;
    private int index;
    

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_lists, container, false);

        setHasOptionsMenu(true);

        PreferenceManager.setDefaultValues(getActivity(), R.xml.user_settings, false);
        PreferenceManager.setDefaultValues(getActivity(), R.xml.user_settings_search, false);
        sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());

        toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
        editText = (EditText) getActivity().findViewById(R.id.editText);
        listBar = (TextView) getActivity().findViewById(R.id.listBar);
        listView = (ListView)rootView.findViewById(R.id.list);
        viewPager = (CustomViewPager) getActivity().findViewById(R.id.viewpager);

        try {
            mahEncryptor = MAHEncryptor.newInstance(sharedPref.getString("saved_key", ""));
        } catch (Exception e) {
            e.printStackTrace();
            Snackbar.make(listView, R.string.toast_error, Snackbar.LENGTH_SHORT).show();
        }

        //calling Notes_DbAdapter
        db = new DbAdapter_Pass(getActivity());
        db.open();

        return rootView;
    }

    private void isEdited () {
        index = listView.getFirstVisiblePosition();
        View v = listView.getChildAt(0);
        top = (v == null) ? 0 : (v.getTop() - listView.getPaddingTop());
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
        final Cursor row = db.fetchAllData(getActivity());
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(getActivity(), layoutstyle, row, column, xml_id, 0);

        listView.setAdapter(adapter);
        listView.setSelectionFromTop(index, top);
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
                    if (sharedPref.getInt("appShortcut", 0) == 0) {
                        viewPager.setCurrentItem(sharedPref.getInt("tab", 0));
                    } else {
                        sharedPref.edit().putInt("appShortcut", 0).apply();
                        viewPager.setCurrentItem(0);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Snackbar.make(listView, R.string.toast_error, Snackbar.LENGTH_SHORT).show();
                }
            }
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

                isEdited();

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
                new AlertDialog.Builder(getActivity())
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

                                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                                        View dialogView = View.inflate(getActivity(), R.layout.dialog_login, null);

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
                                        helper_editText.showKeyboard(getActivity(), pass_titleET, 0, pass_title, getString(R.string.app_search_hint_bookmark));

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
                                        if (sharedPref.getInt("appShortcut", 0) == 0) {
                                            viewPager.setCurrentItem(sharedPref.getInt("tab", 0));
                                        } else {
                                            sharedPref.edit().putInt("appShortcut", 0).apply();
                                            viewPager.setCurrentItem(0);
                                        }

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

    public void doBack() {
        //BackPressed in activity will call this;
        Snackbar snackbar = Snackbar
                .make(listView, getString(R.string.toast_exit), Snackbar.LENGTH_SHORT)
                .setAction(getString(R.string.toast_yes), new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        getActivity().finish();
                    }
                });
        snackbar.show();
    }

    @Override
    public void setUserVisibleHint(final boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if (isVisibleToUser) {
            AppCompatActivity appCompatActivity = (AppCompatActivity)getActivity();
            assert appCompatActivity.getSupportActionBar() != null;
            appCompatActivity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            setFilesList();
            helper_toolbar.toolbarGestures(getActivity(), toolbar, viewPager, editText, listBar, "");
            listBar.setText(R.string.app_title_passStorage);
        } else {
            Log.i("Browser", "Browser: isVisibleToUser false");
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_popup, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        super.onPrepareOptionsMenu(menu);

        menu.findItem(R.id.action_cancel).setVisible(false);
        menu.findItem(R.id.action_sort).setVisible(false);
        menu.findItem(R.id.action_filter).setVisible(false);
        menu.findItem(R.id.action_save_bookmark).setVisible(false);
        menu.findItem(R.id.action_favorite).setVisible(false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch (item.getItemId()) {

            case R.id.action_delete:
                Snackbar snackbar = Snackbar
                        .make(listView, R.string.toast_list, Snackbar.LENGTH_LONG)
                        .setAction(R.string.toast_yes, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                getActivity().deleteDatabase("pass.db");
                                getActivity().recreate();
                            }
                        });
                snackbar.show();
                return true;

            case android.R.id.home:
                if (sharedPref.getInt("appShortcut", 0) == 0) {
                    viewPager.setCurrentItem(sharedPref.getInt("tab", 0));
                } else {
                    sharedPref.edit().putInt("appShortcut", 0).apply();
                    viewPager.setCurrentItem(0);
                }
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
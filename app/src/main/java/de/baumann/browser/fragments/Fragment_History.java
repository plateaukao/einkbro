package de.baumann.browser.fragments;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.FilterQueryProvider;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import de.baumann.browser.R;
import de.baumann.browser.databases.DbAdapter_History;
import de.baumann.browser.helper.class_CustomViewPager;
import de.baumann.browser.helper.helper_browser;
import de.baumann.browser.helper.helper_editText;
import de.baumann.browser.helper.helper_main;
import de.baumann.browser.helper.helper_toolbar;

public class Fragment_History extends Fragment {

    private ListView listView = null;
    private EditText editText;
    private DbAdapter_History db;
    private SimpleCursorAdapter adapter;
    private SharedPreferences sharedPref;
    private TextView listBar;
    private Toolbar toolbar;
    private class_CustomViewPager viewPager;

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
        viewPager = (class_CustomViewPager) getActivity().findViewById(R.id.viewpager);

        //calling Notes_DbAdapter
        db = new DbAdapter_History(getActivity());
        db.open();

        return rootView;
    }

    private void setTitle () {
        if (sharedPref.getString("sortDBH", "title").equals("title")) {
            listBar.setText(getString(R.string.app_title_history) + " | " + getString(R.string.sort_title));
        } else {
            listBar.setText(getString(R.string.app_title_history) + " | " + getString(R.string.sort_date));
        }
    }

    private void isEdited () {
        index = listView.getFirstVisiblePosition();
        View v = listView.getChildAt(0);
        top = (v == null) ? 0 : (v.getTop() - listView.getPaddingTop());
    }

    private void setHistoryList() {

        //display data
        final int layoutstyle=R.layout.list_item;
        int[] xml_id = new int[] {
                R.id.textView_title_notes,
                R.id.textView_des_notes,
                R.id.textView_create_notes
        };
        String[] column = new String[] {
                "history_title",
                "history_content",
                "history_creation"
        };
        final Cursor row = db.fetchAllData(getActivity());
        adapter = new SimpleCursorAdapter(getActivity(), layoutstyle,row,column, xml_id, 0);

        //display data by filter
        final String note_search = sharedPref.getString("filter_historyBY", "history_title");
        sharedPref.edit().putString("filter_historyBY", "history_title").apply();
        editText.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.getFilter().filter(s.toString());
            }
        });
        adapter.setFilterQueryProvider(new FilterQueryProvider() {
            public Cursor runQuery(CharSequence constraint) {
                return db.fetchDataByFilter(constraint.toString(),note_search);
            }
        });

        listView.setAdapter(adapter);
        listView.setSelectionFromTop(index, top);
        //onClick function
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterview, View view, int position, long id) {

                Cursor row = (Cursor) listView.getItemAtPosition(position);
                String history_content = row.getString(row.getColumnIndexOrThrow("history_content"));
                sharedPref.edit().putString("openURL", history_content).apply();
                if (sharedPref.getInt("appShortcut", 0) == 0) {
                    viewPager.setCurrentItem(sharedPref.getInt("tab", 0));
                } else {
                    sharedPref.edit().putInt("appShortcut", 0).apply();
                    viewPager.setCurrentItem(0);
                }
            }
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

                isEdited();

                Cursor row2 = (Cursor) listView.getItemAtPosition(position);
                final String _id = row2.getString(row2.getColumnIndexOrThrow("_id"));
                final String history_title = row2.getString(row2.getColumnIndexOrThrow("history_title"));
                final String history_content = row2.getString(row2.getColumnIndexOrThrow("history_content"));
                final String history_icon = row2.getString(row2.getColumnIndexOrThrow("history_icon"));
                final String history_attachment = row2.getString(row2.getColumnIndexOrThrow("history_attachment"));
                final String history_creation = row2.getString(row2.getColumnIndexOrThrow("history_creation"));

                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                final  View dialogView = View.inflate(getActivity(), R.layout.dialog_context_lists, null);

                builder.setView(dialogView);
                builder.setPositiveButton(R.string.toast_cancel, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.cancel();
                        dialog.cancel();
                    }
                });

                final AlertDialog dialog = builder.create();
                // Display the custom alert dialog on interface
                dialog.show();

                LinearLayout menu_shareSite_Layout = (LinearLayout) dialogView.findViewById(R.id.menu_shareSite_Layout);
                menu_shareSite_Layout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        final CharSequence[] options = {
                                getString(R.string.menu_share_link),
                                getString(R.string.menu_share_link_copy)};
                        new AlertDialog.Builder(getActivity())
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
                                            sharingIntent.putExtra(Intent.EXTRA_SUBJECT, history_title);
                                            sharingIntent.putExtra(Intent.EXTRA_TEXT, history_content);
                                            startActivity(Intent.createChooser(sharingIntent, (getString(R.string.app_share_link))));
                                        }
                                        if (options[item].equals(getString(R.string.menu_share_link_copy))) {
                                            ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                                            clipboard.setPrimaryClip(ClipData.newPlainText("text", history_content));
                                            Snackbar.make(listView, R.string.context_linkCopy_toast, Snackbar.LENGTH_SHORT).show();
                                        }
                                    }
                                }).show();
                        dialog.cancel();
                    }
                });

                LinearLayout menu_saveSite_Layout = (LinearLayout) dialogView.findViewById(R.id.menu_saveSite_Layout);
                menu_saveSite_Layout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        final CharSequence[] options = {
                                getString(R.string.menu_save_bookmark),
                                getString(R.string.menu_save_readLater),
                                getString(R.string.menu_save_pass),
                                getString(R.string.menu_createShortcut)};
                        new AlertDialog.Builder(getActivity())
                                .setPositiveButton(R.string.toast_cancel, new DialogInterface.OnClickListener() {

                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        dialog.cancel();
                                    }
                                })
                                .setItems(options, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int item) {
                                        if (options[item].equals(getString(R.string.menu_save_pass))) {
                                            helper_editText.editText_savePass(getActivity(), listView, history_title, history_content);
                                        }
                                        if (options[item].equals(getString(R.string.menu_save_bookmark))) {
                                            helper_main.save_bookmark(getActivity(), history_title, history_content, listView);
                                        }
                                        if (options[item].equals(getString(R.string.menu_save_readLater))) {
                                            helper_main.save_readLater(getActivity(), history_title, history_content, listView);
                                        }
                                        if (options[item].equals(getString(R.string.menu_createShortcut))) {
                                            helper_main.installShortcut(getActivity(), history_title, history_content, listView);
                                        }
                                    }
                                }).show();
                        dialog.cancel();
                    }
                });

                LinearLayout menu_edit_Layout = (LinearLayout) dialogView.findViewById(R.id.menu_edit_Layout);
                menu_edit_Layout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.cancel();
                        sharedPref.edit().putString("edit_id", _id).apply();
                        sharedPref.edit().putString("edit_content", history_content).apply();
                        sharedPref.edit().putString("edit_icon", history_icon).apply();
                        sharedPref.edit().putString("edit_attachment", history_attachment).apply();
                        sharedPref.edit().putString("edit_creation", history_creation).apply();
                        editText.setVisibility(View.VISIBLE);
                        helper_editText.showKeyboard(getActivity(), editText, 2, history_title, getString(R.string.bookmark_edit_title));
                    }
                });

                LinearLayout menu_remove_Layout = (LinearLayout) dialogView.findViewById(R.id.menu_remove_Layout);
                menu_remove_Layout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.cancel();
                        Snackbar snackbar = Snackbar
                                .make(listView, R.string.bookmark_remove_confirmation, Snackbar.LENGTH_LONG)
                                .setAction(R.string.toast_yes, new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        db.delete(Integer.parseInt(_id));
                                        setHistoryList();
                                    }
                                });
                        snackbar.show();
                    }
                });

                HorizontalScrollView scrollTabs = (HorizontalScrollView) dialogView.findViewById(R.id.scrollTabs);

                TextView context_1 = (TextView) dialogView.findViewById(R.id.context_1);
                ImageView context_1_preView = (ImageView) dialogView.findViewById(R.id.context_1_preView);
                CardView context_1_Layout = (CardView) dialogView.findViewById(R.id.context_1_Layout);
                ImageView close_1 = (ImageView) dialogView.findViewById(R.id.close_1);
                helper_toolbar.cardViewClickMenu(getActivity(), context_1_Layout, scrollTabs, 0, close_1, viewPager, history_content, dialog, "0");
                helper_toolbar.toolBarPreview(getActivity(), context_1,context_1_preView, 0, helper_browser.tab_1(getActivity()), "/tab_0.jpg", close_1);

                TextView context_2 = (TextView) dialogView.findViewById(R.id.context_2);
                ImageView context_2_preView = (ImageView) dialogView.findViewById(R.id.context_2_preView);
                CardView context_2_Layout = (CardView) dialogView.findViewById(R.id.context_2_Layout);
                ImageView close_2 = (ImageView) dialogView.findViewById(R.id.close_2);
                helper_toolbar.cardViewClickMenu(getActivity(), context_2_Layout, scrollTabs, 1, close_2, viewPager, history_content, dialog, "1");
                helper_toolbar.toolBarPreview(getActivity(), context_2,context_2_preView, 1, helper_browser.tab_2(getActivity()), "/tab_1.jpg", close_2);

                TextView context_3 = (TextView) dialogView.findViewById(R.id.context_3);
                ImageView context_3_preView = (ImageView) dialogView.findViewById(R.id.context_3_preView);
                CardView context_3_Layout = (CardView) dialogView.findViewById(R.id.context_3_Layout);
                ImageView close_3 = (ImageView) dialogView.findViewById(R.id.close_3);
                helper_toolbar.cardViewClickMenu(getActivity(), context_3_Layout, scrollTabs, 2, close_3, viewPager, history_content, dialog, "2");
                helper_toolbar.toolBarPreview(getActivity(), context_3,context_3_preView, 2, helper_browser.tab_3(getActivity()), "/tab_2.jpg", close_3);

                TextView context_4 = (TextView) dialogView.findViewById(R.id.context_4);
                ImageView context_4_preView = (ImageView) dialogView.findViewById(R.id.context_4_preView);
                CardView context_4_Layout = (CardView) dialogView.findViewById(R.id.context_4_Layout);
                ImageView close_4 = (ImageView) dialogView.findViewById(R.id.close_4);
                helper_toolbar.cardViewClickMenu(getActivity(), context_4_Layout, scrollTabs, 3, close_4, viewPager, history_content, dialog, "3");
                helper_toolbar.toolBarPreview(getActivity(), context_4,context_4_preView, 3, helper_browser.tab_4(getActivity()), "/tab_3.jpg", close_4);

                TextView context_5 = (TextView) dialogView.findViewById(R.id.context_5);
                ImageView context_5_preView = (ImageView) dialogView.findViewById(R.id.context_5_preView);
                CardView context_5_Layout = (CardView) dialogView.findViewById(R.id.context_5_Layout);
                ImageView close_5 = (ImageView) dialogView.findViewById(R.id.close_5);
                helper_toolbar.cardViewClickMenu(getActivity(), context_5_Layout, scrollTabs, 4, close_5, viewPager, history_content, dialog, "4");
                helper_toolbar.toolBarPreview(getActivity(), context_5,context_5_preView, 4, helper_browser.tab_5(getActivity()), "/tab_4.jpg", close_5);

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
            setTitle();
            helper_toolbar.toolbarGestures(getActivity(), toolbar, viewPager);
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    setHistoryList();
                    listView.setSelection(listView.getCount() - 1);
                }
            }, 100);
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

        if (sharedPref.getInt("keyboard", 0) == 0) {
            // normal
            menu.findItem(R.id.action_cancel).setVisible(false);
            menu.findItem(R.id.action_save_bookmark).setVisible(false);
        } else if (sharedPref.getInt("keyboard", 0) == 1) {
            // filter
            menu.findItem(R.id.action_sort).setVisible(false);
            menu.findItem(R.id.action_delete).setVisible(false);
            menu.findItem(R.id.action_save_bookmark).setVisible(false);
        } else if (sharedPref.getInt("keyboard", 0) == 2) {
            // save
            menu.findItem(R.id.action_filter).setVisible(false);
            menu.findItem(R.id.action_delete).setVisible(false);
            menu.findItem(R.id.action_sort).setVisible(false);
        }
        menu.findItem(R.id.action_favorite).setVisible(false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch (item.getItemId()) {

            case R.id.filter_title:
                sharedPref.edit().putString("filter_historyBY", "history_title").apply();
                setHistoryList();
                editText.setVisibility(View.VISIBLE);
                helper_editText.showKeyboard(getActivity(), editText, 1, "", getString(R.string.action_filter_title));
                return true;
            case R.id.filter_url:
                sharedPref.edit().putString("filter_historyBY", "history_content").apply();
                setHistoryList();
                editText.setVisibility(View.VISIBLE);
                helper_editText.showKeyboard(getActivity(), editText, 1, "", getString(R.string.action_filter_url));
                return true;

            case R.id.filter_today:
                DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Calendar cal = Calendar.getInstance();
                final String search = dateFormat.format(cal.getTime());
                sharedPref.edit().putString("filter_historyBY", "history_creation").apply();
                setHistoryList();
                editText.setText(search);
                listBar.setText(getString(R.string.app_title_history) + " | " + getString(R.string.filter_today));
                return true;
            case R.id.filter_yesterday:
                DateFormat dateFormat2 = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Calendar cal2 = Calendar.getInstance();
                cal2.add(Calendar.DATE, -1);
                final String search2 = dateFormat2.format(cal2.getTime());
                sharedPref.edit().putString("filter_historyBY", "history_creation").apply();
                setHistoryList();
                editText.setText(search2);
                listBar.setText(getString(R.string.app_title_history) + " | " + getString(R.string.filter_yesterday));
                return true;
            case R.id.filter_before:
                DateFormat dateFormat3 = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Calendar cal3 = Calendar.getInstance();
                cal3.add(Calendar.DATE, -2);
                final String search3 = dateFormat3.format(cal3.getTime());
                sharedPref.edit().putString("filter_historyBY", "history_creation").apply();
                setHistoryList();
                editText.setText(search3);
                listBar.setText(getString(R.string.app_title_history) + " | " + getString(R.string.filter_before));
                return true;
            case R.id.filter_month:
                DateFormat dateFormat4 = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
                Calendar cal4 = Calendar.getInstance();
                final String search4 = dateFormat4.format(cal4.getTime());
                sharedPref.edit().putString("filter_historyBY", "history_creation").apply();
                setHistoryList();
                editText.setText(search4);
                listBar.setText(getString(R.string.app_title_history) + " | " + getString(R.string.filter_month));
                return true;
            case R.id.filter_own:
                sharedPref.edit().putString("filter_historyBY", "history_creation").apply();
                setHistoryList();
                editText.setVisibility(View.VISIBLE);
                helper_editText.showKeyboard(getActivity(), editText, 1, "", getString(R.string.action_filter_create));
                return true;
            case R.id.filter_clear:
                editText.setVisibility(View.GONE);
                setTitle();
                helper_editText.hideKeyboard(getActivity(), editText, 0, getString(R.string.app_title_history), getString(R.string.app_search_hint));
                setHistoryList();
                return true;

            case R.id.sort_title:
                sharedPref.edit().putString("sortDBH", "title").apply();
                setHistoryList();
                setTitle();
                return true;
            case R.id.sort_creation:
                sharedPref.edit().putString("sortDBH", "create").apply();
                setHistoryList();
                setTitle();
                return true;

            case R.id.action_cancel:
                editText.setVisibility(View.GONE);
                setTitle();
                helper_editText.hideKeyboard(getActivity(), editText, 0, getString(R.string.app_title_history), getString(R.string.app_search_hint));
                setHistoryList();
                return true;

            case R.id.action_delete:
                Snackbar snackbar = Snackbar
                        .make(listView, R.string.toast_list, Snackbar.LENGTH_LONG)
                        .setAction(R.string.toast_yes, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                getActivity().deleteDatabase("history_DB_v01.db");
                                db.open();
                                setHistoryList();
                            }
                        });
                snackbar.show();
                return true;

            case R.id.action_save_bookmark:

                String edit_id = sharedPref.getString("edit_id", "");
                String edit_content = sharedPref.getString("edit_content", "");
                String edit_icon = sharedPref.getString("edit_icon", "");
                String edit_attachment = sharedPref.getString("edit_attachment", "");
                String edit_creation = sharedPref.getString("edit_creation", "");

                String inputTag = editText.getText().toString().trim();
                db.update(Integer.parseInt(edit_id), inputTag, edit_content, edit_icon, edit_attachment, edit_creation);
                helper_editText.hideKeyboard(getActivity(), editText, 0, getString(R.string.app_title_history), getString(R.string.app_search_hint));
                setHistoryList();

                Snackbar.make(listView, R.string.bookmark_added, Snackbar.LENGTH_SHORT).show();

                editText.setVisibility(View.GONE);
                setTitle();

                sharedPref.edit().putString("edit_id", "").apply();
                sharedPref.edit().putString("edit_content", "").apply();
                sharedPref.edit().putString("edit_icon", "").apply();
                sharedPref.edit().putString("edit_attachment", "").apply();
                sharedPref.edit().putString("edit_creation", "").apply();

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
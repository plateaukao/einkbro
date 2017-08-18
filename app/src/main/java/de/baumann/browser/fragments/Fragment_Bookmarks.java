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
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.CardView;
import android.text.Editable;
import android.text.TextWatcher;
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
import de.baumann.browser.databases.DbAdapter_Bookmarks;
import de.baumann.browser.helper.helper_browser;
import de.baumann.browser.helper.helper_editText;
import de.baumann.browser.helper.helper_main;
import de.baumann.browser.helper.helper_toolbar;

public class Fragment_Bookmarks extends Fragment {

    private ListView listView = null;
    private EditText editText;
    private DbAdapter_Bookmarks db;
    private SimpleCursorAdapter adapter;
    private SharedPreferences sharedPref;
    private TextView listBar;
    private ViewPager viewPager;

    private int top;
    private int index;
    

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_lists, container, false);
        setHasOptionsMenu(true);

        sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());

        editText = (EditText) getActivity().findViewById(R.id.editText);
        listBar = (TextView) getActivity().findViewById(R.id.listBar);
        listView = (ListView)rootView.findViewById(R.id.list);
        viewPager = (ViewPager) getActivity().findViewById(R.id.viewpager);

        //calling Notes_DbAdapter
        db = new DbAdapter_Bookmarks(getActivity());
        db.open();

        return rootView;
    }

    private void setTitle () {
        if (sharedPref.getString("sortDBB", "title").equals("title")) {
            listBar.setText(getString(R.string.app_title_bookmarks) + " | " + getString(R.string.sort_title));
        } else {
            listBar.setText(getString(R.string.app_title_bookmarks) + " | " + getString(R.string.sort_date));
        }
    }

    private void isEdited () {
        index = listView.getFirstVisiblePosition();
        View v = listView.getChildAt(0);
        top = (v == null) ? 0 : (v.getTop() - listView.getPaddingTop());
    }

    private void setBookmarksList() {

        //display data
        final int layoutstyle=R.layout.list_item;
        int[] xml_id = new int[] {
                R.id.textView_title_notes,
                R.id.textView_des_notes,
                R.id.textView_create_notes
        };
        String[] column = new String[] {
                "bookmarks_title",
                "bookmarks_content",
                "bookmarks_creation"
        };
        final Cursor row = db.fetchAllData(getActivity());
        adapter = new SimpleCursorAdapter(getActivity(), layoutstyle,row,column, xml_id, 0) {
            @Override
            public View getView (final int position, View convertView, ViewGroup parent) {

                Cursor row2 = (Cursor) listView.getItemAtPosition(position);
                final String _id = row2.getString(row2.getColumnIndexOrThrow("_id"));
                final String bookmarks_title = row2.getString(row2.getColumnIndexOrThrow("bookmarks_title"));
                final String bookmarks_content = row2.getString(row2.getColumnIndexOrThrow("bookmarks_content"));
                final String bookmarks_icon = row2.getString(row2.getColumnIndexOrThrow("bookmarks_icon"));
                final String bookmarks_attachment = row2.getString(row2.getColumnIndexOrThrow("bookmarks_attachment"));
                final String bookmarks_creation = row2.getString(row2.getColumnIndexOrThrow("bookmarks_creation"));

                View v = super.getView(position, convertView, parent);
                final ImageView iv_attachment = (ImageView) v.findViewById(R.id.att_notes);

                switch (bookmarks_attachment) {
                    case "":
                        iv_attachment.setVisibility(View.VISIBLE);
                        iv_attachment.setImageResource(R.drawable.star_outline);
                        break;
                    default:
                        iv_attachment.setVisibility(View.VISIBLE);
                        iv_attachment.setImageResource(R.drawable.star_grey);
                        break;
                }

                iv_attachment.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View arg0) {

                        isEdited();

                        if (bookmarks_attachment.equals("")) {

                            if(db.isExistFav("true")){
                                Snackbar.make(listView, R.string.bookmark_setFav_not, Snackbar.LENGTH_LONG).show();
                            }else{
                                iv_attachment.setImageResource(R.drawable.star_grey);
                                db.update(Integer.parseInt(_id), bookmarks_title, bookmarks_content, bookmarks_icon, "true", bookmarks_creation);
                                setBookmarksList();
                                sharedPref.edit().putString("startURL", bookmarks_content).apply();
                                Snackbar.make(listView, R.string.bookmark_setFav, Snackbar.LENGTH_LONG).show();
                            }
                        } else {
                            iv_attachment.setImageResource(R.drawable.star_outline);
                            db.update(Integer.parseInt(_id), bookmarks_title, bookmarks_content, bookmarks_icon, "", bookmarks_creation);
                            setBookmarksList();
                        }
                    }
                });
                return v;
            }
        };

        //display data by filter
        final String note_search = sharedPref.getString("filter_bookmarksBY", "bookmarks_title");
        sharedPref.edit().putString("filter_bookmarksBY", "bookmarks_title").apply();
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
                String bookmarks_content = row.getString(row.getColumnIndexOrThrow("bookmarks_content"));
                sharedPref.edit().putString("openURL", bookmarks_content).apply();

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

                Cursor row = (Cursor) listView.getItemAtPosition(position);
                final String _id = row.getString(row.getColumnIndexOrThrow("_id"));
                final String bookmarks_title = row.getString(row.getColumnIndexOrThrow("bookmarks_title"));
                final String bookmarks_content = row.getString(row.getColumnIndexOrThrow("bookmarks_content"));
                final String bookmarks_icon = row.getString(row.getColumnIndexOrThrow("bookmarks_icon"));
                final String bookmarks_attachment = row.getString(row.getColumnIndexOrThrow("bookmarks_attachment"));
                final String bookmarks_creation = row.getString(row.getColumnIndexOrThrow("bookmarks_creation"));

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
                                            sharingIntent.putExtra(Intent.EXTRA_SUBJECT, bookmarks_title);
                                            sharingIntent.putExtra(Intent.EXTRA_TEXT, bookmarks_content);
                                            startActivity(Intent.createChooser(sharingIntent, (getString(R.string.app_share_link))));
                                        }
                                        if (options[item].equals(getString(R.string.menu_share_link_copy))) {
                                            ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                                            clipboard.setPrimaryClip(ClipData.newPlainText("text", bookmarks_content));
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
                                            helper_editText.editText_savePass(getActivity(), listView, bookmarks_title, bookmarks_content);
                                        }
                                        if (options[item].equals(getString(R.string.menu_save_readLater))) {
                                            helper_main.save_readLater(getActivity(), bookmarks_title, bookmarks_content, listView);
                                        }
                                        if (options[item].equals(getString(R.string.menu_createShortcut))) {
                                            helper_main.installShortcut(getActivity(), bookmarks_title, bookmarks_content, listView);
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
                        sharedPref.edit().putString("edit_content", bookmarks_content).apply();
                        sharedPref.edit().putString("edit_icon", bookmarks_icon).apply();
                        sharedPref.edit().putString("edit_attachment", bookmarks_attachment).apply();
                        sharedPref.edit().putString("edit_creation", bookmarks_creation).apply();
                        editText.setVisibility(View.VISIBLE);
                        helper_editText.showKeyboard(getActivity(), editText, 2, bookmarks_title, getString(R.string.bookmark_edit_title));
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
                                        setBookmarksList();
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
                helper_toolbar.cardViewClickMenu(getActivity(), context_1_Layout, scrollTabs, 0, close_1, viewPager, bookmarks_content, dialog, "0");
                helper_toolbar.toolBarPreview(getActivity(), context_1,context_1_preView, 0, helper_browser.tab_1(getActivity()), "/tab_0.jpg", close_1);

                TextView context_2 = (TextView) dialogView.findViewById(R.id.context_2);
                ImageView context_2_preView = (ImageView) dialogView.findViewById(R.id.context_2_preView);
                CardView context_2_Layout = (CardView) dialogView.findViewById(R.id.context_2_Layout);
                ImageView close_2 = (ImageView) dialogView.findViewById(R.id.close_2);
                helper_toolbar.cardViewClickMenu(getActivity(), context_2_Layout, scrollTabs, 1, close_2, viewPager, bookmarks_content, dialog, "1");
                helper_toolbar.toolBarPreview(getActivity(), context_2,context_2_preView, 1, helper_browser.tab_2(getActivity()), "/tab_1.jpg", close_2);

                TextView context_3 = (TextView) dialogView.findViewById(R.id.context_3);
                ImageView context_3_preView = (ImageView) dialogView.findViewById(R.id.context_3_preView);
                CardView context_3_Layout = (CardView) dialogView.findViewById(R.id.context_3_Layout);
                ImageView close_3 = (ImageView) dialogView.findViewById(R.id.close_3);
                helper_toolbar.cardViewClickMenu(getActivity(), context_3_Layout, scrollTabs, 2, close_3, viewPager, bookmarks_content, dialog, "2");
                helper_toolbar.toolBarPreview(getActivity(), context_3,context_3_preView, 2, helper_browser.tab_3(getActivity()), "/tab_2.jpg", close_3);

                TextView context_4 = (TextView) dialogView.findViewById(R.id.context_4);
                ImageView context_4_preView = (ImageView) dialogView.findViewById(R.id.context_4_preView);
                CardView context_4_Layout = (CardView) dialogView.findViewById(R.id.context_4_Layout);
                ImageView close_4 = (ImageView) dialogView.findViewById(R.id.close_4);
                helper_toolbar.cardViewClickMenu(getActivity(), context_4_Layout, scrollTabs, 3, close_4, viewPager, bookmarks_content, dialog, "3");
                helper_toolbar.toolBarPreview(getActivity(), context_4,context_4_preView, 3, helper_browser.tab_4(getActivity()), "/tab_3.jpg", close_4);

                TextView context_5 = (TextView) dialogView.findViewById(R.id.context_5);
                ImageView context_5_preView = (ImageView) dialogView.findViewById(R.id.context_5_preView);
                CardView context_5_Layout = (CardView) dialogView.findViewById(R.id.context_5_Layout);
                ImageView close_5 = (ImageView) dialogView.findViewById(R.id.close_5);
                helper_toolbar.cardViewClickMenu(getActivity(), context_5_Layout, scrollTabs, 4, close_5, viewPager, bookmarks_content, dialog, "4");
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
                        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
                        sharedPref.edit().putInt("closeApp", 1).apply();
                        viewPager.setCurrentItem(5);
                    }
                });
        snackbar.show();
    }

    public void fragmentAction () {
        setTitle();
        setBookmarksList();
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
            menu.findItem(R.id.action_favorite).setVisible(false);
        } else if (sharedPref.getInt("keyboard", 0) == 2) {
            // save
            menu.findItem(R.id.action_filter).setVisible(false);
            menu.findItem(R.id.action_delete).setVisible(false);
            menu.findItem(R.id.action_sort).setVisible(false);
            menu.findItem(R.id.action_favorite).setVisible(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch (item.getItemId()) {

            case R.id.filter_title:
                sharedPref.edit().putString("filter_bookmarksBY", "bookmarks_title").apply();
                setBookmarksList();
                editText.setVisibility(View.VISIBLE);
                helper_editText.showKeyboard(getActivity(), editText, 1, "", getString(R.string.action_filter_title));
                return true;
            case R.id.filter_url:
                sharedPref.edit().putString("filter_bookmarksBY", "bookmarks_content").apply();
                setBookmarksList();
                editText.setVisibility(View.VISIBLE);
                helper_editText.showKeyboard(getActivity(), editText, 1, "", getString(R.string.action_filter_url));
                return true;

            case R.id.filter_today:
                DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Calendar cal = Calendar.getInstance();
                final String search = dateFormat.format(cal.getTime());
                sharedPref.edit().putString("filter_bookmarksBY", "bookmarks_creation").apply();
                setBookmarksList();
                editText.setText(search);
                listBar.setText(getString(R.string.app_title_bookmarks) + " | " + getString(R.string.filter_today));
                return true;
            case R.id.filter_yesterday:
                DateFormat dateFormat2 = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Calendar cal2 = Calendar.getInstance();
                cal2.add(Calendar.DATE, -1);
                final String search2 = dateFormat2.format(cal2.getTime());
                sharedPref.edit().putString("filter_bookmarksBY", "bookmarks_creation").apply();
                setBookmarksList();
                editText.setText(search2);
                listBar.setText(getString(R.string.app_title_bookmarks) + " | " + getString(R.string.filter_yesterday));
                return true;
            case R.id.filter_before:
                DateFormat dateFormat3 = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Calendar cal3 = Calendar.getInstance();
                cal3.add(Calendar.DATE, -2);
                final String search3 = dateFormat3.format(cal3.getTime());
                sharedPref.edit().putString("filter_bookmarksBY", "bookmarks_creation").apply();
                setBookmarksList();
                editText.setText(search3);
                listBar.setText(getString(R.string.app_title_bookmarks) + " | " + getString(R.string.filter_before));
                return true;
            case R.id.filter_month:
                DateFormat dateFormat4 = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
                Calendar cal4 = Calendar.getInstance();
                final String search4 = dateFormat4.format(cal4.getTime());
                sharedPref.edit().putString("filter_bookmarksBY", "bookmarks_creation").apply();
                setBookmarksList();
                editText.setText(search4);
                listBar.setText(getString(R.string.app_title_bookmarks) + " | " + getString(R.string.filter_month));
                return true;
            case R.id.filter_own:
                sharedPref.edit().putString("filter_bookmarksBY", "bookmarks_creation").apply();
                setBookmarksList();
                editText.setVisibility(View.VISIBLE);
                helper_editText.showKeyboard(getActivity(), editText, 1, "", getString(R.string.action_filter_create));
                return true;
            case R.id.filter_clear:
                editText.setVisibility(View.GONE);
                setTitle();
                helper_editText.hideKeyboard(getActivity(), editText, 0, getString(R.string.app_title_history), getString(R.string.app_search_hint));
                setBookmarksList();
                return true;

            case R.id.sort_title:
                sharedPref.edit().putString("sortDBB", "title").apply();
                setBookmarksList();
                setTitle();
                return true;
            case R.id.sort_creation:
                sharedPref.edit().putString("sortDBB", "create").apply();
                setBookmarksList();
                setTitle();
                return true;

            case R.id.action_cancel:
                editText.setVisibility(View.GONE);
                setTitle();
                helper_editText.hideKeyboard(getActivity(), editText, 0, getString(R.string.app_title_bookmarks), getString(R.string.app_search_hint));
                setBookmarksList();
                return true;

            case R.id.action_delete:
                Snackbar snackbar = Snackbar
                        .make(listView, R.string.toast_list, Snackbar.LENGTH_LONG)
                        .setAction(R.string.toast_yes, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                getActivity().deleteDatabase("bookmarks_DB_v01.db");
                                db.open();
                                setBookmarksList();
                            }
                        });
                snackbar.show();
                return true;

            case R.id.action_favorite:
                if(db.isExistFav("true")){
                    Snackbar.make(listView, R.string.bookmark_setFav_not, Snackbar.LENGTH_LONG).show();
                }else{
                    sharedPref.edit().putString("startURL", "").apply();
                    Snackbar.make(listView, R.string.bookmark_setFav, Snackbar.LENGTH_LONG).show();
                }
                return true;

            case R.id.action_save_bookmark:

                String edit_id = sharedPref.getString("edit_id", "");
                String edit_content = sharedPref.getString("edit_content", "");
                String edit_icon = sharedPref.getString("edit_icon", "");
                String edit_attachment = sharedPref.getString("edit_attachment", "");
                String edit_creation = sharedPref.getString("edit_creation", "");

                String inputTag = editText.getText().toString().trim();
                db.update(Integer.parseInt(edit_id), inputTag, edit_content, edit_icon, edit_attachment, edit_creation);
                helper_editText.hideKeyboard(getActivity(), editText, 0, getString(R.string.app_title_bookmarks), getString(R.string.app_search_hint));
                setBookmarksList();

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
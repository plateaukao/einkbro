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

package de.baumann.browser.helper;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.TextView;

import com.mobapphome.mahencryptorlib.MAHEncryptor;

import java.util.ArrayList;
import java.util.List;

import de.baumann.browser.R;
import de.baumann.browser.databases.DbAdapter_Bookmarks;
import de.baumann.browser.databases.DbAdapter_Pass;

public class helper_editText {

    public static void editText_EditorAction(final EditText editText, final Activity from, final WebView mWebView, final TextView urlBar) {

        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

                if ( (actionId == EditorInfo.IME_ACTION_SEARCH) || ((event.getKeyCode() == KeyEvent.KEYCODE_ENTER) && (event.getAction() == KeyEvent.ACTION_DOWN ))){

                    String text = editText.getText().toString();
                    helper_webView.openURL(from, mWebView, editText);
                    helper_editText.hideKeyboard(from, editText, 0, text, from.getString(R.string.app_search_hint));
                    helper_editText.editText_EditorAction(editText, from, mWebView, urlBar);
                    urlBar.setVisibility(View.VISIBLE);
                    editText.setVisibility(View.GONE);
                    return true;
                } else {
                    return false;
                }
            }
        });
    }

    public static void editText_saveBookmark(final EditText editText, final Activity from, final WebView webView) {

        helper_editText.showKeyboard(from, editText, 2, helper_webView.getTitle (webView), from.getString(R.string.app_search_hint_bookmark));
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ( (actionId == EditorInfo.IME_ACTION_SEARCH) || ((event.getKeyCode() == KeyEvent.KEYCODE_ENTER) && (event.getAction() == KeyEvent.ACTION_DOWN ))){
                    helper_editText.editText_saveBookmark_save(editText, from, webView);
                    return true;
                } else {
                    return false;
                }
            }
        });
    }

    public static void editText_saveBookmark_save(final EditText editText, final Activity from, final WebView webView) {

        DbAdapter_Bookmarks db = new DbAdapter_Bookmarks(from);
        db.open();

        String inputTag = editText.getText().toString().trim();

        if(db.isExist(webView.getUrl())){
            Snackbar.make(editText, from.getString(R.string.toast_newTitle), Snackbar.LENGTH_LONG).show();
        }else{
            db.insert(inputTag, webView.getUrl(), "", "", helper_main.createDate());
            Snackbar.make(webView, R.string.bookmark_added, Snackbar.LENGTH_LONG).show();
        }
        helper_editText.hideKeyboard(from, editText, 0, webView.getTitle(), from.getString(R.string.app_search_hint));
    }

    public static void editText_savePass(final Activity from, final View view, final String title, final String url) {

        AlertDialog.Builder builder = new AlertDialog.Builder(from);
        View dialogView = View.inflate(from, R.layout.dialog_login, null);

        final EditText pass_title = (EditText) dialogView.findViewById(R.id.pass_title);
        final EditText pass_userName = (EditText) dialogView.findViewById(R.id.pass_userName);
        final EditText pass_userPW = (EditText) dialogView.findViewById(R.id.pass_userPW);

        new Handler().postDelayed(new Runnable() {
            public void run() {
                pass_title.setText(title);
            }
        }, 100);

        builder.setView(dialogView);
        builder.setTitle(R.string.pass_edit);
        builder.setPositiveButton(R.string.toast_yes, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int whichButton) {

                String input_pass_title = pass_title.getText().toString().trim();

                try {

                    final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(from);
                    MAHEncryptor mahEncryptor = MAHEncryptor.newInstance(sharedPref.getString("saved_key", ""));
                    String encrypted_userName = mahEncryptor.encode(pass_userName.getText().toString().trim());
                    String encrypted_userPW = mahEncryptor.encode(pass_userPW.getText().toString().trim());

                    DbAdapter_Pass db = new DbAdapter_Pass(from);
                    db.open();
                    if(db.isExist(input_pass_title)){
                        Snackbar.make(view, from.getString(R.string.toast_newTitle), Snackbar.LENGTH_LONG).show();
                    }else{
                        db.insert(input_pass_title, url, encrypted_userName, encrypted_userPW, helper_main.createDate());
                        Snackbar.make(view, R.string.pass_success, Snackbar.LENGTH_LONG).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Snackbar.make(view, R.string.toast_error, Snackbar.LENGTH_SHORT).show();
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
        helper_editText.showKeyboard(from, pass_title, 0, "", from.getString(R.string.pass_title));
    }

    public static void editText_searchSite (final EditText editText, final Activity from, final WebView webView, final TextView urlBar) {

        helper_editText.showKeyboard(from, editText, 1, "", from.getString(R.string.app_search_hint_site));
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((actionId == EditorInfo.IME_ACTION_SEARCH) || ((event.getKeyCode() == KeyEvent.KEYCODE_ENTER) && (event.getAction() == KeyEvent.ACTION_DOWN ))){
                    String text = editText.getText().toString();
                    webView.findAllAsync(text);
                    helper_editText.hideKeyboard(from, editText, 1, from.getString(R.string.app_search) + " " + text, from.getString(R.string.app_search_hint_site));
                    helper_editText.editText_EditorAction(editText, from, webView, urlBar);
                    return true;
                } else {
                    return false;
                }
            }
        });
    }

    public static void editText_searchWeb (final EditText editText, final Activity from) {

        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(from);

        List<String> listItems = new ArrayList<>();

        if (sharedPref.getBoolean("Duckduckgo", true)) {
            listItems.add("Duckduckgo");
        }
        if (sharedPref.getBoolean("Flickr", true)) {
            listItems.add("Flickr (creative common license)");
        }
        if (sharedPref.getBoolean("Github", true)) {
            listItems.add("Github");
        }
        if (sharedPref.getBoolean("Google", true)) {
            listItems.add("Google");
        }
        if (sharedPref.getBoolean("MetaGer", true)) {
            listItems.add("MetaGer");
        }
        if (sharedPref.getBoolean("Startpage", true)) {
            listItems.add("Startpage");
        }
        if (sharedPref.getBoolean("Wikipedia", true)) {
            listItems.add("Wikipedia");
        }
        if (sharedPref.getBoolean("YouTube", true)) {
            listItems.add("YouTube");
        }


        final CharSequence[] options = listItems.toArray(new CharSequence[listItems.size()]);

        new AlertDialog.Builder(from)
                .setTitle(R.string.action_searchChooseTitle)
                .setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int item) {
                        if (options[item].equals("Duckduckgo")) {
                            editText.setText(".d ");
                        }
                        if (options[item].equals("Flickr (creative common license)")) {
                            editText.setText(".f ");
                        }
                        if (options[item].equals("Github")) {
                            editText.setText(".g ");
                        }
                        if (options[item].equals("Google")) {
                            editText.setText(".G ");
                        }
                        if (options[item].equals("MetaGer")) {
                            editText.setText(".m ");
                        }
                        if (options[item].equals("Startpage")) {
                            editText.setText(".s ");
                        }
                        if (options[item].equals("Wikipedia")) {
                            editText.setText(".w ");
                        }
                        if (options[item].equals("YouTube")) {
                            editText.setText(".y ");
                        }
                        editText.setSelection(editText.length());
                    }
                }).show();
    }

    public static void hideKeyboard(Activity from, EditText editText, int i, String text, String hint) {
        editText.clearFocus();
        editText.setText(text);
        editText.setHint(hint);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(from);
        sharedPref.edit()
                .putInt("keyboard", i)
                .apply();
        from.invalidateOptionsMenu();
        InputMethodManager imm = (InputMethodManager) from.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
    }

    public static void showKeyboard(final Activity from, final EditText editText, final int i, String text, String hint) {
        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(from);
        editText.requestFocus();
        editText.hasFocus();
        editText.setText(text);
        editText.setHint(hint);
        editText.setSelection(editText.length());
        new Handler().postDelayed(new Runnable() {
            public void run() {
                sharedPref.edit()
                        .putInt("keyboard", i)
                        .apply();
                from.invalidateOptionsMenu();
                InputMethodManager imm = (InputMethodManager) from.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
            }
        }, 200);
    }

    public static void editText_FocusChange(final EditText editText, final Activity from) {

        editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    helper_editText.showKeyboard(from, editText, 3, "", from.getString(R.string.app_search_hint));
                } else {
                    helper_editText.hideKeyboard(from, editText, 0, "", from.getString(R.string.app_search_hint));
                }
            }
        });
    }

    public static void editText_FocusChange_searchSite(final EditText editText, final Activity from) {

        editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    helper_editText.showKeyboard(from, editText, 1, "", from.getString(R.string.app_search_hint_site));
                } else {
                    helper_editText.hideKeyboard(from, editText, 0, "", from.getString(R.string.app_search_hint));
                }
            }
        });
    }
}

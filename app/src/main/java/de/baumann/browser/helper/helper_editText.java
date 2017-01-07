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

import net.sqlcipher.database.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import de.baumann.browser.R;
import de.baumann.browser.databases.Database_Bookmarks;
import de.baumann.browser.databases.Database_Pass;

public class helper_editText {

    public static void editText_EditorAction(final EditText editText, final Activity from, final WebView mWebView, final TextView urlBar) {

        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(from);

        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

                if ( (actionId == EditorInfo.IME_ACTION_SEARCH) || ((event.getKeyCode() == KeyEvent.KEYCODE_ENTER) && (event.getAction() == KeyEvent.ACTION_DOWN ))){

                    String text = editText.getText().toString();
                    String searchEngine = sharedPref.getString("searchEngine", "https://startpage.com/do/search?query=");
                    String wikiLang = sharedPref.getString("wikiLang", "en");


                    if (text.startsWith("www")) {
                        mWebView.loadUrl("http://" + text);
                    } else if (text.contains("http")) {
                        mWebView.loadUrl(text);
                    } else if (text.contains(".w ")) {
                        mWebView.loadUrl("https://" + wikiLang + ".wikipedia.org/wiki/Spezial:Suche?search=" + text.substring(3));
                    } else if (text.startsWith(".f ")) {
                        mWebView.loadUrl("https://www.flickr.com/search/?advanced=1&license=2%2C3%2C4%2C5%2C6%2C9&text=" + text.substring(3));
                    } else  if (text.startsWith(".m ")) {
                        mWebView.loadUrl("https://metager.de/meta/meta.ger3?focus=web&eingabe=" + text.substring(3));
                    } else if (text.startsWith(".g ")) {
                        mWebView.loadUrl("https://github.com/search?utf8=✓&q=" + text.substring(3));
                    } else  if (text.startsWith(".s ")) {
                        if (Locale.getDefault().getLanguage().contentEquals("de")){
                            mWebView.loadUrl("https://startpage.com/do/search?query=" + text.substring(3) + "&lui=deutsch&l=deutsch");
                        } else {
                            mWebView.loadUrl("https://startpage.com/do/search?query=" + text.substring(3));
                        }
                    } else if (text.startsWith(".G ")) {
                        if (Locale.getDefault().getLanguage().contentEquals("de")){
                            mWebView.loadUrl("https://www.google.de/search?&q=" + text.substring(3));
                        } else {
                            mWebView.loadUrl("https://www.google.com/search?&q=" + text.substring(3));
                        }
                    } else  if (text.startsWith(".y ")) {
                        if (Locale.getDefault().getLanguage().contentEquals("de")){
                            mWebView.loadUrl("https://www.youtube.com/results?hl=de&gl=DE&search_query=" + text.substring(3));
                        } else {
                            mWebView.loadUrl("https://www.youtube.com/results?search_query=" + text.substring(3));
                        }
                    } else  if (text.startsWith(".d ")) {
                        if (Locale.getDefault().getLanguage().contentEquals("de")){
                            mWebView.loadUrl("https://duckduckgo.com/?q=" + text.substring(3) + "&kl=de-de&kad=de_DE&k1=-1&kaj=m&kam=osm&kp=-1&kak=-1&kd=1&t=h_&ia=web");
                        } else {
                            mWebView.loadUrl("https://duckduckgo.com/?q=" + text.substring(3));
                        }
                    } else {
                        if (searchEngine.contains("https://duckduckgo.com/?q=")) {
                            if (Locale.getDefault().getLanguage().contentEquals("de")){
                                mWebView.loadUrl("https://duckduckgo.com/?q=" + text + "&kl=de-de&kad=de_DE&k1=-1&kaj=m&kam=osm&kp=-1&kak=-1&kd=1&t=h_&ia=web");
                            } else {
                                mWebView.loadUrl("https://duckduckgo.com/?q=" + text);
                            }
                        } else if (searchEngine.contains("https://metager.de/meta/meta.ger3?focus=web&eingabe=")) {
                            if (Locale.getDefault().getLanguage().contentEquals("de")){
                                mWebView.loadUrl("https://metager.de/meta/meta.ger3?focus=web&eingabe=" + text);
                            } else {
                                mWebView.loadUrl("https://metager.de/meta/meta.ger3?focus=web&eingabe=" + text +"&focus=web&encoding=utf8&lang=eng");
                            }
                        } else if (searchEngine.contains("https://startpage.com/do/search?query=")) {
                            if (Locale.getDefault().getLanguage().contentEquals("de")){
                                mWebView.loadUrl("https://startpage.com/do/search?query=" + text + "&lui=deutsch&l=deutsch");
                            } else {
                                mWebView.loadUrl("https://startpage.com/do/search?query=" + text);
                            }
                        }else {
                            mWebView.loadUrl(searchEngine + text);
                        }
                    }

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

        helper_editText.showKeyboard(from, editText, 2, webView.getTitle(), from.getString(R.string.app_search_hint_bookmark));
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

        try {

            final Database_Bookmarks db = new Database_Bookmarks(from);
            String inputTag = editText.getText().toString().trim();

            db.addBookmark(inputTag, webView.getUrl());
            db.close();
            Snackbar.make(webView, R.string.bookmark_added, Snackbar.LENGTH_SHORT).show();

        } catch (Exception e) {
            e.printStackTrace();
        }
        helper_editText.hideKeyboard(from, editText, 0, webView.getTitle(), from.getString(R.string.app_search_hint));
    }

    public static void editText_savePass(final Activity from, final View view, final String title, final String url) {

        SQLiteDatabase.loadLibs(from);

        try {

            final class_SecurePreferences sharedPrefSec = new class_SecurePreferences(from, "sharedPrefSec", "Ywn-YM.XK$b:/:&CsL8;=L,y4", true);
            final Database_Pass db = new Database_Pass(from);

            AlertDialog.Builder builder = new AlertDialog.Builder(from);
            View dialogView = View.inflate(from, R.layout.dialog_login, null);

            final EditText pass_title = (EditText) dialogView.findViewById(R.id.pass_title);
            pass_title.setText(title);
            final EditText pass_userName = (EditText) dialogView.findViewById(R.id.pass_userName);
            final EditText pass_userPW = (EditText) dialogView.findViewById(R.id.pass_userPW);

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

                    db.addBookmark(
                            sharedPrefSec.getString(url + "TI"),
                            url,
                            sharedPrefSec.getString(url + "UN"),
                            sharedPrefSec.getString(url + "PW"));
                    db.close();
                    Snackbar.make(view, R.string.pass_success, Snackbar.LENGTH_SHORT).show();
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

        } catch (Exception e) {
            e.printStackTrace();
        }
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
        }, 100);
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

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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import de.baumann.browser.R;
import de.baumann.browser.databases.Database_Bookmarks;
import de.baumann.browser.databases.Database_Pass;
import de.baumann.browser.popups.Popup_bookmarks;
import de.baumann.browser.popups.Popup_readLater;

public class helper_editText {


    public static void editText_Touch(EditText editText, final Activity from, final WebView webview) {

        editText.setOnTouchListener(new class_OnSwipeTouchListener_editText(from) {
            public void onSwipeTop() {
                helper_webView.closeWebView(from, webview);
                from.finishAffinity();
            }
            public void onSwipeRight() {
                helper_main.switchToActivity(from, Popup_readLater.class, "", false);
            }
            public void onSwipeLeft() {
                helper_main.switchToActivity(from, Popup_bookmarks.class, "", false);
            }
        });
    }

    public static void editText_Touch_Bookmark (EditText editText, final Activity from) {

        editText.setOnTouchListener(new class_OnSwipeTouchListener_editText(from) {
            public void onSwipeTop() {
                helper_main.isClosed(from);
                from.finishAffinity();
            }
            public void onSwipeRight() {
                helper_main.switchToActivity(from, Popup_readLater.class, "", false);
            }
            public void onSwipeLeft() {
                helper_main.switchToActivity(from, Popup_bookmarks.class, "", false);
            }
        });
    }

    public static void editText_EditorAction(final EditText editText, final Activity from, final WebView webView) {

        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(from);
        final String searchEngine = sharedPref.getString("searchEngine", "https://startpage.com/do/search?query=");
        final String wikiLang = sharedPref.getString("wikiLang", "en");

        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ( (actionId == EditorInfo.IME_ACTION_SEARCH) || ((event.getKeyCode() == KeyEvent.KEYCODE_ENTER) && (event.getAction() == KeyEvent.ACTION_DOWN ))){
                    String text = editText.getText().toString();

                    if (text.length() > 3) {
                        String subStr=text.substring(3);

                        if (text.startsWith("www")) {
                            webView.loadUrl("http://" + text);
                        } else if (text.contains("http")) {
                            webView.loadUrl(text);
                        } else if (text.contains(".w ")) {
                            webView.loadUrl("https://" + wikiLang + ".wikipedia.org/wiki/Spezial:Suche?search=" + subStr);
                        } else if (text.startsWith(".f ")) {
                            webView.loadUrl("https://www.flickr.com/search/?advanced=1&license=2%2C3%2C4%2C5%2C6%2C9&text=" + subStr);
                        } else  if (text.startsWith(".m ")) {
                            webView.loadUrl("https://metager.de/meta/meta.ger3?focus=web&eingabe=" + subStr);
                        } else if (text.startsWith(".g ")) {
                            webView.loadUrl("https://github.com/search?utf8=✓&q=" + subStr);
                        } else  if (text.startsWith(".s ")) {
                            if (Locale.getDefault().getLanguage().contentEquals("de")) {
                                webView.loadUrl("https://startpage.com/do/search?query=" + subStr + "&lui=deutsch&l=deutsch");
                            } else {
                                webView.loadUrl("https://startpage.com/do/search?query=" + subStr);
                            }
                        } else if (text.startsWith(".G ")) {
                            webView.loadUrl("https://www.google.com/search?&q=" + subStr);
                        } else  if (text.startsWith(".y ")) {
                            webView.loadUrl("https://www.youtube.com/results?search_query=" + subStr);
                        } else  if (text.startsWith(".d ")) {
                            webView.loadUrl("https://duckduckgo.com/?q=" + subStr);
                        } else {
                            if (sharedPref.getString("searchEngine", "https://startpage.com/do/search?query=").equals("https://startpage.com/do/search?query=")) {
                                if (Locale.getDefault().getLanguage().contentEquals("de")) {
                                    webView.loadUrl("https://startpage.com/do/search?query=" + text + "&lui=deutsch&l=deutsch");
                                } else {
                                    webView.loadUrl("https://startpage.com/do/search?query=" + text);
                                }
                            } else {
                                webView.loadUrl(searchEngine + text);
                            }
                        }
                    }

                    editText.setText(text);
                    editText.clearFocus();
                    InputMethodManager imm = (InputMethodManager)from.getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
                    return true;
                } else {
                    return false;
                }
            }
        });
    }

    public static void editText_saveBookmark(final EditText editText, final Activity from, final WebView webView) {

        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(from);
        sharedPref.edit()
                .putInt("keyboard", 2)
                .apply();
        from.invalidateOptionsMenu();

        (new Handler()).postDelayed(new Runnable() {
            public void run() {
                editText.requestFocus();
                helper_main.showKeyboard(from, editText);
                editText.setText(webView.getTitle());
                editText.setSelection(editText.getText().length());
            }
        }, 200);

        editText.setHint(R.string.app_search_hint_bookmark);
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ( (actionId == EditorInfo.IME_ACTION_SEARCH) || ((event.getKeyCode() == KeyEvent.KEYCODE_ENTER) && (event.getAction() == KeyEvent.ACTION_DOWN ))){

                    helper_editText.editText_saveBookmark_save(editText,from,webView);
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

        editText.setText(webView.getTitle());
        editText.clearFocus();
        InputMethodManager imm = (InputMethodManager)from.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);

        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(from);
        sharedPref.edit()
                .putInt("keyboard", 0)
                .apply();
        from.invalidateOptionsMenu();
    }

    public static void editText_savePass(final Activity from, final View view, final String title, final String url) {

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

            new Handler().postDelayed(new Runnable() {
                public void run() {
                    helper_main.showKeyboard(from, pass_title);
                }
            }, 200);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void editText_searchSite (final EditText editText, final Activity from, final WebView webView) {

        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(from);
        sharedPref.edit()
                .putInt("keyboard", 1)
                .apply();
        from.invalidateOptionsMenu();

        (new Handler()).postDelayed(new Runnable() {
            public void run() {
                editText.requestFocus();
                editText.setText("");
                editText.setHint(R.string.app_search_hint_site);
                helper_main.showKeyboard(from, editText);
            }
        }, 200);

        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((actionId == EditorInfo.IME_ACTION_SEARCH) || ((event.getKeyCode() == KeyEvent.KEYCODE_ENTER) && (event.getAction() == KeyEvent.ACTION_DOWN ))){
                    String text = editText.getText().toString();
                    editText.setText(from.getString(R.string.app_search) + " " + text);
                    webView.findAllAsync(text);
                    editText.clearFocus();
                    helper_main.hideKeyboard(from, editText);
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

    public static void editText_FocusChange(final EditText editText, final Activity from) {

        editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(from);
                if (hasFocus) {
                    if (sharedPref.getInt("keyboard", 0) == 0) {
                        sharedPref.edit()
                                .putInt("keyboard", 3)
                                .apply();
                        from.invalidateOptionsMenu();
                    }
                    editText.setText("");
                    InputMethodManager imm = (InputMethodManager) from.getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
                } else {
                    if (sharedPref.getInt("keyboard", 0) == 2 || sharedPref.getInt("keyboard", 0) == 3) {
                        sharedPref.edit()
                                .putInt("keyboard", 0)
                                .apply();
                        from.invalidateOptionsMenu();
                    }
                    InputMethodManager imm = (InputMethodManager)from.getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
                }
            }
        });
    }
}

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
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import de.baumann.browser.Browser_1;
import de.baumann.browser.Browser_2;
import de.baumann.browser.Browser_3;
import de.baumann.browser.Browser_4;
import de.baumann.browser.Browser_5;
import de.baumann.browser.R;
import de.baumann.browser.lists.List_bookmarks;
import de.baumann.browser.lists.List_readLater;

public class helper_toolbar {

    public static void toolbarBrowser (final Activity activity, final WebView webview, final Toolbar toolbar) {

        toolbar.setOnTouchListener(new class_OnSwipeTouchListener_editText(activity) {
            public void onSwipeTop() {
                helper_main.closeApp(activity, webview);
            }
            public void onSwipeRight() {
                helper_main.switchToActivity(activity, List_readLater.class, "", false);
            }
            public void onSwipeLeft() {
                helper_main.switchToActivity(activity, List_bookmarks.class, "", false);
            }
        });
        toolbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                helper_toolbar.toolbarOnclick(activity);
            }
        });
    }

    public static void toolbarActivities (final Activity activity, final Toolbar toolbar) {

        toolbar.setOnTouchListener(new class_OnSwipeTouchListener_editText(activity) {
            public void onSwipeTop() {
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(activity);
                sharedPref.edit().putInt("closeApp", 1).apply();
                activity.finish();
            }
            public void onSwipeRight() {
                helper_main.switchToActivity(activity, List_readLater.class, "", true);
            }
            public void onSwipeLeft() {
                helper_main.switchToActivity(activity, List_bookmarks.class, "", true);
            }
        });
        toolbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                helper_toolbar.toolbarOnclick(activity);
            }
        });
    }

    private static void toolbarViews (final Activity activity, TextView textView, ImageView imageView,
                                         CardView cardView, final HorizontalScrollView horizontalScrollView,
                                         int tab, String text, String picture, final Class newTab) {

        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(activity);

        textView.setText(text);
        if ( sharedPref.getInt("actualTab", 1) == tab) {
            textView.setBackgroundColor(ContextCompat.getColor(activity, R.color.colorAccent));
        }

        try {
            Glide.with(activity)
                    .load(activity.getFilesDir() + picture) // or URI/path
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .into(imageView); //imageView to set thumbnail to
        } catch (Exception e) {
            Log.w("Browser", "Error load thumbnail", e);
            imageView.setVisibility(View.GONE);
        }

        cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sharedPref.edit().putString("openURL", "").apply();
                Intent intent = new Intent(activity, newTab);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                activity.startActivity(intent);
                horizontalScrollView.setVisibility(View.GONE);
            }
        });

        cardView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                sharedPref.edit().putString("openURL", sharedPref.getString("startURL", "https://github.com/scoute-dich/browser/")).apply();
                Intent intent = new Intent(activity, newTab);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                activity.startActivity(intent);
                horizontalScrollView.setVisibility(View.GONE);
                return false;
            }
        });

    }

    private static void toolbarOnclick (final Activity activity) {

        final HorizontalScrollView scrollTabs = (HorizontalScrollView) activity.findViewById(R.id.scrollTabs);

        if (scrollTabs.getVisibility() == View.GONE) {
            scrollTabs.setVisibility(View.VISIBLE);

            TextView context_1 = (TextView) activity.findViewById(R.id.context_1);
            ImageView context_1_preView = (ImageView) activity.findViewById(R.id.context_1_preView);
            CardView context_1_Layout = (CardView) activity.findViewById(R.id.context_1_Layout);
            helper_toolbar.toolbarViews(activity, context_1, context_1_preView, context_1_Layout, scrollTabs,
                    1, helper_browser.tab_1(activity), "/tab_1.jpg", Browser_1.class);

            TextView context_2 = (TextView) activity.findViewById(R.id.context_2);
            ImageView context_2_preView = (ImageView) activity.findViewById(R.id.context_2_preView);
            CardView context_2_Layout = (CardView) activity.findViewById(R.id.context_2_Layout);
            helper_toolbar.toolbarViews(activity, context_2, context_2_preView, context_2_Layout, scrollTabs,
                    2, helper_browser.tab_2(activity), "/tab_2.jpg", Browser_2.class);

            TextView context_3 = (TextView) activity.findViewById(R.id.context_3);
            ImageView context_3_preView = (ImageView) activity.findViewById(R.id.context_3_preView);
            CardView context_3_Layout = (CardView) activity.findViewById(R.id.context_3_Layout);
            helper_toolbar.toolbarViews(activity, context_3, context_3_preView, context_3_Layout, scrollTabs,
                    3, helper_browser.tab_3(activity), "/tab_3.jpg", Browser_3.class);

            TextView context_4 = (TextView) activity.findViewById(R.id.context_4);
            ImageView context_4_preView = (ImageView) activity.findViewById(R.id.context_4_preView);
            CardView context_4_Layout = (CardView) activity.findViewById(R.id.context_4_Layout);
            helper_toolbar.toolbarViews(activity, context_4, context_4_preView, context_4_Layout, scrollTabs,
                    4, helper_browser.tab_4(activity), "/tab_4.jpg", Browser_4.class);

            TextView context_5 = (TextView) activity.findViewById(R.id.context_5);
            ImageView context_5_preView = (ImageView) activity.findViewById(R.id.context_5_preView);
            CardView context_5_Layout = (CardView) activity.findViewById(R.id.context_5_Layout);
            helper_toolbar.toolbarViews(activity, context_5, context_5_preView, context_5_Layout, scrollTabs,
                    5, helper_browser.tab_5(activity), "/tab_5.jpg", Browser_5.class);

        } else {
            scrollTabs.setVisibility(View.GONE);
        }
    }
}

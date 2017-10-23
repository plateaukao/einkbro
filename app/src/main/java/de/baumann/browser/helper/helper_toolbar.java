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
import android.app.Dialog;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.design.widget.AppBarLayout;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.Picasso;

import java.io.File;

import de.baumann.browser.R;
import de.baumann.browser.fragments.Fragment_Browser;

import static android.content.ContentValues.TAG;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class helper_toolbar {

    public static void toolBarPreview (final Activity activity, TextView textView, ImageView imageView,
                                       int tab, String text, final String picture, ImageView close) {

        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(activity);

        textView.setText(text);
        if (sharedPref.getInt("tab", 0) == tab) {
            TypedValue typedValue = new TypedValue();
            Resources.Theme theme = activity.getTheme();
            theme.resolveAttribute(R.attr.colorAccent, typedValue, true);
            int color = typedValue.data;
            textView.setBackgroundColor(color);
        } else {
            TypedValue typedValue = new TypedValue();
            Resources.Theme theme = activity.getTheme();
            theme.resolveAttribute(R.attr.colorAccent_trans, typedValue, true);
            int color = typedValue.data;
            textView.setBackgroundColor(color);
        }

        if (text.equals(activity.getString(R.string.context_tab))) {
            close.setVisibility(View.GONE);
        } else {
            close.setVisibility(View.VISIBLE);
        }

        try {
            Uri uri = Uri.fromFile(new File(activity.getFilesDir() + picture));
            Picasso.with(activity).load(uri).memoryPolicy(MemoryPolicy.NO_CACHE).into(imageView);
        } catch (Exception e) {
            Log.w("Browser", "Error load thumbnail", e);
            imageView.setVisibility(View.GONE);
        }
    }

    public static void toolbarGestures(final Activity activity, final Toolbar toolbar, final class_CustomViewPager viewPager) {

        final HorizontalScrollView scrollTabs = (HorizontalScrollView) activity.findViewById(R.id.scrollTabs);
        final AppBarLayout appBarLayout = (AppBarLayout) activity.findViewById(R.id.appBarLayout);
        appBarLayout.setVisibility(View.VISIBLE);

        toolbar.setOnTouchListener(new class_OnSwipeTouchListener_editText(activity) {
            public void onSwipeTop() {
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(activity);
                sharedPref.edit().putInt("closeApp", 1).apply();
                if (viewPager.getCurrentItem() == 0) {
                    viewPager.setCurrentItem(1);
                } else {
                    viewPager.setCurrentItem(0);
                }
            }
            public void onSwipeRight() {
                viewPager.setCurrentItem(6);
                scrollTabs.setVisibility(View.GONE);
            }
            public void onSwipeLeft() {
                viewPager.setCurrentItem(5);
                scrollTabs.setVisibility(View.GONE);
            }
        });

        toolbar.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (viewPager.getCurrentItem() < 5) {
                    final HorizontalScrollView scrollTabs = (HorizontalScrollView) activity.findViewById(R.id.scrollTabs);

                    if (scrollTabs.getVisibility() == View.GONE) {
                        scrollTabs.setVisibility(View.VISIBLE);

                        TextView context_1 = (TextView) activity.findViewById(R.id.context_1);
                        ImageView context_1_preView = (ImageView) activity.findViewById(R.id.context_1_preView);
                        ImageView close_1 = (ImageView) activity.findViewById(R.id.close_1);
                        CardView context_1_Layout = (CardView) activity.findViewById(R.id.context_1_Layout);
                        helper_toolbar.cardViewClick(activity, context_1_Layout, scrollTabs, 0, close_1, viewPager, "0");
                        helper_toolbar.toolBarPreview(activity, context_1,context_1_preView, 0, helper_browser.tab_1(activity), "/tab_0.jpg", close_1);

                        TextView context_2 = (TextView) activity.findViewById(R.id.context_2);
                        ImageView context_2_preView = (ImageView) activity.findViewById(R.id.context_2_preView);
                        ImageView close_2 = (ImageView) activity.findViewById(R.id.close_2);
                        CardView context_2_Layout = (CardView) activity.findViewById(R.id.context_2_Layout);
                        helper_toolbar.cardViewClick(activity, context_2_Layout, scrollTabs, 1, close_2, viewPager, "1");
                        helper_toolbar.toolBarPreview(activity, context_2,context_2_preView, 1, helper_browser.tab_2(activity), "/tab_1.jpg", close_2);

                        TextView context_3 = (TextView) activity.findViewById(R.id.context_3);
                        ImageView context_3_preView = (ImageView) activity.findViewById(R.id.context_3_preView);
                        ImageView close_3 = (ImageView) activity.findViewById(R.id.close_3);
                        CardView context_3_Layout = (CardView) activity.findViewById(R.id.context_3_Layout);
                        helper_toolbar.cardViewClick(activity, context_3_Layout, scrollTabs, 2, close_3, viewPager, "2");
                        helper_toolbar.toolBarPreview(activity, context_3,context_3_preView, 2, helper_browser.tab_3(activity), "/tab_2.jpg", close_3);

                        TextView context_4 = (TextView) activity.findViewById(R.id.context_4);
                        ImageView context_4_preView = (ImageView) activity.findViewById(R.id.context_4_preView);
                        ImageView close_4 = (ImageView) activity.findViewById(R.id.close_4);
                        CardView context_4_Layout = (CardView) activity.findViewById(R.id.context_4_Layout);
                        helper_toolbar.cardViewClick(activity, context_4_Layout, scrollTabs, 3, close_4, viewPager, "3");
                        helper_toolbar.toolBarPreview(activity, context_4,context_4_preView, 3, helper_browser.tab_4(activity), "/tab_3.jpg", close_4);

                        TextView context_5 = (TextView) activity.findViewById(R.id.context_5);
                        ImageView context_5_preView = (ImageView) activity.findViewById(R.id.context_5_preView);
                        ImageView close_5 = (ImageView) activity.findViewById(R.id.close_5);
                        CardView context_5_Layout = (CardView) activity.findViewById(R.id.context_5_Layout);
                        helper_toolbar.cardViewClick(activity, context_5_Layout, scrollTabs, 4, close_5, viewPager, "4");
                        helper_toolbar.toolBarPreview(activity, context_5,context_5_preView, 4, helper_browser.tab_5(activity), "/tab_4.jpg", close_5);

                    } else {
                        scrollTabs.setVisibility(View.GONE);
                    }
                } else {
                    Log.i(TAG, "Switched to list");
                }
                return true;
            }
        });
    }
    
    private static void cardViewClick(final Activity activity, CardView cardView, final HorizontalScrollView horizontalScrollView,
                                      final int newTab, final ImageView close, final class_CustomViewPager viewPager,
                                      final String tab) {

        final int actualTab = viewPager.getCurrentItem();
        final int clickedTab = viewPager.getCurrentItem();
        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(activity);

        cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sharedPref.edit().putString("openURL", "").apply();
                viewPager.setCurrentItem(newTab);
                horizontalScrollView.setVisibility(View.GONE);
            }
        });

        cardView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                sharedPref.edit().putString("openURL", sharedPref.getString("startURL", "https://github.com/scoute-dich/browser/")).apply();
                viewPager.setCurrentItem(newTab);
                horizontalScrollView.setVisibility(View.GONE);

                if (actualTab == clickedTab) {
                    Fragment_Browser fragment = (Fragment_Browser) viewPager.getAdapter().instantiateItem(viewPager, viewPager.getCurrentItem());
                    fragment.fragmentAction();
                }
                return false;
            }
        });

        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                File preview = new File(activity.getFilesDir() + "/tab_" + tab + ".jpg");
                preview.delete();
                sharedPref.edit().putString("tab_" + tab, "").apply();
                sharedPref.edit().putString("openURL","").apply();
                horizontalScrollView.setVisibility(View.GONE);

                if (actualTab > 0 && actualTab == newTab) {
                    viewPager.setCurrentItem(actualTab - 1);
                }
            }
        });
    }

    public static void cardViewClickMenu (final Activity activity, CardView cardView, final HorizontalScrollView horizontalScrollView,
                                          final int newTab, final ImageView close, final class_CustomViewPager viewPager, final String url,
                                          final Dialog dialog, final String tab) {

        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(activity);

        cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sharedPref.edit().putString("openURL", url).apply();
                viewPager.setCurrentItem(newTab);
                dialog.cancel();
            }
        });

        cardView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                sharedPref.edit().putString("openURL", sharedPref.getString("startURL", "https://github.com/scoute-dich/browser/")).apply();
                viewPager.setCurrentItem(newTab);
                dialog.cancel();
                return false;
            }
        });

        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                File preview = new File(activity.getFilesDir() + "/tab_" + tab + ".jpg");
                preview.delete();
                sharedPref.edit().putString("tab_" + tab, "").apply();
                horizontalScrollView.setVisibility(View.GONE);
            }
        });
    }
}

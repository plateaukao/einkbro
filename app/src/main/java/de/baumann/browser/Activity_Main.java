package de.baumann.browser;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import de.baumann.browser.helper.class_CustomViewPager;
import de.baumann.browser.helper.helper_browser;
import de.baumann.browser.helper.helper_editText;
import de.baumann.browser.helper.helper_main;
import de.baumann.browser.fragments.Fragment_Bookmarks;
import de.baumann.browser.fragments.Fragment_Browser;
import de.baumann.browser.fragments.Fragment_Files;
import de.baumann.browser.fragments.Fragment_History;
import de.baumann.browser.fragments.Fragment_Pass;
import de.baumann.browser.fragments.Fragment_ReadLater;
import de.baumann.browser.helper.helper_toolbar;

public class Activity_Main extends AppCompatActivity {


    // Views

    private class_CustomViewPager viewPager;
    private TextView urlBar;
    private TextView listBar;
    private EditText editText;
    private Toolbar toolbar;


    // Others

    private SharedPreferences sharedPref;


    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Activity activity = Activity_Main.this;

        helper_main.setTheme(activity);

        setContentView(R.layout.activity_main);
        WebView.enableSlowWholeDocumentDraw();

        sharedPref = PreferenceManager.getDefaultSharedPreferences(activity);
        sharedPref.edit().putBoolean("isOpened", true).apply();

        helper_main.checkPin(activity);
        helper_main.onStart(activity);
        helper_main.grantPermissionsStorage(activity);
        helper_browser.resetTabs(activity);


        // show changelog

        final String versionName = BuildConfig.VERSION_NAME;
        String oldVersionName = sharedPref.getString("oldVersionName", "0.0");

        if (!oldVersionName.equals(versionName)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle(R.string.app_changelog);
            builder.setMessage(helper_main.textSpannable(activity.getString(R.string.changelog_text)));
            builder.setPositiveButton(
                    getString(R.string.toast_yes),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            sharedPref.edit().putString("oldVersionName", versionName).apply();
                            dialog.cancel();
                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();
        }


        // find Views

        urlBar = (TextView) findViewById(R.id.urlBar);
        listBar = (TextView) findViewById(R.id.listBar);
        editText = (EditText) findViewById(R.id.editText);
        viewPager = (class_CustomViewPager) findViewById(R.id.viewpager);
        viewPager.setOffscreenPageLimit(10);
        toolbar = (Toolbar) findViewById(R.id.toolbar);

        if (sharedPref.getBoolean ("swipe", false)){
            sharedPref.edit().putString("swipe_string", activity.getString(R.string.app_yes)).apply();
            viewPager.setPagingEnabled(true);
        } else {
            sharedPref.edit().putString("swipe_string", activity.getString(R.string.app_no)).apply();
            viewPager.setPagingEnabled(false);
        }

        viewPager.addOnPageChangeListener(new class_CustomViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {

                helper_editText.hideKeyboard(activity, editText, 0, "", getString(R.string.app_search_hint));
                helper_toolbar.toolbarGestures(activity, toolbar, viewPager);

                assert getSupportActionBar() != null;

                if (position < 5) {
                    getSupportActionBar().setDisplayHomeAsUpEnabled(false);
                    urlBar.setVisibility(View.VISIBLE);
                    listBar.setVisibility(View.GONE);
                } else {
                    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                    urlBar.setVisibility(View.GONE);
                    listBar.setVisibility(View.VISIBLE);
                }

                if (position < 5) {
                    Fragment_Browser fragment = (Fragment_Browser) viewPager.getAdapter().instantiateItem(viewPager, viewPager.getCurrentItem());
                    fragment.fragmentAction();
                } else if (position == 5) {
                    Fragment_Bookmarks fragment = (Fragment_Bookmarks) viewPager.getAdapter().instantiateItem(viewPager, viewPager.getCurrentItem());
                    fragment.fragmentAction();
                } else if (position == 6) {
                    Fragment_ReadLater fragment = (Fragment_ReadLater) viewPager.getAdapter().instantiateItem(viewPager, viewPager.getCurrentItem());
                    fragment.fragmentAction();
                } else if (position == 7) {
                    Fragment_History fragment = (Fragment_History) viewPager.getAdapter().instantiateItem(viewPager, viewPager.getCurrentItem());
                    fragment.fragmentAction();
                } else if (position == 8) {
                    Fragment_Pass fragment = (Fragment_Pass) viewPager.getAdapter().instantiateItem(viewPager, viewPager.getCurrentItem());
                    fragment.fragmentAction();
                } else {
                    Fragment_Files fragment = (Fragment_Files) viewPager.getAdapter().instantiateItem(viewPager, viewPager.getCurrentItem());
                    fragment.fragmentAction();
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        setSupportActionBar(toolbar);
        setupViewPager(viewPager);
        helper_toolbar.toolbarGestures(activity, toolbar, viewPager);
        onNewIntent(getIntent());
    }

    @Override
    public void onResume(){
        super.onResume();

        final int position = viewPager.getCurrentItem();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (position < 5) {
                    Fragment_Browser fragment = (Fragment_Browser) viewPager.getAdapter().instantiateItem(viewPager, viewPager.getCurrentItem());
                    fragment.fragmentAction();
                }
            }
        }, 100);
    }

    protected void onNewIntent(final Intent intent) {

        String action = intent.getAction();
        Handler handler = new Handler();

        if (Intent.ACTION_SEND.equals(action)) {
            String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
            String searchEngine = sharedPref.getString("searchEngine", "https://duckduckgo.com/?q=");
            sharedPref.edit().putString("openURL", searchEngine + sharedText).apply();
            handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    viewPager.setCurrentItem(0);
                }
            }, 250);
        } else if (Intent.ACTION_VIEW.equals(action)) {
            Uri data = intent.getData();
            String link = data.toString();
            sharedPref.edit().putString("openURL", link).apply();
            handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    viewPager.setCurrentItem(0);
                }
            }, 250);
        } else if ("readLater".equals(action)) {
            sharedPref.edit().putInt("appShortcut", 1).apply();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    viewPager.setCurrentItem(6);
                }
            }, 250);
        } else if ("bookmarks".equals(action)) {
            sharedPref.edit().putInt("appShortcut", 1).apply();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    viewPager.setCurrentItem(5);
                }
            }, 250);
        } else if ("history".equals(action)) {
            sharedPref.edit().putInt("appShortcut", 1).apply();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    viewPager.setCurrentItem(7);
                }
            }, 250);
        } else if ("pass".equals(action)) {
            sharedPref.edit().putInt("appShortcut", 1).apply();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    viewPager.setCurrentItem(8);
                }
            }, 250);
        }
    }

    private void setupViewPager(final class_CustomViewPager viewPager) {

        final String startTab = sharedPref.getString("tabMain", "0");
        final int startTabInt = Integer.parseInt(startTab);

        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());

        adapter.addFragment(new Fragment_Browser(), String.valueOf(getString(R.string.app_name)));
        adapter.addFragment(new Fragment_Browser(), String.valueOf(getString(R.string.app_name)));
        adapter.addFragment(new Fragment_Browser(), String.valueOf(getString(R.string.app_name)));
        adapter.addFragment(new Fragment_Browser(), String.valueOf(getString(R.string.app_name)));
        adapter.addFragment(new Fragment_Browser(), String.valueOf(getString(R.string.app_name)));
        adapter.addFragment(new Fragment_Bookmarks(), String.valueOf(getString(R.string.app_title_bookmarks)));
        adapter.addFragment(new Fragment_ReadLater(), String.valueOf(getString(R.string.app_title_readLater)));
        adapter.addFragment(new Fragment_History(), String.valueOf(getString(R.string.app_title_history)));
        adapter.addFragment(new Fragment_Pass(), String.valueOf(getString(R.string.app_title_passStorage)));
        adapter.addFragment(new Fragment_Files(), String.valueOf(getString(R.string.app_name)));

        viewPager.setAdapter(adapter);
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                viewPager.setCurrentItem(startTabInt,true);
            }
        }, 250);
    }

    private class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);// add return null; to display only icons
        }
    }

    @Override
    public void onBackPressed() {

        if(viewPager.getCurrentItem() < 5) {
            Fragment_Browser fragment = (Fragment_Browser) viewPager.getAdapter().instantiateItem(viewPager, viewPager.getCurrentItem());
            fragment.doBack();
        } else if(viewPager.getCurrentItem() == 5) {
            Fragment_Bookmarks fragment = (Fragment_Bookmarks) viewPager.getAdapter().instantiateItem(viewPager, viewPager.getCurrentItem());
            fragment.doBack();
        } else if(viewPager.getCurrentItem() == 6) {
            Fragment_ReadLater fragment = (Fragment_ReadLater) viewPager.getAdapter().instantiateItem(viewPager, viewPager.getCurrentItem());
            fragment.doBack();
        } else if(viewPager.getCurrentItem() == 7) {
            Fragment_History fragment = (Fragment_History) viewPager.getAdapter().instantiateItem(viewPager, viewPager.getCurrentItem());
            fragment.doBack();
        } else if(viewPager.getCurrentItem() == 8) {
            Fragment_Pass fragment = (Fragment_Pass) viewPager.getAdapter().instantiateItem(viewPager, viewPager.getCurrentItem());
            fragment.doBack();
        } else if(viewPager.getCurrentItem() == 9) {
            Fragment_Files fragment = (Fragment_Files) viewPager.getAdapter().instantiateItem(viewPager, viewPager.getCurrentItem());
            fragment.doBack();
        }
    }
}
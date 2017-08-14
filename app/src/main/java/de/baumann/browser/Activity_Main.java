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
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import de.baumann.browser.helper.CustomViewPager;
import de.baumann.browser.helper.helper_browser;
import de.baumann.browser.helper.helper_editText;
import de.baumann.browser.helper.helper_main;
import de.baumann.browser.lists.Fragment_Bookmarks;
import de.baumann.browser.lists.Fragment_Browser;
import de.baumann.browser.lists.Fragment_Files;
import de.baumann.browser.lists.Fragment_History;
import de.baumann.browser.lists.Fragment_Pass;
import de.baumann.browser.lists.Fragment_ReadLater;

public class Activity_Main extends AppCompatActivity {


    // Views

    private CustomViewPager viewPager;
    private TextView urlBar;
    private TextView listBar;
    private EditText editText;


    // Others

    private SharedPreferences sharedPref;


    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        WebView.enableSlowWholeDocumentDraw();
        Activity activity = Activity_Main.this;

        PreferenceManager.setDefaultValues(activity, R.xml.user_settings, false);
        PreferenceManager.setDefaultValues(activity, R.xml.user_settings_search, false);
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
        editText = (EditText) findViewById(R.id.editText) ;

        viewPager = (CustomViewPager) findViewById(R.id.viewpager);
        viewPager.setPagingEnabled();
        viewPager.setOffscreenPageLimit(10);

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {

                helper_editText.hideKeyboard(Activity_Main.this, editText, 0, "", getString(R.string.app_search_hint));

                if (position < 5) {
                    Fragment_Browser fragment = (Fragment_Browser) viewPager.getAdapter().instantiateItem(viewPager, viewPager.getCurrentItem());
                    fragment.fragmentAction();
                    urlBar.setVisibility(View.VISIBLE);
                    listBar.setVisibility(View.GONE);
                } else {
                    urlBar.setVisibility(View.GONE);
                    listBar.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setupViewPager(viewPager);

        onNewIntent(getIntent());
    }

    @Override
    public void onResume(){
        super.onResume();
        // put your code here...

        final int position = viewPager.getCurrentItem();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (position < 5) {
                    Fragment_Browser fragment = (Fragment_Browser) viewPager.getAdapter().instantiateItem(viewPager, viewPager.getCurrentItem());
                    fragment.fragmentAction();
                    urlBar.setVisibility(View.VISIBLE);
                    listBar.setVisibility(View.GONE);
                } else {
                    urlBar.setVisibility(View.GONE);
                    listBar.setVisibility(View.VISIBLE);
                }
            }
        }, 150);


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

    private void setupViewPager(CustomViewPager viewPager) {

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
        viewPager.setCurrentItem(startTabInt,true);
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
package de.baumann.browser.Fragment;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;

import de.baumann.browser.Activity.JavascriptActivity;
import de.baumann.browser.Activity.WhitelistActivity;
import de.baumann.browser.Ninja.R;
import de.baumann.browser.Task.ExportBookmarksTask;
import de.baumann.browser.Task.ExportWhitelistJSTask;
import de.baumann.browser.Task.ExportWhitelistTask;
import de.baumann.browser.Task.ImportBookmarksTask;
import de.baumann.browser.Task.ImportWhitelistTask;
import de.baumann.browser.Task.ImportWhitelistTaskJS;

public class Fragment_settings_data extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preference_data);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        switch (preference.getTitleRes()) {
            case R.string.setting_title_whitelist:
                Intent toWhitelist = new Intent(getActivity(), WhitelistActivity.class);
                getActivity().startActivity(toWhitelist);
                break;
            case R.string.setting_title_whitelistJS:
                Intent toJavascript = new Intent(getActivity(), JavascriptActivity.class);
                getActivity().startActivity(toJavascript);
                break;
            case R.string.setting_title_export_whitelist:
                new ExportWhitelistTask(getActivity()).execute();
                break;
            case R.string.setting_title_import_whitelist:
                new ImportWhitelistTask(getActivity()).execute();
                break;

            case R.string.setting_title_export_whitelistJS:
                new ExportWhitelistJSTask(getActivity()).execute();
                break;
            case R.string.setting_title_import_whitelistJS:
                new ImportWhitelistTaskJS(getActivity()).execute();
                break;
            case R.string.setting_title_export_bookmarks:
                new ExportBookmarksTask(getActivity()).execute();
                break;
            case R.string.setting_title_import_bookmarks:
                new ImportBookmarksTask(getActivity()).execute();
                break;

            default:
                break;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }
}

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
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Toast;

import de.baumann.browser.R;
import de.baumann.browser.databases.DbAdapter_ReadLater;

public class Activity_intent_add extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String link = sharedPref.getString("add_readLater_link", "");
        String domain = sharedPref.getString("add_readLater_domain", "");

        DbAdapter_ReadLater db = new DbAdapter_ReadLater(Activity_intent_add.this);
        db.open();
        if(db.isExist(link)){
            Toast.makeText(Activity_intent_add.this, getString(R.string.toast_newTitle), Toast.LENGTH_LONG).show();
        }else{
            db.insert(domain, link, "", "", helper_main.createDate());
        }
        finish();
    }
}

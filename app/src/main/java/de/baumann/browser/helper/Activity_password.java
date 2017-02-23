/*
    This file is part of the HHS Moodle WebApp.

    HHS Moodle WebApp is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    HHS Moodle WebApp is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with the Diaspora Native WebApp.

    If not, see <http://www.gnu.org/licenses/>.
 */

package de.baumann.browser.helper;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.mobapphome.mahencryptorlib.MAHEncryptor;

import de.baumann.browser.R;


public class Activity_password extends AppCompatActivity {

    private TextView text;
    private String protect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_password);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
        }

        PreferenceManager.setDefaultValues(this, R.xml.user_settings, false);
        PreferenceManager.setDefaultValues(this, R.xml.user_settings_search, false);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        text = (TextView) findViewById(R.id.pass_userPin);

        Button ib0 = (Button) findViewById(R.id.button0);
        assert ib0 != null;
        ib0.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                enterNum("0");
            }
        });

        Button ib1 = (Button) findViewById(R.id.button1);
        assert ib1 != null;
        ib1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                enterNum("1");
            }
        });

        Button ib2 = (Button) findViewById(R.id.button2);
        assert ib2 != null;
        ib2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                enterNum("2");
            }
        });

        Button ib3 = (Button) findViewById(R.id.button3);
        assert ib3 != null;
        ib3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                enterNum("3");
            }
        });

        Button ib4 = (Button) findViewById(R.id.button4);
        assert ib4 != null;
        ib4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                enterNum("4");
            }
        });

        Button ib5 = (Button) findViewById(R.id.button5);
        assert ib5 != null;
        ib5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                enterNum("5");
            }
        });

        Button ib6 = (Button) findViewById(R.id.button6);
        assert ib6 != null;
        ib6.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                enterNum("6");
            }
        });

        Button ib7 = (Button) findViewById(R.id.button7);
        assert ib7 != null;
        ib7.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                enterNum("7");
            }
        });

        Button ib8 = (Button) findViewById(R.id.button8);
        assert ib8 != null;
        ib8.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                enterNum("8");
            }
        });

        Button ib9 = (Button) findViewById(R.id.button9);
        assert ib9 != null;
        ib9.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                enterNum("9");
            }
        });

        try {
            final MAHEncryptor mahEncryptor = MAHEncryptor.newInstance(sharedPref.getString("saved_key", ""));
            protect = mahEncryptor.decode(sharedPref.getString("protect_PW", ""));
        } catch (Exception e) {
            e.printStackTrace();
            Snackbar.make(ib0, R.string.toast_error, Snackbar.LENGTH_SHORT).show();
        }


        ImageButton enter = (ImageButton) findViewById(R.id.imageButtonEnter);
        assert enter != null;
        enter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String Password = text.getText().toString().trim();

                if (Password.equals(protect)) {
                    helper_main.isOpened(Activity_password.this);
                    finish();
                } else {
                    Snackbar.make(text, R.string.toast_wrongPW, Snackbar.LENGTH_LONG).show();
                }
            }
        });

        final ImageButton cancel = (ImageButton) findViewById(R.id.imageButtonCancel);
        assert cancel != null;
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                text.setText("");
            }
        });

        final Button clear = (Button) findViewById(R.id.buttonReset);
        assert clear != null;
        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar snackbar = Snackbar
                        .make(clear, getString(R.string.pw_forgotten_dialog), Snackbar.LENGTH_LONG)
                        .setAction(getString(R.string.toast_yes), new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                try {
                                    // clearing app data
                                    Runtime runtime = Runtime.getRuntime();
                                    runtime.exec("pm clear de.baumann.browser");
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                snackbar.show();
            }
        });
    }

    @Override
    public void onBackPressed() {
        helper_main.isClosed(Activity_password.this);
        finishAffinity();
    }

    private void enterNum (String number) {
        String textNow = text.getText().toString().trim();
        String pin = textNow + number;
        text.setText(pin);
    }
}
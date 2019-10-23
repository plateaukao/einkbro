package de.baumann.browser.fragment;

import android.content.SharedPreferences;
import android.os.Bundle;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.preference.PreferenceFragmentCompat;

import java.util.Objects;

import de.baumann.browser.Ninja.R;
import de.baumann.browser.unit.HelperUnit;

public class Fragment_clear extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preference_clear, rootKey);

        Objects.requireNonNull(findPreference("sp_deleteDatabase")).setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(androidx.preference.Preference preference) {
                final SharedPreferences sp = getPreferenceScreen().getSharedPreferences();
                final BottomSheetDialog dialog = new BottomSheetDialog(Objects.requireNonNull(getActivity()));
                View dialogView = View.inflate(getActivity(), R.layout.dialog_action, null);
                TextView textView = dialogView.findViewById(R.id.dialog_text);
                textView.setText(R.string.hint_database);
                Button action_ok = dialogView.findViewById(R.id.action_ok);
                action_ok.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.cancel();
                        getActivity().deleteDatabase("Ninja4.db");
                        getActivity().deleteDatabase("pass_DB_v01.db");
                        sp.edit().putInt("restart_changed", 1).apply();
                        getActivity().finish();
                    }
                });
                Button action_cancel = dialogView.findViewById(R.id.action_cancel);
                action_cancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.cancel();
                    }
                });
                dialog.setContentView(dialogView);
                dialog.show();
                HelperUnit.setBottomSheetBehavior(dialog, dialogView, BottomSheetBehavior.STATE_EXPANDED);
                return false;
            }
        });
    }
}

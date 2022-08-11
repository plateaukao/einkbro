package info.plateaukao.einkbro.fragment;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceFragmentCompat;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import info.plateaukao.einkbro.R;
import info.plateaukao.einkbro.unit.HelperUnit;

public class ClearDataFragment extends PreferenceFragmentCompat implements FragmentTitleInterface {

    @NonNull
    @Override
    public int getTitleId() {
        return R.string.setting_title_clear_control;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preference_clear, rootKey);

        findPreference("sp_deleteDatabase").setOnPreferenceClickListener(preference -> {
            final Activity activity = getActivity();
            final SharedPreferences sp = getPreferenceScreen().getSharedPreferences();
            final BottomSheetDialog dialog = new BottomSheetDialog(activity);
            View dialogView = View.inflate(getActivity(), R.layout.dialog_action, null);
            TextView textView = dialogView.findViewById(R.id.dialog_text);
            textView.setText(R.string.hint_database);
            Button action_ok = dialogView.findViewById(R.id.action_ok);
            action_ok.setOnClickListener(view -> {
                dialog.cancel();
                activity.deleteDatabase("Ninja4.db");
                activity.deleteDatabase("pass_DB_v01.db");
                sp.edit().putBoolean("restart_changed", true).apply();
                getActivity().finish();
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
        });
    }
}

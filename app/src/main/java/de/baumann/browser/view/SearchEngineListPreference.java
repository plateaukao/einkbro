package de.baumann.browser.view;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import android.util.AttributeSet;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import de.baumann.browser.Ninja.R;
import de.baumann.browser.unit.BrowserUnit;

public class SearchEngineListPreference extends ListPreference {

    public SearchEngineListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);

        builder.setPositiveButton(R.string.dialog_button_custom, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                showEditDialog();
            }
        });
    }

    private void showEditDialog() {
        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());

        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(getContext());
        View dialogView = View.inflate(getContext(), R.layout.dialog_edit_title, null);

        final EditText editText = dialogView.findViewById(R.id.dialog_edit);
        String custom = sp.getString(getContext().getString(R.string.sp_search_engine_custom), "");

        editText.setHint(R.string.dialog_title_hint);
        editText.setText(custom);

        Button action_ok = dialogView.findViewById(R.id.action_ok);
        action_ok.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("ApplySharedPref")
            @Override
            public void onClick(View view) {
                String domain = editText.getText().toString().trim();
                if (domain.isEmpty()) {
                    NinjaToast.show(getContext(), R.string.toast_input_empty);
                } else if (!BrowserUnit.isURL(domain)) {
                    NinjaToast.show(getContext(), R.string.toast_invalid_domain);
                } else {
                    sp.edit().putString(getContext().getString(R.string.sp_search_engine), "8").commit();
                    sp.edit().putString(getContext().getString(R.string.sp_search_engine_custom), domain).commit();
                    hideSoftInput(editText);
                    bottomSheetDialog.cancel();
                }

            }
        });
        Button action_cancel = dialogView.findViewById(R.id.action_cancel);
        action_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hideSoftInput(editText);
                bottomSheetDialog.cancel();
            }
        });
        bottomSheetDialog.setContentView(dialogView);
        bottomSheetDialog.show();
    }

    private void hideSoftInput(View view) {
        view.clearFocus();
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        assert imm != null;
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}

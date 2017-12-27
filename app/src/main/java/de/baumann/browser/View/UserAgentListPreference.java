package de.baumann.browser.View;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import de.baumann.browser.Ninja.R;

public class UserAgentListPreference extends ListPreference {



    public UserAgentListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);

        builder.setNeutralButton(R.string.dialog_button_custom, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                showEditDialog();
            }
        });
    }

    private void showEditDialog() {

        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(getContext());
        View dialogView = View.inflate(getContext(), R.layout.dialog_edit, null);

        final EditText editText = dialogView.findViewById(R.id.dialog_edit);
        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());

        editText.setHint(R.string.dialog_ua_hint);
        String custom = sp.getString(getContext().getString(R.string.sp_user_agent_custom), "");
        editText.setText(custom);
        editText.setSelection(custom.length());

        builder.setView(dialogView);
        builder.setTitle(R.string.menu_edit);
        builder.setPositiveButton(R.string.app_ok, new DialogInterface.OnClickListener() {

            @SuppressLint("ApplySharedPref")
            public void onClick(DialogInterface dialog, int whichButton) {

                String ua = editText.getText().toString().trim();
                if (ua.isEmpty()) {
                    NinjaToast.show(getContext(), R.string.toast_input_empty);
                } else {
                    sp.edit().putString(getContext().getString(R.string.sp_user_agent), "2").commit();
                    sp.edit().putString(getContext().getString(R.string.sp_user_agent_custom), ua).commit();
                    hideSoftInput(editText);
                    dialog.cancel();
                }
            }
        });
        builder.setNegativeButton(R.string.app_cancel, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.cancel();
                hideSoftInput(editText);
            }
        });

        android.support.v7.app.AlertDialog dialog = builder.create();
        dialog.show();

        showSoftInput(editText);
    }

    private void hideSoftInput(View view) {
        view.clearFocus();
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void showSoftInput(View view) {
        view.requestFocus();
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }
}

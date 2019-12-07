package de.baumann.browser.task;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import de.baumann.browser.Ninja.R;
import de.baumann.browser.unit.BrowserUnit;
import de.baumann.browser.unit.HelperUnit;
import de.baumann.browser.view.NinjaToast;

@SuppressLint("StaticFieldLeak")
public class ImportWhitelistTask extends AsyncTask<Void, Void, Boolean> {

    private final Context context;
    private BottomSheetDialog dialog;
    private int count;
    private int table;

    public ImportWhitelistTask(Activity activity, int i) {
        this.context = activity;
        this.dialog = null;
        this.count = 0;
        this.table = i;
    }

    @Override
    protected void onPreExecute() {
        dialog = new BottomSheetDialog(context);
        View dialogView = View.inflate(context, R.layout.dialog_progress, null);
        TextView textView = dialogView.findViewById(R.id.dialog_text);
        textView.setText(context.getString(R.string.toast_wait_a_minute));
        dialog.setContentView(dialogView);
        //noinspection ConstantConditions
        dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        dialog.show();
        HelperUnit.setBottomSheetBehavior(dialog, dialogView, BottomSheetBehavior.STATE_EXPANDED);
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        switch (table) {
            case 0:
                count = BrowserUnit.importWhitelist(context, 0);
                break;
            case 1:
                count = BrowserUnit.importWhitelist(context, 1);
                break;
            default:
                count = BrowserUnit.importWhitelist(context, 2);
                break;
        }
        return !isCancelled() && count >= 0;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        dialog.hide();
        dialog.dismiss();

        if (result) {
            NinjaToast.show(context, context.getString(R.string.toast_import_successful) + count);
        } else {
            NinjaToast.show(context, R.string.toast_import_failed);
        }
    }
}

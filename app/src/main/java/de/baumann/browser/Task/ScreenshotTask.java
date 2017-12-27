package de.baumann.browser.Task;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.design.widget.BottomSheetDialog;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import java.io.File;

import de.baumann.browser.Ninja.R;
import de.baumann.browser.Unit.BrowserUnit;
import de.baumann.browser.Unit.ViewUnit;
import de.baumann.browser.View.NinjaToast;
import de.baumann.browser.View.NinjaWebView;

public class ScreenshotTask extends AsyncTask<Void, Void, Boolean> {
    private final Context context;
    private final NinjaWebView webView;
    private int windowWidth;
    private float contentHeight;
    private String title;
    private String path;
    private Activity activity;
    private BottomSheetDialog dialog;

    public ScreenshotTask(Context context, NinjaWebView webView) {
        this.context = context;
        this.webView = webView;
        this.windowWidth = 0;
        this.contentHeight = 0f;
        this.title = null;
        this.path = null;
    }

    @Override
    protected void onPreExecute() {

        activity = (Activity) context;
        NinjaToast.show(activity, context.getString(R.string.toast_wait_a_minute));

        dialog = new BottomSheetDialog(activity);

        View dialogView = View.inflate(activity, R.layout.dialog_progress, null);
        TextView textView = dialogView.findViewById(R.id.dialog_text);
        textView.setText(context.getString(R.string.toast_wait_a_minute));
        dialog.setContentView(dialogView);
        //noinspection ConstantConditions
        dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        dialog.show();

        try {
            windowWidth = ViewUnit.getWindowWidth(context);
            contentHeight = webView.getContentHeight() * ViewUnit.getDensity(context);

            String url = webView.getUrl();
            String domain = Uri.parse(url).getHost().replace("www.", "").trim();
            title = domain.replace(".", "_").trim();

        } catch (Exception e) {
            NinjaToast.show(activity, context.getString(R.string.toast_error));
        }
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        try {
            Bitmap bitmap = ViewUnit.capture(webView, windowWidth, contentHeight, Bitmap.Config.ARGB_8888);
            path = BrowserUnit.screenshot(context, bitmap, title);
        } catch (Exception e) {
            path = null;
        }
        return path != null && !path.isEmpty();
    }

    @Override
    protected void onPostExecute(Boolean result) {

        if (result) {
            dialog.cancel();
            NinjaToast.show(activity, context.getString(R.string.toast_screenshot_successful) + path);
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);

            if (sp.getInt("screenshot",0) == 1) {

                final File pathFile = new File(sp.getString("screenshot_path", ""));

                if (pathFile.exists()) {
                    Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                    sharingIntent.setType("image/*");
                    sharingIntent.putExtra(Intent.EXTRA_SUBJECT, title);
                    sharingIntent.putExtra(Intent.EXTRA_TEXT, path);
                    Uri bmpUri = Uri.fromFile(pathFile);
                    sharingIntent.putExtra(Intent.EXTRA_STREAM, bmpUri);
                    context.startActivity(Intent.createChooser(sharingIntent, context.getString(R.string.menu_share)));
                }
            }

        } else {
            NinjaToast.show(activity, context.getString(R.string.toast_error));
        }
    }
}

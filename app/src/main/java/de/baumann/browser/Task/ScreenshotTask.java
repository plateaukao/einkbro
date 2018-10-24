package de.baumann.browser.Task;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.design.widget.BottomSheetDialog;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;

import de.baumann.browser.Unit.HelperUnit;
import de.baumann.browser.Ninja.R;
import de.baumann.browser.Unit.BrowserUnit;
import de.baumann.browser.Unit.ViewUnit;
import de.baumann.browser.View.NinjaToast;
import de.baumann.browser.View.NinjaWebView;

@SuppressLint("StaticFieldLeak")
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
            title = HelperUnit.fileName(webView.getUrl());

        } catch (Exception e) {
            NinjaToast.show(activity, context.getString(R.string.toast_error));
        }
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            int hasWRITE_EXTERNAL_STORAGE = context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (hasWRITE_EXTERNAL_STORAGE != PackageManager.PERMISSION_GRANTED) {
                NinjaToast.show(context, R.string.toast_permission_sdCard_sec);
                HelperUnit.grantPermissionsStorage(activity);
            } else {
                try {
                    Bitmap bitmap = ViewUnit.capture(webView, windowWidth, contentHeight, Bitmap.Config.ARGB_8888);
                    path = BrowserUnit.screenshot(context, bitmap, title);
                } catch (Exception e) {
                    path = null;
                }
            }
        } else {
            try {
                Bitmap bitmap = ViewUnit.capture(webView, windowWidth, contentHeight, Bitmap.Config.ARGB_8888);
                path = BrowserUnit.screenshot(context, bitmap, title);
            } catch (Exception e) {
                path = null;
            }
        }

        return path != null && !path.isEmpty();
    }

    @Override
    protected void onPostExecute(Boolean result) {

        if (result) {
            dialog.cancel();
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
                    sp.edit().putBoolean("delete_screenshot", true).apply();
                }
            } else {
                final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(activity);
                View dialogView = View.inflate(activity, R.layout.dialog_action, null);
                TextView textView = dialogView.findViewById(R.id.dialog_text);
                textView.setText(R.string.toast_downloadComplete);
                Button action_ok = dialogView.findViewById(R.id.action_ok);
                action_ok.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        activity.startActivity(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS));
                        bottomSheetDialog.cancel();
                    }
                });
                Button action_cancel = dialogView.findViewById(R.id.action_cancel);
                action_cancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        bottomSheetDialog.cancel();
                    }
                });
                bottomSheetDialog.setContentView(dialogView);
                bottomSheetDialog.show();
            }
        } else {
            NinjaToast.show(activity, context.getString(R.string.toast_error));
        }
    }
}

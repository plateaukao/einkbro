package de.baumann.browser.task;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

import de.baumann.browser.unit.HelperUnit;
import de.baumann.browser.Ninja.R;
import de.baumann.browser.unit.ViewUnit;
import de.baumann.browser.view.NinjaToast;
import de.baumann.browser.view.NinjaWebView;

@SuppressLint("StaticFieldLeak")
public class ScreenshotTask extends AsyncTask<Void, Void, Boolean> {

    private final Context context;
    private final NinjaWebView webView;
    private int windowWidth;
    private float contentHeight;
    private String title;
    private String path;
    private Uri uri;
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
        dialog = new BottomSheetDialog(activity);

        View dialogView = View.inflate(activity, R.layout.dialog_progress, null);
        TextView textView = dialogView.findViewById(R.id.dialog_text);
        textView.setText(context.getString(R.string.toast_wait_a_minute));
        dialog.setContentView(dialogView);
        Objects.requireNonNull(dialog.getWindow()).clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        dialog.show();
        HelperUnit.setBottomSheetBehavior(dialog, dialogView, BottomSheetBehavior.STATE_EXPANDED);

        try {
            windowWidth = ViewUnit.getWindowWidth(context);
            contentHeight = webView.getContentHeight() * ViewUnit.getDensity(context);
            title = HelperUnit.fileName(webView.getUrl());
        } catch (Exception e) {
            NinjaToast.show(activity, context.getString(R.string.toast_error));
        }
    }

    private void saveImage(Bitmap bitmap, @NonNull String name) throws IOException {
        OutputStream fos;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentResolver resolver = context.getContentResolver();
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/" + "Screenshots/");
            uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
            fos = resolver.openOutputStream(Objects.requireNonNull(uri));
        } else {
            @SuppressWarnings("deprecation")
            String imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_SCREENSHOTS).toString() + File.separator;
            File file = new File(imagesDir);

            if (!file.exists()) {
                //noinspection ResultOfMethodCallIgnored
                file.mkdir();
            }

            File image = new File(imagesDir, name + ".png");
            fos = new FileOutputStream(image);
            uri = Uri.fromFile(image);
        }

        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
        Objects.requireNonNull(fos).flush();
        fos.close();
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            int hasWRITE_EXTERNAL_STORAGE = context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (hasWRITE_EXTERNAL_STORAGE != PackageManager.PERMISSION_GRANTED) {
                HelperUnit.grantPermissionsStorage(activity);
            } else {
                try {
                    Bitmap bitmap = ViewUnit.capture(webView, windowWidth, contentHeight, Bitmap.Config.ARGB_8888);
                    saveImage(bitmap, title);
                } catch (Exception e) {
                    path = null;
                }
            }
        } else {
            try {
                Bitmap bitmap = ViewUnit.capture(webView, windowWidth, contentHeight, Bitmap.Config.ARGB_8888);
                saveImage(bitmap, title);
            } catch (Exception e) {
                path = null;
            }
        }

        return path != null && !path.isEmpty();
    }

    @Override
    protected void onPostExecute(Boolean result) {
        dialog.cancel();
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);

        if (sp.getInt("screenshot",0) == 1) {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "image/*");
            context.startActivity(Intent.createChooser(intent, null));
        } else {
            final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(activity);
            View dialogView = View.inflate(activity, R.layout.dialog_action, null);
            TextView textView = dialogView.findViewById(R.id.dialog_text);
            textView.setText(R.string.toast_downloadComplete);
            Button action_ok = dialogView.findViewById(R.id.action_ok);
            action_ok.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(uri, "*/*");
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(Intent.createChooser(intent, null));

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
            HelperUnit.setBottomSheetBehavior(bottomSheetDialog, dialogView, BottomSheetBehavior.STATE_EXPANDED);
        }
    }
}

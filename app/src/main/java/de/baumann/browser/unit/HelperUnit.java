/*
    This file is part of the browser WebApp.

    browser WebApp is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    browser WebApp is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with the browser webview app.

    If not, see <http://www.gnu.org/licenses/>.
 */

package de.baumann.browser.unit;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import androidx.preference.PreferenceManager;
import android.provider.Settings;
import androidx.annotation.NonNull;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import android.text.Html;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import de.baumann.browser.Ninja.R;
import de.baumann.browser.view.NinjaToast;

public class HelperUnit {

    private static final int REQUEST_CODE_ASK_PERMISSIONS = 123;
    private static final int REQUEST_CODE_ASK_PERMISSIONS_1 = 1234;
    private static SharedPreferences sp;

    public static void grantPermissionsStorage(final Activity activity) {
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            int hasWRITE_EXTERNAL_STORAGE = activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (hasWRITE_EXTERNAL_STORAGE != PackageManager.PERMISSION_GRANTED) {
                if (!activity.shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(activity);
                    View dialogView = View.inflate(activity, R.layout.dialog_action, null);
                    TextView textView = dialogView.findViewById(R.id.dialog_text);
                    textView.setText(R.string.toast_permission_sdCard);
                    Button action_ok = dialogView.findViewById(R.id.action_ok);
                    action_ok.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            activity.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                    REQUEST_CODE_ASK_PERMISSIONS);
                            bottomSheetDialog.cancel();
                        }
                    });
                    Button action_cancel = dialogView.findViewById(R.id.action_cancel);
                    action_cancel.setText(R.string.setting_label);
                    action_cancel.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent intent = new Intent();
                            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
                            intent.setData(uri);
                            activity.startActivity(intent);
                            bottomSheetDialog.cancel();
                        }
                    });
                    bottomSheetDialog.setContentView(dialogView);
                    bottomSheetDialog.show();
                    HelperUnit.setBottomSheetBehavior(bottomSheetDialog, dialogView, BottomSheetBehavior.STATE_EXPANDED);
                }
            }
        }
    }

    public static void grantPermissionsLoc(final Activity activity) {

        if (android.os.Build.VERSION.SDK_INT >= 23) {

            int hasACCESS_FINE_LOCATION = activity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
            if (hasACCESS_FINE_LOCATION != PackageManager.PERMISSION_GRANTED) {

                final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(activity);
                View dialogView = View.inflate(activity, R.layout.dialog_action, null);
                TextView textView = dialogView.findViewById(R.id.dialog_text);
                textView.setText(R.string.toast_permission_loc);
                Button action_ok = dialogView.findViewById(R.id.action_ok);
                action_ok.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        activity.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                REQUEST_CODE_ASK_PERMISSIONS_1);
                        bottomSheetDialog.cancel();
                    }
                });
                Button action_cancel = dialogView.findViewById(R.id.action_cancel);
                action_cancel.setText(R.string.setting_label);
                action_cancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent();
                        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
                        intent.setData(uri);
                        activity.startActivity(intent);
                        bottomSheetDialog.cancel();
                    }
                });
                bottomSheetDialog.setContentView(dialogView);
                bottomSheetDialog.show();
                HelperUnit.setBottomSheetBehavior(bottomSheetDialog, dialogView, BottomSheetBehavior.STATE_EXPANDED);
            }
        }
    }

    public static void applyTheme(Context context) {
        sp = PreferenceManager.getDefaultSharedPreferences(context);
        String showNavButton = Objects.requireNonNull(sp.getString("sp_theme", "1"));
        switch (showNavButton) {
            case "0":
                context.setTheme(R.style.AppTheme_system);
                break;
            case "1":
                context.setTheme(R.style.AppTheme);
                break;
            case "2":
                context.setTheme(R.style.AppTheme_dark);
                break;
            case "3":
                context.setTheme(R.style.AppTheme_amoled);
                break;
            default:
                context.setTheme(R.style.AppTheme);
                break;
        }
    }

    public static void setFavorite (Context context, String url) {
        sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putString("favoriteURL", url).apply();
        NinjaToast.show(context, R.string.toast_fav);
    }

    public static void showRestartDialog (Context context) {
        final BottomSheetDialog dialog = new BottomSheetDialog(context);
        View dialogView = View.inflate(context, R.layout.dialog_action, null);
        TextView textView = dialogView.findViewById(R.id.dialog_text);
        textView.setText(R.string.toast_restart);
        Button action_ok = dialogView.findViewById(R.id.action_ok);
        action_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                System.exit(0);
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
    }

    public static void setBottomSheetBehavior (final BottomSheetDialog dialog, final View view, int beh) {
        BottomSheetBehavior mBehavior = BottomSheetBehavior.from((View) view.getParent());
        mBehavior.setState(beh);
        mBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    dialog.cancel();
                }
            }
            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
            }
        });
    }

    public static void createShortcut (Context context, String title, String url) {
        try {
            Intent i = new Intent();
            i.setAction(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) { // code for adding shortcut on pre oreo device
                Intent installer = new Intent();
                installer.putExtra("android.intent.extra.shortcut.INTENT", i);
                installer.putExtra("android.intent.extra.shortcut.NAME", title);
                installer.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(context.getApplicationContext(), R.drawable.qc_bookmarks));
                installer.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
                context.sendBroadcast(installer);
            } else {
                ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
                assert shortcutManager != null;
                if (shortcutManager.isRequestPinShortcutSupported()) {
                    ShortcutInfo pinShortcutInfo =
                            new ShortcutInfo.Builder(context, url)
                                    .setShortLabel(title)
                                    .setLongLabel(title)
                                    .setIcon(Icon.createWithResource(context, R.drawable.qc_bookmarks))
                                    .setIntent(new Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                    .build();
                    shortcutManager.requestPinShortcut(pinShortcutInfo, null);
                } else {
                    System.out.println("failed_to_add");
                }
            }
        } catch (Exception e) {
            System.out.println("failed_to_add");
        }
    }

    public static void show_dialogHelp(final Context context) {

        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(context);
        View dialogView = View.inflate(context, R.layout.dialog_help, null);
        ImageButton dialogHelp_tip = dialogView.findViewById(R.id.dialogHelp_tip);
        ImageButton dialogHelp_overview = dialogView.findViewById(R.id.dialogHelp_overview);
        final View dialogHelp_tipView = dialogView.findViewById(R.id.dialogHelp_tipView);
        final View dialogHelp_overviewView = dialogView.findViewById(R.id.dialogHelp_overviewView);
        final TextView dialogHelp_tv_title = dialogView.findViewById(R.id.dialogHelp_title);
        final TextView dialogHelp_tv_text = dialogView.findViewById(R.id.dialogHelp_tv);
        dialogHelp_tv_title.setText(HelperUnit.textSpannable(context.getResources().getString(R.string.dialogHelp_tipTitle)));
        dialogHelp_tv_text.setText(HelperUnit.textSpannable(context.getResources().getString(R.string.dialogHelp_tipText)));

        dialogHelp_tip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogHelp_tipView.setVisibility(View.VISIBLE);
                dialogHelp_overviewView.setVisibility(View.GONE);
                dialogHelp_tv_title.setText(HelperUnit.textSpannable(context.getResources().getString(R.string.dialogHelp_tipTitle)));
                dialogHelp_tv_text.setText(HelperUnit.textSpannable(context.getResources().getString(R.string.dialogHelp_tipText)));
            }
        });

        dialogHelp_overview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogHelp_tipView.setVisibility(View.GONE);
                dialogHelp_overviewView.setVisibility(View.VISIBLE);
                dialogHelp_tv_title.setText(HelperUnit.textSpannable(context.getResources().getString(R.string.dialogHelp_overviewTitle)));
                dialogHelp_tv_text.setText(HelperUnit.textSpannable(context.getResources().getString(R.string.dialogHelp_overviewText)));
            }
        });

        bottomSheetDialog.setContentView(dialogView);
        bottomSheetDialog.show();
        HelperUnit.setBottomSheetBehavior(bottomSheetDialog, dialogView, BottomSheetBehavior.STATE_EXPANDED);
    }

    public static void switchIcon (Activity activity, String string, String fieldDB, ImageView be) {
        sp = PreferenceManager.getDefaultSharedPreferences(activity);
        assert be != null;
        switch (string) {
            case "02":be.setImageResource(R.drawable.circle_pink_big);
                sp.edit().putString(fieldDB, "02").apply();break;
            case "03":be.setImageResource(R.drawable.circle_purple_big);
                sp.edit().putString(fieldDB, "03").apply();break;
            case "04":be.setImageResource(R.drawable.circle_blue_big);
                sp.edit().putString(fieldDB, "04").apply();break;
            case "05":be.setImageResource(R.drawable.circle_teal_big);
                sp.edit().putString(fieldDB, "05").apply();break;
            case "06":be.setImageResource(R.drawable.circle_green_big);
                sp.edit().putString(fieldDB, "06").apply();break;
            case "07":be.setImageResource(R.drawable.circle_lime_big);
                sp.edit().putString(fieldDB, "07").apply();break;
            case "08":be.setImageResource(R.drawable.circle_yellow_big);
                sp.edit().putString(fieldDB, "08").apply();break;
            case "09":be.setImageResource(R.drawable.circle_orange_big);
                sp.edit().putString(fieldDB, "09").apply();break;
            case "10":be.setImageResource(R.drawable.circle_brown_big);
                sp.edit().putString(fieldDB, "10").apply();break;
            case "11":be.setImageResource(R.drawable.circle_grey_big);
                sp.edit().putString(fieldDB, "11").apply();break;
            case "01":
            default:
                be.setImageResource(R.drawable.circle_red_big);
                sp.edit().putString(fieldDB, "01").apply();
                break;
        }
    }

    public static String fileName (String url) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault());
        String currentTime = sdf.format(new Date());
        String domain = Objects.requireNonNull(Uri.parse(url).getHost()).replace("www.", "").trim();
        return domain.replace(".", "_").trim() + "_" + currentTime.trim();
    }

    public static String secString (String string) {
        if(TextUtils.isEmpty(string)){
            return "";
        }else {
            return  string.replaceAll("'", "\'\'");
        }
    }

    public static String domain (String url) {
        if(url == null){
            return "";
        }else {
            try {
                return Objects.requireNonNull(Uri.parse(url).getHost()).replace("www.", "").trim();
            } catch (Exception e) {
                return "";
            }
        }
    }

    public static SpannableString textSpannable (String text) {
        SpannableString s;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            s = new SpannableString(Html.fromHtml(text,Html.FROM_HTML_MODE_LEGACY));
        } else {
            s = new SpannableString(Html.fromHtml(text));
        }
        Linkify.addLinks(s, Linkify.WEB_URLS);
        return s;
    }

    public static boolean isDarkColor(int color) {
        if (android.R.color.transparent == color)
            return false;

        boolean rtnValue = false;
        int[] rgb = { Color.red(color), Color.green(color), Color.blue(color) };
        int brightness = (int) Math.sqrt(rgb[0] * rgb[0] * .241 + rgb[1] * rgb[1] * .691 + rgb[2] * rgb[2] * .068);
        // color is light
        if (brightness >= 200) {
            rtnValue = true;
        }
        return !rtnValue;
    }

    private static final float[] NEGATIVE_COLOR = {
            -1.0f, 0, 0, 0, 255, // Red
            0, -1.0f, 0, 0, 255, // Green
            0, 0, -1.0f, 0, 255, // Blue
            0, 0, 0, 1.0f, 0     // Alpha
    };

    public static void initRendering(View view) {
        if (sp.getBoolean("sp_invert", false)) {
            Paint paint = new Paint();
            ColorMatrix matrix = new ColorMatrix();
            matrix.set(NEGATIVE_COLOR);
            ColorMatrix gcm = new ColorMatrix();
            gcm.setSaturation(0);
            ColorMatrix concat = new ColorMatrix();
            concat.setConcat(matrix, gcm);
            ColorMatrixColorFilter filter = new ColorMatrixColorFilter(concat);
            paint.setColorFilter(filter);
            // maybe sometime LAYER_TYPE_NONE would better?
            view.setLayerType(View.LAYER_TYPE_HARDWARE, paint);
        } else {
            view.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }
    }
}
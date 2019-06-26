/*
    This file is part of the Browser WebApp.

    Browser WebApp is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Browser WebApp is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with the Browser webview app.

    If not, see <http://www.gnu.org/licenses/>.
 */

package de.baumann.browser.Unit;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialog;
import android.text.Html;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import de.baumann.browser.Ninja.R;
import de.baumann.browser.View.NinjaToast;

public class HelperUnit {

    private static final int REQUEST_CODE_ASK_PERMISSIONS = 123;
    private static final int REQUEST_CODE_ASK_PERMISSIONS_1 = 1234;
    private static SharedPreferences sp;

    public Context context;

    public static void grantPermissionsStorage(final Activity activity) {
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            int hasWRITE_EXTERNAL_STORAGE = activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (hasWRITE_EXTERNAL_STORAGE != PackageManager.PERMISSION_GRANTED) {
                if (!activity.shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    new AlertDialog.Builder(activity)
                            .setTitle(R.string.toast_permission_title)
                            .setMessage(R.string.toast_permission_sdCard)
                            .setPositiveButton(activity.getString(R.string.app_ok), new DialogInterface.OnClickListener() {
                                @TargetApi(Build.VERSION_CODES.M)
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    activity.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                            REQUEST_CODE_ASK_PERMISSIONS);
                                }
                            })
                            .setNegativeButton(activity.getString(R.string.app_cancel), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            })
                            .show();
                }
            }
        }
    }

    public static void grantPermissionsLoc(final Activity activity) {
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            int hasACCESS_FINE_LOCATION = activity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
            if (hasACCESS_FINE_LOCATION != PackageManager.PERMISSION_GRANTED) {
                if (!activity.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    new AlertDialog.Builder(activity)
                            .setTitle(R.string.toast_permission_title)
                            .setMessage(R.string.toast_permission_loc)
                            .setPositiveButton(activity.getString(R.string.app_ok), new DialogInterface.OnClickListener() {
                                @TargetApi(Build.VERSION_CODES.M)
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    activity.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                            REQUEST_CODE_ASK_PERMISSIONS_1);
                                }
                            })
                            .setNegativeButton(activity.getString(R.string.app_cancel), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            })
                            .show();
                }
            }
        }
    }

    public static void applyTheme(Context context) {
        sp = PreferenceManager.getDefaultSharedPreferences(context);
        if (sp.getBoolean("sp_darkUI", false)){
            context.setTheme(R.style.AppTheme);
        } else {
            context.setTheme(R.style.AppTheme_dark);
        }
    }

    public static void setFavorite (Context context, String url) {
        sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putString("favoriteURL", url).apply();
        NinjaToast.show(context, R.string.toast_fav);
    }

    public static void setBottomSheetBehavior (final BottomSheetDialog dialog, final View view, int beh) {
        BottomSheetBehavior mBehavior = BottomSheetBehavior.from((View) view.getParent());
        mBehavior.setState(beh);

        mBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {

                try {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            dialog.cancel();
                        }
                    }, 250);
                } catch (Exception e) {
                    Log.w("Browser", "Error cancel dialog");
                }

            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {

            }
        });
    }

    public static void createShortcut (Context context, String title, String url) {
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
    }

    public static void show_dialogHelp(final Context context) {

        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(context);
        View dialogView = View.inflate(context, R.layout.dialog_help, null);

        ImageButton dialogHelp_tip = dialogView.findViewById(R.id.dialogHelp_tip);
        ImageButton dialogHelp_overview = dialogView.findViewById(R.id.dialogHelp_overview);
        ImageButton dialogHelp_gestures = dialogView.findViewById(R.id.dialogHelp_gestures);
        ImageButton dialogHelp_filter = dialogView.findViewById(R.id.dialogHelp_filter);

        final ImageView dialogHelp_iv = dialogView.findViewById(R.id.dialogHelp_iv);
        final TextView dialogHelp_tv_title = dialogView.findViewById(R.id.dialogHelp_title);
        final TextView dialogHelp_tv_text = dialogView.findViewById(R.id.dialogHelp_tv);

        dialogHelp_tv_title.setText(HelperUnit.textSpannable(context.getResources().getString(R.string.dialogHelp_tipTitle)));
        dialogHelp_tv_text.setText(HelperUnit.textSpannable(context.getResources().getString(R.string.dialogHelp_tipText)));
        dialogHelp_iv.setVisibility(View.GONE);

        dialogHelp_tip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogHelp_iv.setVisibility(View.GONE);
                dialogHelp_tv_title.setText(HelperUnit.textSpannable(context.getResources().getString(R.string.dialogHelp_tipTitle)));
                dialogHelp_tv_text.setText(HelperUnit.textSpannable(context.getResources().getString(R.string.dialogHelp_tipText)));
            }
        });

        dialogHelp_overview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogHelp_iv.setVisibility(View.VISIBLE);
                dialogHelp_iv.setImageDrawable(context.getResources().getDrawable(R.drawable.help_start));
                dialogHelp_tv_title.setText(HelperUnit.textSpannable(context.getResources().getString(R.string.dialogHelp_overviewTitle)));
                dialogHelp_tv_text.setText(HelperUnit.textSpannable(context.getResources().getString(R.string.dialogHelp_overviewText)));
            }
        });

        dialogHelp_gestures.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogHelp_iv.setVisibility(View.GONE);
                dialogHelp_tv_title.setText(HelperUnit.textSpannable(context.getResources().getString(R.string.dialogHelp_gesturesTitle)));
                dialogHelp_tv_text.setText(HelperUnit.textSpannable(context.getResources().getString(R.string.dialogHelp_gesturesText)));
            }
        });
        dialogHelp_filter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogHelp_iv.setVisibility(View.VISIBLE);
                dialogHelp_iv.setImageDrawable(context.getResources().getDrawable(R.drawable.help_filter));
                dialogHelp_tv_title.setText(HelperUnit.textSpannable(context.getResources().getString(R.string.dialogHelp_filterTitle)));
                dialogHelp_tv_text.setText(HelperUnit.textSpannable(context.getResources().getString(R.string.dialogHelp_filterText)));
            }
        });

        bottomSheetDialog.setContentView(dialogView);
        bottomSheetDialog.show();
        HelperUnit.setBottomSheetBehavior(bottomSheetDialog, dialogView, BottomSheetBehavior.STATE_EXPANDED);
    }

    public static void show_dialogChangelog (final Context context) {
        final BottomSheetDialog dialog = new BottomSheetDialog(context);
        View dialogView = View.inflate(context, R.layout.dialog_text, null);

        TextView dialog_title = dialogView.findViewById(R.id.dialog_title);
        dialog_title.setText(R.string.changelog_title);

        TextView dialog_text = dialogView.findViewById(R.id.dialog_text);
        dialog_text.setText(HelperUnit.textSpannable(context.getString(R.string.changelog_dialog)));
        dialog_text.setMovementMethod(LinkMovementMethod.getInstance());

        ImageButton fab = dialogView.findViewById(R.id.floatButton_ok);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.cancel();
            }
        });

        ImageButton fab_help = dialogView.findViewById(R.id.floatButton_help);
        fab_help.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.cancel();
                HelperUnit.show_dialogHelp(context);
            }
        });

        ImageButton fab_settings = dialogView.findViewById(R.id.floatButton_settings);
        fab_settings.setVisibility(View.GONE);

        dialog.setContentView(dialogView);
        dialog.show();
        HelperUnit.setBottomSheetBehavior(dialog, dialogView, BottomSheetBehavior.STATE_EXPANDED);
    }

    public static void switchIcon (Activity activity, String string, String fieldDB, ImageView be) {
        sp = PreferenceManager.getDefaultSharedPreferences(activity);
        assert be != null;
        switch (string) {
            case "01":be.setImageResource(R.drawable.circle_red);
                sp.edit().putString(fieldDB, "01").apply();break;
            case "02":be.setImageResource(R.drawable.circle_pink);
                sp.edit().putString(fieldDB, "02").apply();break;
            case "03":be.setImageResource(R.drawable.circle_purple);
                sp.edit().putString(fieldDB, "03").apply();break;
            case "04":be.setImageResource(R.drawable.circle_blue);
                sp.edit().putString(fieldDB, "04").apply();break;
            case "05":be.setImageResource(R.drawable.circle_teal);
                sp.edit().putString(fieldDB, "05").apply();break;
            case "06":be.setImageResource(R.drawable.circle_green);
                sp.edit().putString(fieldDB, "06").apply();break;
            case "07":be.setImageResource(R.drawable.circle_lime);
                sp.edit().putString(fieldDB, "07").apply();break;
            case "08":be.setImageResource(R.drawable.circle_yellow);
                sp.edit().putString(fieldDB, "08").apply();break;
            case "09":be.setImageResource(R.drawable.circle_orange);
                sp.edit().putString(fieldDB, "09").apply();break;
            case "10":be.setImageResource(R.drawable.circle_brown);
                sp.edit().putString(fieldDB, "10").apply();break;
            case "11":be.setImageResource(R.drawable.circle_grey);
                sp.edit().putString(fieldDB, "11").apply();break;
            default:
                be.setImageResource(R.drawable.circle_red);
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

    public static SpannableString textSpannable (String text) {
        SpannableString s;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            s = new SpannableString(Html.fromHtml(text,Html.FROM_HTML_MODE_LEGACY));
        } else {
            //noinspection deprecation
            s = new SpannableString(Html.fromHtml(text));
        }
        Linkify.addLinks(s, Linkify.WEB_URLS);
        return s;
    }
}
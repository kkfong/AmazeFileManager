package com.amaze.filemanager.ui.dialogs;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.hardware.fingerprint.FingerprintManager;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatEditText;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.BaseActivity;
import com.amaze.filemanager.activities.BasicActivity;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.adapters.HiddenAdapter;
import com.amaze.filemanager.adapters.RecyclerAdapter;
import com.amaze.filemanager.exceptions.RootNotPermittedException;
import com.amaze.filemanager.filesystem.BaseFile;
import com.amaze.filemanager.filesystem.HFile;
import com.amaze.filemanager.filesystem.RootHelper;
import com.amaze.filemanager.fragments.AppsList;
import com.amaze.filemanager.fragments.MainFragment;
import com.amaze.filemanager.fragments.preference_fragments.Preffrag;
import com.amaze.filemanager.services.asynctasks.CountFolderItems;
import com.amaze.filemanager.services.asynctasks.GenerateHashes;
import com.amaze.filemanager.services.asynctasks.LoadFolderSpaceData;
import com.amaze.filemanager.ui.LayoutElement;
import com.amaze.filemanager.utils.CryptUtil;
import com.amaze.filemanager.utils.DataUtils;
import com.amaze.filemanager.utils.FingerprintHandler;
import com.amaze.filemanager.utils.Futils;
import com.amaze.filemanager.utils.OpenMode;
import com.amaze.filemanager.utils.PreferenceUtils;
import com.amaze.filemanager.utils.Utils;
import com.amaze.filemanager.utils.color.ColorUsage;
import com.amaze.filemanager.utils.theme.AppTheme;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IValueFormatter;
import com.github.mikephil.charting.utils.ViewPortHandler;

import java.io.File;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import eu.chainfire.libsuperuser.Shell;

import static com.amaze.filemanager.utils.Futils.getFreeSpace;
import static com.amaze.filemanager.utils.Futils.getTotalSpace;
import static com.amaze.filemanager.utils.Futils.toHFileArray;

/**
 * Here are a lot of function that create material dialogs
 *
 * @author Emmanuel
 *         on 17/5/2017, at 13:27.
 */

public class GeneralDialogCreation {

    public static MaterialDialog showBasicDialog(Activity m, String fabskin, AppTheme appTheme, String[] texts) {
        MaterialDialog.Builder a = new MaterialDialog.Builder(m)
                .content(texts[0])
                .widgetColor(Color.parseColor(fabskin))
                .theme(appTheme.getMaterialDialogTheme())
                .title(texts[1])
                .positiveText(texts[2])
                .positiveColor(Color.parseColor(fabskin))
                .negativeText(texts[3])
                .negativeColor(Color.parseColor(fabskin));
        if (texts[4] != (null)) {
            a.neutralText(texts[4])
                    .neutralColor(Color.parseColor(fabskin));
        }
        return a.build();
    }

    public static MaterialDialog showNameDialog(final MainActivity m, String[] texts) {
        MaterialDialog.Builder a = new MaterialDialog.Builder(m);
        a.input(texts[0], texts[1], false, new
                MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(MaterialDialog materialDialog, CharSequence charSequence) {

                    }
                });
        a.widgetColor(Color.parseColor(BaseActivity.accentSkin));

        a.theme(m.getAppTheme().getMaterialDialogTheme());
        a.title(texts[2]);
        a.positiveText(texts[3]);
        a.positiveColor(Color.parseColor(BaseActivity.accentSkin));
        a.neutralText(texts[4]);
        if (texts[5] != (null)) {
            a.negativeText(texts[5]);
            a.negativeColor(Color.parseColor(BaseActivity.accentSkin));
        }
        MaterialDialog dialog = a.build();
        return dialog;
    }

    public static void deleteFilesDialog(ArrayList<LayoutElement> a, final MainFragment b, List<Integer> pos, AppTheme appTheme) {
        final MaterialDialog.Builder c = new MaterialDialog.Builder(b.getActivity());
        c.title(b.getResources().getString(R.string.confirm));

        int fileCounter = 0, dirCounter = 0;
        long longSizeTotal = 0;
        final ArrayList<BaseFile> todelete = new ArrayList<>();
        StringBuilder dirNames = new StringBuilder();
        StringBuilder fileNames = new StringBuilder();
        for (int i = 0; i < pos.size(); i++) {
            final LayoutElement elem = a.get(pos.get(i));
            todelete.add(elem.generateBaseFile());
            if (elem.isDirectory()) {
                dirNames.append("\n")
                        .append(++dirCounter)
                        .append(". ")
                        .append(elem.getTitle());
                // TODO: Get folder size ?
            } else {
                fileNames.append("\n")
                        .append(++fileCounter)
                        .append(". ")
                        .append(elem.getTitle())
                        .append(" (")
                        .append(elem.getSize())
                        .append(")");
                longSizeTotal += elem.getlongSize();
            }
        }

        String titleFiles = b.getResources().getString(R.string.title_files).toUpperCase();
        String titleDirs = b.getResources().getString(R.string.title_dirs).toUpperCase();

        StringBuilder message = new StringBuilder();
        message.append(b.getResources().getString(R.string.questiondelete))
                .append("\n\n");
        if (dirCounter == 0 && fileCounter == 1) {
            final LayoutElement elem = a.get(pos.get(0));
            message.append(elem.getTitle())
                    .append(" (")
                    .append(elem.getSize())
                    .append(")");
        } else if (fileCounter == 0) {
            message.append(titleDirs)
                    .append(":")
                    .append(dirNames);
        } else if(dirCounter == 0) {
            message.append(titleFiles)
                    .append(":")
                    .append(fileNames);
        } else {
            message.append(titleDirs)
                    .append(":")
                    .append(dirNames)
                    .append("\n\n")
                    .append(titleFiles)
                    .append(":")
                    .append(fileNames);
        }

        if (fileCounter + dirCounter > 1 && longSizeTotal > 0) {
            message.append("\n\n")
                    .append(b.getResources().getString(R.string.total))
                    .append(" ")
                    .append(Formatter.formatFileSize(b.getContext(), longSizeTotal));
        }

        c.content(message.toString());
        c.theme(appTheme.getMaterialDialogTheme());
        c.negativeText(b.getResources().getString(R.string.cancel).toUpperCase());
        c.positiveText(b.getResources().getString(R.string.delete).toUpperCase());
        c.positiveColor(Color.parseColor(b.fabSkin));
        c.negativeColor(Color.parseColor(b.fabSkin));
        c.callback(new MaterialDialog.ButtonCallback() {
            @Override
            public void onPositive(MaterialDialog materialDialog) {
                Toast.makeText(b.getActivity(), b.getResources().getString(R.string.deleting), Toast.LENGTH_SHORT).show();
                b.MAIN_ACTIVITY.mainActivityHelper.deleteFiles(todelete);
            }

            @Override
            public void onNegative(MaterialDialog materialDialog) {

                //materialDialog.cancel();
            }
        });
        c.build().show();
    }

    public static void showPropertiesDialogWithPermissions(BaseFile baseFile, final String permissions,
                                                           BasicActivity basic, boolean isRoot, AppTheme appTheme) {
        showPropertiesDialog(baseFile, permissions, basic, isRoot, appTheme, true, false);
    }

    public static void showPropertiesDialogWithoutPermissions(final BaseFile f, BasicActivity activity, AppTheme appTheme) {
        showPropertiesDialog(f, null, activity, false, appTheme, false, false);
    }
    public static void showPropertiesDialogForStorage(final BaseFile f, BasicActivity activity, AppTheme appTheme) {
        showPropertiesDialog(f, null, activity, false, appTheme, false, true);
    }

    private static void showPropertiesDialog(final BaseFile baseFile, final String permissions,
                                             BasicActivity basic, boolean isRoot, AppTheme appTheme,
                                             boolean showPermissions, boolean forStorage) {
        final ExecutorService executor = Executors.newFixedThreadPool(3);
        final Context c = basic.getApplicationContext();
        int accentColor = basic.getColorPreference().getColor(ColorUsage.ACCENT);
        long last = baseFile.getDate();
        final String date = Utils.getDate(last),
                items = basic.getResources().getString(R.string.calculating),
                name  = baseFile.getName(),
                parent = baseFile.getReadablePath(baseFile.getParent(c));

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(c);
        String fabskin = PreferenceUtils.getAccentString(sharedPrefs);

        MaterialDialog.Builder builder = new MaterialDialog.Builder(basic);
        builder.title(basic.getResources().getString(R.string.properties));
        builder.theme(appTheme.getMaterialDialogTheme());

        View v = basic.getLayoutInflater().inflate(R.layout.properties_dialog, null);
        TextView itemsText = (TextView) v.findViewById(R.id.t7);

        /*View setup*/ {
            TextView mNameTitle = (TextView) v.findViewById(R.id.title_name);
            mNameTitle.setTextColor(accentColor);

            TextView mDateTitle = (TextView) v.findViewById(R.id.title_date);
            mDateTitle.setTextColor(accentColor);

            TextView mSizeTitle = (TextView) v.findViewById(R.id.title_size);
            mSizeTitle.setTextColor(accentColor);

            TextView mLocationTitle = (TextView) v.findViewById(R.id.title_location);
            mLocationTitle.setTextColor(accentColor);

            TextView md5Title = (TextView) v.findViewById(R.id.title_md5);
            md5Title.setTextColor(accentColor);

            TextView sha256Title = (TextView) v.findViewById(R.id.title_sha256);
            sha256Title.setTextColor(accentColor);

            ((TextView) v.findViewById(R.id.t5)).setText(name);
            ((TextView) v.findViewById(R.id.t6)).setText(parent);
            itemsText.setText(items);
            ((TextView) v.findViewById(R.id.t8)).setText(date);

            LinearLayout mNameLinearLayout = (LinearLayout) v.findViewById(R.id.properties_dialog_name);
            LinearLayout mLocationLinearLayout = (LinearLayout) v.findViewById(R.id.properties_dialog_location);
            LinearLayout mSizeLinearLayout = (LinearLayout) v.findViewById(R.id.properties_dialog_size);
            LinearLayout mDateLinearLayout = (LinearLayout) v.findViewById(R.id.properties_dialog_date);

            // setting click listeners for long press
            mNameLinearLayout.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    Futils.copyToClipboard(c, name);
                    Toast.makeText(c, c.getResources().getString(R.string.name) + " " +
                            c.getResources().getString(R.string.properties_copied_clipboard), Toast.LENGTH_SHORT).show();
                    return false;
                }
            });
            mLocationLinearLayout.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    Futils.copyToClipboard(c, parent);
                    Toast.makeText(c, c.getResources().getString(R.string.location) + " " +
                            c.getResources().getString(R.string.properties_copied_clipboard), Toast.LENGTH_SHORT).show();
                    return false;
                }
            });
            mSizeLinearLayout.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    Futils.copyToClipboard(c, items);
                    Toast.makeText(c, c.getResources().getString(R.string.size) + " " +
                            c.getResources().getString(R.string.properties_copied_clipboard), Toast.LENGTH_SHORT).show();
                    return false;
                }
            });
            mDateLinearLayout.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    Futils.copyToClipboard(c, date);
                    Toast.makeText(c, c.getResources().getString(R.string.date) + " " +
                            c.getResources().getString(R.string.properties_copied_clipboard), Toast.LENGTH_SHORT).show();
                    return false;
                }
            });
        }

        CountFolderItems countFolderItems = new CountFolderItems(c, itemsText, baseFile);
        countFolderItems.executeOnExecutor(executor);

        GenerateHashes hashGen = new GenerateHashes(baseFile, c, v);
        hashGen.executeOnExecutor(executor);

        /*Chart creation and data loading*/ {
            boolean isRightToLeft = c.getResources().getBoolean(R.bool.is_right_to_left);
            boolean isDarkTheme = appTheme.getMaterialDialogTheme() == Theme.DARK;
            PieChart chart = (PieChart) v.findViewById(R.id.chart);

            chart.setTouchEnabled(false);
            chart.setDrawEntryLabels(false);
            chart.setDescription(null);
            chart.setNoDataText(c.getString(R.string.loading));
            chart.setRotationAngle(!isRightToLeft? 0f:180f);
            chart.setHoleColor(Color.TRANSPARENT);
            chart.setCenterTextColor(isDarkTheme? Color.WHITE:Color.BLACK);

            chart.getLegend().setEnabled(true);
            chart.getLegend().setForm(Legend.LegendForm.CIRCLE);
            chart.getLegend().setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
            chart.getLegend().setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));

            chart.animateY(1000);

            if(forStorage) {
                final String[] LEGENDS = new String[]{c.getString(R.string.used), c.getString(R.string.free)};
                final int[] COLORS = {Utils.getColor(c, R.color.piechart_red), Utils.getColor(c, R.color.piechart_green)};

                long totalSpace = getTotalSpace(baseFile),
                        freeSpace = getFreeSpace(baseFile),
                        usedSpace = totalSpace - freeSpace;

                List<PieEntry> entries = new ArrayList<>();
                entries.add(new PieEntry(usedSpace, LEGENDS[0]));
                entries.add(new PieEntry(freeSpace, LEGENDS[1]));

                PieDataSet set = new PieDataSet(entries, null);
                set.setColors(COLORS);
                set.setXValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
                set.setYValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
                set.setSliceSpace(5f);
                set.setAutomaticallyDisableSliceSpacing(true);
                set.setValueLinePart2Length(1.05f);
                set.setSelectionShift(0f);

                PieData pieData = new PieData(set);
                pieData.setValueFormatter(new SizeFormatter(c));
                pieData.setValueTextColor(isDarkTheme? Color.WHITE:Color.BLACK);

                String totalSpaceFormatted = Formatter.formatFileSize(c, totalSpace);

                chart.setCenterText(new SpannableString(c.getString(R.string.total) + "\n" + totalSpaceFormatted));
                chart.setData(pieData);
            } else {
                LoadFolderSpaceData loadFolderSpaceData = new LoadFolderSpaceData(c, appTheme, chart, baseFile);
                loadFolderSpaceData.executeOnExecutor(executor);
            }

            chart.invalidate();
        }

        if(!forStorage && showPermissions) {
            final MainFragment main = ((MainActivity) basic).mainFragment;
            AppCompatButton appCompatButton = (AppCompatButton) v.findViewById(R.id.permissionsButton);
            appCompatButton.setAllCaps(true);

            final View permissionsTable = v.findViewById(R.id.permtable);
            final View button = v.findViewById(R.id.set);
            if (isRoot && permissions.length() > 6) {
                appCompatButton.setVisibility(View.VISIBLE);
                appCompatButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (permissionsTable.getVisibility() == View.GONE) {
                            permissionsTable.setVisibility(View.VISIBLE);
                            button.setVisibility(View.VISIBLE);
                            setPermissionsDialog(permissionsTable, button, baseFile, permissions, c,
                                    main);
                        } else {
                            button.setVisibility(View.GONE);
                            permissionsTable.setVisibility(View.GONE);
                        }
                    }
                });
            }
        }

        builder.customView(v, true);
        builder.positiveText(basic.getResources().getString(R.string.ok));
        builder.positiveColor(Color.parseColor(fabskin));
        builder.dismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                executor.shutdown();
            }
        });

        MaterialDialog materialDialog = builder.build();
        materialDialog.show();
        materialDialog.getActionButton(DialogAction.NEGATIVE).setEnabled(false);

        /*
        View bottomSheet = c.findViewById(R.id.design_bottom_sheet);
        BottomSheetBehavior bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        bottomSheetBehavior.setPeekHeight(BottomSheetBehavior.STATE_DRAGGING);
        */
    }

    public static class SizeFormatter implements IValueFormatter {

        private Context context;

        public SizeFormatter(Context c) {
            context = c;
        }

        @Override
        public String getFormattedValue(float value, Entry entry, int dataSetIndex,
                                        ViewPortHandler viewPortHandler) {
            String prefix = entry.getData() != null && entry.getData() instanceof String?
                    (String) entry.getData():"";

            return prefix + Formatter.formatFileSize(context, (long) value);
        }
    }

    public static void showCloudDialog(final MainActivity mainActivity, AppTheme appTheme, final OpenMode openMode) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mainActivity);
        String fabskin = PreferenceUtils.getAccentString(sp);
        final MaterialDialog.Builder builder = new MaterialDialog.Builder(mainActivity);

        switch (openMode) {
            case DROPBOX:
                builder.title(mainActivity.getResources().getString(R.string.cloud_dropbox));
                break;
            case BOX:
                builder.title(mainActivity.getResources().getString(R.string.cloud_box));
                break;
            case GDRIVE:
                builder.title(mainActivity.getResources().getString(R.string.cloud_drive));
                break;
            case ONEDRIVE:
                builder.title(mainActivity.getResources().getString(R.string.cloud_onedrive));
                break;
        }

        builder.theme(appTheme.getMaterialDialogTheme());
        builder.content(mainActivity.getResources().getString(R.string.cloud_remove));

        builder.positiveText(mainActivity.getResources().getString(R.string.yes));
        builder.positiveColor(Color.parseColor(fabskin));
        builder.negativeText(mainActivity.getResources().getString(R.string.no));
        builder.negativeColor(Color.parseColor(fabskin));

        builder.onPositive(new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                mainActivity.deleteConnection(openMode);
            }
        });

        builder.onNegative(new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    public static void showEncryptWarningDialog(final Intent intent, final MainFragment main,
                                                AppTheme appTheme,
                                                final RecyclerAdapter.EncryptButtonCallbackInterface
                                                        encryptButtonCallbackInterface) {

        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(main.getContext());

        final MaterialDialog.Builder builder = new MaterialDialog.Builder(main.getActivity());
        builder.title(main.getResources().getString(R.string.warning));
        builder.content(main.getResources().getString(R.string.crypt_warning_key));
        builder.theme(appTheme.getMaterialDialogTheme());
        builder.negativeText(main.getResources().getString(R.string.warning_never_show));
        builder.positiveText(main.getResources().getString(R.string.warning_confirm));
        builder.positiveColor(Color.parseColor(main.fabSkin));

        builder.onPositive(new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                try {
                    encryptButtonCallbackInterface.onButtonPressed(intent);
                } catch (Exception e) {
                    e.printStackTrace();

                    Toast.makeText(main.getActivity(),
                            main.getResources().getString(R.string.crypt_encryption_fail),
                            Toast.LENGTH_LONG).show();
                }
            }
        });

        builder.onNegative(new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                preferences.edit().putBoolean(Preffrag.PREFERENCE_CRYPT_WARNING_REMEMBER, true).apply();
                try {
                    encryptButtonCallbackInterface.onButtonPressed(intent);
                } catch (Exception e) {
                    e.printStackTrace();

                    Toast.makeText(main.getActivity(),
                            main.getResources().getString(R.string.crypt_encryption_fail),
                            Toast.LENGTH_LONG).show();
                }
            }
        });

        builder.show();
    }

    public static void showEncryptAuthenticateDialog(final Intent intent, final MainFragment main, AppTheme appTheme,
                                                     final RecyclerAdapter.EncryptButtonCallbackInterface
                                                             encryptButtonCallbackInterface) {

        MaterialDialog.Builder builder = new MaterialDialog.Builder(main.getActivity());
        builder.title(main.getResources().getString(R.string.crypt_encrypt));

        View rootView = View.inflate(main.getActivity(), R.layout.dialog_encrypt_authenticate, null);

        final AppCompatEditText passwordEditText = (AppCompatEditText)
                rootView.findViewById(R.id.edit_text_dialog_encrypt_password);
        final AppCompatEditText passwordConfirmEditText = (AppCompatEditText)
                rootView.findViewById(R.id.edit_text_dialog_encrypt_password_confirm);

        builder.customView(rootView, true);

        builder.positiveText(main.getResources().getString(R.string.ok));
        builder.negativeText(main.getResources().getString(R.string.cancel));
        builder.theme(appTheme.getMaterialDialogTheme());
        builder.positiveColor(Color.parseColor(main.fabSkin));
        builder.negativeColor(Color.parseColor(main.fabSkin));

        builder.onNegative(new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                dialog.cancel();
            }
        });

        builder.onPositive(new MaterialDialog.SingleButtonCallback() {

            @Override
            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {

                if (TextUtils.isEmpty(passwordEditText.getText()) ||
                        TextUtils.isEmpty(passwordConfirmEditText.getText())) {
                    dialog.cancel();
                    return;
                }

                try {
                    encryptButtonCallbackInterface.onButtonPressed(intent,
                            passwordEditText.getText().toString());
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(main.getActivity(),
                            main.getResources().getString(R.string.crypt_encryption_fail),
                            Toast.LENGTH_LONG).show();
                }
            }
        });

        builder.show();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public static void showDecryptFingerprintDialog(final Intent intent, final MainFragment main, AppTheme appTheme,
                                                    final RecyclerAdapter.DecryptButtonCallbackInterface
                                                            decryptButtonCallbackInterface)
            throws IOException, CertificateException, NoSuchAlgorithmException, InvalidKeyException,
            UnrecoverableEntryException, InvalidAlgorithmParameterException, NoSuchPaddingException,
            NoSuchProviderException, BadPaddingException, KeyStoreException, IllegalBlockSizeException {

        MaterialDialog.Builder builder = new MaterialDialog.Builder(main.getActivity());
        builder.title(main.getResources().getString(R.string.crypt_decrypt));

        View rootView = View.inflate(main.getActivity(),
                R.layout.dialog_decrypt_fingerprint_authentication, null);

        Button cancelButton = (Button) rootView.findViewById(R.id.button_decrypt_fingerprint_cancel);
        cancelButton.setTextColor(Color.parseColor(main.fabSkin));
        builder.customView(rootView, true);
        builder.canceledOnTouchOutside(false);

        builder.theme(appTheme.getMaterialDialogTheme());

        final MaterialDialog dialog = builder.show();
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.cancel();
            }
        });

        FingerprintManager manager = (FingerprintManager) main.getActivity().getSystemService(Context.FINGERPRINT_SERVICE);
        FingerprintManager.CryptoObject object = new
                FingerprintManager.CryptoObject(CryptUtil.initCipher(main.getContext()));

        FingerprintHandler handler = new FingerprintHandler(main.getActivity(), intent, dialog,
                decryptButtonCallbackInterface);
        handler.authenticate(manager, object);
    }

    public static void showDecryptDialog(final Intent intent, final MainFragment main, AppTheme appTheme,
                                  final String password,
                                  final RecyclerAdapter.DecryptButtonCallbackInterface
                                          decryptButtonCallbackInterface) {
        MaterialDialog.Builder builder = new MaterialDialog.Builder(main.getActivity());
        builder.title(main.getResources().getString(R.string.crypt_decrypt));

        builder.input(main.getResources().getString(R.string.authenticate_password), "", false,
                new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {
                    }
                });

        builder.theme(appTheme.getMaterialDialogTheme());
        builder.positiveText(main.getResources().getString(R.string.ok));
        builder.negativeText(main.getResources().getString(R.string.cancel));
        builder.positiveColor(Color.parseColor(main.fabSkin));
        builder.negativeColor(Color.parseColor(main.fabSkin));
        builder.onPositive(new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {

                EditText editText = dialog.getInputEditText();

                if (editText.getText().toString().equals(password))
                    decryptButtonCallbackInterface.confirm(intent);
                else decryptButtonCallbackInterface.failed();
            }
        });
        builder.onNegative(new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    public static void showSMBHelpDialog(Context m,String acc){
        MaterialDialog.Builder b=new MaterialDialog.Builder(m);
        b.content(m.getText(R.string.smb_instructions));
        b.positiveText(R.string.doit);
        b.positiveColor(Color.parseColor(acc));
        b.build().show();
    }

    public static void showPackageDialog(final File f, final MainActivity m) {
        MaterialDialog.Builder mat = new MaterialDialog.Builder(m);
        mat.title(R.string.packageinstaller).content(R.string.pitext)
                .positiveText(R.string.install)
                .negativeText(R.string.view)
                .neutralText(R.string.cancel)
                .positiveColor(Color.parseColor(BaseActivity.accentSkin))
                .negativeColor(Color.parseColor(BaseActivity.accentSkin))
                .neutralColor(Color.parseColor(BaseActivity.accentSkin))
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog materialDialog) {
                        Futils.openunknown(f, m, false);
                    }

                    @Override
                    public void onNegative(MaterialDialog materialDialog) {
                        m.openZip(f.getPath());
                    }
                })
                .theme(m.getAppTheme().getMaterialDialogTheme())
                .build()
                .show();
    }


    public static void showArchiveDialog(final File f, final MainActivity m) {
        MaterialDialog.Builder mat = new MaterialDialog.Builder(m);
        mat.title(R.string.archive)
                .content(R.string.archtext)
                .positiveText(R.string.extract)
                .negativeText(R.string.view)
                .neutralText(R.string.cancel)
                .positiveColor(Color.parseColor(BaseActivity.accentSkin))
                .negativeColor(Color.parseColor(BaseActivity.accentSkin))
                .neutralColor(Color.parseColor(BaseActivity.accentSkin))
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog materialDialog) {
                        m. mainActivityHelper.extractFile(f);
                    }

                    @Override
                    public void onNegative(MaterialDialog materialDialog) {
                        //m.addZipViewTab(f.getPath());
                        if (f.getName().toLowerCase().endsWith(".rar"))
                            m.openRar(Uri.fromFile(f).toString());
                        else
                            m.openZip(Uri.fromFile(f).toString());
                    }
                });
        if (m.getAppTheme().equals(AppTheme.DARK)) mat.theme(Theme.DARK);
        MaterialDialog b = mat.build();

        if (!f.getName().toLowerCase().endsWith(".rar") && !f.getName().toLowerCase().endsWith(".jar") && !f.getName().toLowerCase().endsWith(".apk") && !f.getName().toLowerCase().endsWith(".zip"))
            b.getActionButton(DialogAction.NEGATIVE).setEnabled(false);
        b.show();
    }

    public static void showCompressDialog(final MainActivity m, final ArrayList<BaseFile> b, final String current) {
        MaterialDialog.Builder a = new MaterialDialog.Builder(m);
        a.input(m.getResources().getString(R.string.enterzipname), ".zip", false, new
                MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(MaterialDialog materialDialog, CharSequence charSequence) {

                    }
                });
        a.widgetColor(Color.parseColor(BaseActivity.accentSkin));
        a.theme(m.getAppTheme().getMaterialDialogTheme());
        a.title(m.getResources().getString(R.string.enterzipname));
        a.positiveText(R.string.create);
        a.positiveColor(Color.parseColor(BaseActivity.accentSkin));
        a.onPositive(new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(MaterialDialog materialDialog, DialogAction dialogAction) {
                if (materialDialog.getInputEditText().getText().toString().equals(".zip"))
                    Toast.makeText(m, "File should have a name", Toast.LENGTH_SHORT).show();
                else {
                    String name = current + "/" + materialDialog.getInputEditText().getText().toString();
                    m.mainActivityHelper.compressFiles(new File(name), b);
                }
            }
        });
        a.negativeText(m.getResources().getString(R.string.cancel));
        a.negativeColor(Color.parseColor(BaseActivity.accentSkin));
        a.build().show();
    }

    public static void showSortDialog(final MainFragment m, AppTheme appTheme) {
        String[] sort = m.getResources().getStringArray(R.array.sortby);
        int current = Integer.parseInt(m.sharedPref.getString("sortby", "0"));
        MaterialDialog.Builder a = new MaterialDialog.Builder(m.getActivity());
        a.theme(appTheme.getMaterialDialogTheme());
        a.items(sort).itemsCallbackSingleChoice(current > 3 ? current - 4 : current, new MaterialDialog.ListCallbackSingleChoice() {
            @Override
            public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {

                return true;
            }
        });

        a.negativeText(R.string.ascending).positiveColor(Color.parseColor(BaseActivity.accentSkin));
        a.positiveText(R.string.descending).negativeColor(Color.parseColor(BaseActivity.accentSkin));
        a.onNegative(new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {

                m.sharedPref.edit().putString("sortby", "" + dialog.getSelectedIndex()).commit();
                m.getSortModes();
                m.updateList();
                dialog.dismiss();
            }
        });

        a.onPositive(new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {

                m.sharedPref.edit().putString("sortby", "" + (dialog.getSelectedIndex() + 4)).commit();
                m.getSortModes();
                m.updateList();
                dialog.dismiss();
            }
        });
        a.title(R.string.sortby);
        a.build().show();
    }

    public static void showSortDialog(final AppsList m, AppTheme appTheme) {
        String[] sort = m.getResources().getStringArray(R.array.sortbyApps);
        int current = Integer.parseInt(m.Sp.getString("sortbyApps", "0"));
        MaterialDialog.Builder a = new MaterialDialog.Builder(m.getActivity());
        a.theme(appTheme.getMaterialDialogTheme());
        a.items(sort).itemsCallbackSingleChoice(current > 2 ? current - 3 : current, new MaterialDialog.ListCallbackSingleChoice() {
            @Override
            public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {

                return true;
            }
        });
        a.negativeText(R.string.ascending).positiveColor(Color.parseColor(BaseActivity.accentSkin));
        a.positiveText(R.string.descending).negativeColor(Color.parseColor(BaseActivity.accentSkin));
        a.onNegative(new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {

                m.Sp.edit().putString("sortbyApps", "" + dialog.getSelectedIndex()).commit();
                m.getSortModes();
                m.getLoaderManager().restartLoader(AppsList.ID_LOADER_APP_LIST, null, m);
                dialog.dismiss();
            }
        });

        a.onPositive(new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {

                m.Sp.edit().putString("sortbyApps", "" + (dialog.getSelectedIndex() + 3)).commit();
                m.getSortModes();
                m.getLoaderManager().restartLoader(AppsList.ID_LOADER_APP_LIST, null, m);
                dialog.dismiss();
            }
        });

        a.title(R.string.sortby);
        a.build().show();
    }


    public static void showHistoryDialog(final DataUtils dataUtils, Futils utils, final MainFragment m, AppTheme appTheme) {
        final MaterialDialog.Builder a = new MaterialDialog.Builder(m.getActivity());
        a.positiveText(R.string.cancel);
        a.positiveColor(Color.parseColor(BaseActivity.accentSkin));
        a.negativeText(R.string.clear);
        a.negativeColor(Color.parseColor(BaseActivity.accentSkin));
        a.title(R.string.history);
        a.onNegative(new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                dataUtils.clearHistory();
            }
        });
        a.theme(appTheme.getMaterialDialogTheme());

        a.autoDismiss(true);
        HiddenAdapter adapter = new HiddenAdapter(m.getActivity(), m, utils, R.layout.bookmarkrow,
                toHFileArray(dataUtils.getHistory()), null, true);
        a.adapter(adapter, null);

        MaterialDialog x= a.build();
        adapter.updateDialog(x);
        x.show();
    }

    public static void showHiddenDialog(DataUtils dataUtils, Futils utils, final MainFragment m, AppTheme appTheme) {
        final MaterialDialog.Builder a = new MaterialDialog.Builder(m.getActivity());
        a.positiveText(R.string.cancel);
        a.positiveColor(Color.parseColor(BaseActivity.accentSkin));
        a.title(R.string.hiddenfiles);
        a.theme(appTheme.getMaterialDialogTheme());
        a.autoDismiss(true);
        HiddenAdapter adapter = new HiddenAdapter(m.getActivity(), m, utils, R.layout.bookmarkrow,
                toHFileArray(dataUtils.getHiddenfiles()), null, false);
        a.adapter(adapter, null);
        a.dividerColor(Color.GRAY);
        MaterialDialog x= a.build();
        adapter.updateDialog(x);
        x.show();

    }

    public static void setPermissionsDialog(final View v, View but, final HFile file,
                                     final String f, final Context context, final MainFragment mainFrag) {
        final CheckBox readown = (CheckBox) v.findViewById(R.id.creadown);
        final CheckBox readgroup = (CheckBox) v.findViewById(R.id.creadgroup);
        final CheckBox readother = (CheckBox) v.findViewById(R.id.creadother);
        final CheckBox writeown = (CheckBox) v.findViewById(R.id.cwriteown);
        final CheckBox writegroup = (CheckBox) v.findViewById(R.id.cwritegroup);
        final CheckBox writeother = (CheckBox) v.findViewById(R.id.cwriteother);
        final CheckBox exeown = (CheckBox) v.findViewById(R.id.cexeown);
        final CheckBox exegroup = (CheckBox) v.findViewById(R.id.cexegroup);
        final CheckBox exeother = (CheckBox) v.findViewById(R.id.cexeother);
        String perm = f;
        if (perm.length() < 6) {
            v.setVisibility(View.GONE);
            but.setVisibility(View.GONE);
            Toast.makeText(context, R.string.not_allowed, Toast.LENGTH_SHORT).show();
            return;
        }
        ArrayList<Boolean[]> arrayList = Futils.parse(perm);
        Boolean[] read = arrayList.get(0);
        Boolean[] write = arrayList.get(1);
        final Boolean[] exe = arrayList.get(2);
        readown.setChecked(read[0]);
        readgroup.setChecked(read[1]);
        readother.setChecked(read[2]);
        writeown.setChecked(write[0]);
        writegroup.setChecked(write[1]);
        writeother.setChecked(write[2]);
        exeown.setChecked(exe[0]);
        exegroup.setChecked(exe[1]);
        exeother.setChecked(exe[2]);
        but.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int a = 0, b = 0, c = 0;
                if (readown.isChecked()) a = 4;
                if (writeown.isChecked()) b = 2;
                if (exeown.isChecked()) c = 1;
                int owner = a + b + c;
                int d = 0;
                int e = 0;
                int f = 0;
                if (readgroup.isChecked()) d = 4;
                if (writegroup.isChecked()) e = 2;
                if (exegroup.isChecked()) f = 1;
                int group = d + e + f;
                int g = 0, h = 0, i = 0;
                if (readother.isChecked()) g = 4;
                if (writeother.isChecked()) h = 2;
                if (exeother.isChecked()) i = 1;
                int other = g + h + i;
                String finalValue = owner + "" + group + "" + other;

                String command = "chmod " + finalValue + " " + file.getPath();
                if (file.isDirectory())
                    command = "chmod -R " + finalValue + " \"" + file.getPath() + "\"";

                try {
                    RootHelper.runShellCommand(command, new Shell.OnCommandResultListener() {
                        @Override
                        public void onCommandResult(int commandCode, int exitCode, List<String> output) {
                            if (exitCode < 0) {
                                Toast.makeText(context, mainFrag.getString(R.string.operationunsuccesful),
                                        Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(context,
                                        mainFrag.getResources().getString(R.string.done), Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                    mainFrag.updateList();
                } catch (RootNotPermittedException e1) {
                    Toast.makeText(context, mainFrag.getResources().getString(R.string.rootfailure),
                            Toast.LENGTH_LONG).show();
                    e1.printStackTrace();
                }

            }
        });
    }

}

/*
 * Copyright (C) 2017 The LineageOS Project
 * Copyright (C) 2019 The PixelExperience Project
 * Copyright (C) 2025 Neoteric OS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.
 */

package org.neoteric.ota.prefs;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.BatteryManager;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import org.neoteric.ota.R;
import org.neoteric.ota.UpdatesActivity;
import org.neoteric.ota.controller.UpdaterController;
import org.neoteric.ota.misc.Constants;
import org.neoteric.ota.misc.StringGenerator;
import org.neoteric.ota.misc.Utils;
import org.neoteric.ota.model.Update;
import org.neoteric.ota.model.UpdateInfo;
import org.neoteric.ota.model.UpdateStatus;

import java.text.DateFormat;
import java.text.NumberFormat;

/**
 * A preference that shows the updater card.
 */
public class UpdaterCardPreference extends Preference {

    private static final int BATTERY_PLUGGED_ANY =
            BatteryManager.BATTERY_PLUGGED_AC |
            BatteryManager.BATTERY_PLUGGED_USB |
            BatteryManager.BATTERY_PLUGGED_WIRELESS;

    private final Context mContext;
    private LocalBroadcastManager mBroadcastManager;
    private Vibrator mVibrator;

    private UpdaterController mUpdaterController;
    private UpdateInfo mUpdate;
    private String mDownloadId;

    private Button mAction;
    private ImageView mOptionsButton;

    private TextView mBuildDate;
    private TextView mBuildName;
    private TextView mBuildSize;

    private ProgressBar mProgressBar;
    private TextView mProgressText;

    public UpdaterCardPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        init();
    }

    public UpdaterCardPreference(Context context) {
        super(context);
        mContext = context;
        init();
    }

    private void init() {
        setLayoutResource(R.layout.pref_updater_card);
        mBroadcastManager = LocalBroadcastManager.getInstance(mContext);
        mVibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
    }

    public void setUpdaterController(UpdaterController controller) {
        mUpdaterController = controller;
        notifyChanged();
    }

    public void setDownloadId(String downloadId) {
        mDownloadId = downloadId;
        notifyChanged();
    }

    public void notifyUpdateChanged() {
        notifyChanged();
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        mAction = (Button) holder.findViewById(R.id.update_action);
        mOptionsButton = (ImageView) holder.findViewById(R.id.options_action);

        mBuildDate = (TextView) holder.findViewById(R.id.build_date);
        mBuildName = (TextView) holder.findViewById(R.id.build_name);
        mBuildSize = (TextView) holder.findViewById(R.id.build_size);

        mProgressBar = (ProgressBar) holder.findViewById(R.id.progress_bar);
        mProgressText =(TextView) holder.findViewById(R.id.progress_text);

        if (mAction == null) return;
        if (mUpdaterController == null || mDownloadId == null) {
            mAction.setEnabled(false);
            if (mAction != null && mDownloadId == null) {
                mAction.setText(R.string.action_download);
            }
            if (mOptionsButton != null) mOptionsButton.setVisibility(View.GONE);
            return;
        }

        mUpdate = mUpdaterController.getCurrentUpdate();
        if (mUpdate == null) {
            mAction.setEnabled(false);
            mAction.setText(R.string.action_download);
            if (mOptionsButton != null) mOptionsButton.setVisibility(View.GONE);
            return;
        }

        holder.itemView.setSelected(true);

        if (mBuildDate != null) {
            String buildDate = StringGenerator.getDateLocalizedUTC(mContext,
                    DateFormat.LONG, mUpdate.getTimestamp());
            mBuildDate.setText(buildDate);
        }
        if (mBuildName != null) {
            String buildVersion = mUpdate.getName();
            mBuildName.setText(buildVersion);
            mBuildName.setCompoundDrawables(null, null, null, null);
        }

        boolean activeLayout;
        switch (Utils.getPersistentStatus(mContext)) {
            case UpdateStatus.Persistent.UNKNOWN:
                activeLayout = mUpdate.getStatus() == UpdateStatus.STARTING;
                break;
            case UpdateStatus.Persistent.VERIFIED:
                activeLayout = mUpdate.getStatus() == UpdateStatus.INSTALLING &&
                        mUpdate.getStatus() != UpdateStatus.INSTALLATION_FAILED;
                break;
            case UpdateStatus.Persistent.DOWNLOADING:
            case UpdateStatus.Persistent.STARTING_DOWNLOAD:
                activeLayout = true;
                break;
            default:
                throw new RuntimeException("Unknown update status");
        }

        if (activeLayout) {
            handleActiveStatus(holder);
        } else {
            handleNotActiveStatus(holder);
        }
    }

    @SuppressLint("SetTextI18n")
    private void handleActiveStatus(PreferenceViewHolder holder) {
        boolean canDelete = false;

        if (mUpdaterController == null || mUpdate == null) {
            if (mAction != null) mAction.setEnabled(false);
            return;
        }

        UpdaterController.DownloadInfo downloadInfo = mUpdaterController.getDownloadInfo();
        UpdaterController.InstallInfo installInfo = mUpdaterController.getInstallInfo();

        if (mUpdaterController.isDownloading()) {
            canDelete = mUpdate.getStatus() != UpdateStatus.STARTING;
            long length = mUpdate.getFile() != null &&
                    mUpdate.getStatus() != UpdateStatus.STARTING ?
                    mUpdate.getFile().length() : 0;
            String downloaded = Utils.readableFileSize(length);
            String total = Utils.readableFileSize(mUpdate.getFileSize());
            String percentage = NumberFormat.getPercentInstance().format(
                    downloadInfo.getProgress() / 100.f);
            long eta = downloadInfo.getEta();
            if (mProgressText != null) {
                if (eta > 0) {
                    CharSequence etaString = StringGenerator.formatETA(mContext, eta * 1000);
                    mProgressText.setText(mContext.getString(
                            R.string.list_download_progress_eta_new, downloaded, total, etaString,
                            percentage));
                } else {
                    mProgressText.setText(mContext.getString(
                            R.string.list_download_progress_new, downloaded, total, percentage));
                }
            }
            setButtonAction(mAction, Action.PAUSE, mUpdate.getStatus() != UpdateStatus.STARTING);
            if (mProgressBar != null) {
                mProgressBar.setIndeterminate(mUpdate.getStatus() == UpdateStatus.STARTING);
                mProgressBar.setProgress(downloadInfo.getProgress());
            }
        } else if (mUpdaterController.isInstallingUpdate()) {
            setButtonAction(mAction, Action.INSTALL, mUpdate.getStatus() == UpdateStatus.INSTALLATION_FAILED);
            boolean notAB = !mUpdaterController.isInstallingABUpdate();
            if (mProgressText != null) {
                mProgressText.setText(notAB ? R.string.dialog_prepare_zip_message :
                        installInfo.isFinalizing() ?
                                R.string.finalizing_package :
                                R.string.installing_update);
            }
            if (mProgressBar != null) {
                mProgressBar.setIndeterminate(installInfo.getProgress() == 0);
                mProgressBar.setProgress(installInfo.getProgress());
            }
        } else if (mUpdaterController.isVerifyingUpdate()) {
            setButtonAction(mAction, Action.INSTALL, false);
            if (mProgressText != null) mProgressText.setText(R.string.list_verifying_update);
            if (mProgressBar != null) mProgressBar.setIndeterminate(true);
        } else {
            canDelete = mUpdate.getStatus() != UpdateStatus.STARTING;
            setButtonAction(mAction, Action.RESUME, !isBusy());
            long length = (mUpdate.getFile() != null && mUpdate.getStatus() != UpdateStatus.STARTING)
                    ? mUpdate.getFile().length() : 0;
            String downloaded = Utils.readableFileSize(length);
            String total = Utils.readableFileSize(mUpdate.getFileSize());
            String percentage = NumberFormat.getPercentInstance().format(
                    (mUpdaterController.getDownloadInfo() != null ? mUpdaterController.getDownloadInfo().getProgress() : 0) / 100.f);
            if (mProgressText != null) {
                mProgressText.setText(mContext.getString(R.string.list_download_progress_new,
                        downloaded, total, percentage));
            }
            if (mProgressBar != null) {
                mProgressBar.setIndeterminate(false);
                mProgressBar.setProgress(mUpdaterController.getDownloadInfo() != null ?
                        mUpdaterController.getDownloadInfo().getProgress() : 0);
            }
        }

        if (mBuildName != null) mBuildName.setSelected(false);
        setupOptionMenuListeners(canDelete, holder);
        if (mProgressBar != null) mProgressBar.setVisibility(View.VISIBLE);
        if (mProgressText != null) mProgressText.setVisibility(View.VISIBLE);
        if (mBuildSize != null) mBuildSize.setVisibility(View.GONE);
    }

    private void handleNotActiveStatus(PreferenceViewHolder holder) {
        if (mUpdate == null) return;

        if (Utils.getPersistentStatus(mContext) == UpdateStatus.Persistent.VERIFIED) {
            setupOptionMenuListeners(true, holder);
            setButtonAction(mAction,
                    Utils.canInstall(mUpdate) ? Action.INSTALL : Action.DELETE, !isBusy());
        } else if (!Utils.canInstall(mUpdate)) {
            setupOptionMenuListeners(false, holder);
            if (mAction != null) mAction.setEnabled(false);
        } else {
            setupOptionMenuListeners(false, holder);
            setButtonAction(mAction, Action.DOWNLOAD, !isBusy());
        }

        if (mBuildSize != null) {
            String fileSize = Utils.readableFileSize(mUpdate.getFileSize());
            mBuildSize.setText(fileSize);
        }

        if (mProgressBar != null) mProgressBar.setVisibility(View.GONE);
        if (mProgressText != null) mProgressText.setVisibility(View.GONE);
        if (mBuildSize != null) mBuildSize.setVisibility(View.VISIBLE);
        if (mBuildName != null) mBuildName.setSelected(true);
    }

    private void setButtonAction(Button button, Action action, boolean enabled) {
        final View.OnClickListener clickListener;
        switch (action) {
            case DOWNLOAD:
                button.setText(R.string.action_download);
                button.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_updateui_download, 0, 0, 0);
                button.setEnabled(enabled);
                clickListener = enabled ? view -> startDownloadWithWarning() : null;
                break;
            case PAUSE:
                button.setText(R.string.action_pause);
                button.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pause, 0, 0, 0);
                button.setEnabled(enabled);
                clickListener = enabled ? view -> mUpdaterController.pauseDownload()
                        : null;
                break;
            case RESUME: {
                button.setText(R.string.action_resume);
                button.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_updateui_resume, 0, 0, 0);
                button.setEnabled(enabled);
                final boolean canInstall = Utils.canInstall(mUpdate) ||
                        (mUpdate.getFile() != null && mUpdate.getFile().length() == mUpdate.getFileSize());
                clickListener = enabled ? view -> {
                    if (canInstall) {
                        mUpdaterController.resumeDownload();
                    } else {
                        showSnackbar(mContext.getString(R.string.snack_update_not_installable));
                    }
                } : null;
            }
            break;
            case INSTALL: {
                button.setText(R.string.action_install);
                button.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_updateui_install, 0, 0, 0);
                button.setEnabled(enabled);
                final boolean canInstall = Utils.canInstall(mUpdate);
                clickListener = enabled ? view -> {
                    if (canInstall) {
                        getInstallDialog().show();
                    } else {
                        showSnackbar(mContext.getString(R.string.snack_update_not_installable));
                    }
                } : null;
            }
            break;
            case DELETE: {
                button.setText(R.string.action_delete);
                button.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_updateui_delete, 0, 0, 0);
                button.setEnabled(enabled);
                clickListener = enabled ? view -> getDeleteDialog().show() : null;
            }
            break;
            default:
                clickListener = null;
                button.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        }
        button.setAlpha(enabled ? 1.f : 0.5f);

        button.setOnClickListener(v -> {
            if (clickListener != null) {
                Utils.doHapticFeedback(mContext, mVibrator);
                clickListener.onClick(v);
            }
        });
    }

    private void startDownloadWithWarning() {
        mBroadcastManager.sendBroadcast(new Intent(UpdatesActivity.ACTION_START_DOWNLOAD_WITH_WARNING));
    }

    private boolean isBusy() {
        return mUpdaterController != null && (mUpdaterController.hasActiveDownloads() || mUpdaterController.isVerifyingUpdate()
                || mUpdaterController.isInstallingUpdate());
    }

    private AlertDialog.Builder getDeleteDialog() {
        return new AlertDialog.Builder(mContext, R.style.AppTheme_AlertDialogStyle)
                .setTitle(R.string.confirm_delete_dialog_title)
                .setMessage(R.string.confirm_delete_dialog_message)
                .setPositiveButton(android.R.string.ok,
                        (dialog, which) -> {
                            if (mUpdaterController != null) {
                                mUpdaterController.pauseDownload();
                                mUpdaterController.removeUpdateAndNotify();
                            }
                        })
                .setNegativeButton(android.R.string.cancel, null);
    }

    @SuppressLint("RestrictedApi")
    private void setupOptionMenuListeners(final boolean canDelete, PreferenceViewHolder holder) {
        if (mBuildDate == null && mOptionsButton == null) return;

        View anchor = mOptionsButton != null ? mOptionsButton : mBuildDate;

        anchor.setOnClickListener(v -> {
            View menuView = LayoutInflater.from(mContext)
                    .inflate(R.layout.popup_menu, null);

            PopupWindow popup = new PopupWindow(
                    menuView,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    true
            );

            popup.setOutsideTouchable(true);
            popup.setElevation(8f);

            TextView deleteItem = menuView.findViewById(R.id.menu_delete_action);
            TextView exportItem = menuView.findViewById(R.id.menu_export_update);

            deleteItem.setVisibility(canDelete ? View.VISIBLE : View.GONE);
            exportItem.setVisibility(
                    Utils.getPersistentStatus(mContext) == UpdateStatus.Persistent.VERIFIED
                            && !mUpdate.getDownloadId().equals(Update.LOCAL_ID)
                            ? View.VISIBLE : View.GONE
            );

            deleteItem.setOnClickListener(item -> {
                getDeleteDialog().show();
                popup.dismiss();
            });

            exportItem.setOnClickListener(item -> {
                exportUpdate();
                popup.dismiss();
            });

            popup.showAsDropDown(anchor);
        });

        boolean isOneMenuItemVisible = canDelete ||
                (Utils.getPersistentStatus(mContext) == UpdateStatus.Persistent.VERIFIED
                        && !mUpdate.getDownloadId().equals(Update.LOCAL_ID));
        if (mOptionsButton != null) mOptionsButton.setVisibility(isOneMenuItemVisible ? View.VISIBLE : View.GONE);
    }

    private AlertDialog.Builder getInstallDialog() {
        if (!isBatteryLevelOk()) {
            Resources resources = mContext.getResources();
            String message = resources.getString(R.string.dialog_battery_low_message_pct,
                    resources.getInteger(R.integer.battery_ok_percentage_discharging),
                    resources.getInteger(R.integer.battery_ok_percentage_charging));
            return new AlertDialog.Builder(mContext, R.style.AppTheme_AlertDialogStyle)
                    .setTitle(R.string.dialog_battery_low_title)
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok, null);
        }
        int resId;
        String extraMessage = "";
        if (Utils.isABDevice()) {
            resId = R.string.apply_update_dialog_message_ab;
        } else {
            resId = R.string.apply_update_dialog_message;
            extraMessage = " (" + Constants.DOWNLOAD_PATH + ")";
        }

        return new AlertDialog.Builder(mContext, R.style.AppTheme_AlertDialogStyle)
                .setTitle(R.string.apply_update_dialog_title)
                .setMessage(mContext.getString(resId, mUpdate.getName(),
                        mContext.getString(android.R.string.ok)) + extraMessage)
                .setPositiveButton(android.R.string.ok,
                        (dialog, which) -> Utils.triggerUpdate(mContext))
                .setNegativeButton(android.R.string.cancel, null);
    }

    private boolean isBatteryLevelOk() {
        Intent intent = mContext.registerReceiver(null,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (!intent.getBooleanExtra(BatteryManager.EXTRA_PRESENT, false)) {
            return true;
        }
        int percent = Math.round(100.f * intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 100) /
                intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100));
        int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
        int required = (plugged & BATTERY_PLUGGED_ANY) != 0 ?
                mContext.getResources().getInteger(R.integer.battery_ok_percentage_charging) :
                mContext.getResources().getInteger(R.integer.battery_ok_percentage_discharging);
        return percent >= required;
    }

    private void exportUpdate() {
        if (mUpdate == null) return;
        Intent intent = new Intent(UpdatesActivity.ACTION_EXPORT_UPDATE);
        intent.putExtra(UpdatesActivity.EXTRA_UPDATE_NAME, mUpdate.getName());
        intent.putExtra(UpdatesActivity.EXTRA_UPDATE_FILE, mUpdate.getFile());
        mBroadcastManager.sendBroadcast(intent);
    }

    private void showSnackbar(String text) {
        Intent intent = new Intent(UpdatesActivity.ACTION_SHOW_SNACKBAR);
        intent.putExtra(UpdatesActivity.EXTRA_SNACKBAR_TEXT, text);
        mBroadcastManager.sendBroadcast(intent);
    }

    @Override
    public void onDetached() {
        super.onDetached();
        mAction = null;
        mOptionsButton = null;
        mBuildDate = null;
        mBuildName = null;
        mBuildSize = null;
        mProgressBar = null;
        mProgressText = null;
    }

    private enum Action {
        DOWNLOAD,
        PAUSE,
        RESUME,
        INSTALL,
        DELETE,
    }
}

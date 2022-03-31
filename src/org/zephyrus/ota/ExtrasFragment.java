package org.zephyrus.ota;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;

import org.zephyrus.ota.misc.Utils;

import org.zephyrus.ota.R;

import java.util.ArrayList;

public class ExtrasFragment extends Fragment {

    private View mainView;
    private ExtraCardView maintainerCard;
    private ExtraCardView donateCard;
    private ExtraCardView groupCard;

    private String[] deviceList;
    private String[] maintainerNameList;
    private String[] maintainerLinkList;
    private String[] donateList;
    private String[] groupList;
    private int device_index = -1;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mainView = inflater.inflate(R.layout.extras_fragment, container, false);
        maintainerCard = mainView.findViewById(R.id.maintainer_card);
        donateCard = mainView.findViewById(R.id.donate_card);
        groupCard = mainView.findViewById(R.id.group_card);

        deviceList = getContext().getResources().getStringArray(
                R.array.config_device_list);
        maintainerNameList = getContext().getResources().getStringArray(
                R.array.config_maintainer_name_list);
        maintainerLinkList = getContext().getResources().getStringArray(
                R.array.config_maintainer_link_list);
        donateList = getContext().getResources().getStringArray(
                R.array.config_donate_list);
        groupList = getContext().getResources().getStringArray(
                R.array.config_group_list);
        device_index = getDeviceIndex();

        return mainView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    private int getDeviceIndex() {
        final String device = Utils.getDevice();
        if (device == null || device.isEmpty()) return -1;
        for (int i = 0; i < deviceList.length; ++i) {
            if (device.equals(deviceList[i])) return i;
        }
        return -1;
    }

    void updatePrefs() {
        if (device_index != -1) {
            maintainerCard.setOnClickListener(v -> openUrl(maintainerLinkList[device_index]));
            maintainerCard.setSummary(maintainerNameList[device_index]);
            maintainerCard.setClickable(true);

            donateCard.setOnClickListener(v -> openUrl(donateList[device_index]));
            donateCard.setClickable(true);
            donateCard.setVisibility(View.VISIBLE);

            groupCard.setOnClickListener(v -> openUrl(groupList[device_index]));
            groupCard.setClickable(true);
            groupCard.setVisibility(View.VISIBLE);
        } else {
            maintainerCard.setSummary(getContext().getResources().getString(
                    R.string.maintainer_info_unknown));
            maintainerCard.setClickable(false);
        }
        maintainerCard.setVisibility(View.VISIBLE);
    }

    private void showSnackbar(int stringId, int duration) {
        Snackbar.make(getActivity().findViewById(R.id.main_container), stringId, duration).show();
    }

    private void openUrl(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception ex) {
            showSnackbar(R.string.error_open_url, Snackbar.LENGTH_SHORT);
        }
    }
}

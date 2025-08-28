package com.walklight.safety;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.sidesheet.SideSheetDialog;

public class SettingsSheetDialog extends DialogFragment {

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() instanceof SideSheetDialog) {
            SideSheetDialog d = (SideSheetDialog) getDialog();
            View sheet = d.findViewById(com.google.android.material.R.id.m3_side_sheet);
            if (sheet != null) {
                int screenWidth = requireContext().getResources().getDisplayMetrics().widthPixels;
                int desired = (int) (screenWidth * 0.92f);
                ViewGroup.LayoutParams lp = sheet.getLayoutParams();
                lp.width = desired;
                lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
                sheet.setLayoutParams(lp);
            }
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new SideSheetDialog(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_settings_sheet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Setup toolbar close button
        MaterialToolbar toolbar = view.findViewById(R.id.settingsToolbar);
        if (toolbar != null) {
            toolbar.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.action_close) {
                    dismiss();
                    return true;
                }
                return false;
            });
        }
        
        // Add SettingsFragment to container
        if (savedInstanceState == null) {
            SettingsFragment settingsFragment = new SettingsFragment();
            getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.settings_sheet_container, settingsFragment)
                .commit();
        }
    }
}



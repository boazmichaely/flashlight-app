package com.walklight.safety;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.divider.MaterialDividerItemDecoration;

public class SettingsFragment extends PreferenceFragmentCompat {

    private static final String TAG = "SettingsFragment";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // CRITICAL: Set custom preference store name BEFORE loading XML
        // This ensures UI reads from correct SharedPreferences file
        getPreferenceManager().setSharedPreferencesName("walklight_settings");
        
        // Set preferences from XML resource (now reads from correct store)
        setPreferencesFromResource(R.xml.settings_preferences, rootKey);
        
        // Setup preference change listeners
        setupPreferenceListeners();
    }
    
    private void setupPreferenceListeners() {
        // Set dynamic version number from BuildConfig
        Preference versionPref = findPreference("app_version");
        if (versionPref != null) {
            String versionText = getString(R.string.settings_version_summary, BuildConfig.VERSION_NAME);
            versionPref.setSummary(versionText);
        }
        
        // Setup change listener for switch and verify initial state
        SwitchPreferenceCompat keepLightPref = findPreference("keep_light_on_close");
        if (keepLightPref != null) {
            // Debug: Check initial UI vs stored value alignment
            boolean storedValue = getPreferenceManager().getSharedPreferences().getBoolean("keep_light_on_close", true);
            boolean uiValue = keepLightPref.isChecked();
            Log.d(TAG, "🔍 INITIAL STATE: Stored=" + storedValue + ", UI=" + uiValue + " (should match!)");
            
            keepLightPref.setOnPreferenceChangeListener((preference, newValue) -> {
                Log.d(TAG, "Keep Light setting changed: " + newValue);
                return true; // Allow change
            });
        }
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Settings UI ready
        
        // Apply divider solution from DIVIDERS_RECIPE.md
        // Must be done here when RecyclerView is available
        setupDividers();
    }
    
    private void setupDividers() {
        // 1. Set explicit divider drawable and height
        setDivider(ContextCompat.getDrawable(requireContext(), R.drawable.divider_preference));
        setDividerHeight(1);
        
        // 2. Add MaterialDividerItemDecoration to RecyclerView
        RecyclerView rv = getListView();
        if (rv != null) {
            // Clear existing decorations
            while (rv.getItemDecorationCount() > 0) {
                rv.removeItemDecorationAt(0);
            }
            
            // Add Material divider decoration
            MaterialDividerItemDecoration dec = new MaterialDividerItemDecoration(
                requireContext(), 
                RecyclerView.VERTICAL
            );
            dec.setDividerColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray));
            dec.setDividerThickness(1);
            rv.addItemDecoration(dec);
        }
    }
}

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
        // Set preferences from XML resource
        setPreferencesFromResource(R.xml.settings_preferences, rootKey);
        
        // Set custom preference store name
        getPreferenceManager().setSharedPreferencesName("walklight_settings");
        
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
        
        // Setup change listener for switch (logging only - behavior implementation in B2.2)
        SwitchPreferenceCompat keepLightPref = findPreference("keep_light_on_close");
        if (keepLightPref != null) {
            keepLightPref.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean oldValue = ((SwitchPreferenceCompat) preference).isChecked();
                boolean newValueBool = (Boolean) newValue;
                boolean dataBeforeChange = getPreferenceManager().getSharedPreferences().getBoolean("keep_light_on_close", true);
                
                Log.d(TAG, "🔄 === SWITCH CLICKED ===");
                Log.d(TAG, "🔄 UI BEFORE: " + oldValue);
                Log.d(TAG, "🔄 DATA BEFORE: " + dataBeforeChange);
                Log.d(TAG, "🔄 NEW VALUE: " + newValueBool + " (type: " + newValue.getClass().getSimpleName() + ")");
                
                // Let the change go through first, then check after
                if (getView() != null) {
                    getView().post(() -> {
                        boolean dataAfterChange = getPreferenceManager().getSharedPreferences().getBoolean("keep_light_on_close", true);
                        boolean uiAfterChange = keepLightPref.isChecked();
                        Log.d(TAG, "🔄 UI AFTER: " + uiAfterChange);
                        Log.d(TAG, "🔄 DATA AFTER: " + dataAfterChange);
                        Log.d(TAG, "🔄 === END CLICK ===");
                    });
                }
                
                return true; // Allow change
            });
        }
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Debug: Complete data/UI analysis
        SwitchPreferenceCompat keepLightPref = findPreference("keep_light_on_close");
        if (keepLightPref != null) {
            boolean dataValue = getPreferenceManager().getSharedPreferences().getBoolean("keep_light_on_close", true);
            boolean uiValue = keepLightPref.isChecked();
            boolean xmlDefault = true; // What XML declares
            
            Log.d(TAG, "=== SETTINGS SYNC ANALYSIS ===");
            Log.d(TAG, "XML defaultValue: " + xmlDefault);
            Log.d(TAG, "SharedPreferences DATA: " + dataValue);
            Log.d(TAG, "SwitchPreferenceCompat UI: " + uiValue);
            Log.d(TAG, "Store contains key: " + getPreferenceManager().getSharedPreferences().contains("keep_light_on_close"));
            Log.d(TAG, "Store name: " + getPreferenceManager().getSharedPreferences().toString());
            Log.d(TAG, "============================");
        }
        
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

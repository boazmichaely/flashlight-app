package com.walklight.safety;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.content.pm.PackageManager;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import java.util.List;
import com.google.android.material.button.MaterialButton;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import android.content.ComponentName;
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
    
    // B2.3.2: App picker launcher for interactive testing
    private ActivityResultLauncher<Intent> appPickerLauncher;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // B2.3.2: Initialize app picker launcher
        initializeAppPickerLauncher();
        
        // CRITICAL: Set custom preference store name BEFORE loading XML
        // This ensures UI reads from correct SharedPreferences file
        getPreferenceManager().setSharedPreferencesName("walklight_settings");
        
        // Set preferences from XML resource (now reads from correct store)
        setPreferencesFromResource(R.xml.settings_preferences, rootKey);
        
        // Setup preference change listeners
        setupPreferenceListeners();
    }
    
    /**
     * B2.3.2: Initialize the app picker launcher with result handling
     */
    private void initializeAppPickerLauncher() {
        appPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Log.d("APP_PICKER", "🔍 === APP PICKER RESULT ===");
                Log.d("APP_PICKER", "Result code: " + result.getResultCode());
                
                if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    ComponentName component = data.getComponent();
                    
                    Log.d("APP_PICKER", "✅ SUCCESS: User selected an app!");
                    Log.d("APP_PICKER", "📱 Intent data: " + data.toString());
                    
                    if (component != null) {
                        String packageName = component.getPackageName();
                        String className = component.getClassName();
                        
                        Log.d("APP_PICKER", "✅ COMPONENT DATA:");
                        Log.d("APP_PICKER", "✅ Package: " + packageName);
                        Log.d("APP_PICKER", "✅ Class: " + className);
                        
                        // Get app name for display
                        try {
                            PackageManager pm = requireContext().getPackageManager();
                            ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
                            String appName = pm.getApplicationLabel(appInfo).toString();
                            
                            Log.d("APP_PICKER", "✅ App name: " + appName);
                            
                            // Store the selected app info
                            getPreferenceManager().getSharedPreferences().edit()
                                .putString("companion_app_package", packageName)
                                .putString("companion_app_class", className)
                                .putString("companion_app_name", appName)
                                .apply();
                            
                            Log.d("APP_PICKER", "✅ STORED: " + appName + " (" + packageName + ")");
                            Log.d("APP_PICKER", "✅ Class: " + className);
                            
                            // Update the display
                            updateCompanionAppDisplay();
                            
                            Log.d("APP_PICKER", "✅ === SELECTION COMPLETE ===");
                            
                        } catch (Exception e) {
                            Log.e("APP_PICKER", "❌ Error getting app info", e);
                        }
                    } else {
                        Log.w("APP_PICKER", "⚠️ No component data in result");
                    }
                } else {
                    Log.d("APP_PICKER", "❌ User cancelled or no data");
                }
                
                Log.d("APP_PICKER", "🔍 === END APP PICKER RESULT ===");
            }
        );
        
        Log.d("APP_PICKER", "📱 App picker launcher initialized");
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
        
        // B2.3.1: Setup companion app picker
        setupCompanionAppPicker();
    }
    
    /**
     * B2.3.1: Setup companion app picker with Spotify default detection
     */
    private void setupCompanionAppPicker() {
        // Check if companion app is already stored
        String storedPackage = getPreferenceManager().getSharedPreferences().getString("companion_app_package", null);
        
        // Skip auto-detection, use interactive app picker instead
        Log.d("APP_PICKER", "📱 B2.3.2: Interactive app picker ready");
        
        // Update display with stored app info
        updateCompanionAppDisplay();
    }
    
    // NOTE: Old auto-detection method removed - now using interactive app picker
    
    /**
     * Update companion app preference display with stored app info
     */
    private void updateCompanionAppDisplay() {
        Preference companionPref = findPreference("companion_app_display");
        if (companionPref == null) return;
        
        String storedPackage = getPreferenceManager().getSharedPreferences().getString("companion_app_package", null);
        String storedClass = getPreferenceManager().getSharedPreferences().getString("companion_app_class", null);
        String storedName = getPreferenceManager().getSharedPreferences().getString("companion_app_name", "No app selected");
        
        if (storedPackage != null) {
            try {
                PackageManager pm = requireContext().getPackageManager();
                ApplicationInfo appInfo = pm.getApplicationInfo(storedPackage, 0);
                Drawable icon = pm.getApplicationIcon(appInfo);
                
                // Update preference with app info
                companionPref.setIcon(icon);
                companionPref.setTitle(storedName);
                
                // Show package and class info for debugging
                String debugInfo = "Package: " + storedPackage;
                if (storedClass != null) {
                    debugInfo += "\nClass: " + storedClass;
                }
                companionPref.setSummary(debugInfo);
                
                Log.d("APP_PICKER", "📱 Display updated: " + storedName);
                Log.d("APP_PICKER", "📱 Package: " + storedPackage);
                if (storedClass != null) {
                    Log.d("APP_PICKER", "📱 Class: " + storedClass);
                }
            } catch (PackageManager.NameNotFoundException e) {
                // App was uninstalled
                companionPref.setTitle("App Not Found");
                companionPref.setSummary("Previously selected app was uninstalled");
                Log.w("APP_PICKER", "❌ Stored app not found: " + storedPackage);
            }
        } else {
            companionPref.setTitle("No Companion App");
            companionPref.setSummary("Use Pick App button to select");
        }
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Settings UI ready
        
        // B2.3.1: Setup companion app picker button
        setupCompanionAppButton();
        
        // Apply divider solution from DIVIDERS_RECIPE.md
        // Must be done here when RecyclerView is available
        setupDividers();
    }
    
    /**
     * B2.3.1: Setup companion app picker button click handler
     */
    private void setupCompanionAppButton() {
        // Find the MaterialButton in the custom widget layout
        MaterialButton pickButton = getView().findViewById(R.id.companion_pick_button);
        if (pickButton != null) {
            pickButton.setOnClickListener(v -> {
                Log.d("APP_PICKER", "🔘 INTERACTIVE TEST: Launching native app picker...");
                launchAppPicker();
            });
            Log.d("APP_PICKER", "📱 Companion app button setup complete");
        } else {
            Log.w("APP_PICKER", "❌ Could not find companion_pick_button");
        }
    }
    
    /**
     * B2.3.2: Launch native Android app picker for interactive testing
     */
    private void launchAppPicker() {
        try {
            Log.d("APP_PICKER", "🚀 Creating native app picker intent...");
            
            // Create intent to show all launchable apps (same as reference code)
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            
            // Create chooser dialog
            Intent chooser = Intent.createChooser(intent, "Select Companion App");
            
            Log.d("APP_PICKER", "🚀 Launching app picker dialog...");
            Log.d("APP_PICKER", "🚀 Intent: " + intent.toString());
            Log.d("APP_PICKER", "🚀 Chooser: " + chooser.toString());
            
            // Launch the picker
            appPickerLauncher.launch(chooser);
            
        } catch (Exception e) {
            Log.e("APP_PICKER", "❌ Error launching app picker", e);
        }
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

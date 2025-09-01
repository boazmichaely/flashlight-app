package com.walklight.safety;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.content.pm.PackageManager;
import android.content.pm.ApplicationInfo;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import java.util.List;
import com.google.android.material.button.MaterialButton;
import android.content.ComponentName;
import com.walklight.safety.AppPicker;
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
    
    // B2.3.2: Use Kotlin AppPicker class
    private AppPicker appPicker;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // B2.3.2: Initialize Kotlin app picker
        initializeAppPicker();
        
        // CRITICAL: Set custom preference store name BEFORE loading XML
        // This ensures UI reads from correct SharedPreferences file
        getPreferenceManager().setSharedPreferencesName("walklight_settings");
        
        // Set preferences from XML resource (now reads from correct store)
        setPreferencesFromResource(R.xml.settings_preferences, rootKey);
        
        // Setup preference change listeners
        setupPreferenceListeners();
    }
    
    /**
     * B2.3.2: Initialize Kotlin AppPicker with callback
     */
    private void initializeAppPicker() {
        appPicker = new AppPicker(this);
        
        // Set callback to handle app selection
        appPicker.setCallback(new AppPicker.AppSelectionCallback() {
            @Override
            public void onAppSelected(String packageName, String className, String appName) {
                Log.d("APP_PICKER", "✅ SUCCESS: App selected via Kotlin picker");
                Log.d("APP_PICKER", "✅ Package: " + packageName);
                Log.d("APP_PICKER", "✅ Class: " + className);
                Log.d("APP_PICKER", "✅ App name: " + appName);
                
                // Update the display
                updateCompanionAppDisplay();
                
                Log.d("APP_PICKER", "✅ === SELECTION COMPLETE ===");
            }
        });
        
        Log.d("APP_PICKER", "📱 Kotlin app picker initialized");
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
            // Try multiple approaches to get app info and icon
            PackageManager pm = requireContext().getPackageManager();
            Drawable icon = null;
            String displayName = storedName;
            
            // Try approach 1: Standard ApplicationInfo
            try {
                ApplicationInfo appInfo = pm.getApplicationInfo(storedPackage, 0);
                icon = pm.getApplicationIcon(appInfo);
                // Also try to get better app name if stored name is just package name
                if (displayName.equals(storedPackage)) {
                    displayName = pm.getApplicationLabel(appInfo).toString();
                }
                Log.d("APP_PICKER", "✅ Got icon and name via ApplicationInfo: " + displayName);
            } catch (Exception e1) {
                Log.d("APP_PICKER", "⚠️ ApplicationInfo failed, trying activity approach...");
                
                // Try approach 2: Activity info for launcher activity
                if (storedClass != null) {
                    try {
                        ComponentName component = new ComponentName(storedPackage, storedClass);
                        ActivityInfo activityInfo = pm.getActivityInfo(component, 0);
                        icon = activityInfo.loadIcon(pm);
                        if (displayName.equals(storedPackage)) {
                            displayName = activityInfo.loadLabel(pm).toString();
                        }
                        Log.d("APP_PICKER", "✅ Got icon and name via ActivityInfo: " + displayName);
                    } catch (Exception e2) {
                        Log.d("APP_PICKER", "⚠️ ActivityInfo failed, trying installed apps search...");
                        
                        // Try approach 3: Search installed apps
                        try {
                            java.util.List<ApplicationInfo> installedApps = pm.getInstalledApplications(0); // Use 0 to get ALL apps
                            Log.d("APP_PICKER", "🔍 Display: Searching " + installedApps.size() + " apps for: " + storedPackage);
                            
                            boolean foundApp = false;
                            for (ApplicationInfo app : installedApps) {
                                if (app.packageName.equals(storedPackage)) {
                                    foundApp = true;
                                    try {
                                        icon = pm.getApplicationIcon(app);
                                        if (displayName.equals(storedPackage)) {
                                            displayName = pm.getApplicationLabel(app).toString();
                                        }
                                        Log.d("APP_PICKER", "✅ Got icon and name via installed apps: " + displayName);
                                        break;
                                    } catch (Exception e) {
                                        Log.w("APP_PICKER", "⚠️ Display: Found app but couldn't get icon/label: " + e.getMessage());
                                    }
                                }
                            }
                            
                            if (!foundApp) {
                                Log.w("APP_PICKER", "⚠️ Display: Package " + storedPackage + " not found in installed apps");
                            }
                        } catch (Exception e3) {
                            Log.w("APP_PICKER", "⚠️ Display: Exception in installed apps search: " + e3.getMessage());
                        }
                    }
                }
            }
            
                            // Update preference with app info (with or without icon)
                if (icon != null) {
                    companionPref.setIcon(icon);
                }
                companionPref.setTitle(displayName);
                companionPref.setSummary("");

                Log.d("APP_PICKER", "📱 Display updated: " + displayName);
                Log.d("APP_PICKER", "📱 Icon available: " + (icon != null));
        } else {
            companionPref.setTitle("No Companion App");
            companionPref.setSummary("Use Pick App button to select");
        }
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Settings UI ready
        
        // B2.5: Initialize defaults first (before UI setup)
        initializeDefaults();
        
        // Update companion app display after initialization
        updateCompanionAppDisplay();
        
        // B2.3.1: Setup companion app buttons
        setupCompanionAppButton();
        
        // Apply divider solution from DIVIDERS_RECIPE.md
        // Must be done here when RecyclerView is available
        setupDividers();
    }
    
    /**
     * B2.3.1: Setup companion app picker button click handler
     */
    private void setupCompanionAppButton() {
        // Find the button directly in the preference widget layout after views are ready
        getView().post(() -> {
            findAndSetupButtonInPreference();
        });
    }
    
    /**
     * Helper method to find and setup both buttons in their separate preference widgets
     */
    private void findAndSetupButtonInPreference() {
        try {
            // Search for both MaterialButtons with their IDs in the view hierarchy
            MaterialButton pickButton = findButtonByIdInViewHierarchy(getView(), R.id.companion_pick_button);
            MaterialButton launchButton = findButtonByIdInViewHierarchy(getView(), R.id.companion_launch_button);
            
            // Setup Pick App button (in companion_app_display preference)
            if (pickButton != null) {
                pickButton.setOnClickListener(v -> {
                    Log.d("APP_PICKER", "🔘 Pick App button clicked: Using Kotlin app picker...");
                    Log.d("APP_PICKER", "🔍 appPicker is null: " + (appPicker == null));
                    if (appPicker != null) {
                        Log.d("APP_PICKER", "🔍 Calling appPicker.openAppPicker()...");
                        appPicker.openAppPicker();
                        Log.d("APP_PICKER", "🔍 appPicker.openAppPicker() call completed");
                    } else {
                        Log.e("APP_PICKER", "❌ appPicker is NULL!");
                    }
                });
                Log.d("APP_PICKER", "✅ Pick App button found and setup complete");
            } else {
                Log.w("APP_PICKER", "❌ Could not find companion_pick_button in view hierarchy");
            }
            
            // Setup Launch button (in companion_app_launch preference)
            if (launchButton != null) {
                launchButton.setOnClickListener(v -> {
                    Log.d("APP_PICKER", "🚀 Launch button clicked");
                    launchCompanionApp();
                });
                Log.d("APP_PICKER", "✅ Launch button found and setup complete");
            } else {
                Log.w("APP_PICKER", "❌ Could not find companion_launch_button in view hierarchy");
            }
            

        } catch (Exception e) {
            Log.e("APP_PICKER", "Error searching for buttons in view hierarchy", e);
        }
    }
    
    /**
     * Recursively search for MaterialButton with the specified ID
     */
    private MaterialButton findButtonByIdInViewHierarchy(View parent, int buttonId) {
        if (parent instanceof MaterialButton) {
            MaterialButton button = (MaterialButton) parent;
            if (button.getId() == buttonId) {
                return button;
            }
        }
        
        if (parent instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) parent;
            for (int i = 0; i < group.getChildCount(); i++) {
                MaterialButton found = findButtonByIdInViewHierarchy(group.getChildAt(i), buttonId);
                if (found != null) {
                    return found;
                }
            }
        }
        
        return null;
    }
    
    /**
     * B2.5: Initialize default companion app (Spotify) if none is set
     */
    private void initializeDefaults() {
        String storedPackage = getPreferenceManager().getSharedPreferences().getString("companion_app_package", null);
        
        if (storedPackage == null) {
            Log.d("APP_PICKER", "🔧 No companion app set - initializing Spotify as default");
            
            // Initialize Spotify as default using the same storage mechanism as app picker
            initializeSpotifyAsDefault();
        } else {
            Log.d("APP_PICKER", "✅ Companion app already set: " + storedPackage);
        }
    }
    
    /**
     * B2.5: Set Spotify as default companion app (same as if user selected it)
     */
    private void initializeSpotifyAsDefault() {
        try {
            String spotifyPackage = "com.spotify.music";
            String spotifyClass = null; // Will be resolved by PackageManager
            String spotifyName = "Spotify";
            
            // Try to get the actual launcher activity for Spotify
            PackageManager pm = requireContext().getPackageManager();
            Intent launcherIntent = new Intent(Intent.ACTION_MAIN);
            launcherIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            launcherIntent.setPackage(spotifyPackage);
            
            android.content.pm.ResolveInfo resolveInfo = pm.resolveActivity(launcherIntent, 0);
            if (resolveInfo != null && resolveInfo.activityInfo != null) {
                spotifyClass = resolveInfo.activityInfo.name;
                Log.d("APP_PICKER", "✅ Found Spotify launcher activity: " + spotifyClass);
                
                // Try to get the actual app name and verify it's installed
                try {
                    android.content.pm.ApplicationInfo appInfo = pm.getApplicationInfo(spotifyPackage, 0);
                    spotifyName = pm.getApplicationLabel(appInfo).toString();
                    Log.d("APP_PICKER", "✅ Verified Spotify installation: " + spotifyName);
                } catch (Exception e) {
                    Log.w("APP_PICKER", "⚠️ Could not verify Spotify installation, using default name");
                }
            } else {
                Log.w("APP_PICKER", "⚠️ Spotify not found, setting placeholder - user can change via picker");
                // Set a placeholder that user can change - app picker will work regardless
                spotifyClass = "com.spotify.music.MainActivity"; // Common fallback
            }
            
            // Store exactly like app picker would
            getPreferenceManager().getSharedPreferences().edit()
                .putString("companion_app_package", spotifyPackage)
                .putString("companion_app_class", spotifyClass)
                .putString("companion_app_name", spotifyName)
                .apply();
                
            Log.d("APP_PICKER", "✅ Spotify initialized as default companion app");
            Log.d("APP_PICKER", "📱 Package: " + spotifyPackage);
            Log.d("APP_PICKER", "📱 Class: " + spotifyClass); 
            Log.d("APP_PICKER", "📱 Name: " + spotifyName);
            
        } catch (Exception e) {
            Log.e("APP_PICKER", "❌ Failed to initialize Spotify as default", e);
            // Don't crash - user can still use app picker to select any app
        }
    }
    
    /**
     * Launch the stored companion app
     */
    private void launchCompanionApp() {
        String storedPackage = getPreferenceManager().getSharedPreferences().getString("companion_app_package", null);
        String storedClass = getPreferenceManager().getSharedPreferences().getString("companion_app_class", null);
        
        if (storedPackage != null && storedClass != null) {
            try {
                Log.d("APP_PICKER", "🚀 Launching companion app: " + storedPackage + "/" + storedClass);
                
                Intent launchIntent = new Intent();
                launchIntent.setClassName(storedPackage, storedClass);
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                
                startActivity(launchIntent);
                Log.d("APP_PICKER", "✅ Launch successful");
                
            } catch (Exception e) {
                Log.e("APP_PICKER", "❌ Failed to launch companion app: " + e.getMessage());
                // Show a toast to inform user of failure
                android.widget.Toast.makeText(getContext(), "Failed to launch app", android.widget.Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.d("APP_PICKER", "⚠️ No companion app selected to launch");
            // Show a toast to inform user no app is selected
            android.widget.Toast.makeText(getContext(), "No companion app selected", android.widget.Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Public method for SettingsActivity to trigger reset dialog
     */
    public void triggerResetDialog() {
        Log.d("APP_PICKER", "🔄 Reset triggered from activity");
        showResetConfirmationDialog();
    }
    
    /**
     * B2.6: Show confirmation dialog for reset to defaults
     */
    private void showResetConfirmationDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Reset Settings")
            .setMessage("Are you sure? This will restore both settings to their defaults:\n\n• Keep Light On: ON\n• Companion App: Spotify")
            .setPositiveButton("Yes", (dialog, which) -> {
                Log.d("APP_PICKER", "✅ User confirmed reset");
                resetToDefaults();
            })
            .setNegativeButton("Cancel", (dialog, which) -> {
                Log.d("APP_PICKER", "❌ User cancelled reset");
                dialog.dismiss();
            })
            .show();
    }
    
    /**
     * B2.6: Reset both settings to their defaults
     */
    private void resetToDefaults() {
        try {
            Log.d("APP_PICKER", "🔄 Starting reset to defaults...");
            
            // Reset Keep Light On switch to true (default)
            androidx.preference.SwitchPreferenceCompat keepLightPref = 
                findPreference("keep_light_on_close");
            if (keepLightPref != null) {
                keepLightPref.setChecked(true);
                Log.d("APP_PICKER", "✅ Keep Light On reset to: true");
            }
            
            // Reset companion app to Spotify (clear current settings first)
            getPreferenceManager().getSharedPreferences().edit()
                .remove("companion_app_package")
                .remove("companion_app_class") 
                .remove("companion_app_name")
                .apply();
            Log.d("APP_PICKER", "✅ Companion app settings cleared");
            
            // Reinitialize Spotify as default
            initializeSpotifyAsDefault();
            
            // Update UI to reflect changes
            updateCompanionAppDisplay();
            
            // Show confirmation to user
            android.widget.Toast.makeText(getContext(), "Settings reset to default", android.widget.Toast.LENGTH_SHORT).show();
            
            Log.d("APP_PICKER", "✅ Reset to defaults complete");
            
        } catch (Exception e) {
            Log.e("APP_PICKER", "❌ Error during reset", e);
            android.widget.Toast.makeText(getContext(), "Reset failed", android.widget.Toast.LENGTH_SHORT).show();
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

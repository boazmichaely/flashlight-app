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
        
        // B2.3.1: Setup companion app picker
        setupCompanionAppPicker();
    }
    
    /**
     * B2.3.1: Setup companion app picker with Spotify default detection
     */
    private void setupCompanionAppPicker() {
        // Check if companion app is already stored
        String storedPackage = getPreferenceManager().getSharedPreferences().getString("companion_app_package", null);
        
        if (storedPackage == null) {
            // First run: Search for Spotify using URI scheme (same as launch code)
            Log.d(TAG, "🔍 B2.3.1: First run - detecting Spotify via URI scheme...");
            detectAndStoreSpotify();
        } else {
            Log.d(TAG, "📱 B2.3.1: Companion app already stored: " + storedPackage);
        }
        
        // Update display with stored app info
        updateCompanionAppDisplay();
    }
    
    /**
     * Detect Spotify app using URI scheme (same method as existing launch code)
     * SAFE: Only changes detection, keeps all existing launch functionality intact
     */
    private void detectAndStoreSpotify() {
        try {
            PackageManager pm = requireContext().getPackageManager();
            
            Log.d(TAG, "🔍 B2.3.1: === COMPREHENSIVE SPOTIFY DETECTION DEBUG ===");
            
            // 1. Test the spotify: URI (same as existing launch code)
            Intent testIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("spotify:"));
            List<ResolveInfo> handlers = pm.queryIntentActivities(testIntent, 0);
            
            Log.d(TAG, "🔍 B2.3.1: Testing spotify: URI...");
            Log.d(TAG, "🔍 Found " + handlers.size() + " handler(s) for spotify: URI");
            
            // 2. Log all handlers found (if any)
            for (int i = 0; i < handlers.size(); i++) {
                ResolveInfo handler = handlers.get(i);
                String packageName = handler.activityInfo.packageName;
                String appName = handler.loadLabel(pm).toString();
                Log.d(TAG, "🔍 Handler " + i + ": " + appName + " (" + packageName + ")");
                Log.d(TAG, "🔍 Activity: " + handler.activityInfo.name);
            }
            
            // 3. Search for Spotify-like apps in installed applications
            Log.d(TAG, "🔍 B2.3.1: Searching all installed apps for Spotify...");
            List<ApplicationInfo> allApps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
            int spotifyLikeApps = 0;
            
            for (ApplicationInfo app : allApps) {
                try {
                    String appName = pm.getApplicationLabel(app).toString().toLowerCase();
                    String packageName = app.packageName.toLowerCase();
                    
                    // Look for Spotify-related apps
                    if (appName.contains("spotify") || packageName.contains("spotify")) {
                        String displayName = pm.getApplicationLabel(app).toString();
                        Log.d(TAG, "🎵 FOUND SPOTIFY-LIKE APP: " + displayName + " (" + app.packageName + ")");
                        spotifyLikeApps++;
                        
                        // Test if this specific app can handle spotify: URI
                        Intent specificTest = pm.getLaunchIntentForPackage(app.packageName);
                        if (specificTest != null) {
                            Log.d(TAG, "🎵 " + displayName + " has launch intent: YES");
                        } else {
                            Log.d(TAG, "🎵 " + displayName + " has launch intent: NO");
                        }
                    }
                } catch (Exception e) {
                    // Skip problematic apps
                }
            }
            
            Log.d(TAG, "🔍 B2.3.1: Found " + spotifyLikeApps + " Spotify-like apps in total");
            
            // 4. Test alternative URI schemes
            String[] testUris = {
                "spotify:",
                "spotify://",
                "https://open.spotify.com",
                "market://details?id=com.spotify.music"
            };
            
            for (String uriStr : testUris) {
                Intent altTest = new Intent(Intent.ACTION_VIEW, Uri.parse(uriStr));
                List<ResolveInfo> altHandlers = pm.queryIntentActivities(altTest, 0);
                Log.d(TAG, "🔍 URI '" + uriStr + "' has " + altHandlers.size() + " handler(s)");
            }
            
            // 5. Use the original spotify: URI result
            if (!handlers.isEmpty()) {
                // Found app that can handle Spotify URI!
                ResolveInfo spotifyHandler = handlers.get(0);
                String packageName = spotifyHandler.activityInfo.packageName;
                String appName = spotifyHandler.loadLabel(pm).toString();
                
                // Store the detected app
                getPreferenceManager().getSharedPreferences().edit()
                    .putString("companion_app_package", packageName)
                    .putString("companion_app_name", appName)
                    .apply();
                
                Log.d(TAG, "✅ B2.3.1: STORED Spotify handler: " + appName + " (" + packageName + ")");
            } else {
                Log.d(TAG, "❌ B2.3.1: No handler for spotify: URI found");
                Log.d(TAG, "❌ B2.3.1: Either Spotify not installed OR URI scheme not registered");
            }
            
            Log.d(TAG, "🔍 B2.3.1: === END SPOTIFY DETECTION DEBUG ===");
            
        } catch (Exception e) {
            Log.e(TAG, "Error in comprehensive Spotify detection", e);
        }
    }
    
    /**
     * Update companion app preference display with stored app info
     */
    private void updateCompanionAppDisplay() {
        Preference companionPref = findPreference("companion_app_display");
        if (companionPref == null) return;
        
        String storedPackage = getPreferenceManager().getSharedPreferences().getString("companion_app_package", null);
        String storedName = getPreferenceManager().getSharedPreferences().getString("companion_app_name", "No app selected");
        
        if (storedPackage != null) {
            try {
                PackageManager pm = requireContext().getPackageManager();
                ApplicationInfo appInfo = pm.getApplicationInfo(storedPackage, 0);
                Drawable icon = pm.getApplicationIcon(appInfo);
                
                // Update preference with app info
                companionPref.setIcon(icon);
                companionPref.setTitle(storedName);
                companionPref.setSummary("Package: " + storedPackage); // Debug info - will remove later
                
                Log.d(TAG, "📱 B2.3.1: Updated display for " + storedName + " (" + storedPackage + ")");
            } catch (PackageManager.NameNotFoundException e) {
                // App was uninstalled
                companionPref.setTitle("App Not Found");
                companionPref.setSummary("Previously selected app was uninstalled");
                Log.w(TAG, "Stored app not found: " + storedPackage);
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
                Log.d(TAG, "🔘 B2.3.1: Pick App button clicked - B2.3.2 will implement chooser");
                // B2.3.2 will implement the actual app chooser here
            });
            Log.d(TAG, "📱 B2.3.1: Companion app button setup complete");
        } else {
            Log.w(TAG, "❌ B2.3.1: Could not find companion_pick_button");
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

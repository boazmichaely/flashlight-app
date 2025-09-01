package com.walklight.safety;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/**
 * Extracted split-screen behavior (no functional change from POC logic).
 */
public class SplitScreenController {
    private static final String TAG = "SplitScreen";
    private final Context context;

    public SplitScreenController(Context context) {
        this.context = context;
    }

    public void enterSplitScreen(boolean alreadyInMultiWindow, Runnable onBringBackFull) {
        Log.d(TAG, "Attempting to launch Spotify...");
        Log.d(TAG, "Currently in multi-window mode: " + alreadyInMultiWindow);

        if (!alreadyInMultiWindow) {
            Log.d(TAG, "Not in split-screen - forcing split-screen then launching Spotify");
            new Handler(Looper.getMainLooper()).postDelayed(this::launchSpotifyInSplitScreen, 200);
        } else {
            Log.d(TAG, "Already in split-screen - launching Spotify in adjacent window");
            launchSpotifyInSplitScreen();
        }
    }

    public void exitSplitScreen(Runnable moveTaskToBack, Runnable bringToFront) {
        Log.d(TAG, "=== EXITING SPLIT-SCREEN MODE ===");
        Log.d(TAG, "Task Manipulation: Moving to background then bringing to front");
        moveTaskToBack.run();
        new Handler(Looper.getMainLooper()).postDelayed(bringToFront, 150);
    }

    public void launchSpotifyInSplitScreen() {
        try {
            Intent spotifyIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("spotify:"));
            spotifyIntent.addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT | Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(spotifyIntent);
            Log.d(TAG, "Spotify launched in adjacent window");
        } catch (Exception e) {
            Log.d(TAG, "Spotify app not available, opening web in split-screen");
            Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://open.spotify.com"));
            webIntent.addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT | Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(webIntent);
        }
    }

    /**
     * B3: Launch Companion App in Split-Screen (Dynamic App System)
     * 
     * Launches the user's selected companion app in split-screen mode.
     * Falls back to Spotify if no companion app is configured.
     */
    public void launchCompanionAppInSplitScreen() {
        SharedPreferences prefs = context.getSharedPreferences("walklight_settings", Context.MODE_PRIVATE);
        String packageName = prefs.getString("companion_app_package", null);
        String className = prefs.getString("companion_app_class", null);
        String appName = prefs.getString("companion_app_name", "Unknown App");

        Log.d(TAG, "=== LAUNCHING COMPANION APP IN SPLIT-SCREEN ===");
        Log.d(TAG, "Stored package: " + packageName);
        Log.d(TAG, "Stored class: " + className);
        Log.d(TAG, "App name: " + appName);

        if (packageName != null && className != null) {
            // Launch the selected companion app
            try {
                Intent companionIntent = new Intent();
                companionIntent.setClassName(packageName, className);
                companionIntent.addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT | Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(companionIntent);
                Log.d(TAG, "✅ Companion app '" + appName + "' launched in split-screen");
            } catch (Exception e) {
                Log.e(TAG, "❌ Failed to launch companion app '" + appName + "', falling back to Spotify", e);
                // Fall back to Spotify
                launchSpotifyInSplitScreen();
            }
        } else {
            // No companion app configured, use Spotify as fallback
            Log.d(TAG, "⚠️ No companion app configured, falling back to Spotify");
            launchSpotifyInSplitScreen();
        }
    }
}



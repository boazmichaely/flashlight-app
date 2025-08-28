package com.walklight.safety;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * Exit policy: encapsulates what happens when the user exits the app.
 * B1: Respect the "keep light on when closed" preference (default: ON).
 */
public class ExitPolicy {

    private static final String PREFS = "walklight_settings";
    private static final String KEY_KEEP_ON = "keep_light_on_close";

    private boolean readKeepOn(MainActivity activity) {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_KEEP_ON, true); // default ON until Settings exists
    }

    public void onExitRequested(MainActivity activity) {
        boolean keepOn = readKeepOn(activity);
        Log.d("ExitPolicy", "onExitRequested: keep_on=" + keepOn + ", torch_on=" + activity.isFlashlightCurrentlyOn());

        if (activity.isFlashlightCurrentlyOn() && !keepOn) {
            activity.turnOffFlashlightSafely();
            Log.d("ExitPolicy", "Torch turned OFF per setting");
        } else if (activity.isFlashlightCurrentlyOn()) {
            Log.d("ExitPolicy", "Torch kept ON per setting");
        }

        activity.finishAndRemoveTask();
    }
}



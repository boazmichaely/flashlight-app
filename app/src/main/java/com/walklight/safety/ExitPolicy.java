package com.walklight.safety;

import android.hardware.camera2.CameraAccessException;

/**
 * Exit policy (skeleton): encapsulates what happens when the user exits the app.
 * This step preserves current behavior (turn torch off before exit, if on).
 */
public class ExitPolicy {

    public void onExitRequested(MainActivity activity) {
        if (activity.isFlashlightCurrentlyOn()) {
            activity.turnOffFlashlightSafely();
        }
        activity.finishAndRemoveTask();
    }
}



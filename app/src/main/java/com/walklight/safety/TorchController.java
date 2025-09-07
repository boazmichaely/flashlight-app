package com.walklight.safety;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.util.Log;

/**
 * Minimal, stateless torch helper. No behavior change.
 */
public final class TorchController {
    private static final String DEBUG_TAG = "WalklightD3";
    private TorchController() {}

    public static void setOn(CameraManager cameraManager, String cameraId) throws CameraAccessException {
        Log.d(DEBUG_TAG, "--> Entering TorchController.setOn()");
        cameraManager.setTorchMode(cameraId, true);
        Log.d(DEBUG_TAG, "<-- Exiting TorchController.setOn()");
    }

    public static void setOff(CameraManager cameraManager, String cameraId) throws CameraAccessException {
        Log.d(DEBUG_TAG, "--> Entering TorchController.setOff()");
        cameraManager.setTorchMode(cameraId, false);
        Log.d(DEBUG_TAG, "<-- Exiting TorchController.setOff()");
    }

    public static void setStrength(CameraManager cameraManager, String cameraId, int strengthLevel)
            throws CameraAccessException {
        Log.d(DEBUG_TAG, "--> Entering TorchController.setStrength(strengthLevel=" + strengthLevel + ")");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            cameraManager.turnOnTorchWithStrengthLevel(cameraId, strengthLevel);
        }
        Log.d(DEBUG_TAG, "<-- Exiting TorchController.setStrength()");
    }
}



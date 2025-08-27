package com.walklight.safety;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Build;

/**
 * Minimal, stateless torch helper. No behavior change.
 */
public final class TorchController {
    private TorchController() {}

    public static void setOn(CameraManager cameraManager, String cameraId) throws CameraAccessException {
        cameraManager.setTorchMode(cameraId, true);
    }

    public static void setOff(CameraManager cameraManager, String cameraId) throws CameraAccessException {
        cameraManager.setTorchMode(cameraId, false);
    }

    public static void setStrength(CameraManager cameraManager, String cameraId, int strengthLevel)
            throws CameraAccessException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            cameraManager.turnOnTorchWithStrengthLevel(cameraId, strengthLevel);
        }
    }
}



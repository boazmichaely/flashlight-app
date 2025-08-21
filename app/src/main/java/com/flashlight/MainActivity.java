package com.flashlight;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.Slider;

public class MainActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST = 100;
    
    private CameraManager cameraManager;
    private String cameraId;
    private boolean isFlashlightOn = false;
    private boolean hasFlash = false;
    
    private MaterialButton flashlightToggle;
    private Slider intensitySlider;
    private View colorRectangle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        initializeCamera();
        setupClickListeners();
    }

    private void initializeViews() {
        flashlightToggle = findViewById(R.id.flashlightToggle);
        intensitySlider = findViewById(R.id.intensitySlider);
        colorRectangle = findViewById(R.id.colorRectangle);
    }

    private void initializeCamera() {
        // Check if device has flash
        hasFlash = getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
        
        if (!hasFlash) {
            showToast("Device doesn't have flash!");
            flashlightToggle.setEnabled(false);
            return;
        }

        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            cameraId = cameraManager.getCameraIdList()[0];
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void setupClickListeners() {
        flashlightToggle.setOnClickListener(v -> {
            if (checkCameraPermission()) {
                toggleFlashlight();
            } else {
                requestCameraPermission();
            }
        });

        intensitySlider.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser && isFlashlightOn) {
                updateFlashlightIntensity(value);
            }
            updateColorRectangleBrightness(value);
        });
    }

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            showToast(getString(R.string.permission_camera_rationale));
        }
        ActivityCompat.requestPermissions(this, 
                new String[]{Manifest.permission.CAMERA}, 
                CAMERA_PERMISSION_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                         @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                toggleFlashlight();
            } else {
                showToast(getString(R.string.permission_denied));
            }
        }
    }

    private void toggleFlashlight() {
        if (!hasFlash) return;

        try {
            if (isFlashlightOn) {
                turnOffFlashlight();
            } else {
                turnOnFlashlight();
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
            showToast("Error controlling flashlight");
        }
    }

    private void turnOnFlashlight() throws CameraAccessException {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ supports intensity control
            float intensity = intensitySlider.getValue();
            cameraManager.turnOnTorchWithStrengthLevel(cameraId, (int)(intensity * 100));
        } else {
            // Fallback for older devices
            cameraManager.setTorchMode(cameraId, true);
        }
        
        isFlashlightOn = true;
        flashlightToggle.setText(getString(R.string.turn_off_flashlight));
        updateColorRectangleBrightness(intensitySlider.getValue());
    }

    private void turnOffFlashlight() throws CameraAccessException {
        cameraManager.setTorchMode(cameraId, false);
        isFlashlightOn = false;
        flashlightToggle.setText(getString(R.string.turn_on_flashlight));
        colorRectangle.setBackgroundColor(Color.BLACK);
    }

    private void updateFlashlightIntensity(float intensity) {
        if (!isFlashlightOn) return;
        
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                cameraManager.turnOnTorchWithStrengthLevel(cameraId, (int)(intensity * 100));
            }
            // For older devices, intensity control isn't available
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void updateColorRectangleBrightness(float intensity) {
        if (isFlashlightOn) {
            // Calculate brightness based on intensity (0.1 to 1.0)
            int brightness = (int)(255 * intensity);
            int color = Color.rgb(brightness, brightness, brightness);
            colorRectangle.setBackgroundColor(color);
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isFlashlightOn) {
            try {
                turnOffFlashlight();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isFlashlightOn) {
            try {
                turnOffFlashlight();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }
}

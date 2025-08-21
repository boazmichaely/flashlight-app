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
import com.google.android.material.switchmaterial.SwitchMaterial;

public class MainActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST = 100;
    
    private CameraManager cameraManager;
    private String cameraId;
    private boolean isFlashlightOn = false;
    private boolean hasFlash = false;
    private int maxTorchStrength = 100; // Default, will be updated from device capabilities
    
    private MaterialButton flashlightToggle;
    private Slider ledIntensitySlider;
    private Slider screenBrightnessSlider;
    private SwitchMaterial syncSwitch;
    private View colorRectangle;
    
    private boolean isUpdatingSliders = false; // Prevent infinite loops during sync

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
        ledIntensitySlider = findViewById(R.id.ledIntensitySlider);
        screenBrightnessSlider = findViewById(R.id.screenBrightnessSlider);
        syncSwitch = findViewById(R.id.syncSwitch);
        colorRectangle = findViewById(R.id.colorRectangle);
        
        // Show initial screen brightness based on screen slider default value
        updateColorRectangleBrightness(screenBrightnessSlider.getValue());
    }

    private void initializeCamera() {
        // Check if device has flash
        hasFlash = getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
        
        if (!hasFlash) {
            showToast("Device doesn't have flash!");
            flashlightToggle.setEnabled(false);
            return;
        }

        try {
            cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            if (cameraManager != null) {
                String[] cameraIdList = cameraManager.getCameraIdList();
                if (cameraIdList.length > 0) {
                    cameraId = cameraIdList[0];
                    
                    // Check if device supports torch strength levels
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        try {
                            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
                            Integer maxTorchStrengthObj = characteristics.get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL);
                            Integer defaultTorchStrength = characteristics.get(CameraCharacteristics.FLASH_INFO_STRENGTH_DEFAULT_LEVEL);
                            
                            if (maxTorchStrengthObj != null) {
                                maxTorchStrength = maxTorchStrengthObj;
                                // Device supports intensity control - no need to show diagnostic message
                            }
                        } catch (Exception strengthCheckException) {
                            // Torch strength check failed - silently fall back to basic flashlight
                        }
                    }
                } else {
                    showToast("No camera found!");
                    flashlightToggle.setEnabled(false);
                    hasFlash = false;
                }
            } else {
                showToast("Camera service not available!");
                flashlightToggle.setEnabled(false);
                hasFlash = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            showToast("Error initializing camera: " + e.getMessage());
            flashlightToggle.setEnabled(false);
            hasFlash = false;
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

        // LED Intensity Slider
        ledIntensitySlider.addOnChangeListener((slider, value, fromUser) -> {
            if (isUpdatingSliders) return; // Prevent callback loops
            
            try {
                if (fromUser && isFlashlightOn) {
                    updateFlashlightIntensity(value);
                }
                
                // If sync is enabled, update screen brightness AND move screen slider
                if (fromUser && syncSwitch != null && syncSwitch.isChecked()) {
                    updateColorRectangleBrightness(value);
                    // Safely move screen slider to match
                    isUpdatingSliders = true;
                    screenBrightnessSlider.setValue(value);
                    isUpdatingSliders = false;
                }
            } catch (Exception e) {
                e.printStackTrace();
                isUpdatingSliders = false; // Reset flag on error
            }
        });

        // Screen Brightness Slider  
        screenBrightnessSlider.addOnChangeListener((slider, value, fromUser) -> {
            if (isUpdatingSliders) return; // Prevent callback loops
            
            try {
                updateColorRectangleBrightness(value);
                
                // If sync is enabled, update LED intensity AND move LED slider
                if (fromUser && syncSwitch != null && syncSwitch.isChecked()) {
                    if (isFlashlightOn) {
                        updateFlashlightIntensity(value);
                    }
                    // Safely move LED slider to match
                    isUpdatingSliders = true;
                    ledIntensitySlider.setValue(value);
                    isUpdatingSliders = false;
                }
            } catch (Exception e) {
                e.printStackTrace();
                isUpdatingSliders = false; // Reset flag on error
            }
        });

        // Sync Switch - keep it simple
        syncSwitch.setOnCheckedChangeListener((button, isChecked) -> {
            // Just enable/disable sync mode - don't force values to match immediately
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
        boolean torchSuccess = false;
        
        try {
            // Try basic torch mode first - this is more reliable
            cameraManager.setTorchMode(cameraId, true);
            torchSuccess = true;
            
            // If basic torch worked, try to apply intensity (if supported)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                try {
                    float intensity = ledIntensitySlider.getValue();
                    // Use the actual device's max strength level (Samsung devices often use 1-99, not 1-100)
                    int strengthLevel = Math.max(1, Math.min(maxTorchStrength, Math.round(intensity * maxTorchStrength)));
                    cameraManager.turnOnTorchWithStrengthLevel(cameraId, strengthLevel);
                    // Intensity control is working - no need to show diagnostic message
                } catch (Exception strengthException) {
                    // Log the exact error for debugging
                    android.util.Log.e("FlashlightApp", "Intensity control failed: " + strengthException.getClass().getSimpleName() + ": " + strengthException.getMessage(), strengthException);
                    // Intensity control not supported - silently use basic flashlight (LED still works, screen brightness provides feedback)
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            showToast("Flashlight error: " + e.getMessage());
            return; // Don't update UI if flashlight failed
        }
        
        // Update UI only if basic flashlight succeeded
        if (torchSuccess) {
            isFlashlightOn = true;
            flashlightToggle.setText(getString(R.string.turn_off_flashlight));
            updateColorRectangleBrightness(screenBrightnessSlider.getValue());
        }
    }

    private void turnOffFlashlight() throws CameraAccessException {
        try {
            cameraManager.setTorchMode(cameraId, false);
        } catch (Exception e) {
            // Handle cases where flashlight hardware isn't available (like emulators)
            e.printStackTrace();
        }
        
        // Update UI regardless of hardware success
        isFlashlightOn = false;
        flashlightToggle.setText(getString(R.string.turn_on_flashlight));
        // Maintain current screen brightness setting when torch turns off
        updateColorRectangleBrightness(screenBrightnessSlider.getValue());
    }

    private void updateFlashlightIntensity(float intensity) {
        if (!isFlashlightOn) return;
        
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                // Use device's actual max strength level (e.g., Samsung: 1-99, not 1-100)
                int strengthLevel = Math.max(1, Math.min(maxTorchStrength, Math.round(intensity * maxTorchStrength)));
                cameraManager.turnOnTorchWithStrengthLevel(cameraId, strengthLevel);
            }
            // For older devices or devices that don't support intensity, only screen changes
        } catch (Exception e) {
            // Log slider intensity errors for debugging
            android.util.Log.e("FlashlightApp", "Slider intensity update failed: " + e.getClass().getSimpleName() + ": " + e.getMessage(), e);
        }
    }

    private void updateColorRectangleBrightness(float intensity) {
        try {
            // Always use full brightness scale - screen brightness should match slider position consistently
            int brightness = (int)(255 * intensity);
            int color = Color.rgb(brightness, brightness, brightness);
            colorRectangle.setBackgroundColor(color);
        } catch (Exception e) {
            e.printStackTrace();
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

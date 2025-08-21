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
import android.widget.LinearLayout;
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
    private Slider syncedIntensitySlider;
    private Slider screenOnlySlider;
    private SwitchMaterial syncSwitch;
    private View colorRectangle;
    private LinearLayout syncModeContainer;
    private LinearLayout independentModeContainer;
    private LinearLayout screenOnlyModeContainer;
    
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
        syncedIntensitySlider = findViewById(R.id.syncedIntensitySlider);
        screenOnlySlider = findViewById(R.id.screenOnlySlider);
        syncSwitch = findViewById(R.id.syncSwitch);
        colorRectangle = findViewById(R.id.colorRectangle);
        syncModeContainer = findViewById(R.id.syncModeContainer);
        independentModeContainer = findViewById(R.id.independentModeContainer);
        screenOnlyModeContainer = findViewById(R.id.screenOnlyModeContainer);
        
        // Show initial screen brightness (app starts in sync mode)
        updateColorRectangleBrightness(syncedIntensitySlider.getValue());
        
        // Set initial layout mode based on sync switch state
        updateLayoutMode();
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

        // Synced Intensity Slider (for sync mode)
        syncedIntensitySlider.addOnChangeListener((slider, value, fromUser) -> {
            try {
                if (fromUser && !isUpdatingSliders) {
                    updateFlashlightIntensity(value);
                    updateColorRectangleBrightness(value);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // Screen Only Slider (for screen-only mode)
        screenOnlySlider.addOnChangeListener((slider, value, fromUser) -> {
            try {
                if (fromUser && !isUpdatingSliders) {
                    updateColorRectangleBrightness(value);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // Sync Switch - toggle between layouts and sync values
        syncSwitch.setOnCheckedChangeListener((button, isChecked) -> {
            updateLayoutMode(); // Switch between sync/independent layouts
            if (isChecked) {
                // Entering sync mode - sync both sliders to the current synced slider value
                syncSlidersToSyncedValue();
            } else {
                // Leaving sync mode - set synced slider to match current LED value
                syncSyncedSliderToCurrentValues();
            }
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
                    float intensity = getCurrentLedIntensity();
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
            float currentScreenBrightness = getCurrentScreenBrightness();
            updateColorRectangleBrightness(currentScreenBrightness);
            updateLayoutMode(); // Update layout based on new flashlight state
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
        float currentScreenBrightness = getCurrentScreenBrightness();
        updateColorRectangleBrightness(currentScreenBrightness);
        updateLayoutMode(); // Update layout based on new flashlight state
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

    private void updateLayoutMode() {
        try {
            if (syncSwitch != null && syncModeContainer != null && independentModeContainer != null && screenOnlyModeContainer != null) {
                boolean isSyncMode = syncSwitch.isChecked();
                boolean isFlashlightOn = this.isFlashlightOn;
                
                // Debug logging
                android.util.Log.d("FlashlightApp", "updateLayoutMode: sync=" + isSyncMode + ", flashlight=" + isFlashlightOn);
                
                // Three-state layout logic:
                // 1. Sync ON -> Single full-width "Intensity" slider
                // 2. Sync OFF + Flashlight ON -> Dual sliders (35% LED + 65% Screen) 
                // 3. Sync OFF + Flashlight OFF -> Single full-width "Screen" slider (hide inactive LED)
                
                if (isSyncMode) {
                    // SYNC MODE: Single full-width slider
                    android.util.Log.d("FlashlightApp", "Setting SYNC mode");
                    syncModeContainer.setVisibility(View.VISIBLE);
                    independentModeContainer.setVisibility(View.GONE);
                    screenOnlyModeContainer.setVisibility(View.GONE);
                } else if (isFlashlightOn) {
                    // INDEPENDENT MODE: Dual sliders (flashlight is on, so LED is functional)
                    android.util.Log.d("FlashlightApp", "Setting INDEPENDENT mode (dual sliders)");
                    syncModeContainer.setVisibility(View.GONE);
                    independentModeContainer.setVisibility(View.VISIBLE);
                    screenOnlyModeContainer.setVisibility(View.GONE);
                } else {
                    // SCREEN ONLY MODE: Hide inactive LED, show full-width screen slider
                    android.util.Log.d("FlashlightApp", "Setting SCREEN ONLY mode");
                    syncModeContainer.setVisibility(View.GONE);
                    independentModeContainer.setVisibility(View.GONE);
                    screenOnlyModeContainer.setVisibility(View.VISIBLE);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e("FlashlightApp", "Error in updateLayoutMode", e);
        }
    }

    private void syncSlidersToSyncedValue() {
        try {
            if (syncedIntensitySlider != null && ledIntensitySlider != null && screenBrightnessSlider != null && screenOnlySlider != null) {
                isUpdatingSliders = true;
                float syncedValue = syncedIntensitySlider.getValue();
                ledIntensitySlider.setValue(syncedValue);
                screenBrightnessSlider.setValue(syncedValue);
                screenOnlySlider.setValue(syncedValue);
                isUpdatingSliders = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            isUpdatingSliders = false;
        }
    }

    private void syncSyncedSliderToCurrentValues() {
        try {
            if (syncedIntensitySlider != null) {
                isUpdatingSliders = true;
                // Use current screen brightness as reference when leaving sync mode
                float currentScreenValue = getCurrentScreenBrightness();
                syncedIntensitySlider.setValue(currentScreenValue);
                isUpdatingSliders = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            isUpdatingSliders = false;
        }
    }

    private float getCurrentLedIntensity() {
        try {
            if (syncSwitch != null && syncSwitch.isChecked()) {
                // Sync mode - use synced slider
                return syncedIntensitySlider != null ? syncedIntensitySlider.getValue() : 1.0f;
            } else {
                // Independent mode - use LED slider
                return ledIntensitySlider != null ? ledIntensitySlider.getValue() : 1.0f;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return 1.0f; // Safe default
        }
    }

    private float getCurrentScreenBrightness() {
        try {
            if (syncSwitch != null && syncSwitch.isChecked()) {
                // Sync mode - use synced slider
                return syncedIntensitySlider != null ? syncedIntensitySlider.getValue() : 1.0f;
            } else if (isFlashlightOn) {
                // Independent mode (flashlight on) - use screen brightness slider
                return screenBrightnessSlider != null ? screenBrightnessSlider.getValue() : 1.0f;
            } else {
                // Screen-only mode (flashlight off) - use screen-only slider
                return screenOnlySlider != null ? screenOnlySlider.getValue() : 1.0f;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return 1.0f; // Safe default
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

package com.walklight.safety;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.content.res.ColorStateList;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;
import androidx.appcompat.app.AlertDialog;

public class MainActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST = 100;
    
    private CameraManager cameraManager;
    private String cameraId;
    private boolean isFlashlightOn = false;
    private boolean hasFlash = false;
    private int maxTorchStrength = 100; // Default, will be updated from device capabilities
    
    private SwitchMaterial lightToggle;
    private Slider ledIntensitySlider;
    private Slider screenBrightnessSlider;
    private Slider syncedIntensitySlider;
    private Slider screenOnlySlider;
    private SwitchMaterial syncSwitch;
    private TextView syncLabel;
    private View colorRectangle;
    private LinearLayout syncModeContainer;
    private LinearLayout independentModeContainer;
    private LinearLayout screenOnlyModeContainer;
    private TextView syncedIntensityLabel;
    private LinearLayout aboutButton;
    
    private boolean isUpdatingSliders = false; // Prevent infinite loops during sync
    private boolean wasFlashlightOnBeforePause = false; // Track state for resume
    private boolean lastSyncState = false; // Remember sync state when flashlight is off
    
    // Track actual current values for proper sync initialization
    private float currentActualScreenBrightness = 1.0f;
    private float currentActualLedIntensity = 1.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Handle system insets for proper layout positioning
        setupWindowInsets();

        initializeViews();
        initializeCamera();
        setupClickListeners();
        
        // Auto-turn on flashlight when app launches (AFTER camera initialization)
        // For fresh installs, request camera permission automatically
        if (hasFlash && !checkCameraPermission()) {
            requestCameraPermission();
        } else {
            autoStartFlashlight();
        }
    }
    
    private void setupWindowInsets() {
        View rootView = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            
            // Apply bottom inset to control panel to avoid navigation bar overlap
            LinearLayout controlPanel = findViewById(R.id.controlPanel);
            if (controlPanel != null) {
                ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) controlPanel.getLayoutParams();
                params.bottomMargin = systemBars.bottom;
                controlPanel.setLayoutParams(params);
            }
            
            return insets;
        });
    }
    
    private void autoStartFlashlight() {
        // Only auto-start if we have flash capability and permission
        if (hasFlash && checkCameraPermission()) {
            try {
                turnOnFlashlight();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }

    private void initializeViews() {
        lightToggle = findViewById(R.id.lightToggle);
        ledIntensitySlider = findViewById(R.id.ledIntensitySlider);
        screenBrightnessSlider = findViewById(R.id.screenBrightnessSlider);
        syncedIntensitySlider = findViewById(R.id.syncedIntensitySlider);
        screenOnlySlider = findViewById(R.id.screenOnlySlider);
        syncSwitch = findViewById(R.id.syncSwitch);
        syncLabel = findViewById(R.id.syncLabel);
        colorRectangle = findViewById(R.id.colorRectangle);
        syncModeContainer = findViewById(R.id.syncModeContainer);
        independentModeContainer = findViewById(R.id.independentModeContainer);
        screenOnlyModeContainer = findViewById(R.id.screenOnlyModeContainer);
        syncedIntensityLabel = findViewById(R.id.syncedIntensityLabel);
        aboutButton = findViewById(R.id.aboutButton);
        
        // Initialize all sliders consistently using centralized logic
        initializeAllSlidersToCurrentState();
        
        // Set initial layout mode based on sync switch state
        updateLayoutMode();
        
        // Set initial light toggle appearance (starts OFF)
        updateLightToggleAppearance(false);
        
        // Set initial sync toggle appearance (starts OFF) and hide since flashlight starts OFF
        updateSyncToggleAppearance(false);
        syncSwitch.setVisibility(View.GONE);  // Hidden initially since flashlight starts OFF
        syncLabel.setVisibility(View.GONE);   // Hidden initially since flashlight starts OFF
        
        // Set initial synced intensity label
        updateSyncedIntensityLabel();
    }

    private void initializeCamera() {
        // Check if device has flash
        hasFlash = getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
        
        if (!hasFlash) {
            showToast("Device doesn't have flash!");
            lightToggle.setEnabled(false);
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
                    lightToggle.setEnabled(false);
                    hasFlash = false;
                }
            } else {
                showToast("Camera service not available!");
                lightToggle.setEnabled(false);
                hasFlash = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            showToast("Error initializing camera: " + e.getMessage());
            lightToggle.setEnabled(false);
            hasFlash = false;
        }
    }

    private void setupClickListeners() {
        // Light toggle switch
        lightToggle.setOnCheckedChangeListener((button, isChecked) -> {
            try {
                if (checkCameraPermission()) {
                    if (isChecked) {
                        turnOnFlashlight();
                    } else {
                        turnOffFlashlight();
                    }
                } else {
                    requestCameraPermission();
                    // Reset toggle if permission denied
                    lightToggle.setChecked(false);
                }
            } catch (Exception e) {
                e.printStackTrace();
                showToast("Error controlling flashlight: " + e.getMessage());
                // Reset toggle on error
                lightToggle.setChecked(false);
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
            // Save sync state for when flashlight turns off/on
            lastSyncState = isChecked;
            
            updateSyncToggleAppearance(isChecked);
            updateLayoutMode(); // This now handles all slider synchronization
            updateSyncedIntensityLabel();
        });

        // About Button
        aboutButton.setOnClickListener(v -> showAboutDialog());
    }
    

    

    
    private void initializeAllSlidersToCurrentState() {
        try {
            // Initialize all sliders to match the screen-only slider (the visible one at startup)
            float initialValue = screenOnlySlider != null ? screenOnlySlider.getValue() : 1.0f;
            
            isUpdatingSliders = true;
            if (syncedIntensitySlider != null) {
                syncedIntensitySlider.setValue(initialValue);
            }
            if (ledIntensitySlider != null) {
                ledIntensitySlider.setValue(initialValue);
            }
            if (screenBrightnessSlider != null) {
                screenBrightnessSlider.setValue(initialValue);
            }
            isUpdatingSliders = false;
            
            // Set initial screen brightness and track it
            updateColorRectangleBrightness(initialValue);
            
        } catch (Exception e) {
            e.printStackTrace();
            isUpdatingSliders = false;
        }
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
                showToast("Camera permission granted. You can now control the light.");
                // Auto-start flashlight after permission is granted (for fresh installs)
                autoStartFlashlight();
            } else {
                showToast(getString(R.string.permission_denied));
            }
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
                    float intensity = getCurrentActualLedIntensity();
                    // Use the actual device's max strength level (Samsung devices often use 1-99, not 1-100)
                    int strengthLevel = Math.max(1, Math.min(maxTorchStrength, Math.round(intensity * maxTorchStrength)));
                    cameraManager.turnOnTorchWithStrengthLevel(cameraId, strengthLevel);
                    // Track the intensity that was applied
                    currentActualLedIntensity = intensity;
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
            lightToggle.setChecked(true);
            updateLightToggleAppearance(true); // Yellow track when ON
            // DON'T call getCurrentScreenBrightness() here - it will read wrong slider
            // Keep current screen brightness unchanged when light turns on
            updateLayoutMode(); // Update layout based on new flashlight state
            updateSyncedIntensityLabel(); // Update label based on new flashlight state
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
        lightToggle.setChecked(false);
        updateLightToggleAppearance(false); // Gray track when OFF
        // DON'T call getCurrentScreenBrightness() here - keep current screen unchanged
        updateLayoutMode(); // Update layout based on new flashlight state
        updateSyncedIntensityLabel(); // Update label based on new flashlight state
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
            
            // Track actual current LED intensity
            currentActualLedIntensity = intensity;
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
            
            // Track actual current screen brightness
            currentActualScreenBrightness = intensity;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateLayoutMode() {
        try {
            if (syncSwitch != null && syncModeContainer != null && independentModeContainer != null && screenOnlyModeContainer != null) {
                boolean isFlashlightOn = this.isFlashlightOn;
                float currentBrightness = getCurrentActualScreenBrightness();
                
                if (isFlashlightOn) {
                    // FLASHLIGHT ON: Show sync button + restore last sync state
                    syncSwitch.setVisibility(View.VISIBLE);
                    syncLabel.setVisibility(View.VISIBLE); // Show sync label too
                    
                    // Restore last sync state
                    syncSwitch.setChecked(lastSyncState);
                    boolean isSyncMode = lastSyncState;
                    
                    if (isSyncMode) {
                        // SYNC MODE: Single full-width slider controlling both
                        if (syncedIntensitySlider != null) {
                            isUpdatingSliders = true;
                            syncedIntensitySlider.setValue(currentBrightness);
                            isUpdatingSliders = false;
                        }
                        
                        syncModeContainer.setVisibility(View.VISIBLE);
                        independentModeContainer.setVisibility(View.GONE);
                        screenOnlyModeContainer.setVisibility(View.GONE);
                        
                        syncModeContainer.post(() -> {
                            updateColorRectangleBrightness(currentBrightness);
                            updateFlashlightIntensity(currentBrightness);
                        });
                        
                    } else {
                        // INDEPENDENT MODE: Dual sliders (LED + Screen separate)
                        isUpdatingSliders = true;
                        if (screenBrightnessSlider != null) {
                            screenBrightnessSlider.setValue(currentBrightness);
                        }
                        if (ledIntensitySlider != null) {
                            ledIntensitySlider.setValue(currentActualLedIntensity);
                        }
                        isUpdatingSliders = false;
                        
                        syncModeContainer.setVisibility(View.GONE);
                        independentModeContainer.setVisibility(View.VISIBLE);
                        screenOnlyModeContainer.setVisibility(View.GONE);
                        
                        independentModeContainer.post(() -> {
                            updateColorRectangleBrightness(currentBrightness);
                            updateFlashlightIntensity(currentActualLedIntensity);
                        });
                    }
                    
                } else {
                    // FLASHLIGHT OFF: Hide sync button completely, show single screen slider
                    syncSwitch.setVisibility(View.GONE);
                    syncLabel.setVisibility(View.GONE); // Hide sync label too
                    
                    // SCREEN ONLY MODE: Just screen brightness control
                    if (screenOnlySlider != null) {
                        isUpdatingSliders = true;
                        screenOnlySlider.setValue(currentBrightness);
                        isUpdatingSliders = false;
                    }
                    
                    syncModeContainer.setVisibility(View.GONE);
                    independentModeContainer.setVisibility(View.GONE);
                    screenOnlyModeContainer.setVisibility(View.VISIBLE);
                    
                    screenOnlyModeContainer.post(() -> {
                        updateColorRectangleBrightness(currentBrightness);
                    });
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }





    // SINGLE SOURCE OF TRUTH - always return the actual LED intensity
    private float getCurrentActualLedIntensity() {
        return currentActualLedIntensity;
    }

    // SINGLE SOURCE OF TRUTH - always return the actual displayed screen brightness
    private float getCurrentActualScreenBrightness() {
        return currentActualScreenBrightness;
    }

    private void updateLightToggleAppearance(boolean isLightOn) {
        try {
            if (lightToggle != null) {
                if (isLightOn) {
                    // Light blue track when light is ON - consistent with sync toggle
                    lightToggle.setTrackTintList(ColorStateList.valueOf(getColor(R.color.toggle_on_track)));
                } else {
                    // Gray track when light is OFF
                    lightToggle.setTrackTintList(ColorStateList.valueOf(getColor(R.color.toggle_off_track)));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateSyncToggleAppearance(boolean isSyncOn) {
        try {
            if (syncSwitch != null) {
                if (isSyncOn) {
                    // Light blue track when sync is ON - consistent with light toggle
                    syncSwitch.setTrackTintList(ColorStateList.valueOf(getColor(R.color.toggle_on_track)));
                } else {
                    // Gray track when sync is OFF
                    syncSwitch.setTrackTintList(ColorStateList.valueOf(getColor(R.color.toggle_off_track)));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateSyncedIntensityLabel() {
        try {
            if (syncedIntensityLabel != null && syncSwitch != null) {
                if (syncSwitch.isChecked()) {
                    if (isFlashlightOn) {
                        syncedIntensityLabel.setText(getString(R.string.intensity));
                    } else {
                        syncedIntensityLabel.setText(getString(R.string.screen_brightness));
                    }
                }
            }
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
        // Save current flashlight state
        wasFlashlightOnBeforePause = isFlashlightOn;
        
        // Turn off flashlight when pausing
        if (isFlashlightOn) {
            try {
                turnOffFlashlight();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Restore flashlight state if it was on before pause
        if (wasFlashlightOnBeforePause && hasFlash && checkCameraPermission()) {
            try {
                turnOnFlashlight();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }

    private void showAboutDialog() {
        try {
            String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            String message = getString(R.string.about_version, versionName);

            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.about_title))
                    .setMessage(message)
                    .setPositiveButton("OK", null)
                    .show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

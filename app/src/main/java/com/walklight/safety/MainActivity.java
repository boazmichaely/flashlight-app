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
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;
import androidx.appcompat.app.AlertDialog;

public class MainActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST = 100;
    
    // Configuration now lives in: app/src/main/res/values/exit_button_config.xml
    
    private CameraManager cameraManager;
    private String cameraId;
    private boolean isFlashlightOn = false;
    private boolean hasFlash = false;
    private boolean hasFlashIntensityControl = false; // PHASE 1: Track if device supports variable intensity
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
    private MaterialButton exitButtonTopCorner; // OPTION B: Top corner exit button
    private FloatingActionButton exitButtonFloating; // OPTION C: Floating exit button
    
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

        initializeCamera(); // MUST come before initializeViews() to set hardware capabilities
        initializeViews();
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
        
        // Initialize exit buttons (configuration now in exit_button_config.xml)
        exitButtonTopCorner = findViewById(R.id.exitButtonTopCorner);
        exitButtonFloating = findViewById(R.id.exitButtonFloating);
        
        // Apply show/hide configuration from resources
        if (exitButtonTopCorner != null) {
            boolean showTopCorner = getResources().getBoolean(R.bool.show_top_corner_exit);
            exitButtonTopCorner.setVisibility(showTopCorner ? View.VISIBLE : View.GONE);
            android.util.Log.d("FlashlightApp", "✅ Top corner button found! Show: " + showTopCorner + ", Text: " + exitButtonTopCorner.getText());
        } else {
            android.util.Log.e("FlashlightApp", "❌ Top corner button is NULL!");
        }
        
        if (exitButtonFloating != null) {
            boolean showFloating = getResources().getBoolean(R.bool.show_floating_exit);
            exitButtonFloating.setVisibility(showFloating ? View.VISIBLE : View.GONE);
            android.util.Log.d("FlashlightApp", "✅ Floating button found! Show: " + showFloating);
        } else {
            android.util.Log.e("FlashlightApp", "❌ Floating button is NULL!");
        }
        
        // PHASE 1: TEMPORARILY DISABLED - Apply hardware-based UI configuration BEFORE initializing sliders
        // TODO: Re-enable after testing
        // configureUIBasedOnHardware();
        
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

    // Exit button configuration now handled by resources in exit_button_config.xml

    // ================================
    // PHASE 1: HARDWARE-BASED UI CONFIGURATION
    // ================================
    
    /**
     * PHASE 1: Configure UI elements based on device hardware capabilities
     * Hides LED intensity controls and sync functionality if device doesn't support intensity control
     */
    private void configureUIBasedOnHardware() {
        android.util.Log.d("FlashlightHardware", "🎨 Configuring UI based on hardware capabilities...");
        
        if (!hasFlashIntensityControl) {
            // Device only supports basic on/off flash - hide intensity-related controls
            android.util.Log.d("FlashlightHardware", "⚠️ No intensity control - hiding LED slider and sync controls");
            
            // Hide LED intensity slider (in independent mode)
            if (ledIntensitySlider != null) {
                ledIntensitySlider.setVisibility(View.GONE);
            }
            
            // Hide synced intensity slider (in sync mode)  
            if (syncedIntensitySlider != null) {
                syncedIntensitySlider.setVisibility(View.GONE);
            }
            
            // Hide sync controls completely since there's nothing to sync
            if (syncSwitch != null) {
                syncSwitch.setVisibility(View.GONE);
            }
            if (syncLabel != null) {
                syncLabel.setVisibility(View.GONE);
            }
            
            // Hide sync mode container
            if (syncModeContainer != null) {
                syncModeContainer.setVisibility(View.GONE);
            }
            
            // Hide independent mode container  
            if (independentModeContainer != null) {
                independentModeContainer.setVisibility(View.GONE);
            }
            
            // Ensure screen-only mode is always visible for devices without intensity control
            if (screenOnlyModeContainer != null) {
                screenOnlyModeContainer.setVisibility(View.VISIBLE);
            }
            
            android.util.Log.d("FlashlightHardware", "✅ Simple UI configured: Light ON/OFF + Screen brightness only");
            
        } else {
            // Device supports intensity control - show all controls
            android.util.Log.d("FlashlightHardware", "✅ Full UI configured: All intensity controls available");
        }
    }

    private void initializeCamera() {
        // Check if device has flash
        hasFlash = getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
        
        if (!hasFlash) {
            showToast("Device doesn't have flash!");
            lightToggle.setEnabled(false);
            hasFlashIntensityControl = false;
            return;
        }

        try {
            cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            if (cameraManager != null) {
                String[] cameraIdList = cameraManager.getCameraIdList();
                if (cameraIdList.length > 0) {
                    cameraId = cameraIdList[0];
                    
                    // PHASE 1: Check if device supports torch strength levels
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        try {
                            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
                            Integer maxTorchStrengthObj = characteristics.get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL);
                            Integer defaultTorchStrength = characteristics.get(CameraCharacteristics.FLASH_INFO_STRENGTH_DEFAULT_LEVEL);
                            
                            if (maxTorchStrengthObj != null && maxTorchStrengthObj > 1) {
                                maxTorchStrength = maxTorchStrengthObj;
                                hasFlashIntensityControl = true;
                                android.util.Log.d("FlashlightHardware", "✅ Device supports intensity control - Max strength: " + maxTorchStrength);
                            } else {
                                hasFlashIntensityControl = false;
                                android.util.Log.d("FlashlightHardware", "⚠️ Device has flash but NO intensity control (simple on/off only)");
                            }
                        } catch (Exception strengthCheckException) {
                            // Torch strength check failed - assume no intensity control
                            hasFlashIntensityControl = false;
                            android.util.Log.d("FlashlightHardware", "⚠️ Torch strength check failed - assuming no intensity control");
                        }
                    } else {
                        // Older Android versions don't support intensity control
                        hasFlashIntensityControl = false;
                        android.util.Log.d("FlashlightHardware", "⚠️ Android version < API 33 - no intensity control available");
                    }
                    
                    // Log hardware capabilities summary
                    android.util.Log.d("FlashlightHardware", "🔍 Hardware Detection Summary:");
                    android.util.Log.d("FlashlightHardware", "- Has Flash: " + hasFlash);
                    android.util.Log.d("FlashlightHardware", "- Has Intensity Control: " + hasFlashIntensityControl);
                    android.util.Log.d("FlashlightHardware", "- Max Torch Strength: " + maxTorchStrength);
                } else {
                    showToast("No camera found!");
                    lightToggle.setEnabled(false);
                    hasFlash = false;
                    hasFlashIntensityControl = false;
                }
            } else {
                showToast("Camera service not available!");
                lightToggle.setEnabled(false);
                hasFlash = false;
                hasFlashIntensityControl = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            showToast("Error initializing camera: " + e.getMessage());
            lightToggle.setEnabled(false);
            hasFlash = false;
            hasFlashIntensityControl = false;
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
        
        // Exit Button Click Listeners  
        if (exitButtonTopCorner != null) {
            exitButtonTopCorner.setOnClickListener(v -> exitApp());
            android.util.Log.d("FlashlightApp", "✅ Top corner exit button click listener set");
        }
        
        if (exitButtonFloating != null) {
            exitButtonFloating.setOnClickListener(v -> exitApp());
            android.util.Log.d("FlashlightApp", "✅ Floating exit button click listener set");
        }
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
                
                // PHASE 1: TEMPORARILY DISABLED - For devices without intensity control, always use simple mode
                // TODO: Re-enable after testing that hardware detection works correctly
                if (false && !hasFlashIntensityControl) {
                    // Simple mode: Just light on/off + screen brightness
                    syncModeContainer.setVisibility(View.GONE);
                    independentModeContainer.setVisibility(View.GONE);
                    screenOnlyModeContainer.setVisibility(View.VISIBLE);
                    
                    // Hide sync controls
                    syncSwitch.setVisibility(View.GONE);
                    syncLabel.setVisibility(View.GONE);
                    
                    // Update screen-only slider
                    if (screenOnlySlider != null) {
                        screenOnlySlider.post(() -> {
                            isUpdatingSliders = true;
                            screenOnlySlider.setValue(currentBrightness);
                            isUpdatingSliders = false;
                        });
                    }
                    
                    android.util.Log.d("FlashlightHardware", "📱 Simple layout mode: Light ON/OFF + Screen only");
                    return; // Skip complex sync logic
                }
                
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
        
        // PHASE 2.2: Apply consistent behavior logic - keep light on for ALL pause scenarios
        boolean shouldKeepOn = shouldKeepLightOnDuringPause();
        String reason = getPauseDecisionReason();
        
        android.util.Log.d("FlashlightLifecycle", "=== onPause() PHASE 2.2: CONSISTENT BEHAVIOR ===");
        android.util.Log.d("FlashlightLifecycle", "Multi-window mode: " + isInMultiWindowMode());
        android.util.Log.d("FlashlightLifecycle", "Picture-in-Picture: " + isInPictureInPictureMode());  
        android.util.Log.d("FlashlightLifecycle", "Has window focus: " + hasWindowFocus());
        android.util.Log.d("FlashlightLifecycle", "Current light state: " + isFlashlightOn);
        android.util.Log.d("FlashlightLifecycle", "Decision reason: " + reason);
        android.util.Log.d("FlashlightLifecycle", "🎯 CONSISTENT DECISION: " + (shouldKeepOn ? "KEEP ON" : "TURN OFF"));
        
        // Save current flashlight state
        wasFlashlightOnBeforePause = isFlashlightOn;
        
        // PHASE 2.2: Apply consistent behavior logic to actual behavior
        if (isFlashlightOn) {
            if (shouldKeepOn) {
                // CONSISTENT BEHAVIOR: Keep light on for ALL pause scenarios  
                android.util.Log.d("FlashlightLifecycle", "🌟 Light KEPT ON (consistent behavior)");
                android.util.Log.d("FlashlightLifecycle", "💡 Reason: " + reason);
            } else {
                // FALLBACK: Only turn off light in exceptional cases (should not happen with consistent behavior)
                try {
                    turnOffFlashlight();
                    android.util.Log.d("FlashlightLifecycle", "⚠️ Light turned OFF (fallback - should not happen)");
                } catch (CameraAccessException e) {
                    android.util.Log.e("FlashlightLifecycle", "❌ Error turning off light", e);
                    e.printStackTrace();
                }
            }
        }
        
        android.util.Log.d("FlashlightLifecycle", "=========================");
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        android.util.Log.d("FlashlightLifecycle", "=== onResume() Analysis ===");
        android.util.Log.d("FlashlightLifecycle", "Was light on before pause: " + wasFlashlightOnBeforePause);
        android.util.Log.d("FlashlightLifecycle", "Current light state: " + isFlashlightOn);
        android.util.Log.d("FlashlightLifecycle", "Multi-window mode: " + isInMultiWindowMode());
        android.util.Log.d("FlashlightLifecycle", "Has window focus: " + hasWindowFocus());
        
        // PHASE 2.1: Smart restore logic (with multi-window transition handling)
        if (wasFlashlightOnBeforePause && hasFlash && checkCameraPermission()) {
            if (isFlashlightOn) {
                // Light is already on - either kept on during pause OR restored by onMultiWindowModeChanged()
                if (isInMultiWindowMode()) {
                    android.util.Log.d("FlashlightLifecycle", "🌟 Light restored by multi-window transition");
                } else {
                    android.util.Log.d("FlashlightLifecycle", "🌟 Light was KEPT ON during pause (smart behavior)");
                }
                android.util.Log.d("FlashlightLifecycle", "➡️ No restore needed - light is already on");
            } else {
                // Light is still off - restore it (for non-multi-window scenarios)
                if (!isInMultiWindowMode()) {
                    try {
                        turnOnFlashlight();
                        android.util.Log.d("FlashlightLifecycle", "✅ Light restored (normal resume)");
                    } catch (CameraAccessException e) {
                        android.util.Log.e("FlashlightLifecycle", "❌ Error restoring light", e);
                        e.printStackTrace();
                    }
                } else {
                    android.util.Log.d("FlashlightLifecycle", "➡️ Multi-window mode - light should have been restored already");
                }
            }
        } else {
            String reason = !wasFlashlightOnBeforePause ? "wasn't on before" : 
                           !hasFlash ? "no flash capability" : 
                           "no camera permission";
            android.util.Log.d("FlashlightLifecycle", "➡️ Not restoring light: " + reason);
        }
        
        android.util.Log.d("FlashlightLifecycle", "==========================");
    }

    // ================================
    // PHASE 2.2: CONSISTENT PAUSE BEHAVIOR LOGIC 
    // ================================
    
    /**
     * PHASE 2.2: Determine if we should keep the flashlight on during onPause()
     * 
     * CONSISTENT BEHAVIOR: Keep light on for ALL pause scenarios
     * - Multi-window mode (split-screen) → KEEP ON
     * - Picture-in-Picture mode → KEEP ON  
     * - Still has window focus (notifications, dialogs) → KEEP ON
     * - Home button / app switcher → KEEP ON (consistent behavior)
     * - Only turn OFF for system-forced scenarios (low battery, etc.)
     * 
     * @return true if light should stay on, false if it should turn off
     */
    private boolean shouldKeepLightOnDuringPause() {
        try {
            // NEW APPROACH: Keep light on for ALL user-initiated pause scenarios
            // This provides consistent behavior whether user:
            // - Presses home button
            // - Uses split-screen  
            // - Opens notifications
            // - Switches apps
            
            android.util.Log.d("FlashlightLifecycle", "🔄 Using CONSISTENT behavior: Always keep light on during pause");
            return true; // Keep light on for consistent UX
            
            // Note: We still have the multi-window callback as a safety net
            // and onDestroy() will turn off light when app is actually closed
            
        } catch (Exception e) {
            android.util.Log.e("FlashlightLifecycle", "Error in pause detection", e);
            // Fallback to safe default - keep light on
            return true;
        }
    }
    
    /**
     * PHASE 2.2: Get human-readable reason for pause decision (for logging)
     */
    private String getPauseDecisionReason() {
        try {
            if (isInMultiWindowMode()) {
                return "Multi-window mode (split-screen) - consistent behavior";
            }
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N 
                && isInPictureInPictureMode()) {
                return "Picture-in-Picture mode - consistent behavior";
            }
            
            if (hasWindowFocus()) {
                return "Still has window focus - consistent behavior";
            }
            
            return "Home button/app switcher - consistent behavior (keep light on)";
            
        } catch (Exception e) {
            return "Error detecting state: " + e.getMessage();
        }
    }

    // ================================
    // PHASE 2.1: MULTI-WINDOW TRANSITION DETECTION
    // ================================
    
    /**
     * Called when multi-window mode changes - this is the RIGHT time to detect transitions!
     * This fires exactly when entering/exiting split-screen, before onPause/onResume
     */
    @Override
    public void onMultiWindowModeChanged(boolean isInMultiWindowMode) {
        super.onMultiWindowModeChanged(isInMultiWindowMode);
        
        android.util.Log.d("FlashlightLifecycle", "=== onMultiWindowModeChanged() ===");
        android.util.Log.d("FlashlightLifecycle", "New multi-window mode: " + isInMultiWindowMode);
        android.util.Log.d("FlashlightLifecycle", "Current light state: " + isFlashlightOn);
        
        // If entering multi-window mode and light was turned off during transition, restore it!
        if (isInMultiWindowMode && wasFlashlightOnBeforePause && !isFlashlightOn) {
            android.util.Log.d("FlashlightLifecycle", "🌟 ENTERING MULTI-WINDOW: Restoring light!");
            try {
                turnOnFlashlight();
                android.util.Log.d("FlashlightLifecycle", "✅ Light restored for multi-window mode");
            } catch (CameraAccessException e) {
                android.util.Log.e("FlashlightLifecycle", "❌ Error restoring light in multi-window", e);
                e.printStackTrace();
            }
        }
        
        android.util.Log.d("FlashlightLifecycle", "=====================================");
    }

    // ================================
    // PHASE 1: EXIT APP FUNCTIONALITY
    // ================================
    
    /**
     * PHASE 1: Exit the application completely
     * Turns off flashlight before closing and removes from recent tasks
     */
    private void exitApp() {
        android.util.Log.d("FlashlightApp", "🚪 User requested app exit");
        
        try {
            // Turn off flashlight before exiting if it's on
            if (isFlashlightOn) {
                android.util.Log.d("FlashlightApp", "🔦 Turning off flashlight before exit");
                turnOffFlashlight();
            }
        } catch (Exception e) {
            android.util.Log.e("FlashlightApp", "Error turning off flashlight during exit", e);
        }
        
        // Close app and remove from recent tasks
        finishAndRemoveTask();
        android.util.Log.d("FlashlightApp", "✅ App exit completed");
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

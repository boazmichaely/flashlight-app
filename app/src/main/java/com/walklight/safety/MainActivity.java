package com.walklight.safety;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.util.Log;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.content.pm.PackageManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import android.widget.ImageButton;
import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceManager;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String DEBUG_TAG = "WalklightD3";
    
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
    private MaterialButton exitButtonTopCorner; // UNUSED: Top corner exit button (kept for potential future use)
    private ImageButton exitButtonFloating; // ACTIVE: Custom PNG exit button
    private ImageButton multiWindowButton; // ACTIVE: Multi-window toggle button (changes icon based on mode)
    
    private boolean isUpdatingSliders = false; // Prevent infinite loops during sync
    private boolean wasFlashlightOnBeforePause = false; // Track state for resume
    private boolean lastSyncState = false; // Remember sync state when flashlight is off
    
    // Multi-window mode tracking
    private boolean isInMultiWindowMode = false;
    
    // Track actual current values for proper sync initialization
    private float currentActualScreenBrightness = 1.0f;
    private float currentActualLedIntensity = 1.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(DEBUG_TAG, "--> Entering onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set default values for preferences on first run
        PreferenceManager.setDefaultValues(this, "walklight_settings", MODE_PRIVATE, R.xml.settings_preferences, false);
        
        // Log current preference state for debugging  
        android.content.SharedPreferences prefs = getSharedPreferences("walklight_settings", MODE_PRIVATE);
        boolean keepLightOn = prefs.getBoolean("keep_light_on_close", true);
        Log.d(TAG, "MainActivity: keep_light_on_close DATA = " + keepLightOn);

        // Handle system insets for proper layout positioning
        setupWindowInsets();

        initializeCamera(); // MUST come before initializeViews() to set hardware capabilities
        initializeViews();
        setupClickListeners();
        
        // Auto-turn on flashlight when app launches (AFTER camera initialization)
        autoStartFlashlight();
    }
    
    private void setupWindowInsets() {
        Log.d(DEBUG_TAG, "--> Entering setupWindowInsets()");
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
        Log.d(DEBUG_TAG, "<-- Exiting setupWindowInsets()");
    }
    
    private void autoStartFlashlight() {
        Log.d(DEBUG_TAG, "--> Entering autoStartFlashlight()");
        // Only auto-start if we have flash capability
        if (hasFlash) {
            try {
                turnOnFlashlight();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
        Log.d(DEBUG_TAG, "<-- Exiting autoStartFlashlight()");
    }

    private void initializeViews() {
        Log.d(DEBUG_TAG, "--> Entering initializeViews()");
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
        
        // Initialize exit button (configuration in exit_button_config.xml)
        exitButtonTopCorner = findViewById(R.id.exitButtonTopCorner);
        exitButtonFloating = findViewById(R.id.exitButtonFloating);
        multiWindowButton = findViewById(R.id.multiWindowButton);
        
        // Hide unused top corner button
        if (exitButtonTopCorner != null) {
            exitButtonTopCorner.setVisibility(View.GONE);
        }
        
        if (exitButtonFloating != null) {
            boolean showCustom = getResources().getBoolean(R.bool.show_custom_exit);
            exitButtonFloating.setVisibility(showCustom ? View.VISIBLE : View.GONE);
        }
        
        // Initialize multi-window button
        if (multiWindowButton != null) {
            boolean showSplitScreen = getResources().getBoolean(R.bool.show_split_screen_button);
            multiWindowButton.setVisibility(showSplitScreen ? View.VISIBLE : View.GONE);
            // Set initial icon based on current mode
            updateMultiWindowButtonIcon();
            Log.d(TAG, "Multi-window button initialized. Visible: " + (showSplitScreen ? "YES" : "NO"));
        } else {
            Log.e(TAG, "Multi-window button NOT FOUND in layout!");
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
        Log.d(DEBUG_TAG, "<-- Exiting initializeViews()");
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
        lightToggle.setOnCheckedChangeListener(this::onLightToggleChanged);

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
        syncSwitch.setOnCheckedChangeListener(this::onSyncSwitchChanged);

        // About Button
        aboutButton.setOnClickListener(this::onAboutButtonClicked);
        
        // Button Click Listeners
        if (exitButtonFloating != null) {
            exitButtonFloating.setOnClickListener(this::onExitButtonClicked);
        }
        
        // Multi-window toggle button
        if (multiWindowButton != null) {
            multiWindowButton.setOnClickListener(this::onMultiWindowButtonClicked);
        }
    }

    // ================================
    // UI EVENT HANDLERS
    // ================================

    /**
     * Handle light toggle switch changes (user interaction)
     */
    private void onLightToggleChanged(CompoundButton button, boolean isChecked) {
        Log.d(DEBUG_TAG, "--> Light toggle changed to: " + isChecked);
        try {
            if (isChecked) {
                Log.d(DEBUG_TAG, "User wants flashlight ON");
                turnOnFlashlight();
            } else {
                Log.d(DEBUG_TAG, "User wants flashlight OFF");
                turnOffFlashlight();
            }
        } catch (Exception e) {
            e.printStackTrace();
            showToast("Error controlling flashlight: " + e.getMessage());
            // Reset toggle on error
            lightToggle.setChecked(false);
        }
        Log.d(DEBUG_TAG, "<-- Light toggle change handled");
    }

    /**
     * Handle sync switch changes (user interaction)
     */
    private void onSyncSwitchChanged(CompoundButton button, boolean isChecked) {
        Log.d(DEBUG_TAG, "--> Sync switch changed to: " + isChecked);
        try {
            // Save sync state for when flashlight turns off/on
            lastSyncState = isChecked;
            
            updateSyncToggleAppearance(isChecked);
            updateLayoutMode(); // This now handles all slider synchronization
            updateSyncedIntensityLabel();
        } catch (Exception e) {
            e.printStackTrace();
            showToast("Error updating sync mode: " + e.getMessage());
        }
        Log.d(DEBUG_TAG, "<-- Sync switch change handled");
    }

    /**
     * Handle about button clicks (user interaction)
     */
    private void onAboutButtonClicked(View view) {
        Log.d(DEBUG_TAG, "--> About button clicked");
        try {
            new SettingsSheetDialog().show(getSupportFragmentManager(), "settings");
        } catch (Exception e) {
            e.printStackTrace();
            showToast("Error opening settings: " + e.getMessage());
        }
        Log.d(DEBUG_TAG, "<-- About button click handled");
    }

    /**
     * Handle exit button clicks (user interaction)
     */
    private void onExitButtonClicked(View view) {
        Log.d(DEBUG_TAG, "--> Exit button clicked");
        exitApp();
        Log.d(DEBUG_TAG, "<-- Exit button click handled");
    }

    /**
     * Handle multi-window button clicks (user interaction)
     */
    private void onMultiWindowButtonClicked(View view) {
        Log.d(DEBUG_TAG, "--> Multi-window button clicked");
        toggleMultiWindowMode();
        Log.d(DEBUG_TAG, "<-- Multi-window button click handled");
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


    private void turnOnFlashlight() throws CameraAccessException {
        Log.d(DEBUG_TAG, "--> Entering turnOnFlashlight()");
        boolean torchSuccess = false;
        
        try {
            // Try basic torch mode first - this is more reliable
            Log.d(DEBUG_TAG, "+++ TORCH ON +++");
            TorchController.setOn(cameraManager, cameraId);
            torchSuccess = true;
            
            // If basic torch worked, try to apply intensity (if supported)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                try {
                    float intensity = getCurrentActualLedIntensity();
                    // D1 DEBUG: Log intensity restoration for debugging multi-window transitions
                    android.util.Log.d("FlashlightApp", "D1 DEBUG: turnOnFlashlight() using intensity: " + intensity + " (from active slider)");
                    // Use the actual device's max strength level (Samsung devices often use 1-99, not 1-100)
                    int strengthLevel = Math.max(1, Math.min(maxTorchStrength, Math.round(intensity * maxTorchStrength)));
                    TorchController.setStrength(cameraManager, cameraId, strengthLevel);
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
        Log.d(DEBUG_TAG, "<-- Exiting turnOnFlashlight()");
    }

    private void turnOffFlashlight() throws CameraAccessException {
        Log.d(DEBUG_TAG, "--> Entering turnOffFlashlight()");
        try {
            Log.d(DEBUG_TAG, "--- TORCH OFF ---");
            TorchController.setOff(cameraManager, cameraId);
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
        Log.d(DEBUG_TAG, "<-- Exiting turnOffFlashlight()");
    }

    // Exposed for ExitPolicy (read-only)
    public boolean isFlashlightCurrentlyOn() {
        return isFlashlightOn;
    }

    // Exposed safe wrapper for ExitPolicy
    public void turnOffFlashlightSafely() {
        try {
            turnOffFlashlight();
        } catch (android.hardware.camera2.CameraAccessException ignored) {
        }
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
        Log.d(DEBUG_TAG, "--> Entering updateLayoutMode()");
        try {
            if (syncSwitch != null && syncModeContainer != null && independentModeContainer != null && screenOnlyModeContainer != null) {
                boolean isFlashlightOn = this.isFlashlightOn;
                // D1 FIX: Use actual visual brightness state instead of reading from sliders during transitions
                // This prevents the "two memories" issue where sliders override each other
                float currentBrightness = currentActualScreenBrightness;
                android.util.Log.d("BrightnessSync", "D1: Mode transition using visual brightness: " + currentBrightness + 
                    " (light=" + isFlashlightOn + ")");
                
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
                    // D2 FIX: Don't override slider values during mode transitions
                    // screenOnlySlider retains its natural value - no need to force it
                    
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
                        // D2 FIX: Don't override slider values during mode transitions 
                        // syncedIntensitySlider retains its natural value
                        
                    syncModeContainer.setVisibility(View.VISIBLE);
                    independentModeContainer.setVisibility(View.GONE);
                    screenOnlyModeContainer.setVisibility(View.GONE);
                        
                        syncModeContainer.post(() -> {
                            // D2 FIX: Use actual slider state instead of cached value
                            updateColorRectangleBrightness(getCurrentActualScreenBrightness());
                            // D3 FIX: Remove intensity update - light is already on with correct intensity
                            // updateFlashlightIntensity() during transitions causes unnecessary flash
                        });
                        
                    } else {
                        // INDEPENDENT MODE: Dual sliders (LED + Screen separate)  
                        // D2 FIX: Don't override screen slider value during mode transitions
                        // screenBrightnessSlider retains its natural value
                        isUpdatingSliders = true;
                        if (ledIntensitySlider != null) {
                            // D1 FIX: Preserve current slider value instead of overwriting with cached value
                            // Only set if the slider has no valid value yet
                            if (ledIntensitySlider.getValue() <= 0f) {
                                ledIntensitySlider.setValue(Math.max(0.1f, currentActualLedIntensity));
                            }
                        }
                        isUpdatingSliders = false;
                        
                    syncModeContainer.setVisibility(View.GONE);
                    independentModeContainer.setVisibility(View.VISIBLE);
                    screenOnlyModeContainer.setVisibility(View.GONE);
                        
                        independentModeContainer.post(() -> {
                            // D2 FIX: Use actual slider state instead of cached value
                            updateColorRectangleBrightness(getCurrentActualScreenBrightness());
                            // D3 FIX: Remove intensity update - light is already on with correct intensity
                            // updateFlashlightIntensity() during transitions causes unnecessary flash
                        });
                    }
                    
                } else {
                    // FLASHLIGHT OFF: Hide sync button completely, show single screen slider
                    syncSwitch.setVisibility(View.GONE);
                    syncLabel.setVisibility(View.GONE); // Hide sync label too
                    
                    // SCREEN ONLY MODE: Just screen brightness control
                    // D2 FIX: Don't override slider values during mode transitions
                    // screenOnlySlider retains its natural value
                    
                    syncModeContainer.setVisibility(View.GONE);
                    independentModeContainer.setVisibility(View.GONE);
                    screenOnlyModeContainer.setVisibility(View.VISIBLE);
                    
                    screenOnlyModeContainer.post(() -> {
                        // D2 FIX: Use actual slider state instead of cached value
                        updateColorRectangleBrightness(getCurrentActualScreenBrightness());
                    });
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }





    // SINGLE SOURCE OF TRUTH - always return the actual LED intensity from active slider
    private float getCurrentActualLedIntensity() {
        try {
            // TARGETED FIX: Read from whichever slider is currently active and visible to user
            if (syncSwitch != null && syncSwitch.isChecked() && syncedIntensitySlider != null) {
                // Sync mode: single slider controls both LED and screen
                float value = syncedIntensitySlider.getValue();
                android.util.Log.d("FlashlightApp", "LED intensity from SYNC slider: " + value);
                return value;
            } else if (ledIntensitySlider != null) {
                // Independent mode: separate LED slider (remove isFlashlightOn check - slider always has value)
                float value = ledIntensitySlider.getValue();
                android.util.Log.d("FlashlightApp", "LED intensity from LED slider: " + value);
                return value;
            } else {
                // Fallback to cached value for edge cases
                android.util.Log.d("FlashlightApp", "LED intensity from CACHED value: " + currentActualLedIntensity);
                return currentActualLedIntensity;
            }
        } catch (Exception e) {
            android.util.Log.e("FlashlightApp", "Error reading LED intensity from sliders", e);
            return currentActualLedIntensity; // Safe fallback
        }
    }

    // SINGLE SOURCE OF TRUTH - always return the actual displayed screen brightness from active slider  
    private float getCurrentActualScreenBrightness() {
        try {
            // D1 FIX: Read from whichever slider is currently active and visible to user
            if (syncSwitch != null && syncSwitch.isChecked() && syncedIntensitySlider != null) {
                // Sync mode: single slider controls both LED and screen
                return syncedIntensitySlider.getValue();
            } else if (isFlashlightOn && screenBrightnessSlider != null) {
                // Independent mode: separate screen slider
                return screenBrightnessSlider.getValue();
            } else if (screenOnlySlider != null) {
                // Screen-only mode: single screen slider
                return screenOnlySlider.getValue();
            } else {
                // Fallback to cached value for edge cases
                return currentActualScreenBrightness;
            }
        } catch (Exception e) {
            android.util.Log.e("FlashlightApp", "Error reading screen brightness from sliders", e);
            return currentActualScreenBrightness; // Safe fallback
        }
    }

    private void updateLightToggleAppearance(boolean isLightOn) {
        // C1.1: M3 switches handle theming automatically - no custom colors needed
    }

    private void updateSyncToggleAppearance(boolean isSyncOn) {
        // C1.1: M3 switches handle theming automatically - no custom colors needed  
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
        if (wasFlashlightOnBeforePause && hasFlash) {
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
            String reason = !wasFlashlightOnBeforePause ? "wasn't on before" : "no flash capability";
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
            // B2.2: Read preference to determine close behavior
            // This respects user's "Keep flashlight on when closed" setting
            SharedPreferences prefs = getSharedPreferences("walklight_settings", MODE_PRIVATE);
            boolean keepLightOn = prefs.getBoolean("keep_light_on_close", true);
            
            android.util.Log.d("FlashlightLifecycle", "🔄 B2.2: Reading user preference for close behavior");
            android.util.Log.d("FlashlightLifecycle", "Keep light on when closed: " + keepLightOn);
            android.util.Log.d("FlashlightLifecycle", "🎯 DECISION: " + (keepLightOn ? "KEEP ON" : "TURN OFF"));
            
            return keepLightOn; // Respect user preference
            
            // Note: We still have the multi-window callback as a safety net
            // and onDestroy() will turn off light when app is actually closed
            
        } catch (Exception e) {
            android.util.Log.e("FlashlightLifecycle", "Error reading preference", e);
            // Fallback to XML default - keep light on
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
        Log.d(DEBUG_TAG, "--> Entering onMultiWindowModeChanged(isInMultiWindowMode=" + isInMultiWindowMode + ")");
        super.onMultiWindowModeChanged(isInMultiWindowMode);
        
        android.util.Log.d("FlashlightLifecycle", "=== onMultiWindowModeChanged() ===");
        android.util.Log.d("FlashlightLifecycle", "New multi-window mode: " + isInMultiWindowMode);
        android.util.Log.d("FlashlightLifecycle", "Current light state: " + isFlashlightOn);
        
        // D3 FIX: Block unnecessary flashlight restoration logic
        // This was causing the flash during mode transitions - light should stay as-is
        /*
        // FLASHLIGHT RESTORATION LOGIC - DISABLED
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
        */
        android.util.Log.d("FlashlightLifecycle", "D3: Skipping flashlight restoration - letting light maintain natural state");
        
        // MULTI-WINDOW BUTTON UPDATE LOGIC
        // Update button icon when mode changes
        runOnUiThread(() -> {
            updateMultiWindowButtonIcon();
            Log.d(TAG, "Multi-window button updated for mode: " + (isInMultiWindowMode ? "SPLIT-SCREEN" : "FULLSCREEN"));
        });
        
        android.util.Log.d("FlashlightLifecycle", "=====================================");
        Log.d(DEBUG_TAG, "<-- Exiting onMultiWindowModeChanged()");
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
        new ExitPolicy().onExitRequested(this);
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
    
    // Phase 2: Multi-window functionality
    private void updateMultiWindowButtonIcon() {
        if (multiWindowButton == null) return;
        
        // Check current multi-window state
        isInMultiWindowMode = isInMultiWindowMode();
        
        if (isInMultiWindowMode) {
            // Currently in split-screen → show fullscreen icon (to exit)
            multiWindowButton.setImageResource(R.drawable.full_screen);
            multiWindowButton.setContentDescription("Exit Split-Screen");
            Log.d(TAG, "Button updated: Showing fullscreen icon (currently in split-screen)");
        } else {
            // Currently fullscreen → show split-screen icon (to enter)
            multiWindowButton.setImageResource(R.drawable.split_vertical_line3);
            multiWindowButton.setContentDescription("Enter Split-Screen");
            Log.d(TAG, "Button updated: Showing split-screen icon (currently fullscreen)");
        }
    }
    
    private void toggleMultiWindowMode() {
        if (isInMultiWindowMode) {
            // Currently in split-screen → exit to fullscreen
            exitSplitScreenMode();
        } else {
            // Currently fullscreen → enter split-screen
            enterSplitScreenMode();
        }
    }
    
    private void enterSplitScreenMode() {
        SplitScreenController split = new SplitScreenController(this);
        boolean isInMultiWindow = isInMultiWindowMode();
        split.enterSplitScreen(isInMultiWindow, () -> {});
    }
    
    private void exitSplitScreenMode() {
        if (!isInMultiWindowMode()) {
            showUserFeedback("Already in fullscreen mode");
            return;
        }
        SplitScreenController split = new SplitScreenController(this);
        split.exitSplitScreen(() -> moveTaskToBack(true), () -> {
            Intent bringBackIntent = new Intent(this, MainActivity.class);
            bringBackIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(bringBackIntent);
        });
    }
    
    // ================================
    // MUSIC APP LAUNCH FUNCTIONALITY
    // ================================
    
    /**
     * B3: Launch Companion App with Split-Screen Intent Flags
     * 
     * KEY FLAG COMBINATION (from proven POC):
     * - FLAG_ACTIVITY_LAUNCH_ADJACENT: Forces launch in split-screen mode
     * - FLAG_ACTIVITY_NEW_TASK: Creates new task stack for the target app
     * 
     * Now uses stored companion app instead of hardcoded Spotify!
     */
    private void launchMusicAppInSplitScreen() {
        new SplitScreenController(this).launchCompanionAppInSplitScreen();
    }
    
    /**
     * LEGACY METHOD: Launch Spotify in split-screen mode (kept for backward compatibility)
     * B3 UPDATE: Now handled by SplitScreenController.launchCompanionAppInSplitScreen()
     * @deprecated Use SplitScreenController.launchCompanionAppInSplitScreen() instead
     */
    @Deprecated
    private void launchSpotifyInSplitScreen() {
        // Delegate to new companion app method
        new SplitScreenController(this).launchCompanionAppInSplitScreen();
    }
    
    /**
     * B3 UPDATED: Launch Companion App Normally (No Split-Screen Forcing)
     * 
     * This is the traditional app launch method when split-screen forcing fails.
     * User would need to manually enter split-screen via system gestures.
     */
    private void launchCompanionAppNormally() {
        // Read stored companion app data
        android.content.SharedPreferences prefs = getSharedPreferences("walklight_settings", MODE_PRIVATE);
        String companionPackage = prefs.getString("companion_app_package", null);
        String companionClass = prefs.getString("companion_app_class", null);
        String companionName = prefs.getString("companion_app_name", "Spotify");

        if (companionPackage != null && companionClass != null) {
            try {
                // Launch stored companion app normally
                Intent companionIntent = new Intent();
                companionIntent.setClassName(companionPackage, companionClass);
                startActivity(companionIntent);
                Log.d(TAG, companionName + " launched normally");
                return;
            } catch (Exception e) {
                Log.w(TAG, "Failed to launch " + companionName + " normally: " + e.getMessage());
            }
        }

        // Fallback to Spotify (backward compatibility)
        launchSpotifyNormallyFallback();
    }

    /**
     * LEGACY FALLBACK: Launch Spotify normally
     * @deprecated Use launchCompanionAppNormally() instead
     */
    @Deprecated
    private void launchSpotifyNormallyFallback() {
        try {
            // Standard launch without split-screen flags
            Intent spotifyIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("spotify:"));
            startActivity(spotifyIntent);
            Log.d(TAG, "Spotify launched normally (fallback)");
        } catch (Exception e) {
            // Web fallback for normal launch
            Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://open.spotify.com"));
            startActivity(webIntent);
            Log.d(TAG, "Spotify web opened as fallback");
        }
    }
    
    /**
     * Show user feedback with consistent logging
     */
    private void showUserFeedback(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        Log.d(TAG, "User feedback: " + message);
    }
    
}

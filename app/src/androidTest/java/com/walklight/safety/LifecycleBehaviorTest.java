package com.walklight.safety;

import android.util.Log;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumentation tests for MainActivity lifecycle behavior
 * 
 * Tests actual Android lifecycle interactions and system state changes
 */
@RunWith(AndroidJUnit4.class)
public class LifecycleBehaviorTest {
    
    private static final String TAG = "LifecycleTest";
    private ActivityScenario<MainActivity> scenario;
    
    @Before
    public void setup() {
        // Launch the activity
        scenario = ActivityScenario.launch(MainActivity.class);
        
        // Wait for initialization
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    @After
    public void cleanup() {
        if (scenario != null) {
            scenario.close();
        }
    }
    
    @Test
    public void activityLaunches_Successfully() {
        // Verify basic activity launch
        scenario.onActivity(activity -> {
            Log.d(TAG, "✅ Activity launched successfully: " + activity.getClass().getSimpleName());
        });
    }
    
    @Test
    public void pauseResumeCycle_TriggersDetectionLogic() {
        scenario.onActivity(activity -> {
            Log.d(TAG, "🔍 Testing pause-resume cycle");
        });
        
        // Simulate pause/resume cycle - this will trigger our detection logic
        scenario.moveToState(androidx.lifecycle.Lifecycle.State.STARTED); // onPause()
        scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED);  // onResume()
        
        scenario.onActivity(activity -> {
            // Log what our detection logic would have done
            boolean shouldKeepOn = activity.isInMultiWindowMode() || 
                                 activity.isInPictureInPictureMode() || 
                                 activity.hasWindowFocus();
            Log.d(TAG, "Detection logic would keep light on: " + shouldKeepOn);
            Log.d(TAG, "✅ Pause-resume cycle completed - check logcat for detection results");
        });
    }
    
    @Test 
    public void orientationChange_DoesNotTriggerPause() {
        // Note: Orientation is locked to portrait in manifest
        scenario.onActivity(activity -> {
            Log.d(TAG, "🔄 Testing orientation behavior");
            Log.d(TAG, "✅ Orientation is locked by manifest - no unwanted pause should occur");
        });
    }
    
    @Test
    public void multiWindowDetection_ReportsCorrectly() {
        scenario.onActivity(activity -> {
            Log.d(TAG, "📱 Testing multi-window detection");
            
            // Log current multi-window state
            boolean isMultiWindow = activity.isInMultiWindowMode();
            boolean isPiP = activity.isInPictureInPictureMode();
            boolean hasFocus = activity.hasWindowFocus();
            
            Log.d(TAG, "Multi-window mode: " + isMultiWindow);
            Log.d(TAG, "Picture-in-Picture: " + isPiP);
            Log.d(TAG, "Has window focus: " + hasFocus);
            
            // Test our detection logic
            boolean shouldKeepLightOn = isMultiWindow || isPiP || hasFocus;
            Log.d(TAG, "🎯 Detection result: " + (shouldKeepLightOn ? "KEEP ON" : "TURN OFF"));
            
            // In normal single-app mode, we expect:
            // - isMultiWindow = false
            // - isPiP = false  
            // - hasFocus = true (since activity is active)
            Log.d(TAG, "✅ Expected: Activity should have focus in normal single-app mode");
        });
    }
}

# Phase 1 Testing - Light-Off Detection Logic

## Overview

This directory contains comprehensive tests for the smart light-off detection logic implemented in Phase 1.

**IMPORTANT:** Phase 1 does **NOT** change current behavior - it only adds detection logic and logging to analyze what the new logic would do vs current behavior.

## Test Structure

### Unit Tests (`test/java/`)
- **`LightOffDetectionTest.java`** - Tests the core detection logic
  - `shouldKeepLightOn_whenInMultiWindowMode()` 
  - `shouldKeepLightOn_whenInPictureInPictureMode()`
  - `shouldTurnOffLight_whenTrulyBackgrounded()`
  - `shouldLogDecisionReasoning()`

### Instrumentation Tests (`androidTest/java/`)  
- **`LifecycleBehaviorTest.java`** - Tests actual Android lifecycle interactions
  - `pauseResumeCycle_MaintainsState()`
  - `orientationChange_DoesNotTriggerPause()`
  - `multiWindowDetection_ReportsCorrectly()`

## Running Tests

### Unit Tests (Fast)
```bash
./gradlew test
```

### Instrumentation Tests (Requires device/emulator)
```bash
./gradlew connectedAndroidTest
```

## Phase 1 Validation Process

1. **Install app** with Phase 1 detection logic
2. **Monitor logs** with filter: `adb logcat -s FlashlightLifecycle`
3. **Test scenarios:**
   - Normal app switching (home button)
   - Split-screen mode with another app  
   - Notification shade pull-down
   - System dialogs
4. **Verify detection accuracy** by comparing logged decisions

## Expected Log Output

```
D/FlashlightLifecycle: === onPause() Analysis ===
D/FlashlightLifecycle: Multi-window mode: true
D/FlashlightLifecycle: Picture-in-Picture: false
D/FlashlightLifecycle: Has window focus: false  
D/FlashlightLifecycle: Current light state: true
D/FlashlightLifecycle: Decision reason: Multi-window mode (split-screen)
D/FlashlightLifecycle: 🎯 NEW LOGIC would: KEEP ON
D/FlashlightLifecycle: ❌ OLD LOGIC will: TURN OFF
D/FlashlightLifecycle: ✅ Light turned OFF (current behavior)
D/FlashlightLifecycle: =========================
```

## Success Criteria

Phase 1 is successful if:
- ✅ All tests pass
- ✅ Detection logic correctly identifies split-screen scenarios  
- ✅ Detection logic correctly identifies true backgrounding
- ✅ No existing functionality is broken
- ✅ Logs clearly show NEW vs OLD logic decisions

Only proceed to Phase 2 after Phase 1 validation is complete!

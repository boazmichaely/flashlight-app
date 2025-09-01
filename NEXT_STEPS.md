## Current Status

- **Version**: `1.15.14` 
- **State**: **Phase 3 IN PROGRESS** - Visual Polish & Architecture Cleanup
- **Latest Tag**: `v1.15.14` (Companion App Integration Complete)

## Safe Recovery Procedures

### **Available Checkpoints:**
- **v1.15.14**: Companion App Integration Complete *(LATEST)*
- **v1.14.14**: Phase 2 COMPLETE - Professional Dynamic Companion App System
- **v1.13.14**: B3 Split-Screen Integration

## NEXT STEPS

## Phase 2 & 3 Accomplishments Summary

**Phase 2**: Hardcoded Spotify split-screen → Full dynamic companion app system  
**Phase 3**: Recovery from disaster + Final companion app integration

### ✅ **Phase 2 Completed Features:**
- **App Picker**: Native chooser with proper name/icon display  
- **Launch Testing**: Dedicated button to test selected apps
- **Smart Defaults**: Spotify auto-initialized on fresh installs
- **Reset Functionality**: One-button restore with confirmation
- **Professional UI**: Aligned layout with descriptive text
- **Split-Screen Integration**: Uses selected app instead of hardcoded Spotify

### ✅ **Phase 3 Completed (v1.15.14):**
- **Compilation Fix**: Added missing `launchCompanionAppInSplitScreen()` method
- **Settings Restoration**: Restored "Test your Companion App" line with Launch button
- **Split-Screen Fix**: Split-screen button now uses stored companion app (not hardcoded Spotify)
- **Git Workflow**: Added comprehensive version bump procedures to README
- **Recovery Procedures**: Created disaster-proof restore instructions
- **UI Polish**: C1.1 (switches), C1.2 (exit icon), C1.3 (split-screen icon) marked complete

### ✅ **Technical Achievements:**
- **Android 11+ Compatibility**: Package visibility properly handled via `<queries>` manifest
- **Kotlin-Java Integration**: Mixed codebase working seamlessly  
- **Material 3 UI**: Consistent design throughout settings
- **Robust Error Handling**: Fallbacks and comprehensive logging

## NEXT STEPS - Remaining Tasks

### **C1.4: Replace Full Screen Icon**
- Replace the current full screen icon with a better M3 design
- Maintain current functionality
- Test icon visibility and clarity

### **C1.5: Re-scale the Sliders to 50:50 Ratio**  
- Currently using 35:65 (or 45:55) ratio with less space for Light slider
- Change to even 50:50 split between Light and Screen sliders
- Update layout dimensions and constraints

### **C2: Code Architecture Cleanup** *(Optional)*
- Theme simplification: Remove unused color overrides
- Extract preference reading to utility methods  
- Consolidate multi-window detection logic

### **Success Criteria:**
- ✅ All icons follow M3 design language
- ✅ Balanced 50:50 slider layout  
- ✅ Clean, maintainable architecture

---

## Appendix

### **Version Management**

#### **Versioning Scheme: M.n.C**
- **M** = Major version (big architectural/UI changes, requires approval)
- **n** = Minor version (new features, significant improvements)  
- **C** = Code version (Play Store upload version, change only when publishing)

#### **Version Bump Rules:**
- **Minor features/improvements**: Increment `n` only (e.g., 1.14.14 → 1.15.14)
- **Major changes**: Increment `M`, reset `n` (requires explicit approval)
- **Play Store upload**: Increment `C` and `versionCode` (only when publishing)

#### **Recent Examples:**
- `1.13.14` → `1.14.14` (B2.7 settings UI polish - minor bump)
- `1.14.14` → `2.0.14` (hypothetical major UI overhaul - major bump)
- `1.14.14` → `1.14.15` (Play Store upload - code bump)

### **Key Lessons Learned**

#### **Android Development:**
- **Package Visibility**: Android 11+ requires `<queries>` in manifest for `PackageManager` calls
- **Preference Systems**: Standard components more reliable than custom `widgetLayout` widgets
- **Intent Actions**: Use `ACTION_PICK_ACTIVITY` for app selection, not `createChooser()` 
- **Kotlin-Java**: Seamless interoperability when properly configured in Gradle

#### **Development Process:**
- **Anti-spinning approach**: Small, focused changes with immediate testing
- **Logging is critical**: All preference changes must log old→new values
- **Test fresh installs**: Uninstall → reinstall → verify defaults
- **Functionality first**: Choose working solutions over perfect styling initially

#### **UI/UX Design:**
- **Material 3**: Consistency across all components improves user experience
- **Descriptive text**: Clear labels like "Test your companion app:" improve usability  
- **Confirmation dialogs**: Prevent accidental resets with proper confirmation flows
- **Visual alignment**: Left text / right button alignment creates professional appearance

#### **Git Workflow & AI Collaboration:**
- **NEVER accumulate 12+ hours of uncommitted work** - commit early, commit often
- **Create backup branches** before major operations: `git branch backup-current-state`
- **Use descriptive commit messages** following established pattern (🎯, ✅, 🛠️, 🐛)
- **Branch for major features**: `git checkout -b phase-3-visual-polish`
- **Tag releases**: `git tag v1.15.14` when phases complete
- **AI assistants must follow proper git practices** - no destructive operations without explicit approval
- **Document all changes** in commit messages for future recovery
- **Always verify git state** before starting new work sessions



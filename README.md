# Flashlight App

A simple native Android flashlight app with intensity control for both the flashlight and the screen.

## Features

- **Basic flashlight on/off toggle** - Works on all Android devices with flashlight
- **Flashlight intensity control** - Automatic device capability detection (Samsung: 1-5 levels, others: 1-100)
- **Screen brightness feedback** - Visual intensity indicator with smooth brightness changes
- **Device compatibility** - Optimized for Samsung devices and other Android manufacturers
- **Clean Material Design UI** - Simple toggle button and intensity slider
- **Proper permissions handling** - Camera permission with user-friendly prompts
- **Free and ad-free** - No ads, no tracking, no premium features

## Technical Details

This is a native Android app built with:
- **Java** - Main development language
- **Android Camera2 API** - For flashlight control and intensity management
- **Material Design Components** - For modern UI elements
- **Target SDK: Android API 34** - Latest Android compatibility
- **Minimum SDK: Android API 26** - Supports Android 8.0+

## Device Compatibility

- ✅ **Samsung devices** - Full intensity control (1-5 brightness levels)
- ✅ **Google Pixel devices** - Standard intensity control (1-100 levels)
- ✅ **Other Android devices** - Auto-detection of capabilities with graceful fallbacks
- ✅ **Android emulators** - UI testing support (flashlight simulation)

## Status

✨ **Latest Release v1.3.2** - Polished Unified Toggle Interface

### Version History
- **v1.3.2** - **POLISHED:** Unified light blue toggle design, perfect size consistency, superior UX discovery flow (sync OFF by default), refined visual language
- **v1.2** - Three-state adaptive layout (sync/independent/screen-only modes), intelligent UI that hides inactive controls, compact horizontal layout
- **v1.1** - Dual-slider system (LED + screen brightness), smooth operation, visual syncing, "Pay It Forward" icon  
- **v1.0** - Basic flashlight with intensity control and device compatibility

## How the Smart Layout System Works

Our app features an intelligent three-state layout system that adapts based on user context, showing only the controls you actually need:

```mermaid
flowchart TD
    Start([Start]) --> A
    
    A["SYNC MODE<br/><br/>Single 'Intensity' slider<br/>Controls LED + Screen"]
    B["SCREEN ONLY<br/><br/>Single 'Screen' slider<br/>LED hidden"]  
    C["INDEPENDENT<br/><br/>Two sliders<br/>35% LED + 65% Screen"]
    
    A -->|sync OFF| B
    A -->|flashlight ON/OFF| A
    
    B -->|sync ON| A
    B -->|flashlight ON| C
    
    C -->|flashlight OFF| B
    C -->|sync ON| A
    
    style A fill:#4CAF50,stroke:#333,stroke-width:3px,color:#fff
    style B fill:#2196F3,stroke:#333,stroke-width:3px,color:#fff  
    style C fill:#FF9800,stroke:#333,stroke-width:3px,color:#fff
```

**Why this matters:** Instead of cluttering the screen with disabled controls (like most apps), ours intelligently shows only what's functional. When the flashlight is off, why show a grayed-out LED slider? Our interface adapts to give you maximum precision where it counts.

## Development Credits

**Developed with AI Assistance** - This project was created with the assistance of Claude (Anthropic's AI assistant), providing:
- Complete Android app architecture and implementation
- Samsung device compatibility optimization
- Camera2 API integration and error handling
- Material Design UI implementation
- Device capability detection and graceful fallbacks

## License

Free and open source

# Walklight - Safety Flashlight

**The smart safety light for outdoor walking and running.**

Unlike basic flashlight apps, Walklight gives you intelligent dual-intensity control for both your phone's LED flashlight AND screen brightness. Perfect for evening walks, early morning runs, or any time you need safe, adaptable lighting that won't blind you or drain your battery.

**Why Walklight?** Most flashlight apps are either too bright (destroying your night vision) or too basic (just on/off). Walklight adapts to your needs with precise intensity control and an innovative sync feature for seamless lighting transitions.

## Features

✅ **Smart dual lighting** - LED flashlight + screen brightness with precise intensity control  
✅ **Adaptive sync mode** - Control both lights together or independently as needed  
✅ **Three intelligent layouts** - Interface adapts to show only relevant controls  
✅ **Night vision friendly** - Fine intensity control prevents eye strain  
✅ **Walking optimized** - Designed for outdoor safety and comfort  
✅ **Free & Ad-Free** - No subscriptions, no ads, completely free to use  
✅ **Open Source** - MIT License, built with community transparency

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

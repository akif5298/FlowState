# Logo Setup Guide

This guide explains how to set up your logos for the Energy Predictor app.

## Overview

The app uses **two different types of logos**:
1. **Transparent Logo** - Used inside the app UI (always the same)
2. **Light/Dark Logos** - Used for the app launcher icon (changes with system theme)

## File Structure

```
app/src/main/res/
├── drawable/
│   ├── app_logo_transparent.png        ← Your transparent logo (for app UI)
│   ├── app_logo_transparent.xml        ← References the PNG
│   ├── app_logo_light.png              ← Your light logo (for launcher)
│   ├── app_logo_light_icon.xml         ← References light logo
│   └── app_logo.xml                    ← References transparent logo (for UI)
│
├── drawable-night/
│   ├── app_logo_dark.png               ← Your dark logo (for launcher)
│   ├── app_logo_dark_icon.xml          ← References dark logo
│   └── app_logo.xml                     ← References transparent logo (same as light)
│
└── mipmap-*/                           ← Launcher icons (all reference light_icon/dark_icon)
    ├── ic_launcher.xml
    └── ic_launcher_round.xml
```

## Step 1: Add Your Transparent Logo (App UI)

1. Copy your transparent logo PNG file
2. Paste it into: `app/src/main/res/drawable/`
3. Name it exactly: `app_logo_transparent.png`
4. This logo will appear in:
   - Login screen (above "Welcome Back")
   - Main screen header
   - Anywhere else the app shows the logo

## Step 2: Add Your Light Logo (Launcher Icon - Light Mode)

1. Copy your light logo PNG file
2. Paste it into: `app/src/main/res/drawable/`
3. Name it exactly: `app_logo_light.png`
4. This will be used for the app launcher icon in light mode

## Step 3: Add Your Dark Logo (Launcher Icon - Dark Mode)

1. Copy your dark logo PNG file
2. Paste it into: `app/src/main/res/drawable-night/` (create folder if needed)
3. Name it exactly: `app_logo_dark.png`
4. This will be used for the app launcher icon in dark mode

## How It Works

### App UI (Transparent Logo)
- The layouts use `@drawable/app_logo`
- This references `app_logo.xml` which points to `app_logo_transparent.png`
- The same transparent logo is used regardless of system theme
- Appears in login screen, main screen, etc.

### Launcher Icon (Light/Dark Logos)
- Android uses adaptive icons from `mipmap-*/ic_launcher.xml`
- These reference `@drawable/app_logo_light_icon` (light mode)
- In dark mode, Android automatically uses resources from `drawable-night/` if available
- The launcher icons adapt based on the system theme

## Image Requirements

### Transparent Logo (App UI)
- **Format:** PNG with transparency
- **Size:** At least 512x512px (will be scaled to 120dp in login, 64dp in main)
- **Transparency:** Alpha channel required
- **Background:** Should be transparent

### Light/Dark Logos (Launcher)
- **Format:** PNG
- **Size:** At least 512x512px for adaptive icons
- **Background:** Can have background or be transparent
- **Note:** Adaptive icons will crop to a circular/square shape

## Testing

1. **Transparent Logo in UI:**
   - Open the app and check login screen and main screen
   - Logo should be transparent and visible on any background

2. **Launcher Icons:**
   - Install the app on a device/emulator
   - Enable/disable dark mode in system settings
   - Check the app launcher icon changes accordingly

## Summary

- **App UI:** Always uses transparent logo (`app_logo_transparent.png`)
- **Launcher Icon:** Uses light logo in light mode, dark logo in dark mode
- **File Locations:**
  - Transparent: `drawable/app_logo_transparent.png`
  - Light: `drawable/app_logo_light.png`
  - Dark: `drawable-night/app_logo_dark.png`

That's it! Once you add the three PNG files, everything will work automatically.


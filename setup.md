# CameraX10 Setup Guide

A camera app built for the **Sony Ericsson Xperia X10 Mini (E10i)** running Android 2.1 (API 7). Inspired by OpenCamera's UI.

## Target Device

- Sony Ericsson Xperia X10 Mini (E10i)
- Android 2.1 Eclair (API 7)
- 240x320 QVGA, 2.55" display
- 5MP rear camera, LED flash, autofocus, no front camera
- 600MHz CPU, 128MB RAM
- SD card required for saving photos/videos

## Prerequisites

- **Android SDK** installed at `~/Library/Android/sdk`
- **Android Studio** (for its bundled JDK at `/Applications/Android Studio.app/Contents/jre/`)
- **Build Tools 28.0.3** — `sdkmanager "build-tools;28.0.3"`
- **Android 7 Platform** — `sdkmanager "platforms;android-7"`
- **Debug keystore** at `~/.android/debug.keystore` (auto-created by Android Studio on first run)

## Why a Manual Build?

The app cannot be built with Gradle/aapt2. Android 2.1 does not support aapt2's UTF-8 string pools, which causes the device's launcher to crash. The manual `build.sh` uses **aapt1** and avoids custom XML resources entirely — all UI is built programmatically in Java.

### Build Constraints

| Constraint | Reason |
|---|---|
| No custom XML layouts/strings/drawables | aapt2 UTF-8 string pools crash Android 2.1's launcher; even aapt1 from modern build-tools generates incompatible `resources.arsc` for custom drawables |
| All UI built in Java | Programmatic `View` construction, `GradientDrawable`, `Canvas` drawing |
| Java 1.7 source/target | Required for dx compatibility |
| v1-only APK signing (SHA1) | Android 2.1 does not support v2/v3 signatures |
| Compiled against `android-7` platform jar | Ensures no accidental use of newer APIs at compile time |
| Reflection for API 8+ features | Zoom (`setZoom`, `isZoomSupported`, `getMaxZoom`) accessed via `java.lang.reflect.Method` |

### APIs NOT Available on API 7

- `Camera.setDisplayOrientation()` (API 8+) — activity is landscape-locked instead
- `Camera.CameraInfo` (API 9+) — sensor orientation hardcoded to 90° for X10 Mini
- `CamcorderProfile` (API 8+) — video settings hardcoded (H.263 + AMR-NB in 3GP)
- `MediaRecorder.setOrientationHint()` (API 9+) — no video rotation metadata
- `MediaRecorder.setVideoEncodingBitRate()` (API 8+) — bitrate controlled by firmware
- `ThumbnailUtils.createVideoThumbnail()` (API 8+) — play icon drawn with `Canvas`
- `View.setRotation()` (API 11+) — custom `RotatableButton` with `canvas.rotate()` in `onDraw()`

## Building

```bash
./build.sh
```

Output: `app/build/manual/app-debug.apk`

### Build + Install + Launch

```bash
./build.sh --install
```

Requires a connected device or emulator via ADB.

## Project Structure

```
camerax10/
├── build.sh                    # Manual build script (aapt1 + javac + dx + jarsigner)
├── app/
│   ├── build.gradle            # Gradle config (for IDE support only, not used for building)
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── res/                # Empty — no custom resources
│       └── java/com/camerax10/
│           ├── CameraActivity.java   # Main activity — all camera logic and UI (~1700 lines)
│           ├── CameraPreview.java    # SurfaceView for camera preview
│           └── SettingsActivity.java # PreferenceActivity (all preferences built in Java)
├── gradle.properties           # android.useAndroidX=false, android.enableAapt2=false
└── settings.gradle
```

## Features

### Camera
- Photo capture with JPEG quality setting (70/80/90/100)
- Video recording (H.263 + AMR-NB, 3GP container)
- Video resolution selection (480p, 320x240, 176x144 with fallback chain)
- Flash mode cycling (off/auto/on/torch)
- Focus mode cycling (auto/macro/infinity/fixed)
- Scene mode cycling
- White balance and color effect settings
- Auto-geotagging of photos (GPS EXIF data when location available)
- Zoom via volume keys (using reflection for API 8+ methods)

### UI
- Orientation-aware button rotation via `OrientationEventListener` + `canvas.rotate()`
- Camera-style crosshair focus indicator (yellow=focusing, green=success, red=failure)
- Grid line overlay options (rule of thirds, 4x4, crosshair, golden ratio)
- Recording indicator (blinking red dot)
- Last captured photo/video thumbnail preview (queries MediaStore on startup)
- Play icon overlay on video thumbnails
- GPS availability indicator (green=location available, gray=no location)
- Picture size button showing current resolution

### Sound Effects
- Sony Alpha-style synthesized sounds (generated as WAV files on first launch):
  - **Shutter**: single 2500 Hz click with fast decay
  - **Focus confirmation**: double-beep at 2700 Hz
  - **Video record start**: ascending two-note chime (2000 Hz → 2700 Hz)
  - **Video record stop**: descending two-note chime (2700 Hz → 2000 Hz)
- Falls back to system sounds from `/system/media/audio/ui/` if available
- System shutter stream muted to prevent double-play with camera HAL

### Settings
- JPEG quality
- Volume key action (none / capture / zoom)
- Grid lines (none / rule of thirds / 4x4 / crosshair / golden ratio)
- White balance (dynamic from camera capabilities)
- Color effect (dynamic from camera capabilities)

### Storage
- Photos/videos saved to `SD card/DCIM/`
- SD card is mandatory — preview-only mode when no SD card is inserted (shutter disabled, toast warning)

## Permissions

| Permission | Usage |
|---|---|
| `CAMERA` | Camera access |
| `WRITE_EXTERNAL_STORAGE` | Saving photos/videos to SD card |
| `RECORD_AUDIO` | Video recording audio |
| `ACCESS_FINE_LOCATION` | Geotagging photos with GPS coordinates |

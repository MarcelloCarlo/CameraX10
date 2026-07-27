#!/bin/bash
set -e

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
SDK="$HOME/Library/Android/sdk"
BUILD_TOOLS="$SDK/build-tools/28.0.3"
PLATFORM="$SDK/platforms/android-7"
export JAVA_HOME="/Applications/Android Studio.app/Contents/jre/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"

AAPT="$BUILD_TOOLS/aapt"
DX="$BUILD_TOOLS/dx"
ZIPALIGN="$BUILD_TOOLS/zipalign"
JAVAC="$JAVA_HOME/bin/javac"
JARSIGNER="$JAVA_HOME/bin/jarsigner"

SRC_DIR="$PROJECT_DIR/app/src/main"
BUILD_DIR="$PROJECT_DIR/app/build/manual"
GEN_DIR="$BUILD_DIR/gen"
CLASSES_DIR="$BUILD_DIR/classes"
APK_UNSIGNED="$BUILD_DIR/app-unsigned.apk"
APK_ALIGNED="$BUILD_DIR/app-aligned.apk"
APK_FINAL="$BUILD_DIR/app-debug.apk"

DEBUG_KEYSTORE="$HOME/.android/debug.keystore"

echo "=== Cleaning ==="
rm -rf "$BUILD_DIR"
mkdir -p "$GEN_DIR" "$CLASSES_DIR"

echo "=== Generating R.java (aapt1) ==="
"$AAPT" package -f -m \
    -S "$SRC_DIR/res" \
    -J "$GEN_DIR" \
    -M "$SRC_DIR/AndroidManifest.xml" \
    -I "$PLATFORM/android.jar"

echo "=== Compiling Java ==="
find "$SRC_DIR/java" "$GEN_DIR" -name "*.java" > "$BUILD_DIR/sources.txt"
"$JAVAC" -source 1.7 -target 1.7 \
    -bootclasspath "$PLATFORM/android.jar" \
    -classpath "$PLATFORM/android.jar" \
    -d "$CLASSES_DIR" \
    @"$BUILD_DIR/sources.txt"

echo "=== Creating DEX ==="
"$DX" --dex --output="$BUILD_DIR/classes.dex" "$CLASSES_DIR"

echo "=== Packaging APK ==="
"$AAPT" package -f \
    -S "$SRC_DIR/res" \
    -M "$SRC_DIR/AndroidManifest.xml" \
    -I "$PLATFORM/android.jar" \
    -F "$APK_UNSIGNED"

cd "$BUILD_DIR"
"$AAPT" add -f "$APK_UNSIGNED" classes.dex
cd "$PROJECT_DIR"

echo "=== Aligning APK ==="
"$ZIPALIGN" -f 4 "$APK_UNSIGNED" "$APK_ALIGNED"

echo "=== Signing APK (SHA1 for Android 2.1) ==="
cp "$APK_ALIGNED" "$APK_FINAL"
"$JARSIGNER" -verbose \
    -digestalg SHA1 \
    -sigalg SHA1withRSA \
    -keystore "$DEBUG_KEYSTORE" \
    -storepass android \
    -keypass android \
    "$APK_FINAL" \
    androiddebugkey

echo ""
echo "=== Build complete ==="
echo "APK: $APK_FINAL"
echo ""

if [ "$1" = "--install" ]; then
    ADB="$SDK/platform-tools/adb"
    echo "=== Installing ==="
    "$ADB" install -r -t "$APK_FINAL"
    echo ""
    echo "=== Launching ==="
    "$ADB" shell am start -n com.camerax10/.CameraActivity
fi

#!/usr/bin/env bash
set -euo pipefail

APP_NAME="Uno"
MAIN_CLASS="application.Main"
JAVAFX_VERSION="${JAVAFX_VERSION:-21.0.2}"
PACKAGE_TYPE="${PACKAGE_TYPE:-}"
VERSION="${APP_VERSION:-1.0}"
ENABLE_PREVIEW="${ENABLE_PREVIEW:-false}"

VERSION="${VERSION#v}"
BUILD_DIR="build"
INPUT_DIR="$BUILD_DIR/package-input"
RELEASE_DIR="release"

if [[ -z "$PACKAGE_TYPE" ]]; then
  case "$(uname -s)" in
    Linux*) PACKAGE_TYPE="deb" ;;
    Darwin*) PACKAGE_TYPE="dmg" ;;
    *) echo "Unsupported OS for this packaging script: $(uname -s)" >&2; exit 1 ;;
  esac
fi

case "$(uname -s)-$(uname -m)" in
  Linux-x86_64) JAVAFX_PLATFORM="linux-x64"; TARGET="linux-x64" ;;
  Linux-aarch64|Linux-arm64) JAVAFX_PLATFORM="linux-aarch64"; TARGET="linux-aarch64" ;;
  Darwin-x86_64) JAVAFX_PLATFORM="osx-x64"; TARGET="macos-x64" ;;
  Darwin-arm64) JAVAFX_PLATFORM="osx-aarch64"; TARGET="macos-aarch64" ;;
  *) echo "Unsupported platform: $(uname -s)-$(uname -m)" >&2; exit 1 ;;
esac

rm -rf "$BUILD_DIR/classes" "$INPUT_DIR" "$RELEASE_DIR"
mkdir -p "$BUILD_DIR/classes" "$INPUT_DIR" "$RELEASE_DIR" "$BUILD_DIR/javafx"

SDK_ZIP="$BUILD_DIR/javafx/javafx-sdk.zip"
JMODS_ZIP="$BUILD_DIR/javafx/javafx-jmods.zip"
SDK_URL="https://download2.gluonhq.com/openjfx/${JAVAFX_VERSION}/openjfx-${JAVAFX_VERSION}_${JAVAFX_PLATFORM}_bin-sdk.zip"
JMODS_URL="https://download2.gluonhq.com/openjfx/${JAVAFX_VERSION}/openjfx-${JAVAFX_VERSION}_${JAVAFX_PLATFORM}_bin-jmods.zip"

if [[ ! -f "$SDK_ZIP" ]]; then
  curl -L --fail -o "$SDK_ZIP" "$SDK_URL"
fi

if [[ ! -f "$JMODS_ZIP" ]]; then
  curl -L --fail -o "$JMODS_ZIP" "$JMODS_URL"
fi

rm -rf "$BUILD_DIR/javafx/sdk" "$BUILD_DIR/javafx/jmods"
mkdir -p "$BUILD_DIR/javafx/sdk" "$BUILD_DIR/javafx/jmods"
unzip -q "$SDK_ZIP" -d "$BUILD_DIR/javafx/sdk"
unzip -q "$JMODS_ZIP" -d "$BUILD_DIR/javafx/jmods"

JAVAFX_SDK_DIR="$(find "$BUILD_DIR/javafx/sdk" -maxdepth 1 -type d -name 'javafx-sdk-*' | head -n 1)"
JAVAFX_JMODS_DIR="$(find "$BUILD_DIR/javafx/jmods" -maxdepth 1 -type d -name 'javafx-jmods-*' | head -n 1)"

JAVAC_PREVIEW_ARGS=()
JPACKAGE_PREVIEW_ARGS=()
if [[ "$ENABLE_PREVIEW" == "true" || "$ENABLE_PREVIEW" == "1" ]]; then
  JAVA_RELEASE="$(java -XshowSettings:properties -version 2>&1 | sed -n 's/^[[:space:]]*java.specification.version = //p' | head -n 1)"
  JAVAC_PREVIEW_ARGS=(--enable-preview --release "$JAVA_RELEASE")
  JPACKAGE_PREVIEW_ARGS=(--java-options --enable-preview)
fi

javac \
  "${JAVAC_PREVIEW_ARGS[@]}" \
  --module-path "$JAVAFX_SDK_DIR/lib" \
  --add-modules javafx.controls \
  -d "$BUILD_DIR/classes" \
  $(find src/application -name '*.java' | sort)

cp -R src/resources "$BUILD_DIR/classes/resources"

jar \
  --create \
  --file "$INPUT_DIR/$APP_NAME.jar" \
  --main-class "$MAIN_CLASS" \
  -C "$BUILD_DIR/classes" .

ICON_ARGS=()
if [[ "$(uname -s)" == "Linux" ]]; then
  ICON_ARGS+=(--icon src/resources/uno.png)
elif [[ "$(uname -s)" == "Darwin" ]]; then
  ICONSET="$BUILD_DIR/Uno.iconset"
  ICNS="$BUILD_DIR/Uno.icns"
  rm -rf "$ICONSET"
  mkdir -p "$ICONSET"
  sips -z 16 16 src/resources/uno.png --out "$ICONSET/icon_16x16.png" >/dev/null
  sips -z 32 32 src/resources/uno.png --out "$ICONSET/icon_16x16@2x.png" >/dev/null
  sips -z 32 32 src/resources/uno.png --out "$ICONSET/icon_32x32.png" >/dev/null
  sips -z 64 64 src/resources/uno.png --out "$ICONSET/icon_32x32@2x.png" >/dev/null
  sips -z 128 128 src/resources/uno.png --out "$ICONSET/icon_128x128.png" >/dev/null
  sips -z 256 256 src/resources/uno.png --out "$ICONSET/icon_128x128@2x.png" >/dev/null
  sips -z 256 256 src/resources/uno.png --out "$ICONSET/icon_256x256.png" >/dev/null
  sips -z 512 512 src/resources/uno.png --out "$ICONSET/icon_256x256@2x.png" >/dev/null
  sips -z 512 512 src/resources/uno.png --out "$ICONSET/icon_512x512.png" >/dev/null
  sips -z 1024 1024 src/resources/uno.png --out "$ICONSET/icon_512x512@2x.png" >/dev/null
  iconutil -c icns "$ICONSET" -o "$ICNS"
  ICON_ARGS+=(--icon "$ICNS")
fi

JPACKAGE_ARGS=(
  --type "$PACKAGE_TYPE"
  --name "$APP_NAME"
  --input "$INPUT_DIR"
  --main-jar "$APP_NAME.jar"
  --main-class "$MAIN_CLASS"
  --app-version "$VERSION"
  --dest dist
  --module-path "$JAVAFX_JMODS_DIR"
  --add-modules java.desktop,java.net.URL,javafx.controls
  "${JPACKAGE_PREVIEW_ARGS[@]}"
  "${ICON_ARGS[@]}"
)

if [[ "$PACKAGE_TYPE" == "deb" ]]; then
  JPACKAGE_ARGS+=(--linux-package-name madlibs --linux-app-release 1 --linux-shortcut)
fi

rm -rf dist
mkdir -p dist
jpackage "${JPACKAGE_ARGS[@]}"

for package in dist/*; do
  extension="${package##*.}"
  cp "$package" "$RELEASE_DIR/$APP_NAME-$VERSION-$TARGET.$extension"
done

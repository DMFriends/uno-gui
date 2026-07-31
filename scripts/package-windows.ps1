<#
    Builds a Windows installer (.msi by default) for MadLibs.

    Mirrors scripts/package-unix.sh: it does NOT use the Eclipse "Runnable JAR"
    (which bundles JavaFX as native-less nested jars and only runs inside Eclipse).
    Instead it compiles a thin app jar and lets jpackage build a custom runtime
    that includes the JavaFX modules (with their native libraries) from the
    downloaded JavaFX jmods.

    Tools (javac/jar/jpackage) are taken from JAVA_HOME so CI's setup-java action
    drives the version. Env vars APP_VERSION / PACKAGE_TYPE / JAVAFX_VERSION mirror
    the unix script; they can also be passed as parameters when running locally.
#>

param(
    [string]$Version       = $(if ($env:APP_VERSION)    { $env:APP_VERSION }    else { "1.0" }),
    [string]$PackageType   = $(if ($env:PACKAGE_TYPE)   { $env:PACKAGE_TYPE }   else { "msi" }),
    [string]$JavaFxVersion = $(if ($env:JAVAFX_VERSION) { $env:JAVAFX_VERSION } else { "21.0.2" }),
    [string]$JavaHome      = $env:JAVA_HOME
)

$ErrorActionPreference = "Stop"

$AppName   = "Uno"
$MainClass = "application.Main"
$Platform  = "windows-x64"
$Target    = "windows-x64"

$ScriptDir  = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectDir = Split-Path -Parent $ScriptDir      # ...\Java
Set-Location $ProjectDir

$Version = $Version -replace '^v', ''

# Resolve a JDK 26 toolchain. In CI this is JAVA_HOME (setup-java); locally fall
# back to a JavaFX-capable JDK 26 if JAVA_HOME is unset or points at an old JDK.
if (-not $JavaHome -or -not (Test-Path (Join-Path $JavaHome "bin\jpackage.exe"))) {
    foreach ($candidate in @(
        "C:\Program Files\BellSoft\LibericaJDK-26-Full",
        "C:\Program Files\Java\jdk-26"
    )) {
        if (Test-Path (Join-Path $candidate "bin\jpackage.exe")) { $JavaHome = $candidate; break }
    }
}
if (-not $JavaHome) { throw "No JDK found. Set JAVA_HOME to a JDK 26 with jpackage." }

$Javac    = Join-Path $JavaHome "bin\javac.exe"
$Jar      = Join-Path $JavaHome "bin\jar.exe"
$JPackage = Join-Path $JavaHome "bin\jpackage.exe"

$BuildDir   = Join-Path $ProjectDir "build"
$Classes    = Join-Path $BuildDir "classes"
$InputDir   = Join-Path $BuildDir "package-input"
$ReleaseDir = Join-Path $ProjectDir "release"
$DistDir    = Join-Path $ProjectDir "dist"
$FxDir      = Join-Path $BuildDir "javafx"

Remove-Item $Classes, $InputDir, $ReleaseDir, $DistDir -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path $Classes, $InputDir, $ReleaseDir, $DistDir, $FxDir | Out-Null

# 1. Download the JavaFX SDK (for compiling) and jmods (for the bundled runtime).
$SdkZip   = Join-Path $FxDir "javafx-sdk.zip"
$JmodsZip = Join-Path $FxDir "javafx-jmods.zip"
$SdkUrl   = "https://download2.gluonhq.com/openjfx/$JavaFxVersion/openjfx-${JavaFxVersion}_${Platform}_bin-sdk.zip"
$JmodsUrl = "https://download2.gluonhq.com/openjfx/$JavaFxVersion/openjfx-${JavaFxVersion}_${Platform}_bin-jmods.zip"

if (-not (Test-Path $SdkZip))   { Invoke-WebRequest -Uri $SdkUrl   -OutFile $SdkZip }
if (-not (Test-Path $JmodsZip)) { Invoke-WebRequest -Uri $JmodsUrl -OutFile $JmodsZip }

$SdkExtract   = Join-Path $FxDir "sdk"
$JmodsExtract = Join-Path $FxDir "jmods"
Remove-Item $SdkExtract, $JmodsExtract -Recurse -Force -ErrorAction SilentlyContinue
Expand-Archive -Path $SdkZip   -DestinationPath $SdkExtract   -Force
Expand-Archive -Path $JmodsZip -DestinationPath $JmodsExtract -Force

$FxSdkLib = (Get-ChildItem $SdkExtract   -Directory -Filter "javafx-sdk-*"   | Select-Object -First 1).FullName + "\lib"
$FxJmods  = (Get-ChildItem $JmodsExtract -Directory -Filter "javafx-jmods-*" | Select-Object -First 1).FullName

# 2. Compile the app against the JavaFX SDK.
$Sources = Get-ChildItem -Recurse -Filter *.java (Join-Path $ProjectDir "src\application") | ForEach-Object { $_.FullName }
& $Javac --module-path $FxSdkLib --add-modules javafx.controls -d $Classes $Sources
if ($LASTEXITCODE -ne 0) { throw "javac failed" }

Copy-Item (Join-Path $ProjectDir "src\resources") (Join-Path $Classes "resources") -Recurse

# 3. Thin runnable jar (app classes + resources only; JavaFX comes from the runtime).
& $Jar --create --file (Join-Path $InputDir "$AppName.jar") --main-class $MainClass -C $Classes .
if ($LASTEXITCODE -ne 0) { throw "jar failed" }

# 4. Package. --module-path/--add-modules bake JavaFX into the generated runtime;
#    --java-options add it as a root module at launch and silence native-access noise.
& $JPackage `
    --type $PackageType `
    --name $AppName `
    --input $InputDir `
    --main-jar "$AppName.jar" `
    --main-class $MainClass `
    --module-path $FxJmods `
    --add-modules javafx.controls,java.desktop,java.logging,java.net.http `
    --java-options "--add-modules=javafx.controls" `
    --java-options "--enable-native-access=javafx.graphics" `
    --app-version $Version `
    --win-menu `
    --win-shortcut `
    --icon (Join-Path $ProjectDir "src\resources\uno.ico") `
    --dest $DistDir
if ($LASTEXITCODE -ne 0) { throw "jpackage failed" }

# 5. Copy to release/ with a platform-tagged name (matches package-unix.sh output).
foreach ($package in Get-ChildItem $DistDir -File) {
    $extension = $package.Extension.TrimStart('.')
    Copy-Item $package.FullName (Join-Path $ReleaseDir "$AppName-$Version-$Target.$extension")
}

Write-Output "Done. Packages in: $ReleaseDir"
Get-ChildItem $ReleaseDir | Select-Object Name, Length
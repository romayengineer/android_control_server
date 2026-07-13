param(
    [ValidateSet("debug", "release")]
    [string]$BuildType = "debug",
    [switch]$Install,
    [switch]$Clean
)

# Setup environment variables
Write-Host "=== Android Control Server Build Script ===" -ForegroundColor Cyan
Write-Host "Configuring environment..." -ForegroundColor Yellow

$JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.17.10-hotspot"
$ANDROID_SDK_ROOT = "$env:USERPROFILE\AppData\Local\Android\Sdk"
$PROJECT_DIR = Split-Path -Parent $PSCommandPath

# Validate Java installation
if (!(Test-Path "$JAVA_HOME\bin\java.exe")) {
    Write-Host "ERROR: Java not found at $JAVA_HOME" -ForegroundColor Red
    exit 1
}

# Validate Android SDK installation
if (!(Test-Path "$ANDROID_SDK_ROOT\platforms")) {
    Write-Host "ERROR: Android SDK not found at $ANDROID_SDK_ROOT" -ForegroundColor Red
    exit 1
}

# Set environment variables for this session
$env:JAVA_HOME = $JAVA_HOME
$env:ANDROID_SDK_ROOT = $ANDROID_SDK_ROOT
$env:ANDROID_HOME = $ANDROID_SDK_ROOT
$env:PATH = "$JAVA_HOME\bin;$ANDROID_SDK_ROOT\platform-tools;$env:PATH"

Write-Host "[OK] JAVA_HOME: $JAVA_HOME" -ForegroundColor Green
Write-Host "[OK] ANDROID_SDK_ROOT: $ANDROID_SDK_ROOT" -ForegroundColor Green
Write-Host "[OK] Project Directory: $PROJECT_DIR" -ForegroundColor Green

# Navigate to project directory
Set-Location $PROJECT_DIR

# Clean if requested
if ($Clean) {
    Write-Host ""
    Write-Host "Cleaning build artifacts..." -ForegroundColor Yellow
    & ".\gradlew.bat" clean
    if ($LASTEXITCODE -ne 0) {
        Write-Host "ERROR: Clean failed" -ForegroundColor Red
        exit 1
    }
    Write-Host "[OK] Clean completed" -ForegroundColor Green
}

# Build the APK
Write-Host ""
Write-Host "Building APK (type: $BuildType)..." -ForegroundColor Yellow
$startTime = Get-Date

if ($BuildType -eq "debug") {
    & ".\gradlew.bat" assembleDebug
} else {
    & ".\gradlew.bat" assembleRelease
}

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "ERROR: Build failed" -ForegroundColor Red
    exit 1
}

$endTime = Get-Date
$duration = [math]::Round(($endTime - $startTime).TotalSeconds, 1)

# Find the built APK
$apkDir = "$PROJECT_DIR\app\build\outputs\apk\$BuildType"
$apkPath = $null
if (Test-Path $apkDir) {
    $apkPath = Get-ChildItem -Path $apkDir -Filter "app-*.apk" -ErrorAction SilentlyContinue | Select-Object -First 1
}

if ($apkPath) {
    $apkSize = [math]::Round($apkPath.Length / 1MB, 2)
    Write-Host ""
    Write-Host "[OK] Build completed successfully!" -ForegroundColor Green
    Write-Host "     Duration: $duration seconds"
    Write-Host "     APK: $($apkPath.Name)"
    Write-Host "     Size: $apkSize MB"
    Write-Host "     Path: $($apkPath.FullName)"
} else {
    Write-Host ""
    Write-Host "WARNING: Could not locate built APK" -ForegroundColor Yellow
}

# Install if requested
if ($Install) {
    Write-Host ""
    Write-Host "Installing APK to connected device..." -ForegroundColor Yellow

    if ($BuildType -eq "debug") {
        $installTask = "installDebug"
    } else {
        $installTask = "installRelease"
    }

    & ".\gradlew.bat" $installTask

    if ($LASTEXITCODE -eq 0) {
        Write-Host "[OK] APK installed successfully!" -ForegroundColor Green
    } else {
        Write-Host "ERROR: Installation failed" -ForegroundColor Red
        exit 1
    }
}

Write-Host ""
Write-Host "=== Build Complete ===" -ForegroundColor Cyan

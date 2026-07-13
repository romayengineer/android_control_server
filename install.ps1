param(
    [ValidateSet("debug", "release")]
    [string]$BuildType = "debug"
)

# Setup environment variables
$JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.17.10-hotspot"
$ANDROID_SDK_ROOT = "$env:USERPROFILE\AppData\Local\Android\Sdk"
$PROJECT_DIR = Split-Path -Parent $PSCommandPath

# Set environment variables for this session
$env:JAVA_HOME = $JAVA_HOME
$env:ANDROID_SDK_ROOT = $ANDROID_SDK_ROOT
$env:ANDROID_HOME = $ANDROID_SDK_ROOT
$env:PATH = "$JAVA_HOME\bin;$ANDROID_SDK_ROOT\platform-tools;$env:PATH"

Write-Host "=== Android Control Server Build & Install ===" -ForegroundColor Cyan
Write-Host ""

# Step 1: Build the project
Write-Host "Step 1: Building project..." -ForegroundColor Yellow
& "$PROJECT_DIR\build.ps1" -BuildType $BuildType

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "ERROR: Build failed!" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Step 2: Getting connected devices..." -ForegroundColor Yellow

# Step 2: Get connected devices
$devicesOutput = & adb devices
$connectedDevices = @()
$notReadyDevices = @()
$allDevices = @()

# Parse adb devices output
$lines = $devicesOutput -split "`n" | Where-Object { $_.Trim() -and $_ -notmatch "List of devices" -and $_ -notmatch "^$" }

foreach ($line in $lines) {
    $trimmedLine = $line.Trim()
    if (-not $trimmedLine) { continue }

    # Device ID is everything before the last whitespace, status is the last token
    # This handles device IDs with spaces like "adb-xxx (2)._adb-tls-connect._tcp"
    $lastSpaceIndex = $trimmedLine.LastIndexOf(' ')
    $lastTabIndex = $trimmedLine.LastIndexOf("`t")
    $separatorIndex = [Math]::Max($lastSpaceIndex, $lastTabIndex)

    if ($separatorIndex -gt 0 -and $separatorIndex -lt $trimmedLine.Length - 1) {
        $deviceId = $trimmedLine.Substring(0, $separatorIndex).Trim()
        $status = $trimmedLine.Substring($separatorIndex + 1).Trim()

        if ([string]::IsNullOrWhiteSpace($deviceId) -or [string]::IsNullOrWhiteSpace($status)) { continue }

        $allDevices += @{id=$deviceId; status=$status}

        if ($status -eq "device") {
            $connectedDevices += $deviceId
        } else {
            # Any status other than "device" means it's not ready
            $notReadyDevices += $deviceId
        }
    }
}

# If no connected devices but devices exist, try to connect to them
if ($connectedDevices.Count -eq 0 -and $notReadyDevices.Count -gt 0) {
    Write-Host "[!] Found $($notReadyDevices.Count) device(s) not ready, attempting to connect..." -ForegroundColor Yellow
    Write-Host ""

    foreach ($deviceId in $notReadyDevices) {
        Write-Host "Connecting to: $deviceId" -ForegroundColor Yellow
        & adb connect $deviceId
        Write-Host "Waiting for connection..." -ForegroundColor Gray
        Start-Sleep -Seconds 3
    }

    # Re-check devices after connection attempt
    Write-Host ""
    Write-Host "Re-checking device status..." -ForegroundColor Yellow
    Start-Sleep -Seconds 1

    $devicesOutput = & adb devices
    $connectedDevices = @()
    $notReadyDevices = @()

    $lines = $devicesOutput -split "`n" | Where-Object { $_.Trim() -and $_ -notmatch "List of devices" -and $_ -notmatch "^$" }

    foreach ($line in $lines) {
        $trimmedLine = $line.Trim()
        if (-not $trimmedLine) { continue }

        # Device ID is everything before the last whitespace, status is the last token
        $lastSpaceIndex = $trimmedLine.LastIndexOf(' ')
        $lastTabIndex = $trimmedLine.LastIndexOf("`t")
        $separatorIndex = [Math]::Max($lastSpaceIndex, $lastTabIndex)

        if ($separatorIndex -gt 0 -and $separatorIndex -lt $trimmedLine.Length - 1) {
            $deviceId = $trimmedLine.Substring(0, $separatorIndex).Trim()
            $status = $trimmedLine.Substring($separatorIndex + 1).Trim()

            if ([string]::IsNullOrWhiteSpace($deviceId) -or [string]::IsNullOrWhiteSpace($status)) { continue }

            if ($status -eq "device") {
                Write-Host "[OK] Device connected: $deviceId" -ForegroundColor Green
                $connectedDevices += $deviceId
            } else {
                Write-Host "[!] Device not ready: $deviceId (status: $status)" -ForegroundColor Yellow
                $notReadyDevices += $deviceId
            }
        }
    }
    Write-Host ""
}

if ($connectedDevices.Count -eq 0) {
    Write-Host ""
    Write-Host "ERROR: No connected devices found!" -ForegroundColor Red
    Write-Host "Please connect an Android device via USB or WiFi" -ForegroundColor Yellow

    if ($allDevices.Count -gt 0) {
        Write-Host ""
        Write-Host "Available devices:" -ForegroundColor Yellow
        foreach ($device in $allDevices) {
            Write-Host "  - $($device.id) [Status: $($device.status)]"
        }
        Write-Host ""
        Write-Host "Tip: Check device screen for authorization prompts" -ForegroundColor Cyan
    }
    exit 1
}

if ($connectedDevices.Count -gt 1) {
    Write-Host ""
    Write-Host "WARNING: Multiple devices found:" -ForegroundColor Yellow
    foreach ($device in $connectedDevices) {
        Write-Host "  - $device"
    }
    Write-Host "Installing to first device: $($connectedDevices[0])" -ForegroundColor Yellow
}

$deviceId = $connectedDevices[0]

Write-Host "[OK] Device ready: $deviceId" -ForegroundColor Green
Write-Host ""

# Step 3: Install the APK
Write-Host "Step 3: Installing APK to device..." -ForegroundColor Yellow

$apkPath = "$PROJECT_DIR\app\build\outputs\apk\$BuildType\app-$BuildType.apk"

if (!(Test-Path $apkPath)) {
    Write-Host ""
    Write-Host "ERROR: APK not found at: $apkPath" -ForegroundColor Red
    exit 1
}

Write-Host "APK: $(Split-Path -Leaf $apkPath)" -ForegroundColor Gray
Write-Host "Device: $deviceId" -ForegroundColor Gray
Write-Host ""

& adb -s $deviceId install -r $apkPath

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "=== Installation Complete ===" -ForegroundColor Cyan
    Write-Host "[OK] App successfully installed to $deviceId" -ForegroundColor Green
    Write-Host ""

    # Step 4: Launch the app
    Write-Host "Step 4: Launching app..." -ForegroundColor Yellow
    & adb -s $deviceId shell am start -n "com.romayengineer.controlserver/.MainActivity"

    if ($LASTEXITCODE -eq 0) {
        Write-Host "[OK] App launched successfully!" -ForegroundColor Green
        Write-Host ""
        Write-Host "=== Setup Complete ===" -ForegroundColor Cyan
        Write-Host ""
        Write-Host "App is now running on your device!" -ForegroundColor Green
        Write-Host ""
        Write-Host "Next steps:" -ForegroundColor Cyan
        Write-Host "1. Check your device screen - the app should be open"
        Write-Host "2. Note the IP address displayed in the app"
        Write-Host "3. Enable AccessibilityService in Settings → Accessibility (optional but recommended)"
        Write-Host "4. Connect from your PC with: ./mouse_event.sh <ip> <event_type> <x> <y> [button]"
        Write-Host ""
        Write-Host "Example:" -ForegroundColor Gray
        Write-Host "  ./mouse_event.sh 192.168.1.100 move 500 300" -ForegroundColor Gray
        Write-Host "  ./mouse_event.sh 192.168.1.100 click 500 300 LEFT" -ForegroundColor Gray
    } else {
        Write-Host ""
        Write-Host "WARNING: Installation successful but could not launch app" -ForegroundColor Yellow
        Write-Host "Please open the WiFi Mouse Server app manually on your device" -ForegroundColor Yellow
        Write-Host ""
        Write-Host "Troubleshooting app launch:" -ForegroundColor Yellow
        Write-Host "• Check device is still connected: adb devices"
        Write-Host "• Try opening the app manually on the device"
    }
} else {
    Write-Host ""
    Write-Host "ERROR: Installation failed!" -ForegroundColor Red
    Write-Host ""
    Write-Host "Troubleshooting:" -ForegroundColor Yellow
    Write-Host "• Check device is properly connected: adb devices"
    Write-Host "• Enable USB Debugging on the device"
    Write-Host "• Grant any permission prompts on the device screen"
    exit 1
}

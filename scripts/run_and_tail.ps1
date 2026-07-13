param(
    [ValidateSet("debug", "release")]
    [string]$BuildType = "debug"
)

$ADB = "C:\Users\Admin\AppData\Local\Android\Sdk\platform-tools\adb.exe"

Write-Host "=== Build, Install and Monitor Logs ===" -ForegroundColor Cyan
Write-Host ""

# Step 1: Build and install
Write-Host "Step 1: Building and installing app..." -ForegroundColor Yellow
$scriptPath = Split-Path -Parent $PSCommandPath
& "$scriptPath\install.ps1" -BuildType $BuildType

if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Installation failed!" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Step 2: Monitoring logs for 15 seconds..." -ForegroundColor Yellow
Write-Host "Click Start button or send a command NOW..." -ForegroundColor Cyan
Write-Host ""

# Clear logcat buffer
& $ADB logcat -c

# Start tailing logs with timeout
$endTime = (Get-Date).AddSeconds(15)

while ((Get-Date) -lt $endTime) {
    try {
        $process = Start-Process -FilePath $ADB -ArgumentList "logcat", "-v", "threadtime" `
                                 -RedirectStandardOutput $env:TEMP\logcat.txt `
                                 -PassThru -NoNewWindow -ErrorAction SilentlyContinue

        if ($process) {
            # Give it time to collect some logs
            Start-Sleep -Milliseconds 100

            # Read and display filtered logs
            if (Test-Path $env:TEMP\logcat.txt) {
                $logs = Get-Content $env:TEMP\logcat.txt |
                        Where-Object { $_ -match 'WiFiMouseService|ServerSocket|controlserver|romay|LogManager|PermissionChecker|AccessibilityService|Accessibility' }

                if ($logs) {
                    $logs | ForEach-Object { Write-Host $_ }
                }

                Remove-Item $env:TEMP\logcat.txt -Force -ErrorAction SilentlyContinue
            }

            if ($process.Id) {
                Stop-Process -Id $process.Id -Force -ErrorAction SilentlyContinue
            }
        }

        Start-Sleep -Seconds 1
    }
    catch {
        # Continue on error
    }
}

Write-Host ""
Write-Host "=== Log Monitoring Complete ===" -ForegroundColor Cyan
Write-Host "Last 20 lines of logcat:" -ForegroundColor Yellow
Write-Host ""

# Show final log snapshot
& $ADB logcat -d -v threadtime |
    Where-Object { $_ -match 'WiFiMouseService|ServerSocket|controlserver|romay|LogManager|PermissionChecker|AccessibilityService|Accessibility' } |
    Select-Object -Last 20 |
    ForEach-Object { Write-Host $_ }

Write-Host ""
Write-Host "Done!" -ForegroundColor Green

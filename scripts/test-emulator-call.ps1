param([string]$Serial = 'emulator-5554', [switch]$AutoModes, [switch]$LockedScreen,
    [ValidateSet('BUTTONS','VERTICAL_SWIPE','HORIZONTAL_SLIDE')][string]$IncomingStyle = 'BUTTONS')

$ErrorActionPreference = 'Stop'
if ($Serial -notmatch '^emulator-\d+$') { throw 'This test only supports a disposable Android emulator.' }
if ($AutoModes -and $LockedScreen) { throw 'Run automatic-mode and locked-screen checks separately.' }

$dialerRoot = Split-Path -Parent $PSScriptRoot
$dialerSdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { Join-Path $env:LOCALAPPDATA 'Android/Sdk' }
$dialerAdb = Join-Path $dialerSdk 'platform-tools/adb.exe'
if (-not (Test-Path -LiteralPath $dialerAdb)) { throw 'Set ANDROID_HOME to your Android SDK directory.' }

Push-Location $dialerRoot
try {
    & ./gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest --console=plain
    if ($LASTEXITCODE -ne 0) { throw 'Instrumentation build failed.' }
    & $dialerAdb -s $Serial install -r app/build/outputs/apk/debug/app-debug.apk
    if ($LASTEXITCODE -ne 0) { throw 'App install failed.' }
    & $dialerAdb -s $Serial install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
    if ($LASTEXITCODE -ne 0) { throw 'Test install failed.' }

    $dialerPreviousRoles = @(& $dialerAdb -s $Serial shell cmd role get-role-holders android.app.role.DIALER)
    $dialerOutput = Join-Path $dialerRoot 'build/simulated-call-test.log'
    $dialerError = Join-Path $dialerRoot 'build/simulated-call-test.err'
    $dialerTestClass = if ($LockedScreen) { 'info.pakkudev.simple_dialer.EmulatorLockedCallTest' } elseif ($AutoModes) { 'info.pakkudev.simple_dialer.EmulatorAutomaticCallsTest' } else { 'info.pakkudev.simple_dialer.EmulatorCallFlowTest' }
    $dialerTestPinSet = $false
    $dialerPreviousLockDisabled = $null
    $dialerPreviousTimeout = $null
    try {
        & $dialerAdb -s $Serial shell cmd role add-role-holder android.app.role.DIALER info.pakkudev.simple_dialer
        if ($LASTEXITCODE -ne 0) { throw 'Could not select the test dialer role.' }
        $dialerEmulatorSdk = [int]((& $dialerAdb -s $Serial shell getprop ro.build.version.sdk).Trim())
        if ($dialerEmulatorSdk -ge 33) {
            & $dialerAdb -s $Serial shell pm grant info.pakkudev.simple_dialer android.permission.POST_NOTIFICATIONS
        }
        if ($LockedScreen) {
            if ((& $dialerAdb -s $Serial shell getprop ro.kernel.qemu).Trim() -ne '1') { throw 'Locked-screen checks require a disposable emulator.' }
            $dialerPreviousLockDisabled = (& $dialerAdb -s $Serial shell locksettings get-disabled).Trim()
            $dialerPreviousTimeout = (& $dialerAdb -s $Serial shell settings get system screen_off_timeout).Trim()
            & $dialerAdb -s $Serial shell locksettings set-pin 8426
            if ($LASTEXITCODE -ne 0) { throw 'Use a disposable emulator with no existing lock credential.' }
            $dialerTestPinSet = $true
            & $dialerAdb -s $Serial shell settings put system screen_off_timeout 1000
            & $dialerAdb -s $Serial emu sensor set proximity 5
            & $dialerAdb -s $Serial shell input keyevent KEYCODE_SLEEP
            & $dialerAdb -s $Serial shell rm -f /sdcard/Android/data/info.pakkudev.simple_dialer/files/locked-call-ready
            Start-Sleep -Seconds 2
        }
        $dialerTestProcess = Start-Process -FilePath $dialerAdb -WindowStyle Hidden -PassThru `
            -ArgumentList @('-s', $Serial, 'shell', 'am', 'instrument', '-w', '-e', 'class', $dialerTestClass, '-e', 'simulatedCall', 'true', '-e', 'incomingStyle', $IncomingStyle, '-e', 'captureScreens', 'true', 'info.pakkudev.simple_dialer.test/androidx.test.runner.AndroidJUnitRunner') `
            -RedirectStandardOutput $dialerOutput -RedirectStandardError $dialerError
        if ($LockedScreen) {
            $dialerReadyDeadline = [DateTime]::UtcNow.AddSeconds(25)
            while ($true) {
                & $dialerAdb -s $Serial shell test -f /sdcard/Android/data/info.pakkudev.simple_dialer/files/locked-call-ready
                if ($LASTEXITCODE -eq 0) { break }
                if ($dialerTestProcess.HasExited -or [DateTime]::UtcNow -ge $dialerReadyDeadline) { throw 'Locked-screen test did not become ready. See build/simulated-call-test.log.' }
                Start-Sleep -Milliseconds 250
            }
        } else { Start-Sleep -Seconds 3 }
        if ($AutoModes) { & $dialerAdb -s $Serial emu sensor set proximity 5 }
        & $dialerAdb -s $Serial emu gsm call 5550100
        if ($AutoModes) {
            Start-Sleep -Seconds 2
            & $dialerAdb -s $Serial emu sensor set proximity 0
            Start-Sleep -Seconds 2
            & $dialerAdb -s $Serial emu sensor set proximity 5
        }
        if (-not $dialerTestProcess.WaitForExit(45000)) { throw 'The simulated call test timed out.' }
        $dialerResult = Get-Content -Raw -LiteralPath $dialerOutput
        Write-Output $dialerResult
        if ($dialerResult -notmatch 'OK \(1 test\)') { throw 'The simulated call test failed. See build/simulated-call-test.log.' }
    } finally {
        if ($AutoModes) { & $dialerAdb -s $Serial emu sensor set proximity 5 }
        & $dialerAdb -s $Serial emu gsm cancel 5550100
        & $dialerAdb -s $Serial shell cmd role remove-role-holder android.app.role.DIALER info.pakkudev.simple_dialer
        foreach ($dialerPreviousRole in $dialerPreviousRoles) {
            if ($dialerPreviousRole.Trim()) {
                & $dialerAdb -s $Serial shell cmd role add-role-holder android.app.role.DIALER $dialerPreviousRole.Trim()
            }
        }
        if ($dialerTestPinSet) {
            & $dialerAdb -s $Serial shell rm -f /sdcard/Android/data/info.pakkudev.simple_dialer/files/locked-call-ready
            & $dialerAdb -s $Serial shell locksettings clear --old 8426
            & $dialerAdb -s $Serial shell locksettings set-disabled $dialerPreviousLockDisabled
            if ($dialerPreviousTimeout -eq 'null') { & $dialerAdb -s $Serial shell settings delete system screen_off_timeout }
            else { & $dialerAdb -s $Serial shell settings put system screen_off_timeout $dialerPreviousTimeout }
            & $dialerAdb -s $Serial shell input keyevent KEYCODE_WAKEUP
            if ($dialerPreviousLockDisabled -eq 'true') { & $dialerAdb -s $Serial shell wm dismiss-keyguard }
        }
    }
} finally { Pop-Location }

param([string]$Serial = 'emulator-5554')
$ErrorActionPreference = 'Stop'
if ($Serial -notmatch '^emulator-\d+$') { throw 'Use a disposable emulator.' }
$dialerProject = Split-Path -Parent $PSScriptRoot
$dialerAdb = Join-Path $env:LOCALAPPDATA 'Android/Sdk/platform-tools/adb.exe'
function Invoke-StoreShell([string]$Command) {
    $result = (& $dialerAdb -s $Serial shell $Command 2>&1) -join "`n"
    if ($LASTEXITCODE -ne 0 -or $result -match 'Error|Exception|\[ERROR\]') { throw $result }
    return $result
}
function Quote-StoreShell([string]$Value) { return "'" + $Value.Replace("'", "'\''") + "'" }
if ((Invoke-StoreShell 'getprop ro.kernel.qemu').Trim() -ne '1') { throw 'Not an emulator.' }
$dialerNight = if ((Invoke-StoreShell 'cmd uimode night') -match 'yes') { 'yes' } else { 'no' }
$dialerDemoAllowed = (Invoke-StoreShell 'settings get global sysui_demo_allowed').Trim()
$dialerFixtures = @(
    @('simple-dialer-debug-ada','Ada Morgan',1),
    @('simple-dialer-debug-ben','Ben Carter',1),
    @('simple-dialer-debug-cara','Cara Wilson',1),
    @('simple-dialer-debug-deniz','Deniz Kaya',1),
    @('simple-dialer-debug-ece','Eva Reed',0),
    @('simple-dialer-store-felix','Felix Brooks',0),
    @('simple-dialer-store-grace','Grace Miller',0),
    @('simple-dialer-store-hannah','Hannah Scott',0),
    @('simple-dialer-store-isaac','Isaac Bennett',0),
    @('simple-dialer-store-julia','Julia Hayes',0),
    @('simple-dialer-store-leo','Leo Parker',0),
    @('simple-dialer-store-maya','Maya Collins',0)
)
Push-Location $dialerProject
try {
    $null = Invoke-StoreShell 'wm size 1080x1920'
    $null = Invoke-StoreShell 'cmd uimode night no'
    $null = Invoke-StoreShell 'settings put global sysui_demo_allowed 1'
    $null = Invoke-StoreShell 'am broadcast -a com.android.systemui.demo -e command enter'
    $null = Invoke-StoreShell 'am broadcast -a com.android.systemui.demo -e command clock -e hhmm 0941'
    $null = Invoke-StoreShell 'am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false'
    $null = Invoke-StoreShell 'am broadcast -a com.android.systemui.demo -e command notifications -e visible false'
    $dialerIndex = 0
    foreach ($fixture in $dialerFixtures) {
        $dialerIndex++
        $sourceWhere = Quote-StoreShell "sourceid='$($fixture[0])' AND deleted=0"
        $existing = Invoke-StoreShell "content query --uri content://com.android.contacts/raw_contacts --projection _id --where $sourceWhere"
        if ($existing -notmatch '_id=(\d+)') {
            $null = Invoke-StoreShell "content insert --uri content://com.android.contacts/raw_contacts --bind account_type:n: --bind account_name:n: --bind sourceid:s:$($fixture[0])"
            $existing = Invoke-StoreShell "content query --uri content://com.android.contacts/raw_contacts --projection _id --where $sourceWhere"
        }
        if ($existing -notmatch '_id=(\d+)') { throw 'Fixture creation failed.' }
        $rawId = $Matches[1]
        $phone = '+1202555' + (100 + $dialerIndex).ToString('0000')
        foreach ($data in @(@('vnd.android.cursor.item/name',$fixture[1]),@('vnd.android.cursor.item/phone_v2',$phone))) {
            $dataWhere = Quote-StoreShell "raw_contact_id=$rawId AND mimetype='$($data[0])'"
            $found = Invoke-StoreShell "content query --uri content://com.android.contacts/data --projection _id --where $dataWhere"
            $binding = Quote-StoreShell "data1:s:$($data[1])"
            if ($found -match '_id=\d+') {
                $null = Invoke-StoreShell "content update --uri content://com.android.contacts/data --bind $binding --where $dataWhere"
            } else {
                $null = Invoke-StoreShell "content insert --uri content://com.android.contacts/data --bind raw_contact_id:i:$rawId --bind mimetype:s:$($data[0]) --bind $binding"
            }
        }
        $null = Invoke-StoreShell "content update --uri content://com.android.contacts/raw_contacts --bind starred:i:$($fixture[2]) --where _id=$rawId"
    }
    if (-not $env:JAVA_HOME) { $env:JAVA_HOME = 'C:/Program Files/Android/Android Studio/jbr' }
    $env:ANDROID_SERIAL = $Serial
    & ./gradlew.bat :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=info.pakkudev.simple_dialer.StoreScreenshotsTest' '-Pandroid.testInstrumentationRunnerArguments.captureStoreScreenshots=true' --console=plain
    if ($LASTEXITCODE -ne 0) { throw 'Screenshot capture failed.' }
    # adb writes successful transfer summaries to stderr on Windows.
    $ErrorActionPreference = 'Continue'
    $pullOutput = & $dialerAdb -s $Serial pull /sdcard/Download/simple-dialer-store/. artwork/store/en-US/screenshots/ 2>&1
    $pullExit = $LASTEXITCODE
    $ErrorActionPreference = 'Stop'
    Write-Output ($pullOutput -join "`n")
    if ($pullExit -ne 0) { throw 'Screenshot download failed.' }
} finally {
    $null = Invoke-StoreShell 'am broadcast -a com.android.systemui.demo -e command exit'
    if ($dialerDemoAllowed -eq 'null') { $null = Invoke-StoreShell 'settings delete global sysui_demo_allowed' }
    else { $null = Invoke-StoreShell "settings put global sysui_demo_allowed $dialerDemoAllowed" }
    $null = Invoke-StoreShell 'wm size reset'
    $null = Invoke-StoreShell "cmd uimode night $dialerNight"
    $cleanupWhere = Quote-StoreShell "sourceid LIKE 'simple-dialer-store-%'"
    $null = Invoke-StoreShell "content delete --uri content://com.android.contacts/raw_contacts --where $cleanupWhere"
    & ./scripts/seed-emulator-contacts.ps1 -Serial $Serial
    Pop-Location
}

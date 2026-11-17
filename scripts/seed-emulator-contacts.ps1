param([string]$Serial = 'emulator-5554')

$ErrorActionPreference = 'Stop'
if ($Serial -notmatch '^emulator-\d+$') { throw 'Choose an Android emulator serial, not a physical device.' }
$dialerSdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { Join-Path $env:LOCALAPPDATA 'Android/Sdk' }
$dialerAdb = Join-Path $dialerSdk 'platform-tools/adb.exe'
if (-not (Test-Path -LiteralPath $dialerAdb)) { throw 'Set ANDROID_HOME to your Android SDK directory.' }
if ((& $dialerAdb -s $Serial shell getprop ro.kernel.qemu).Trim() -ne '1') { throw 'The selected device is not an emulator.' }

function Invoke-DialerContactsCommand([string]$Command) {
    $dialerResult = (& $dialerAdb -s $Serial shell $Command 2>&1) -join "`n"
    if ($LASTEXITCODE -ne 0 -or $dialerResult -match 'Error|Exception|\[ERROR\]') { throw $dialerResult }
    return $dialerResult
}

function ConvertTo-DialerShellLiteral([string]$Value) {
    return "'" + $Value.Replace("'", "'\''") + "'"
}

# Each fictional person has exactly one unique number in the reserved 202-555-01xx range.
# Stable source IDs make reruns update the same five records instead of adding copies.
$dialerFixtures = @(
    @{ Key = 'ada'; Name = 'Debug Ada'; Number = '+1 202 555 0101'; Starred = 1 },
    @{ Key = 'ben'; Name = 'Debug Ben'; Number = '+1 202 555 0102'; Starred = 0 },
    @{ Key = 'cara'; Name = 'Debug Cara'; Number = '+1 202 555 0103'; Starred = 0 },
    @{ Key = 'deniz'; Name = 'Debug Deniz'; Number = '+1 202 555 0104'; Starred = 0 },
    @{ Key = 'ece'; Name = 'Debug Ece'; Number = '+1 202 555 0105'; Starred = 0 }
)
foreach ($dialerFixture in $dialerFixtures) {
    $dialerSource = 'simple-dialer-debug-' + $dialerFixture.Key
    $dialerSourceWhere = ConvertTo-DialerShellLiteral "sourceid='$dialerSource' AND deleted=0"
    $dialerExisting = Invoke-DialerContactsCommand "content query --uri content://com.android.contacts/raw_contacts --projection _id --where $dialerSourceWhere"
    if ($dialerExisting -match '_id=(\d+)') {
        $dialerRawId = $Matches[1]
    } else {
        $null = Invoke-DialerContactsCommand "content insert --uri content://com.android.contacts/raw_contacts --bind account_type:n: --bind account_name:n: --bind sourceid:s:$dialerSource"
        $dialerInserted = Invoke-DialerContactsCommand "content query --uri content://com.android.contacts/raw_contacts --projection _id --where $dialerSourceWhere"
        if ($dialerInserted -notmatch '_id=(\d+)') { throw "Could not find new contact ID: $dialerInserted" }
        $dialerRawId = $Matches[1]
    }
    foreach ($dialerData in @(
        @{ Mime = 'vnd.android.cursor.item/name'; Value = $dialerFixture.Name },
        @{ Mime = 'vnd.android.cursor.item/phone_v2'; Value = $dialerFixture.Number }
    )) {
        $dialerWhere = ConvertTo-DialerShellLiteral "raw_contact_id=$dialerRawId AND mimetype='$($dialerData.Mime)'"
        $dialerExistingData = Invoke-DialerContactsCommand "content query --uri content://com.android.contacts/data --projection _id --where $dialerWhere"
        $dialerBindings = '--bind ' + (ConvertTo-DialerShellLiteral "data1:s:$($dialerData.Value)")
        if ($dialerExistingData -match '_id=\d+') {
            $null = Invoke-DialerContactsCommand "content update --uri content://com.android.contacts/data $dialerBindings --where $dialerWhere"
        } else {
            $null = Invoke-DialerContactsCommand "content insert --uri content://com.android.contacts/data --bind raw_contact_id:i:$dialerRawId --bind mimetype:s:$($dialerData.Mime) $dialerBindings"
        }
    }
    $null = Invoke-DialerContactsCommand "content update --uri content://com.android.contacts/raw_contacts --bind starred:i:$($dialerFixture.Starred) --where _id=$dialerRawId"
    Write-Output "$($dialerFixture.Name): $($dialerFixture.Number)"
}
Write-Output "Five unique fake contacts are ready on $Serial. Debug Ada is starred."

param([switch]$IncludeRelease)

$ErrorActionPreference = 'Stop'
$dialerProjectRoot = Split-Path -Parent $PSScriptRoot

if (-not $env:JAVA_HOME) {
    $dialerStudioJdk = Join-Path $env:ProgramFiles 'Android/Android Studio/jbr'
    if (Test-Path -LiteralPath (Join-Path $dialerStudioJdk 'bin/java.exe')) {
        $env:JAVA_HOME = $dialerStudioJdk
    }
}

$dialerTasks = @(':domain:test', ':platform:testDebugUnitTest', ':data:testDebugUnitTest', ':app:testDebugUnitTest', ':app:assembleDebug', ':app:lintDebug')
if ($IncludeRelease) { $dialerTasks += ':app:assembleRelease' }

Push-Location $dialerProjectRoot
try {
    & python -X utf8 ./scripts/validate_localizations.py
    if ($LASTEXITCODE -ne 0) { throw 'Localization validation failed.' }
    & ./gradlew.bat @dialerTasks --console=plain
    if ($LASTEXITCODE -ne 0) { throw "Gradle checks failed with exit code $LASTEXITCODE." }
    & python -X utf8 ./scripts/verify_native_libraries.py ./app/build/outputs/apk/debug/app-debug.apk
    if ($LASTEXITCODE -ne 0) { throw 'Native library 16 KB layout validation failed.' }
    if ($IncludeRelease) {
        & python -X utf8 ./scripts/verify_native_libraries.py ./app/build/outputs/apk/release/app-release.apk
        if ($LASTEXITCODE -ne 0) { throw 'Release native library 16 KB layout validation failed.' }
    }
} finally {
    Pop-Location
}

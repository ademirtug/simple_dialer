# Launcher and in-app icon assets from one continuous receiver. Requires Windows PowerShell/System.Drawing.
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$dialerRoot = Split-Path -Parent $PSScriptRoot
$dialerEncoding = New-Object System.Text.UTF8Encoding($false)
[xml]$dialerSource = Get-Content -Raw -LiteralPath (Join-Path $dialerRoot 'artwork/launcher/handset.svg')
$dialerHandsetData = $dialerSource.svg.g.path.d
$dialerBackground = '#FFFFFF'
$dialerDefaultPalette = 'ocean'
# Match the app's light-mode primary accents for contrast against white.
$dialerPalettes = [ordered]@{ monochrome = '#202020'; ocean = '#175DB5'; forest = '#256B43'; rose = '#A53465'; sunset = '#915000' }
function Write-DialerIconFile([string]$Relative, [string]$Text) {
    $dialerDestination = Join-Path $dialerRoot $Relative
    [System.IO.Directory]::CreateDirectory((Split-Path -Parent $dialerDestination)) | Out-Null
    [System.IO.File]::WriteAllText($dialerDestination, $Text + [Environment]::NewLine, $dialerEncoding)
}
function New-DialerPath([string]$Data) {
    $dialerPath = [System.Drawing.Drawing2D.GraphicsPath]::new()
    $dialerTokens = [regex]::Matches($Data, '[MCLZ]|-?\d+(?:\.\d+)?')
    $dialerIndex = 0
    $dialerPoint = [System.Drawing.PointF]::new(0, 0)
    while ($dialerIndex -lt $dialerTokens.Count) {
        $dialerCommand = $dialerTokens[$dialerIndex++].Value
        $dialerCount = switch ($dialerCommand) { M { 2 }; L { 2 }; C { 6 }; Z { 0 }; default { throw 'Unsupported SVG command' } }
        $dialerValues = @(for ($dialerOffset = 0; $dialerOffset -lt $dialerCount; $dialerOffset++) {
            [float]::Parse($dialerTokens[$dialerIndex++].Value, [System.Globalization.CultureInfo]::InvariantCulture)
        })
        switch ($dialerCommand) {
            M { $dialerPath.StartFigure(); $dialerPoint = [System.Drawing.PointF]::new($dialerValues[0], $dialerValues[1]) }
            L {
                $dialerNext = [System.Drawing.PointF]::new($dialerValues[0], $dialerValues[1])
                $dialerPath.AddLine($dialerPoint, $dialerNext); $dialerPoint = $dialerNext
            }
            C {
                $dialerNext = [System.Drawing.PointF]::new($dialerValues[4], $dialerValues[5])
                $dialerPath.AddBezier($dialerPoint, [System.Drawing.PointF]::new($dialerValues[0], $dialerValues[1]), [System.Drawing.PointF]::new($dialerValues[2], $dialerValues[3]), $dialerNext)
                $dialerPoint = $dialerNext
            }
            Z { $dialerPath.CloseFigure() }
        }
    }
    return ,$dialerPath
}
function Save-DialerIconPng([string]$Relative, [int]$Size, [string]$Foreground) {
    $dialerPixels = $Size * 4
    $dialerBitmap = [System.Drawing.Bitmap]::new($dialerPixels, $dialerPixels)
    $dialerGraphics = [System.Drawing.Graphics]::FromImage($dialerBitmap)
    $dialerTile = [System.Drawing.Drawing2D.GraphicsPath]::new()
    $dialerDiameter = [float]($dialerPixels * 0.44)
    $dialerFar = [float]($dialerPixels - $dialerDiameter)
    $dialerTile.AddArc(0, 0, $dialerDiameter, $dialerDiameter, 180, 90)
    $dialerTile.AddArc($dialerFar, 0, $dialerDiameter, $dialerDiameter, 270, 90)
    $dialerTile.AddArc($dialerFar, $dialerFar, $dialerDiameter, $dialerDiameter, 0, 90)
    $dialerTile.AddArc(0, $dialerFar, $dialerDiameter, $dialerDiameter, 90, 90)
    $dialerTile.CloseFigure()
    $dialerBackgroundBrush = [System.Drawing.SolidBrush]::new([System.Drawing.ColorTranslator]::FromHtml($dialerBackground))
    $dialerForegroundBrush = [System.Drawing.SolidBrush]::new([System.Drawing.ColorTranslator]::FromHtml($Foreground))
    $dialerHandset = New-DialerPath $dialerHandsetData
    # Adaptive icons expose the central 72/108 of the foreground layer.
    $dialerScale = [float]($dialerPixels / 16.0)
    $dialerTransform = [System.Drawing.Drawing2D.Matrix]::new()
    $dialerTransform.Scale(0.57, 0.57)
    $dialerTransform.Translate([float](12 * 0.43 - 4 - 0.5), [float](12 * 0.43 - 4 + 0.5), [System.Drawing.Drawing2D.MatrixOrder]::Append)
    $dialerTransform.Scale($dialerScale, $dialerScale, [System.Drawing.Drawing2D.MatrixOrder]::Append)
    try {
        $dialerGraphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
        $dialerGraphics.Clear([System.Drawing.Color]::Transparent)
        $dialerGraphics.FillPath($dialerBackgroundBrush, $dialerTile)
        $dialerHandset.Transform($dialerTransform)
        $dialerGraphics.FillPath($dialerForegroundBrush, $dialerHandset)
        $dialerOutput = [System.Drawing.Bitmap]::new($Size, $Size)
        $dialerResizer = [System.Drawing.Graphics]::FromImage($dialerOutput)
        try {
            $dialerResizer.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
            $dialerResizer.DrawImage($dialerBitmap, 0, 0, $Size, $Size)
            $dialerDestination = Join-Path $dialerRoot $Relative
            [System.IO.Directory]::CreateDirectory((Split-Path -Parent $dialerDestination)) | Out-Null
            $dialerOutput.Save($dialerDestination, [System.Drawing.Imaging.ImageFormat]::Png)
        } finally { $dialerResizer.Dispose(); $dialerOutput.Dispose() }
    } finally {
        $dialerGraphics.Dispose(); $dialerBitmap.Dispose(); $dialerTile.Dispose(); $dialerBackgroundBrush.Dispose(); $dialerForegroundBrush.Dispose()
        $dialerHandset.Dispose(); $dialerTransform.Dispose()
    }
}
$dialerColors = @('<resources>', "    <color name=""launcher_background"">$dialerBackground</color>")
foreach ($dialerPalette in $dialerPalettes.GetEnumerator()) {
    $dialerName = $dialerPalette.Key
    $dialerColors += "    <color name=""launcher_$dialerName"">$($dialerPalette.Value)</color>"
    $dialerVector = @"
<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="108dp" android:height="108dp" android:viewportWidth="24" android:viewportHeight="24">
    <group android:pivotX="12" android:pivotY="12" android:scaleX="0.57" android:scaleY="0.57" android:translateX="-0.5" android:translateY="0.5">
        <path android:fillColor="@color/launcher_$dialerName" android:pathData="$dialerHandsetData" />
    </group>
</vector>
"@
    Write-DialerIconFile "app/src/main/res/drawable/ic_launcher_handset_$dialerName.xml" $dialerVector
    if ($dialerName -eq $dialerDefaultPalette) { Write-DialerIconFile 'app/src/main/res/drawable/ic_launcher_handset.xml' $dialerVector }
    foreach ($dialerVersion in @(26,33)) {
        $dialerMono = if ($dialerVersion -eq 33) { '    <monochrome android:drawable="@drawable/ic_launcher_handset" />' } else { '' }
        $dialerAdaptive = @"
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_handset_$dialerName" />
$dialerMono
</adaptive-icon>
"@
        Write-DialerIconFile "app/src/main/res/mipmap-anydpi-v$dialerVersion/ic_launcher_$dialerName.xml" $dialerAdaptive
        if ($dialerName -eq $dialerDefaultPalette) { Write-DialerIconFile "app/src/main/res/mipmap-anydpi-v$dialerVersion/ic_launcher.xml" $dialerAdaptive }
    }
    foreach ($dialerDensity in @(@('mdpi',48),@('hdpi',72),@('xhdpi',96),@('xxhdpi',144),@('xxxhdpi',192))) {
        Save-DialerIconPng "app/src/main/res/mipmap-$($dialerDensity[0])/ic_launcher_$dialerName.png" $dialerDensity[1] $dialerPalette.Value
        if ($dialerName -eq $dialerDefaultPalette) {
            Save-DialerIconPng "app/src/main/res/mipmap-$($dialerDensity[0])/ic_launcher.png" $dialerDensity[1] $dialerPalette.Value
        }
    }
    Save-DialerIconPng "artwork/launcher/$dialerName.png" 256 $dialerPalette.Value
}
$dialerColors += '</resources>'
Write-DialerIconFile 'app/src/main/res/values/launcher_colors.xml' ($dialerColors -join [Environment]::NewLine)
# UI icons use the same receiver without the adaptive launcher's extra padding.
# Keep the 5% size reduction, and let Compose/Android supply the action tint.
foreach ($dialerUiName in @('ic_phone', 'ic_phone_end', 'ic_phone_add')) {
    $dialerUiRotation = if ($dialerUiName -eq 'ic_phone_end') { 125 } else { 0 }
    $dialerUiPlus = if ($dialerUiName -eq 'ic_phone_add') {
        '    <path android:fillColor="#FFFFFF" android:pathData="M18,2 L20,2 L20,5 L23,5 L23,7 L20,7 L20,10 L18,10 L18,7 L15,7 L15,5 L18,5 Z" />'
    } else { '' }
    $dialerUiVector = @"
<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <group android:pivotX="12" android:pivotY="12" android:scaleX="0.95" android:scaleY="0.95" android:rotation="$dialerUiRotation" android:translateX="-0.5" android:translateY="0.5">
        <path android:fillColor="#FFFFFF" android:pathData="$dialerHandsetData" />
    </group>
$dialerUiPlus
</vector>
"@
    Write-DialerIconFile "app/src/main/res/drawable/$dialerUiName.xml" $dialerUiVector
}
Write-Output 'Generated five launcher palettes and call, end-call and add-call icons from the shared receiver; Ocean is the default.'

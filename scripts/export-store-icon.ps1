param(
    [ValidateRange(16, 2048)]
    [int]$Size = 512,
    [string]$Destination
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$dialerProject = Split-Path -Parent $PSScriptRoot
# Reuse the existing receiver parser without executing the launcher-generation script.
$dialerAst = [System.Management.Automation.Language.Parser]::ParseFile((Join-Path $PSScriptRoot 'generate-launcher-icons.ps1'), [ref]$null, [ref]$null)
$dialerFunction = $dialerAst.Find({ param($node) $node -is [System.Management.Automation.Language.FunctionDefinitionAst] -and $node.Name -eq 'New-DialerPath' }, $true)
Invoke-Expression $dialerFunction.Extent.Text
[xml]$dialerSvg = Get-Content -Raw -LiteralPath (Join-Path $dialerProject 'artwork/launcher/handset.svg')
$dialerReceiver = New-DialerPath $dialerSvg.svg.g.path.d
$dialerBitmap = [System.Drawing.Bitmap]::new(2048, 2048, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$dialerGraphics = [System.Drawing.Graphics]::FromImage($dialerBitmap)
$dialerBrush = [System.Drawing.SolidBrush]::new([System.Drawing.ColorTranslator]::FromHtml('#175DB5'))
$dialerTransform = [System.Drawing.Drawing2D.Matrix]::new()
$dialerTransform.Scale(0.57, 0.57)
$dialerTransform.Translate([float](12 * 0.43 - 4 - 0.5), [float](12 * 0.43 - 4 + 0.5), [System.Drawing.Drawing2D.MatrixOrder]::Append)
$dialerTransform.Scale(128, 128, [System.Drawing.Drawing2D.MatrixOrder]::Append)
try {
    $dialerGraphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $dialerGraphics.Clear([System.Drawing.Color]::White)
    $dialerReceiver.Transform($dialerTransform)
    $dialerGraphics.FillPath($dialerBrush, $dialerReceiver)
    $dialerOutput = [System.Drawing.Bitmap]::new($Size, $Size, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $dialerResizer = [System.Drawing.Graphics]::FromImage($dialerOutput)
    try {
        $dialerResizer.Clear([System.Drawing.Color]::White)
        $dialerResizer.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
        $dialerResizer.DrawImage($dialerBitmap, 0, 0, $Size, $Size)
        $dialerDestination = if ($Destination) {
            $ExecutionContext.SessionState.Path.GetUnresolvedProviderPathFromPSPath($Destination)
        } else {
            Join-Path $dialerProject "artwork/store/en-US/app-icon-$Size.png"
        }
        [System.IO.Directory]::CreateDirectory((Split-Path -Parent $dialerDestination)) | Out-Null
        $dialerOutput.Save($dialerDestination, [System.Drawing.Imaging.ImageFormat]::Png)
        Write-Output $dialerDestination
    } finally { $dialerResizer.Dispose(); $dialerOutput.Dispose() }
} finally {
    $dialerGraphics.Dispose(); $dialerBitmap.Dispose(); $dialerBrush.Dispose()
    $dialerTransform.Dispose(); $dialerReceiver.Dispose()
}

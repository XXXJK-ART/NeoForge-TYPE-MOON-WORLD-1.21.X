param(
    [string]$OutputDirectory = (Join-Path $PSScriptRoot "..\src\main\resources\assets\typemoonaddon\textures\entity")
)

Add-Type -AssemblyName System.Drawing

$resolvedOutput = [System.IO.Path]::GetFullPath($OutputDirectory)
[System.IO.Directory]::CreateDirectory($resolvedOutput) | Out-Null

function New-ShadowBitmap([bool]$GlowOnly) {
    $bitmap = [System.Drawing.Bitmap]::new(64, 64, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    for ($y = 0; $y -lt 64; $y++) {
        for ($x = 0; $x -lt 64; $x++) {
            if ($GlowOnly) {
                $color = [System.Drawing.Color]::FromArgb(0, 0, 0, 0)
            } else {
                $grain = (($x * 13 + $y * 7) % 7)
                $color = [System.Drawing.Color]::FromArgb(236, 9 + $grain, 3, 17 + $grain)
            }
            $bitmap.SetPixel($x, $y, $color)
        }
    }
    return $bitmap
}

function Add-PanelEdge($Bitmap, [int]$X, [int]$Y, [int]$Width, [int]$Height) {
    $edge = [System.Drawing.Color]::FromArgb(242, 43, 25, 57)
    for ($column = $X; $column -lt ($X + $Width); $column++) {
        $Bitmap.SetPixel($column, $Y, $edge)
        $Bitmap.SetPixel($column, $Y + $Height - 1, $edge)
    }
    for ($row = $Y; $row -lt ($Y + $Height); $row++) {
        $Bitmap.SetPixel($X, $row, $edge)
        $Bitmap.SetPixel($X + $Width - 1, $row, $edge)
    }
}

$base = New-ShadowBitmap $false
$glow = New-ShadowBitmap $true

Add-PanelEdge $base 0 0 36 20
Add-PanelEdge $base 0 20 44 13
Add-PanelEdge $base 36 0 28 13
Add-PanelEdge $base 36 14 22 9
Add-PanelEdge $base 0 33 26 10
Add-PanelEdge $base 28 33 16 13
Add-PanelEdge $base 44 33 12 11
Add-PanelEdge $base 28 48 14 14

for ($y = 56; $y -lt 59; $y++) {
    for ($x = 52; $x -lt 58; $x++) {
        $distance = [Math]::Abs($x - 54.5) + [Math]::Abs($y - 57)
        if ($distance -le 1.5) {
            $baseColor = [System.Drawing.Color]::FromArgb(255, 240, 218, 255)
            $glowColor = [System.Drawing.Color]::FromArgb(255, 247, 225, 255)
        } else {
            $baseColor = [System.Drawing.Color]::FromArgb(255, 139, 54, 232)
            $glowColor = [System.Drawing.Color]::FromArgb(245, 153, 66, 255)
        }
        $base.SetPixel($x, $y, $baseColor)
        $glow.SetPixel($x, $y, $glowColor)
    }
}

$basePath = Join-Path $resolvedOutput "shadow_familiar.png"
$glowPath = Join-Path $resolvedOutput "shadow_familiar_glow.png"
$outlinePath = Join-Path $resolvedOutput "shadow_familiar_outline.png"
$base.Save($basePath, [System.Drawing.Imaging.ImageFormat]::Png)
$glow.Save($glowPath, [System.Drawing.Imaging.ImageFormat]::Png)
$outline = [System.Drawing.Bitmap]::new(64, 64, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
for ($row = 0; $row -lt 64; $row++) {
    for ($column = 0; $column -lt 64; $column++) {
        $outline.SetPixel($column, $row, [System.Drawing.Color]::FromArgb(255, 238, 232, 246))
    }
}
$outline.Save($outlinePath, [System.Drawing.Imaging.ImageFormat]::Png)
$base.Dispose()
$glow.Dispose()
$outline.Dispose()

Write-Output $basePath
Write-Output $glowPath
Write-Output $outlinePath

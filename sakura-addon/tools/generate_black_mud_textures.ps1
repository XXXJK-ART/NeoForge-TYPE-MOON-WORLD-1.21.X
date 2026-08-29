param(
    [string]$AssetDirectory = (Join-Path $PSScriptRoot "..\src\main\resources\assets\typemoonaddon\textures")
)

Add-Type -AssemblyName System.Drawing

$resolvedAssets = [System.IO.Path]::GetFullPath($AssetDirectory)
$blockDirectory = Join-Path $resolvedAssets "block"
$miscDirectory = Join-Path $resolvedAssets "misc"
$effectDirectory = Join-Path $resolvedAssets "mob_effect"
[System.IO.Directory]::CreateDirectory($blockDirectory) | Out-Null
[System.IO.Directory]::CreateDirectory($miscDirectory) | Out-Null
[System.IO.Directory]::CreateDirectory($effectDirectory) | Out-Null

function New-BlackMudAnimation {
    param(
        [int]$Size,
        [int]$FrameCount,
        [double]$FlowStretch,
        [string]$OutputPath
    )

    $bitmap = [System.Drawing.Bitmap]::new(
        $Size,
        $Size * $FrameCount,
        [System.Drawing.Imaging.PixelFormat]::Format32bppArgb
    )
    for ($frame = 0; $frame -lt $FrameCount; $frame++) {
        $phase = 2.0 * [Math]::PI * $frame / $FrameCount
        for ($y = 0; $y -lt $Size; $y++) {
            for ($x = 0; $x -lt $Size; $x++) {
                $sampleX = 16.0 * $x / $Size
                $sampleY = 16.0 * $y / $Size
                $broad = [Math]::Sin($sampleX * 0.52 + $sampleY * 0.31 * $FlowStretch + $phase)
                $cross = [Math]::Sin($sampleX * -0.27 + $sampleY * 0.76 * $FlowStretch - $phase * 1.35)
                $ripple = [Math]::Cos(($sampleX + $sampleY * $FlowStretch) * 1.13 + $phase * 0.65)
                $vein = [Math]::Max(0.0, ($broad * 0.55 + $cross * 0.32 + $ripple * 0.13) - 0.22)
                $glint = [Math]::Max(0.0, [Math]::Sin($sampleX * 1.7 - $sampleY * 0.9 + $phase * 1.8) - 0.82)
                $redAccent = [Math]::Min(1.0, $vein * 1.7 + $glint * 0.8)
                $red = [int](5 + 65 * $redAccent)
                $green = [int](3 + 8 * $redAccent)
                $blue = [int](3 + 7 * $redAccent)
                $bitmap.SetPixel(
                    $x,
                    $frame * $Size + $y,
                    [System.Drawing.Color]::FromArgb(255, $red, $green, $blue)
                )
            }
        }
    }
    $bitmap.Save($OutputPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $bitmap.Dispose()
}

New-BlackMudAnimation 16 16 1.0 (Join-Path $blockDirectory "black_mud_still.png")
New-BlackMudAnimation 32 16 1.8 (Join-Path $blockDirectory "black_mud_flow.png")

$overlay = [System.Drawing.Bitmap]::new(16, 16, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
for ($y = 0; $y -lt 16; $y++) {
    for ($x = 0; $x -lt 16; $x++) {
        $vein = [Math]::Max(0.0, [Math]::Sin($x * 0.62 + $y * 0.37) - 0.35)
        $overlay.SetPixel(
            $x,
            $y,
            [System.Drawing.Color]::FromArgb(210, [int](4 + 52 * $vein), 1, [int](2 + 5 * $vein))
        )
    }
}
$overlay.Save((Join-Path $miscDirectory "black_mud_overlay.png"), [System.Drawing.Imaging.ImageFormat]::Png)
$overlay.Dispose()

$corruption = [System.Drawing.Bitmap]::new(16, 16, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$effect = [System.Drawing.Bitmap]::new(18, 18, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
for ($y = 0; $y -lt 18; $y++) {
    for ($x = 0; $x -lt 18; $x++) {
        if ($x -lt 16 -and $y -lt 16) {
            $corruption.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(255, 0, 0, 0))
        }
        $dx = $x - 8.5
        $dy = $y - 8.5
        $distance = [Math]::Sqrt($dx * $dx + $dy * $dy)
        if ($distance -le 7.5) {
            $color = if ($distance -ge 5.7) {
                [System.Drawing.Color]::FromArgb(255, 142, 24, 24)
            } else {
                [System.Drawing.Color]::FromArgb(255, 4, 0, 0)
            }
            $effect.SetPixel($x, $y, $color)
        } else {
            $effect.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(0, 0, 0, 0))
        }
    }
}
$corruption.Save((Join-Path $miscDirectory "black_mud_corruption.png"), [System.Drawing.Imaging.ImageFormat]::Png)
$effect.Save((Join-Path $effectDirectory "black_mud_corruption.png"), [System.Drawing.Imaging.ImageFormat]::Png)
$corruption.Dispose()
$effect.Dispose()

Write-Output (Join-Path $blockDirectory "black_mud_still.png")
Write-Output (Join-Path $blockDirectory "black_mud_flow.png")

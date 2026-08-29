param(
    [string]$OutputDirectory = (Join-Path $PSScriptRoot "..\src\main\resources\assets\typemoonaddon\textures\entity")
)

Add-Type -AssemblyName System.Drawing

$resolvedOutput = [System.IO.Path]::GetFullPath($OutputDirectory)
[System.IO.Directory]::CreateDirectory($resolvedOutput) | Out-Null
$base = [System.Drawing.Bitmap]::new(64, 64, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$outline = [System.Drawing.Bitmap]::new(64, 64, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)

for ($y = 0; $y -lt 64; $y++) {
    for ($x = 0; $x -lt 64; $x++) {
        if ($x -lt 32) {
            $base.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(255, 0, 0, 0))
        } else {
            $clothX = $x - 32
            if ($y -le 2) {
                $base.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(255, 8, 5, 10))
            } elseif ($clothX -le 2 -or $clothX -ge 29) {
                $base.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(255, 210, 12, 28))
            } elseif ($clothX -eq 3 -or $clothX -eq 28) {
                $base.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(255, 112, 8, 19))
            } else {
                $base.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(255, 8, 5, 10))
            }
        }
        $outline.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(255, 230, 14, 32))
    }
}

$basePath = Join-Path $resolvedOutput "black_shadow.png"
$outlinePath = Join-Path $resolvedOutput "black_shadow_outline.png"
$obsoleteGlowPath = Join-Path $resolvedOutput "black_shadow_glow.png"
$base.Save($basePath, [System.Drawing.Imaging.ImageFormat]::Png)
$outline.Save($outlinePath, [System.Drawing.Imaging.ImageFormat]::Png)
$base.Dispose()
$outline.Dispose()
if (Test-Path -LiteralPath $obsoleteGlowPath) {
    Remove-Item -LiteralPath $obsoleteGlowPath
}

Write-Output $basePath
Write-Output $outlinePath

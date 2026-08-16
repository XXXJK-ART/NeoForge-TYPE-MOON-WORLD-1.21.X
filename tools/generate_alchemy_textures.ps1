param(
    [string]$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
)

$ErrorActionPreference = 'Stop'

Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName System.IO.Compression.FileSystem

$assetRoot = Join-Path $ProjectRoot 'src/main/resources/assets/typemoonworld/textures'
$itemOut = Join-Path $assetRoot 'item'
$blockOut = Join-Path $assetRoot 'block'
$vanillaRoot = Join-Path $env:TEMP 'tmw_vanilla_textures'
$clientJar = Join-Path $ProjectRoot 'build/jars/extra/client/1.21.1/client-extra.jar'

$vanillaFiles = @(
    'assets/minecraft/textures/item/glass_bottle.png',
    'assets/minecraft/textures/item/redstone.png',
    'assets/minecraft/textures/item/glowstone_dust.png',
    'assets/minecraft/textures/block/redstone_ore.png',
    'assets/minecraft/textures/block/redstone_block.png'
)

function Ensure-VanillaTexture {
    param([string]$RelativePath)

    $target = Join-Path $vanillaRoot $RelativePath
    if (Test-Path -LiteralPath $target) {
        return $target
    }
    if (-not (Test-Path -LiteralPath $clientJar)) {
        throw "Missing vanilla client jar: $clientJar"
    }

    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $target) | Out-Null
    $zip = [System.IO.Compression.ZipFile]::OpenRead($clientJar)
    try {
        $entry = $zip.GetEntry($RelativePath)
        if ($null -eq $entry) {
            throw "Missing texture in vanilla jar: $RelativePath"
        }
        [System.IO.Compression.ZipFileExtensions]::ExtractToFile($entry, $target, $true)
    }
    finally {
        $zip.Dispose()
    }

    return $target
}

function New-BitmapLike {
    param([System.Drawing.Bitmap]$Source)

    return [System.Drawing.Bitmap]::new($Source.Width, $Source.Height, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
}

function Save-Png {
    param(
        [System.Drawing.Bitmap]$Bitmap,
        [string]$Path
    )

    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $Path) | Out-Null
    if (Test-Path -LiteralPath $Path) {
        Remove-Item -LiteralPath $Path -Force
    }
    $Bitmap.Save($Path, [System.Drawing.Imaging.ImageFormat]::Png)
}

function Copy-WithBottleFill {
    param(
        [string]$Name,
        [int]$R,
        [int]$G,
        [int]$B,
        [int]$A = 190
    )

    $source = [System.Drawing.Bitmap]::new((Ensure-VanillaTexture 'assets/minecraft/textures/item/glass_bottle.png'))
    $result = New-BitmapLike $source
    try {
        for ($y = 0; $y -lt $source.Height; $y++) {
            for ($x = 0; $x -lt $source.Width; $x++) {
                $result.SetPixel($x, $y, $source.GetPixel($x, $y))
            }
        }

        $fillRows = @{
            7  = @(7, 8, 9)
            8  = @(6, 7, 8, 9, 10)
            9  = @(5, 6, 7, 8, 9, 10, 11)
            10 = @(5, 6, 7, 8, 9, 10, 11)
            11 = @(5, 6, 7, 8, 9)
            12 = @(5, 6, 7, 8, 9)
            13 = @(6, 7, 8, 9)
        }

        foreach ($row in $fillRows.GetEnumerator()) {
            $y = [int]$row.Key
            foreach ($x in $row.Value) {
                if ($x -lt $source.Width -and $y -lt $source.Height -and $source.GetPixel($x, $y).A -eq 0) {
                    $result.SetPixel($x, $y, [System.Drawing.Color]::FromArgb($A, $R, $G, $B))
                }
            }
        }

        Save-Png $result (Join-Path $itemOut "$Name.png")
    }
    finally {
        $source.Dispose()
        $result.Dispose()
    }
}

function Recolor-Texture {
    param(
        [string]$SourceRelativePath,
        [string]$OutputPath,
        [scriptblock]$MapColor
    )

    $source = [System.Drawing.Bitmap]::new((Ensure-VanillaTexture $SourceRelativePath))
    $result = New-BitmapLike $source
    try {
        for ($y = 0; $y -lt $source.Height; $y++) {
            for ($x = 0; $x -lt $source.Width; $x++) {
                $color = $source.GetPixel($x, $y)
                if ($color.A -eq 0) {
                    $result.SetPixel($x, $y, $color)
                    continue
                }
                $mapped = & $MapColor $color $x $y
                $result.SetPixel($x, $y, $mapped)
            }
        }

        Save-Png $result $OutputPath
    }
    finally {
        $source.Dispose()
        $result.Dispose()
    }
}

function Clamp-Byte {
    param([double]$Value)
    return [Math]::Max(0, [Math]::Min(255, [int][Math]::Round($Value)))
}

function Color-ByBrightness {
    param(
        [System.Drawing.Color]$Color,
        [int]$BaseR,
        [int]$BaseG,
        [int]$BaseB,
        [double]$Contrast = 0.75
    )

    $brightness = (($Color.R + $Color.G + $Color.B) / 3.0) / 255.0
    $shade = 0.55 + (($brightness - 0.45) * $Contrast)
    return [System.Drawing.Color]::FromArgb(
        $Color.A,
        (Clamp-Byte ($BaseR * $shade)),
        (Clamp-Byte ($BaseG * $shade)),
        (Clamp-Byte ($BaseB * $shade))
    )
}

Copy-WithBottleFill 'mercury_bottle' 150 158 164 205
Copy-WithBottleFill 'molten_ruby_bottle' 218 38 42
Copy-WithBottleFill 'molten_sapphire_bottle' 48 92 224
Copy-WithBottleFill 'molten_emerald_bottle' 34 178 82
Copy-WithBottleFill 'molten_topaz_bottle' 232 176 34
Copy-WithBottleFill 'molten_white_gemstone_bottle' 218 238 246 175
Copy-WithBottleFill 'molten_cyan_gemstone_bottle' 32 210 214
Copy-WithBottleFill 'molten_black_shard_bottle' 58 50 72 200

Recolor-Texture 'assets/minecraft/textures/item/redstone.png' (Join-Path $itemOut 'cinnabar.png') {
    param($color, $x, $y)
    Color-ByBrightness $color 205 67 31 0.9
}

Recolor-Texture 'assets/minecraft/textures/item/glowstone_dust.png' (Join-Path $itemOut 'sulfur.png') {
    param($color, $x, $y)
    Color-ByBrightness $color 238 202 64 0.85
}

Recolor-Texture 'assets/minecraft/textures/block/redstone_ore.png' (Join-Path $blockOut 'cinnabar_ore.png') {
    param($color, $x, $y)
    $isOre = $color.R -gt 95 -and $color.R -gt ($color.G * 1.45) -and $color.R -gt ($color.B * 1.45)
    if ($isOre) {
        Color-ByBrightness $color 214 70 30 0.95
    }
    else {
        Color-ByBrightness $color 118 106 96 0.55
    }
}

Recolor-Texture 'assets/minecraft/textures/block/redstone_block.png' (Join-Path $blockOut 'cinnabar_block.png') {
    param($color, $x, $y)
    Color-ByBrightness $color 190 57 31 0.85
}

Write-Host 'Generated alchemy item and block textures.'

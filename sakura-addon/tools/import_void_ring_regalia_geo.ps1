param(
    [Parameter(Mandatory = $true)]
    [string]$ModelSource,
    [Parameter(Mandatory = $true)]
    [string]$TextureSource,
    [switch]$TransparentUnusedUv,
    [string]$ProjectRoot = (Split-Path -Parent $PSScriptRoot)
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$model = Get-Content -Raw -LiteralPath $ModelSource | ConvertFrom-Json
$geometries = @($model.'minecraft:geometry')
if ($geometries.Count -ne 1) {
    throw "Expected one geometry, found $($geometries.Count)"
}

$geometry = $geometries[0]
$requiredBones = @(
    'bipedHead', 'armorHead',
    'bipedBody', 'armorBody',
    'bipedRightArm', 'armorRightArm',
    'bipedLeftArm', 'armorLeftArm',
    'bipedRightLeg', 'armorRightLeg', 'armorRightBoot',
    'bipedLeftLeg', 'armorLeftLeg', 'armorLeftBoot'
)
$sourceBoneNames = @($geometry.bones | ForEach-Object { [string]$_.name })
$missingBones = @($requiredBones | Where-Object { $_ -notin $sourceBoneNames })
if ($missingBones.Count -gt 0) {
    throw "Missing player armor bones: $($missingBones -join ', ')"
}

$cubeCount = (@($geometry.bones) | ForEach-Object {
    if ($null -eq $_.cubes) { 0 } else { @($_.cubes).Count }
} | Measure-Object -Sum).Sum
if ($cubeCount -eq 0) {
    throw 'The supplied geometry contains no cubes'
}

$assetRoot = Join-Path $ProjectRoot 'src\main\resources\assets\typemoonaddon'
$geoDestination = Join-Path $assetRoot 'geo\void_ring_regalia.geo.json'
$textureDestination = Join-Path $assetRoot 'textures\armor\void_ring_regalia.png'
$editableRoot = Join-Path $ProjectRoot 'blockbench'
foreach ($directory in @((Split-Path $geoDestination), (Split-Path $textureDestination), $editableRoot)) {
    New-Item -ItemType Directory -Force -Path $directory | Out-Null
}

& (Join-Path $PSScriptRoot 'convert_geo_armor.ps1') `
    -Source $ModelSource `
    -Output $geoDestination `
    -Identifier 'geometry.typemoonaddon.void_ring_regalia'

$editableModel = Join-Path $editableRoot 'void_ring_regalia_fha_source.geo.json'
$editableTexture = Join-Path $editableRoot 'void_ring_regalia_fha_texture.png'
if ([IO.Path]::GetFullPath($ModelSource) -ne [IO.Path]::GetFullPath($editableModel)) {
    Copy-Item -LiteralPath $ModelSource -Destination $editableModel -Force
}
if ([IO.Path]::GetFullPath($TextureSource) -ne [IO.Path]::GetFullPath($editableTexture)) {
    Copy-Item -LiteralPath $TextureSource -Destination $editableTexture -Force
}

$sourceTexture = [Drawing.Bitmap]::new((Resolve-Path -LiteralPath $TextureSource).Path)
try {
    $textureWidth = [double]$geometry.description.texture_width
    $textureHeight = [double]$geometry.description.texture_height
    if ($textureWidth -le 0 -or $textureHeight -le 0) {
        throw 'Geometry declares an invalid texture canvas'
    }

    $baseTexture = [Drawing.Bitmap]::new(
        $sourceTexture.Width,
        $sourceTexture.Height,
        [Drawing.Imaging.PixelFormat]::Format32bppArgb
    )
    try {
        $graphics = [Drawing.Graphics]::FromImage($baseTexture)
        try {
            $graphics.DrawImageUnscaled($sourceTexture, 0, 0)
        } finally {
            $graphics.Dispose()
        }

        $scaleX = $sourceTexture.Width / $textureWidth
        $scaleY = $sourceTexture.Height / $textureHeight
        $armorBlack = [Drawing.Color]::FromArgb(255, 13, 11, 16)
        $usedUvPixels = [bool[]]::new($baseTexture.Width * $baseTexture.Height)

        function Fill-TransparentUvRect(
            [Drawing.Bitmap]$bitmap,
            [double]$u,
            [double]$v,
            [double]$width,
            [double]$height
        ) {
            $endU = $u + $width
            $endV = $v + $height
            $minX = [Math]::Max(0, [Math]::Floor([Math]::Min($u, $endU) * $scaleX))
            $minY = [Math]::Max(0, [Math]::Floor([Math]::Min($v, $endV) * $scaleY))
            $maxX = [Math]::Min($bitmap.Width, [Math]::Ceiling([Math]::Max($u, $endU) * $scaleX))
            $maxY = [Math]::Min($bitmap.Height, [Math]::Ceiling([Math]::Max($v, $endV) * $scaleY))
            for ($pixelY = $minY; $pixelY -lt $maxY; $pixelY++) {
                for ($pixelX = $minX; $pixelX -lt $maxX; $pixelX++) {
                    $usedUvPixels[($pixelY * $bitmap.Width) + $pixelX] = $true
                    if ($bitmap.GetPixel($pixelX, $pixelY).A -lt 16) {
                        $bitmap.SetPixel($pixelX, $pixelY, $armorBlack)
                    }
                }
            }
        }

        foreach ($bone in @($geometry.bones)) {
            foreach ($cube in @($bone.cubes)) {
                if ($null -eq $cube) {
                    continue
                }
                if ($cube.uv -is [Array]) {
                    $u = [double]$cube.uv[0]
                    $v = [double]$cube.uv[1]
                    $sizeX = [double]$cube.size[0]
                    $sizeY = [double]$cube.size[1]
                    $sizeZ = [double]$cube.size[2]

                    Fill-TransparentUvRect $baseTexture ($u + $sizeZ) $v $sizeX $sizeZ
                    Fill-TransparentUvRect $baseTexture ($u + $sizeZ + $sizeX) $v $sizeX $sizeZ
                    Fill-TransparentUvRect $baseTexture $u ($v + $sizeZ) $sizeZ $sizeY
                    Fill-TransparentUvRect $baseTexture ($u + $sizeZ) ($v + $sizeZ) $sizeX $sizeY
                    Fill-TransparentUvRect $baseTexture ($u + $sizeZ + $sizeX) ($v + $sizeZ) $sizeZ $sizeY
                    Fill-TransparentUvRect $baseTexture ($u + ($sizeZ * 2.0) + $sizeX) ($v + $sizeZ) $sizeX $sizeY
                    continue
                }

                foreach ($faceProperty in $cube.uv.PSObject.Properties) {
                    $face = $faceProperty.Value
                    if ($null -eq $face.uv -or $null -eq $face.uv_size) {
                        continue
                    }
                    Fill-TransparentUvRect `
                        $baseTexture `
                        ([double]$face.uv[0]) `
                        ([double]$face.uv[1]) `
                        ([double]$face.uv_size[0]) `
                        ([double]$face.uv_size[1])
                }
            }
        }

        if ($TransparentUnusedUv) {
            for ($pixelY = 0; $pixelY -lt $baseTexture.Height; $pixelY++) {
                for ($pixelX = 0; $pixelX -lt $baseTexture.Width; $pixelX++) {
                    if (-not $usedUvPixels[($pixelY * $baseTexture.Width) + $pixelX]) {
                        $baseTexture.SetPixel($pixelX, $pixelY, [Drawing.Color]::Transparent)
                    }
                }
            }
        }

        $baseTexture.Save($textureDestination, [Drawing.Imaging.ImageFormat]::Png)
    } finally {
        $baseTexture.Dispose()
    }
} finally {
    $sourceTexture.Dispose()
}

Write-Host "Imported $cubeCount FHA regalia cubes from $ModelSource"
Write-Host "Texture: $textureDestination"

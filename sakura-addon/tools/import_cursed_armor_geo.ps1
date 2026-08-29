param(
    [Parameter(Mandatory = $true)]
    [string]$ModelSource,
    [Parameter(Mandatory = $true)]
    [string]$TextureSource,
    [switch]$AllowPartialGeometry,
    [switch]$CompletePlayerCoverage,
    [switch]$PreserveTexture,
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

$bonesByParent = @{}
foreach ($sourceBone in @($geometry.bones)) {
    $parentName = [string]$sourceBone.parent
    if (-not $bonesByParent.ContainsKey($parentName)) {
        $bonesByParent[$parentName] = [Collections.Generic.List[object]]::new()
    }
    $bonesByParent[$parentName].Add($sourceBone)
}

function Get-SubtreeCubeCount([string]$boneName) {
    $bone = @($geometry.bones | Where-Object { [string]$_.name -eq $boneName })[0]
    $count = @($bone.cubes | Where-Object { $null -ne $_ }).Count
    if ($bonesByParent.ContainsKey($boneName)) {
        foreach ($child in $bonesByParent[$boneName]) {
            $count += Get-SubtreeCubeCount ([string]$child.name)
        }
    }
    return $count
}

function Get-Bone([string]$boneName) {
    return @($geometry.bones | Where-Object { [string]$_.name -eq $boneName })[0]
}

function Set-BoneCubes([object]$bone, [object[]]$cubes) {
    $bone | Add-Member -NotePropertyName cubes -NotePropertyValue @($cubes) -Force
}

function New-ArmorCube(
    [double[]]$origin,
    [double[]]$size,
    [double[]]$uv,
    [double]$inflate
) {
    return [pscustomobject][ordered]@{
        origin = @($origin)
        size = @($size)
        uv = @($uv)
        inflate = $inflate
    }
}

function Copy-MirroredArmCubes([object]$sourceBone, [object]$targetBone) {
    $mirroredCubes = [Collections.Generic.List[object]]::new()
    foreach ($sourceCube in @($sourceBone.cubes)) {
        if ($null -eq $sourceCube) {
            continue
        }
        $cube = $sourceCube | ConvertTo-Json -Depth 30 | ConvertFrom-Json
        $cube.origin[0] = -([double]$cube.origin[0] + [double]$cube.size[0])
        if (@($cube.pivot).Count -eq 3) {
            $cube.pivot[0] = -[double]$cube.pivot[0]
        }
        if (@($cube.rotation).Count -eq 3) {
            $cube.rotation[1] = -[double]$cube.rotation[1]
            $cube.rotation[2] = -[double]$cube.rotation[2]
        }
        $mirrorUv = -not [bool]$cube.mirror
        $cube | Add-Member -NotePropertyName mirror -NotePropertyValue $mirrorUv -Force
        $mirroredCubes.Add($cube)
    }
    Set-BoneCubes $targetBone @($mirroredCubes)
}

if ($CompletePlayerCoverage) {
    $rightArm = Get-Bone 'armorRightArm'
    $leftArm = Get-Bone 'armorLeftArm'
    $rightArmCount = @($rightArm.cubes | Where-Object { $null -ne $_ }).Count
    $leftArmCount = @($leftArm.cubes | Where-Object { $null -ne $_ }).Count
    if ($rightArmCount -eq 0 -and $leftArmCount -gt 0) {
        Copy-MirroredArmCubes $leftArm $rightArm
        Write-Host "Completed armorRightArm by mirroring armorLeftArm"
    } elseif ($leftArmCount -eq 0 -and $rightArmCount -gt 0) {
        Copy-MirroredArmCubes $rightArm $leftArm
        Write-Host "Completed armorLeftArm by mirroring armorRightArm"
    }

    $fallbackCubes = [ordered]@{
        armorRightArm = New-ArmorCube @(-8, 12, -2) @(4, 12, 4) @(64, 96) 0.15
        armorLeftArm = New-ArmorCube @(4, 12, -2) @(4, 12, 4) @(80, 96) 0.15
        armorRightLeg = New-ArmorCube @(-4, 6, -2) @(4, 6, 4) @(0, 96) 0.10
        armorRightBoot = New-ArmorCube @(-4, 0, -2) @(4, 6, 4) @(16, 96) 0.20
        armorLeftLeg = New-ArmorCube @(0, 6, -2) @(4, 6, 4) @(32, 96) 0.10
        armorLeftBoot = New-ArmorCube @(0, 0, -2) @(4, 6, 4) @(48, 96) 0.20
    }
    foreach ($entry in $fallbackCubes.GetEnumerator()) {
        if ((Get-SubtreeCubeCount ([string]$entry.Key)) -eq 0) {
            Set-BoneCubes (Get-Bone ([string]$entry.Key)) @($entry.Value)
            Write-Host "Added fitted fallback geometry below $($entry.Key)"
        }
    }
}

$requiredGeometryRoots = @(
    'armorHead', 'armorBody',
    'armorRightArm', 'armorLeftArm',
    'armorRightLeg', 'armorLeftLeg',
    'armorRightBoot', 'armorLeftBoot'
)
$emptyGeometryRoots = [Collections.Generic.List[string]]::new()
foreach ($rootName in $requiredGeometryRoots) {
    $cubeCount = Get-SubtreeCubeCount $rootName
    Write-Host ("Validated {0}: {1} cube(s)" -f $rootName, $cubeCount)
    if ($cubeCount -eq 0) {
        $emptyGeometryRoots.Add($rootName)
    }
}
if ($emptyGeometryRoots.Count -gt 0) {
    if (-not $AllowPartialGeometry) {
        throw "Armor geometry is empty below: $($emptyGeometryRoots -join ', ')"
    }
    Write-Warning "Keeping intentional empty armor roots: $($emptyGeometryRoots -join ', ')"
}

$assetRoot = Join-Path $ProjectRoot 'src\main\resources\assets\typemoonaddon'
$geoDestination = Join-Path $assetRoot 'geo\cursed_armor.geo.json'
$textureDestination = Join-Path $assetRoot 'textures\armor\cursed_armor.png'
$glowDestination = Join-Path $assetRoot 'textures\armor\cursed_armor_glowmask.png'
$editableRoot = Join-Path $ProjectRoot 'blockbench'
foreach ($directory in @((Split-Path $geoDestination), (Split-Path $textureDestination), $editableRoot)) {
    New-Item -ItemType Directory -Force -Path $directory | Out-Null
}

$outputBones = [Collections.Generic.List[object]]::new()
$pieceIndex = 0
foreach ($sourceBone in @($geometry.bones)) {
    $bone = [ordered]@{}
    foreach ($property in $sourceBone.PSObject.Properties) {
        if ($property.Name -ne 'cubes') {
            $bone[$property.Name] = $property.Value
        }
    }
    $outputBones.Add([pscustomobject]$bone)

    foreach ($cube in @($sourceBone.cubes)) {
        if ($null -eq $cube) {
            continue
        }
        $pieceIndex++
        $centerY = ([double]$cube.origin[1] + ([double]$cube.size[1] / 2.0))
        $piece = [ordered]@{
            name = ('piece_{0:d2}_{1}' -f $pieceIndex, [Math]::Round($centerY * 10.0))
            parent = [string]$sourceBone.name
            pivot = @(0, 0, 0)
            cubes = @($cube)
        }
        $outputBones.Add([pscustomobject]$piece)
    }
}
if ($pieceIndex -eq 0) {
    throw 'The supplied geometry contains no cubes'
}

$geometry.description.identifier = 'geometry.typemoonaddon.cursed_armor'
$geometry.description.visible_bounds_width = [Math]::Max(3.0, [double]$geometry.description.visible_bounds_width)
$geometry.description.visible_bounds_height = [Math]::Max(3.5, [double]$geometry.description.visible_bounds_height)
$geometry.bones = $outputBones

$output = [ordered]@{
    format_version = [string]$model.format_version
    'minecraft:geometry' = @($geometry)
}
$utf8 = [Text.UTF8Encoding]::new($false)
[IO.File]::WriteAllText(
    $geoDestination,
    ($output | ConvertTo-Json -Depth 30),
    $utf8
)

$editableModel = Join-Path $editableRoot 'cursed_armor_fha_source.geo.json'
$editableTexture = Join-Path $editableRoot 'cursed_armor_fha_texture.png'
if ([IO.Path]::GetFullPath($ModelSource) -ne [IO.Path]::GetFullPath($editableModel)) {
    Copy-Item -LiteralPath $ModelSource -Destination $editableModel -Force
}
if ([IO.Path]::GetFullPath($TextureSource) -ne [IO.Path]::GetFullPath($editableTexture)) {
    Copy-Item -LiteralPath $TextureSource -Destination $editableTexture -Force
}

$sourceTexture = [Drawing.Bitmap]::new((Resolve-Path -LiteralPath $TextureSource).Path)
try {
    if ($PreserveTexture -and $TransparentUnusedUv) {
        throw 'PreserveTexture and TransparentUnusedUv cannot be used together'
    }

    $scaleX = $sourceTexture.Width / [double]$geometry.description.texture_width
    $scaleY = $sourceTexture.Height / [double]$geometry.description.texture_height
    $usedUvPixels = [bool[]]::new($sourceTexture.Width * $sourceTexture.Height)

    if ($PreserveTexture) {
        Copy-Item -LiteralPath $TextureSource -Destination $textureDestination -Force
    } else {
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

            # Preserve the black-red source palette while keeping armor planes readable in daylight.
            for ($pixelY = 0; $pixelY -lt $baseTexture.Height; $pixelY++) {
                for ($pixelX = 0; $pixelX -lt $baseTexture.Width; $pixelX++) {
                    $pixel = $baseTexture.GetPixel($pixelX, $pixelY)
                    if ($pixel.A -ge 16 -and $pixel.R -lt 24 -and $pixel.G -lt 24 -and $pixel.B -lt 24) {
                        $baseTexture.SetPixel(
                            $pixelX,
                            $pixelY,
                            [Drawing.Color]::FromArgb($pixel.A, 13, 11, 16)
                        )
                    }
                }
            }

            $armorBlack = [Drawing.Color]::FromArgb(255, 13, 11, 16)

        function Fill-TransparentUvRect(
            [Drawing.Bitmap]$bitmap,
            [double]$u,
            [double]$v,
            [double]$width,
            [double]$height
        ) {
            $minX = [Math]::Max(0, [Math]::Floor($u * $scaleX))
            $minY = [Math]::Max(0, [Math]::Floor($v * $scaleY))
            $maxX = [Math]::Min($bitmap.Width, [Math]::Ceiling(($u + $width) * $scaleX))
            $maxY = [Math]::Min($bitmap.Height, [Math]::Ceiling(($v + $height) * $scaleY))
            for ($pixelY = $minY; $pixelY -lt $maxY; $pixelY++) {
                for ($pixelX = $minX; $pixelX -lt $maxX; $pixelX++) {
                    $usedUvPixels[($pixelY * $bitmap.Width) + $pixelX] = $true
                    if ($bitmap.GetPixel($pixelX, $pixelY).A -lt 16) {
                        $bitmap.SetPixel($pixelX, $pixelY, $armorBlack)
                    }
                }
            }
        }

            foreach ($pieceBone in @($outputBones | Where-Object { $_.name -like 'piece_*' })) {
                $cube = @($pieceBone.cubes)[0]
                $u = [double]$cube.uv[0]
                $v = [double]$cube.uv[1]
                $sizeX = [double]$cube.size[0]
                $sizeY = [double]$cube.size[1]
                $sizeZ = [double]$cube.size[2]

                # Bedrock box UV layout: up/down on the first row, four side faces below it.
                Fill-TransparentUvRect $baseTexture ($u + $sizeZ) $v $sizeX $sizeZ
                Fill-TransparentUvRect $baseTexture ($u + $sizeZ + $sizeX) $v $sizeX $sizeZ
                Fill-TransparentUvRect $baseTexture $u ($v + $sizeZ) $sizeZ $sizeY
                Fill-TransparentUvRect $baseTexture ($u + $sizeZ) ($v + $sizeZ) $sizeX $sizeY
                Fill-TransparentUvRect $baseTexture ($u + $sizeZ + $sizeX) ($v + $sizeZ) $sizeZ $sizeY
                Fill-TransparentUvRect $baseTexture ($u + ($sizeZ * 2.0) + $sizeX) ($v + $sizeZ) $sizeX $sizeY
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
    }

    $glowTexture = [Drawing.Bitmap]::new(
        $sourceTexture.Width,
        $sourceTexture.Height,
        [Drawing.Imaging.PixelFormat]::Format32bppArgb
    )
    try {
        for ($y = 0; $y -lt $sourceTexture.Height; $y++) {
            for ($x = 0; $x -lt $sourceTexture.Width; $x++) {
                $pixel = $sourceTexture.GetPixel($x, $y)
                $redDominance = $pixel.R - [Math]::Max($pixel.G, $pixel.B)
                $isUsedUv = -not $TransparentUnusedUv -or $usedUvPixels[($y * $sourceTexture.Width) + $x]
                if ($isUsedUv -and $pixel.A -ge 16 -and $pixel.R -ge 45 -and $redDominance -ge 15) {
                    $glowTexture.SetPixel($x, $y, $pixel)
                } else {
                    $glowTexture.SetPixel($x, $y, [Drawing.Color]::Transparent)
                }
            }
        }
        $glowTexture.Save($glowDestination, [Drawing.Imaging.ImageFormat]::Png)
    } finally {
        $glowTexture.Dispose()
    }
} finally {
    $sourceTexture.Dispose()
}

Write-Host "Imported $pieceIndex cursed armor pieces from $ModelSource"
Write-Host "Texture: $textureDestination"
Write-Host "Glow mask: $glowDestination"

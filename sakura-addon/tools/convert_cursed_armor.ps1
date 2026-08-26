param(
    [Parameter(Mandatory = $true)]
    [string]$Source,
    [string]$ProjectRoot = (Split-Path -Parent $PSScriptRoot)
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$model = Get-Content -Raw -LiteralPath $Source | ConvertFrom-Json
$assetRoot = Join-Path $ProjectRoot 'src\main\resources\assets\typemoonaddon'
$geoDir = Join-Path $assetRoot 'geo'
$animationDir = Join-Path $assetRoot 'animations'
$textureDir = Join-Path $assetRoot 'textures\armor'
$editableDir = Join-Path $ProjectRoot 'blockbench'
foreach ($directory in @($geoDir, $animationDir, $textureDir, $editableDir)) {
    New-Item -ItemType Directory -Force -Path $directory | Out-Null
}
Copy-Item -LiteralPath $Source -Destination (Join-Path $editableDir 'cursed_armor.bbmodel') -Force

function Save-Texture([object]$texture, [string]$destination, [bool]$emissive) {
    $encoded = $texture.source.Substring($texture.source.IndexOf(',') + 1)
    $stream = [IO.MemoryStream]::new([Convert]::FromBase64String($encoded))
    $sourceImage = [Drawing.Bitmap]::new($stream)
    $target = [Drawing.Bitmap]::new(128, 128, [Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $graphics = [Drawing.Graphics]::FromImage($target)
    $graphics.InterpolationMode = [Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
    $graphics.PixelOffsetMode = [Drawing.Drawing2D.PixelOffsetMode]::Half
    $graphics.DrawImage($sourceImage, 0, 0, 128, 128)
    $graphics.Dispose()
    if ($emissive) {
        for ($y = 0; $y -lt 128; $y++) {
            for ($x = 0; $x -lt 128; $x++) {
                $pixel = $target.GetPixel($x, $y)
                $redDominance = $pixel.R - [Math]::Max($pixel.G, $pixel.B)
                if ($pixel.A -lt 16 -or $pixel.R -lt 45 -or $redDominance -lt 15) {
                    $target.SetPixel($x, $y, [Drawing.Color]::Transparent)
                } else {
                    $target.SetPixel($x, $y, [Drawing.Color]::FromArgb($pixel.A, $pixel.R, $pixel.G, $pixel.B))
                }
            }
        }
    }
    $target.Save($destination, [Drawing.Imaging.ImageFormat]::Png)
    $target.Dispose()
    $sourceImage.Dispose()
    $stream.Dispose()
}

Save-Texture $model.textures[0] (Join-Path $textureDir 'cursed_armor.png') $false
Save-Texture $model.textures[1] (Join-Path $textureDir 'cursed_armor_glowmask.png') $true

$elements = @{}
foreach ($element in $model.elements) { $elements[$element.uuid] = $element }
$groups = @{}
foreach ($group in $model.groups) { $groups[$group.uuid] = $group }
$boneNames = @{
    'bipedHead' = 'armorHead'
    'bipedBody' = 'armorBody'
    'bipedRightArm' = 'armorRightArm'
    'bipedLeftArm' = 'armorLeftArm'
    'bipedRightLeg' = 'armorRightLeg'
    'bipedLeftLeg' = 'armorLeftLeg'
}
$bones = [Collections.Generic.List[object]]::new()
$cubeIndex = 0
$groupIndex = 0

function Add-OutlinerNode([object]$node, [string]$parentName) {
    if ($node -is [string]) {
        if (-not $elements.ContainsKey($node)) { return }
        Add-CubeBone $elements[$node] $parentName
        return
    }
    if ($null -eq $node.uuid -or -not $groups.ContainsKey([string]$node.uuid)) { return }
    $group = $groups[[string]$node.uuid]
    $mapped = $boneNames.ContainsKey([string]$group.name)
    $name = if ($mapped) { $boneNames[[string]$group.name] } else { [string]$group.name }
    if ($mapped -or $name -match '^armor(Head|Body|RightArm|LeftArm|RightLeg|LeftLeg|RightBoot|LeftBoot)$') {
        $normalizedParent = $parentName
        if ($name -match '^armor' -and $parentName -match '^armor') { $name = $parentName }
        if (-not ($bones | Where-Object name -eq $name)) {
            $bone = [ordered]@{ name = $name; pivot = @($group.origin[0], $group.origin[1], $group.origin[2]) }
            if ($normalizedParent -and $normalizedParent -ne $name) { $bone.parent = $normalizedParent }
            $bones.Add([pscustomobject]$bone)
        }
        $parentName = $name
    } else {
        $script:groupIndex++
        $safeName = ([string]$group.name -replace '[^A-Za-z0-9_]', '_')
        $name = "detail_$($script:groupIndex)_$safeName"
        $bone = [ordered]@{
            name = $name
            parent = $parentName
            pivot = @([double]$group.origin[0], [double]$group.origin[1], [double]$group.origin[2])
        }
        if ($group.rotation -and (@($group.rotation) | Where-Object { [Math]::Abs([double]$_) -gt 0.0001 })) {
            $bone.rotation = @([double]$group.rotation[0], [double]$group.rotation[1], [double]$group.rotation[2])
        }
        $bones.Add([pscustomobject]$bone)
        $parentName = $name
    }
    foreach ($child in @($node.children)) { Add-OutlinerNode $child $parentName }
}

function Add-CubeBone([object]$element, [string]$parentName) {
    $script:cubeIndex++
    $origin = @([double]$element.from[0], [double]$element.from[1], [double]$element.from[2])
    $sizeX = [double]$element.to[0] - [double]$element.from[0]
    $sizeY = [double]$element.to[1] - [double]$element.from[1]
    $sizeZ = [double]$element.to[2] - [double]$element.from[2]
    $size = @($sizeX, $sizeY, $sizeZ)
    $uv = if ($null -ne $element.uv_offset) {
        @([double]$element.uv_offset[0], [double]$element.uv_offset[1])
    } elseif ($null -ne $element.faces.north.uv) {
        @([double]$element.faces.north.uv[0], [double]$element.faces.north.uv[1])
    } else {
        @(0, 0)
    }
    $cube = [ordered]@{ origin = $origin; size = $size; uv = $uv }
    if ($element.rotation -and (@($element.rotation) | Where-Object { [Math]::Abs([double]$_) -gt 0.0001 })) {
        $cube.pivot = @([double]$element.origin[0], [double]$element.origin[1], [double]$element.origin[2])
        $cube.rotation = @([double]$element.rotation[0], [double]$element.rotation[1], [double]$element.rotation[2])
    }
    $centerY = ([double]$element.from[1] + [double]$element.to[1]) / 2.0
    $bone = [ordered]@{
        name = ('piece_{0:d2}_{1}' -f $script:cubeIndex, [Math]::Round($centerY * 10))
        parent = $parentName
        pivot = @(0, 0, 0)
        cubes = @([pscustomobject]$cube)
    }
    $bones.Add([pscustomobject]$bone)
}

foreach ($node in $model.outliner) { Add-OutlinerNode $node '' }
if ($cubeIndex -ne $model.elements.Count) {
    throw "Expected $($model.elements.Count) exported cubes, got $cubeIndex"
}

$geometry = [ordered]@{
    format_version = '1.12.0'
    'minecraft:geometry' = @([ordered]@{
        description = [ordered]@{
            identifier = 'geometry.typemoonaddon.cursed_armor'
            texture_width = 128
            texture_height = 128
            visible_bounds_width = 4
            visible_bounds_height = 4
            visible_bounds_offset = @(0, 1.2, 0)
        }
        bones = $bones
    })
}
$utf8 = [Text.UTF8Encoding]::new($false)
$geometryJson = $geometry | ConvertTo-Json -Depth 20
[IO.File]::WriteAllText((Join-Path $geoDir 'cursed_armor.geo.json'), $geometryJson, $utf8)
$animationJson = @{
    format_version = '1.8.0'
    animations = @{ 'animation.cursed_armor.idle' = @{ loop = $true; animation_length = 1.0 } }
} | ConvertTo-Json -Depth 10
[IO.File]::WriteAllText((Join-Path $animationDir 'cursed_armor.animation.json'), $animationJson, $utf8)

Write-Host "Exported $cubeIndex cube bones from $Source"

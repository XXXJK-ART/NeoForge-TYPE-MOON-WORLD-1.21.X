param(
    [Parameter(Mandatory = $true)]
    [string]$Source,
    [string]$ProjectRoot = (Split-Path -Parent $PSScriptRoot)
)

$ErrorActionPreference = 'Stop'
$model = Get-Content -Raw -Encoding UTF8 -LiteralPath $Source | ConvertFrom-Json
$geometries = @($model.'minecraft:geometry')
if ($geometries.Count -ne 1) {
    throw "Expected one geometry, found $($geometries.Count)"
}

$geometry = $geometries[0]
$bones = @($geometry.bones)
$cubes = @($bones | ForEach-Object { @($_.cubes) } | Where-Object { $null -ne $_ })
if ($bones.Count -ne 1 -or $cubes.Count -ne 1) {
    throw "Expected one plane bone and one cube, found $($bones.Count) bone(s) and $($cubes.Count) cube(s)"
}
$textureWidth = [double]$geometry.description.texture_width
$textureHeight = [double]$geometry.description.texture_height
if ($textureWidth -ne 16.0 -or $textureHeight -ne 16.0) {
    throw 'Expected a 16x16 UV canvas'
}

$size = @($cubes[0].size)
$width = if ($size.Count -gt 0) { [double]$size[0] } else { 0.0 }
$height = if ($size.Count -gt 1) { [double]$size[1] } else { 0.0 }
$length = if ($size.Count -gt 2) { [double]$size[2] } else { 0.0 }
if ($size.Count -ne 3 -or [Math]::Abs($width - 2.6) -gt 0.0001 -or [Math]::Abs($height) -gt 0.0001 -or [Math]::Abs($length - 3.8) -gt 0.0001) {
    throw "Expected a flat 2.6x3.8 plane, got $($size -join 'x')"
}

$geometry.description.identifier = 'geometry.typemoonaddon.shadow_art_ribbon'
$bones[0].name = 'ribbon'

$assetRoot = Join-Path $ProjectRoot 'src\main\resources\assets\typemoonaddon'
$destination = Join-Path $assetRoot 'geo\shadow_art_ribbon.geo.json'
$editable = Join-Path $ProjectRoot 'blockbench\shadow_art_connection_3.geo.json'
foreach ($directory in @((Split-Path $destination), (Split-Path $editable))) {
    New-Item -ItemType Directory -Force -Path $directory | Out-Null
}

$utf8 = [Text.UTF8Encoding]::new($false)
[IO.File]::WriteAllText($destination, ($model | ConvertTo-Json -Depth 30), $utf8)
if ([IO.Path]::GetFullPath($Source) -ne [IO.Path]::GetFullPath($editable)) {
    Copy-Item -LiteralPath $Source -Destination $editable -Force
}

Write-Host "Imported exact 2.6x3.8 shadow connection plane from $Source"
Write-Host "Geometry: $destination"

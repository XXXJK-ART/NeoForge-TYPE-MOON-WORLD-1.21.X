param(
    [Parameter(Mandatory = $true)][string]$Source,
    [string]$ProjectRoot = (Split-Path -Parent $PSScriptRoot)
)

$ErrorActionPreference = 'Stop'
$model = Get-Content -Raw -LiteralPath $Source | ConvertFrom-Json
if ($model.elements.Count -ne 1 -or $model.textures.Count -lt 1) {
    throw 'The Shadow Art source must contain the single textured plane used by the runtime ribbon tessellator.'
}

$element = $model.elements[0]
$width = [Math]::Abs([double]$element.to[0] - [double]$element.from[0])
$height = [Math]::Abs([double]$element.to[1] - [double]$element.from[1])
$length = [Math]::Abs([double]$element.to[2] - [double]$element.from[2])
if ($width -le 0 -or $length -le 0 -or $height -gt 0.001) {
    throw "Expected a flat X/Z ribbon plane, got width=$width height=$height length=$length."
}

$texture = $model.textures[0]
if ([int]$texture.width -ne [int]$texture.uv_width -or [int]$texture.height -ne [int]$texture.uv_height) {
    throw 'Texture dimensions and UV dimensions must match before conversion.'
}

$asset = Join-Path $ProjectRoot 'src\main\resources\assets\typemoonaddon'
foreach ($directory in @('geo', 'animations', 'textures\entity')) {
    [System.IO.Directory]::CreateDirectory((Join-Path $asset $directory)) | Out-Null
}
[System.IO.Directory]::CreateDirectory((Join-Path $ProjectRoot 'blockbench')) | Out-Null
Copy-Item -LiteralPath $Source -Destination (Join-Path $ProjectRoot 'blockbench\shadow_art_ribbon.bbmodel') -Force

$sourcePrefix = $texture.source.IndexOf(',')
if ($sourcePrefix -lt 0) { throw 'The source model texture is not embedded.' }
$bytes = [Convert]::FromBase64String($texture.source.Substring($sourcePrefix + 1))
[System.IO.File]::WriteAllBytes((Join-Path $asset 'textures\entity\shadow_art_ribbon.png'), $bytes)

# The runtime bends repeated copies of this exact plane. These files preserve the editable GeckoLib reference geometry.
$geometry = @{
    format_version = '1.12.0'
    'minecraft:geometry' = @(@{
        description = @{
            identifier = 'geometry.typemoonaddon.shadow_art_ribbon'
            texture_width = [int]$texture.uv_width
            texture_height = [int]$texture.uv_height
            visible_bounds_width = [Math]::Max(4.0, $width)
            visible_bounds_height = [Math]::Max(4.0, $length)
            visible_bounds_offset = @(0, 1, 0)
        }
        bones = @(@{
            name = 'ribbon'
            pivot = @(0, 0, 0)
            cubes = @(@{
                origin = @([double]$element.from[0], [double]$element.from[1], [double]$element.from[2])
                size = @($width, 0.001, $length)
                uv = @{
                    up = @{ uv = @(7, 16); uv_size = @(-7, -10) }
                    down = @{ uv = @(7, 6); uv_size = @(-7, 10) }
                }
            })
        })
    })
}
$animation = @{
    format_version = '1.8.0'
    animations = @{
        'animation.shadow_art_ribbon.reference' = @{ loop = $false; animation_length = 0.0 }
    }
}
$utf8 = [System.Text.UTF8Encoding]::new($false)
[System.IO.File]::WriteAllText((Join-Path $asset 'geo\shadow_art_ribbon.geo.json'), ($geometry | ConvertTo-Json -Depth 20), $utf8)
[System.IO.File]::WriteAllText((Join-Path $asset 'animations\shadow_art_ribbon.animation.json'), ($animation | ConvertTo-Json -Depth 20), $utf8)

Write-Host "Exported exact Shadow Art plane: ${width}x${length}, texture $($texture.uv_width)x$($texture.uv_height)"

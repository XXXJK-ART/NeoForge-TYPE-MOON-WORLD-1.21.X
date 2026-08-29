param(
    [Parameter(Mandatory = $true)]
    [string]$Source,
    [Parameter(Mandatory = $true)]
    [string]$Output,
    [Parameter(Mandatory = $true)]
    [string]$Identifier,
    [switch]$SplitIntoPieces
)

$ErrorActionPreference = 'Stop'
$model = Get-Content -LiteralPath $Source -Raw -Encoding UTF8 | ConvertFrom-Json
$geometry = @($model.'minecraft:geometry')
if ($geometry.Count -ne 1) {
    throw "Expected exactly one geometry in $Source"
}

$geometry[0].description.identifier = $Identifier
if ($geometry[0].description.texture_width -ne 128 -or $geometry[0].description.texture_height -ne 128) {
    throw "Expected a 128x128 UV canvas in $Source"
}

if ($SplitIntoPieces) {
    $bones = [Collections.Generic.List[object]]::new()
    $pieceIndex = 0
    $sourceCubeCount = (@($geometry[0].bones) | ForEach-Object {
        if ($null -eq $_.cubes) { 0 } else { @($_.cubes).Count }
    } | Measure-Object -Sum).Sum
    foreach ($bone in @($geometry[0].bones)) {
        $hasCubes = $bone.PSObject.Properties.Name -contains 'cubes'
        $cubes = if ($hasCubes -and $null -ne $bone.cubes) { @($bone.cubes) } else { @() }
        if ($hasCubes) {
            $bone.PSObject.Properties.Remove('cubes')
        }
        $bones.Add($bone)
        foreach ($cube in $cubes) {
            $pieceIndex++
            $centerY = [double]$cube.origin[1] + [double]$cube.size[1] / 2.0
            $bones.Add([pscustomobject][ordered]@{
                name = ('piece_{0:d2}_{1}' -f $pieceIndex, [Math]::Round($centerY * 10))
                parent = [string]$bone.name
                pivot = @(0, 0, 0)
                cubes = @($cube)
            })
        }
    }
    if ($pieceIndex -ne $sourceCubeCount) {
        throw "Expected $sourceCubeCount converted cubes, got $pieceIndex"
    }
    $geometry[0].bones = $bones
}

$outputDirectory = Split-Path -Parent $Output
New-Item -ItemType Directory -Force -Path $outputDirectory | Out-Null
$utf8 = [Text.UTF8Encoding]::new($false)
[IO.File]::WriteAllText(
    $Output,
    ($model | ConvertTo-Json -Depth 30),
    $utf8
)
Write-Host "Converted $Source -> $Output"

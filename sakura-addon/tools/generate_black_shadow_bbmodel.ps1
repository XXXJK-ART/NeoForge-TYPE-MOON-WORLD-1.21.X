param(
    [string]$OutputPath = (Join-Path $PSScriptRoot "..\blockbench\black_shadow.bbmodel")
)

$segments = 24
$clothPanelCount = 6
$bodyPanelSegments = [int]($segments / $clothPanelCount)
$clothPanelSegments = 6
$draggingRingCount = 33
$draggingSplitRing = 12
$draggingGroundRing = 26
$floatingRingCount = 25
$floatingSplitRing = 8
$modelScale = 1.80 / 1.56
$modelHeight = 24.96
$profileY = @(0.0, -1.7, -4.3, -7.8, -12.1, -17.3, -21.1, -21.8, -22.7, -23.5, -24.2, -24.7, -24.9)
$profileRadius = @(3.72, 3.52, 3.43, 3.42, 3.44, 3.47, 3.49, 3.46, 3.30, 2.91, 2.25, 1.45, 0.65)
$vertices = [ordered]@{}
$faces = [ordered]@{}
$vertexIds = @()

function Get-CubicBezier {
    param(
        [double]$Value,
        [double]$Start,
        [double]$Control1,
        [double]$Control2,
        [double]$End
    )
    $inverse = 1.0 - $Value
    return $inverse * $inverse * $inverse * $Start `
        + 3.0 * $inverse * $inverse * $Value * $Control1 `
        + 3.0 * $inverse * $Value * $Value * $Control2 `
        + $Value * $Value * $Value * $End
}

for ($ring = 0; $ring -lt $profileY.Count; $ring++) {
    $ringIds = @()
    for ($side = 0; $side -lt $segments; $side++) {
        $angle = 2.0 * [Math]::PI * $side / $segments
        $id = "00000000-0000-4000-8001-{0:x4}{1:x4}0000" -f $ring, $side
        $ringIds += $id
        $vertices[$id] = @(
            [Math]::Round($profileRadius[$ring] * [Math]::Cos($angle) * $modelScale, 5),
            [Math]::Round(24.0 + $profileY[$ring] * $modelScale, 5),
            [Math]::Round($profileRadius[$ring] * [Math]::Sin($angle) * $modelScale, 5)
        )
    }
    $vertexIds += ,$ringIds
}

for ($ring = 0; $ring -lt ($profileY.Count - 1); $ring++) {
    for ($side = 0; $side -lt $segments; $side++) {
        $nextSide = ($side + 1) % $segments
        $lower0 = $vertexIds[$ring][$side]
        $lower1 = $vertexIds[$ring][$nextSide]
        $upper1 = $vertexIds[$ring + 1][$nextSide]
        $upper0 = $vertexIds[$ring + 1][$side]
        $panelSection = ($side + [int]($bodyPanelSegments / 2)) % $bodyPanelSegments
        $u0 = 32.5 + 31.0 * $panelSection / $bodyPanelSegments
        $u1 = 32.5 + 31.0 * ($panelSection + 1) / $bodyPanelSegments
        $v0 = 64.0 * (1.0 + $profileY[$ring] / $modelHeight)
        $v1 = 64.0 * (1.0 + $profileY[$ring + 1] / $modelHeight)
        $uv = [ordered]@{}
        $uv[$lower0] = @($u0, $v0)
        $uv[$lower1] = @($u1, $v0)
        $uv[$upper1] = @($u1, $v1)
        $uv[$upper0] = @($u0, $v1)
        $faceId = "00000000-0000-4000-8002-{0:x4}{1:x4}0000" -f $ring, $side
        $faces[$faceId] = [ordered]@{
            uv = $uv
            texture = 0
            vertices = @($lower0, $lower1, $upper1, $upper0)
        }
    }
}

$topId = "00000000-0000-4000-8003-000000000000"
$vertices[$topId] = @(0.0, (24.0 - $modelHeight * $modelScale), 0.0)
$lastRing = $profileY.Count - 1
for ($side = 0; $side -lt $segments; $side++) {
    $nextSide = ($side + 1) % $segments
    $edge0 = $vertexIds[$lastRing][$side]
    $edge1 = $vertexIds[$lastRing][$nextSide]
    $panelSection = ($side + [int]($bodyPanelSegments / 2)) % $bodyPanelSegments
    $u0 = 32.5 + 31.0 * $panelSection / $bodyPanelSegments
    $u1 = 32.5 + 31.0 * ($panelSection + 1) / $bodyPanelSegments
    $ringV = 64.0 * (1.0 + $profileY[$lastRing] / $modelHeight)
    $uv = [ordered]@{}
    $uv[$edge0] = @($u0, $ringV)
    $uv[$edge1] = @($u1, $ringV)
    $uv[$topId] = @((($u0 + $u1) * 0.5), 0.0)
    $faceId = "00000000-0000-4000-8004-00000000{0:x4}" -f $side
    $faces[$faceId] = [ordered]@{
        uv = $uv
        texture = 0
        vertices = @($edge0, $edge1, $topId)
    }
}

$meshId = "00000000-0000-4000-8005-000000000000"

function New-ClothLayer {
    param(
        [string]$Name,
        [int]$Color,
        [string]$VertexGroup,
        [string]$FaceGroup,
        [string]$MeshGroup,
        [string]$Style,
        [double]$LayerScale,
        [double]$BaseLift,
        [double]$BaseAngleOffset
    )

    $layerVertices = [ordered]@{}
    $layerFaces = [ordered]@{}
    $layerVertexIds = @()
    $dragging = $Style -eq "dragging"
    $layerRingCount = if ($dragging) { $draggingRingCount } else { $floatingRingCount }
    $layerSplitRing = if ($dragging) { $draggingSplitRing } else { $floatingSplitRing }

    for ($panel = 0; $panel -lt $clothPanelCount; $panel++) {
        $panelRingIds = @()
        $baseCenterAngle = $panel * 2.0 * [Math]::PI / $clothPanelCount

        for ($ring = 0; $ring -lt $layerRingCount; $ring++) {
            if ($ring -le $layerSplitRing) {
                $upperProgress = $ring / [double]$layerSplitRing
                $splitDown = if ($dragging) { 0.5 } else { 1.0 / 3.0 }
                $down = 0.01 + ($splitDown - 0.01) * $upperProgress
                $spread = 0.0
                $motionProgress = 0.0
                $widthScale = 1.0
                $trailProgress = 0.0
            } elseif (-not $dragging) {
                $curveProgress = ($ring - $floatingSplitRing) / [double]($floatingRingCount - 1 - $floatingSplitRing)
                $motionProgress = $curveProgress * $curveProgress * (3.0 - 2.0 * $curveProgress)
                $down = Get-CubicBezier $curveProgress (1.0 / 3.0) 0.49 0.75 0.75
                $spread = Get-CubicBezier $curveProgress $BaseLift $BaseLift 3.80 7.50
                $widthScale = 1.0 + (0.58 - 1.0) * $motionProgress
                $trailProgress = 0.0
            } elseif ($ring -le $draggingGroundRing) {
                $curveProgress = ($ring - $draggingSplitRing) / [double]($draggingGroundRing - $draggingSplitRing)
                $motionProgress = $curveProgress * $curveProgress * (3.0 - 2.0 * $curveProgress)
                $down = Get-CubicBezier $curveProgress 0.5 0.72 1.0 1.0
                $spread = Get-CubicBezier $curveProgress 0.0 0.0 3.80 6.86
                $widthScale = 1.0 + (0.62 - 1.0) * $motionProgress
                $trailProgress = 0.0
            } else {
                $trailProgress = ($ring - $draggingGroundRing) / [double]($draggingRingCount - 1 - $draggingGroundRing)
                $trailProgress = $trailProgress * $trailProgress * (3.0 - 2.0 * $trailProgress)
                $motionProgress = 1.0
                $down = 1.0
                $spread = 6.86 + (11.86 - 6.86) * $trailProgress
                $widthScale = 0.62 + (0.44 - 0.62) * $trailProgress
            }

            $modelY = -$modelHeight * (1.0 - $down)
            $radius = $profileRadius[$profileRadius.Count - 1]
            for ($profileRing = 0; $profileRing -lt ($profileY.Count - 1); $profileRing++) {
                if ($modelY -le $profileY[$profileRing] -and $modelY -ge $profileY[$profileRing + 1]) {
                    $progress = ($modelY - $profileY[$profileRing]) / ($profileY[$profileRing + 1] - $profileY[$profileRing])
                    $radius = $profileRadius[$profileRing] + ($profileRadius[$profileRing + 1] - $profileRadius[$profileRing]) * $progress
                    break
                }
            }

            if ($dragging) {
                $radius += $BaseLift + $spread * $LayerScale
            } else {
                $floatingLift = if ($ring -le $layerSplitRing) { $BaseLift } else { $spread }
                $radius += $floatingLift
            }
            $halfWidth = ([Math]::PI / $clothPanelCount) * $widthScale
            $direction = if (($panel -band 2) -eq 0) { 1.0 } else { -1.0 }
            if ($ring -le $layerSplitRing) {
                $twist = 0.0
            } elseif (-not $dragging) {
                $twist = $direction * 0.22 * [Math]::Sin($motionProgress * [Math]::PI)
            } else {
                $twist = $direction * 0.18 * [Math]::Sin($motionProgress * [Math]::PI * 0.7)
                if ($ring -gt $draggingGroundRing) {
                    $twist += $direction * 0.12 * $trailProgress
                }
            }
            $centerAngle = $baseCenterAngle + $BaseAngleOffset * $motionProgress + $twist
            $ringIds = @()

            for ($section = 0; $section -le $clothPanelSegments; $section++) {
                $sectionProgress = $section / [double]$clothPanelSegments
                $angle = $centerAngle - $halfWidth + 2.0 * $halfWidth * $sectionProgress
                $id = "00000000-0000-4000-$VertexGroup-{0:x2}{1:x2}{2:x2}000000" -f $panel, $ring, $section
                $ringIds += $id
                $layerVertices[$id] = @(
                    [Math]::Round($radius * [Math]::Cos($angle) * $modelScale, 5),
                    [Math]::Round(24.0 + $modelY * $modelScale, 5),
                    [Math]::Round($radius * [Math]::Sin($angle) * $modelScale, 5)
                )
            }
            $panelRingIds += ,$ringIds
        }
        $layerVertexIds += ,$panelRingIds
    }

    for ($panel = 0; $panel -lt $clothPanelCount; $panel++) {
        for ($ring = 0; $ring -lt ($layerRingCount - 1); $ring++) {
            for ($section = 0; $section -lt $clothPanelSegments; $section++) {
                $upper0 = $layerVertexIds[$panel][$ring][$section]
                $upper1 = $layerVertexIds[$panel][$ring][$section + 1]
                $lower1 = $layerVertexIds[$panel][$ring + 1][$section + 1]
                $lower0 = $layerVertexIds[$panel][$ring + 1][$section]
                $u0 = 32.5 + 31.0 * $section / $clothPanelSegments
                $u1 = 32.5 + 31.0 * ($section + 1) / $clothPanelSegments
                $v0 = 64.0 * $ring / ($layerRingCount - 1)
                $v1 = 64.0 * ($ring + 1) / ($layerRingCount - 1)
                $uv = [ordered]@{}
                $uv[$upper0] = @($u0, $v0)
                $uv[$upper1] = @($u1, $v0)
                $uv[$lower1] = @($u1, $v1)
                $uv[$lower0] = @($u0, $v1)
                $faceId = "00000000-0000-4000-$FaceGroup-{0:x2}{1:x2}{2:x2}000000" -f $panel, $ring, $section
                $layerFaces[$faceId] = [ordered]@{
                    uv = $uv
                    texture = 0
                    vertices = @($upper0, $upper1, $lower1, $lower0)
                }
            }
        }
    }

    $layerMeshId = "00000000-0000-4000-$MeshGroup-000000000000"
    return [PSCustomObject]@{
        MeshId = $layerMeshId
        Element = [ordered]@{
            name = $Name
            color = $Color
            locked = $false
            visibility = $true
            export = $true
            origin = @(0, 24, 0)
            vertices = $layerVertices
            faces = $layerFaces
            type = "mesh"
            uuid = $layerMeshId
        }
    }
}

$middleLayer = New-ClothLayer "middle_dragging_cloth" 2 "8010" "8011" "8012" "dragging" 0.72 0.07 ([Math]::PI / $clothPanelCount)
$outerLayer = New-ClothLayer "outer_floating_cloth" 1 "8007" "8008" "8009" "floating" 1.0 0.14 0.0
$bodyElement = [ordered]@{
    name = "black_shadow_cylinder"
    color = 0
    locked = $false
    visibility = $true
    export = $true
    origin = @(0, 24, 0)
    vertices = $vertices
    faces = $faces
    type = "mesh"
    uuid = $meshId
}

function New-NegativeOutlineMesh {
    param(
        [System.Collections.Specialized.OrderedDictionary]$SourceElement,
        [string]$Name,
        [string]$VertexGroup,
        [string]$FaceGroup,
        [string]$MeshGroup
    )

    $outlineExpansion = 0.18 * $modelScale
    $modelCenterY = 24.0 - $modelHeight * $modelScale * 0.5
    $verticalScale = 1.0 + 0.36 / $modelHeight
    $outlineVertices = [ordered]@{}
    $outlineFaces = [ordered]@{}
    $vertexMap = [ordered]@{}
    $vertexIndex = 0

    foreach ($sourceVertex in $SourceElement.vertices.GetEnumerator()) {
        $sourcePosition = $sourceVertex.Value
        $radius = [Math]::Sqrt($sourcePosition[0] * $sourcePosition[0] + $sourcePosition[2] * $sourcePosition[2])
        $radialScale = if ($radius -gt 0.00001) { ($radius + $outlineExpansion) / $radius } else { 1.0 }
        $outlineId = "00000000-0000-4000-$VertexGroup-{0:x12}" -f $vertexIndex
        $vertexMap[$sourceVertex.Key] = $outlineId
        $outlineVertices[$outlineId] = @(
            [Math]::Round($sourcePosition[0] * $radialScale, 5),
            [Math]::Round($modelCenterY + ($sourcePosition[1] - $modelCenterY) * $verticalScale, 5),
            [Math]::Round($sourcePosition[2] * $radialScale, 5)
        )
        $vertexIndex++
    }

    $faceIndex = 0
    foreach ($sourceFace in $SourceElement.faces.GetEnumerator()) {
        $outlineUv = [ordered]@{}
        $outlineFaceVertices = @()
        foreach ($sourceVertexId in $sourceFace.Value.vertices) {
            $outlineVertexId = $vertexMap[$sourceVertexId]
            $outlineFaceVertices += $outlineVertexId
            $outlineUv[$outlineVertexId] = $sourceFace.Value.uv[$sourceVertexId]
        }
        [array]::Reverse($outlineFaceVertices)
        $outlineFaceId = "00000000-0000-4000-$FaceGroup-{0:x12}" -f $faceIndex
        $outlineFaces[$outlineFaceId] = [ordered]@{
            uv = $outlineUv
            texture = 1
            vertices = $outlineFaceVertices
        }
        $faceIndex++
    }

    $outlineMeshId = "00000000-0000-4000-$MeshGroup-000000000000"
    return [PSCustomObject]@{
        MeshId = $outlineMeshId
        Element = [ordered]@{
            name = $Name
            color = 3
            locked = $false
            visibility = $true
            export = $true
            origin = @(0, 24, 0)
            vertices = $outlineVertices
            faces = $outlineFaces
            type = "mesh"
            uuid = $outlineMeshId
        }
    }
}

$bodyOutline = New-NegativeOutlineMesh $bodyElement "black_shadow_cylinder_negative_outline" "8016" "8017" "8018"
$middleOutline = New-NegativeOutlineMesh $middleLayer.Element "middle_dragging_cloth_negative_outline" "8019" "801a" "801b"
$outerOutline = New-NegativeOutlineMesh $outerLayer.Element "outer_floating_cloth_negative_outline" "801c" "801d" "801e"
$model = [ordered]@{
    meta = [ordered]@{
        format_version = "4.10"
        model_format = "modded_entity"
        box_uv = $false
    }
    name = "black_shadow"
    geometry_name = "BlackShadowModel"
    modded_entity_version = "1.17"
    model_identifier = "typemoonaddon:black_shadow"
    visible_box = @(2.5, 2.0, 0)
    resolution = [ordered]@{ width = 64; height = 64 }
    elements = @(
        $bodyElement,
        $middleLayer.Element,
        $outerLayer.Element,
        $bodyOutline.Element,
        $middleOutline.Element,
        $outerOutline.Element
    )
    outliner = @(
        $meshId,
        $middleLayer.MeshId,
        $outerLayer.MeshId,
        $bodyOutline.MeshId,
        $middleOutline.MeshId,
        $outerOutline.MeshId
    )
    textures = @(
        [ordered]@{
            path = "../src/main/resources/assets/typemoonaddon/textures/entity/black_shadow.png"
            name = "black_shadow.png"
            folder = "entity"
            namespace = "typemoonaddon"
            id = "0"
            particle = $false
            render_mode = "default"
            render_sides = "auto"
            visible = $true
            internal = $false
            saved = $true
            uuid = "00000000-0000-4000-8006-000000000000"
        },
        [ordered]@{
            path = "../src/main/resources/assets/typemoonaddon/textures/entity/black_shadow_outline.png"
            name = "black_shadow_outline.png"
            folder = "entity"
            namespace = "typemoonaddon"
            id = "1"
            particle = $false
            render_mode = "default"
            render_sides = "auto"
            visible = $true
            internal = $false
            saved = $true
            uuid = "00000000-0000-4000-801f-000000000000"
        }
    )
}

$resolvedOutput = [System.IO.Path]::GetFullPath($OutputPath)
[System.IO.Directory]::CreateDirectory([System.IO.Path]::GetDirectoryName($resolvedOutput)) | Out-Null
$json = $model | ConvertTo-Json -Depth 20 -Compress
[System.IO.File]::WriteAllText($resolvedOutput, $json, [System.Text.UTF8Encoding]::new($false))
Write-Output $resolvedOutput

param(
    [string]$OutputDirectory = (Join-Path $PSScriptRoot "..\src\main\resources\assets\typemoonaddon\textures\item")
)

Add-Type -AssemblyName System.Drawing
[System.IO.Directory]::CreateDirectory($OutputDirectory) | Out-Null

function New-PixelIcon {
    return [System.Drawing.Bitmap]::new(32, 32, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
}

function Fill-Rect {
    param(
        [System.Drawing.Graphics]$Graphics,
        [string]$Color,
        [int]$X,
        [int]$Y,
        [int]$Width,
        [int]$Height
    )

    $brush = [System.Drawing.SolidBrush]::new([System.Drawing.ColorTranslator]::FromHtml($Color))
    try {
        $Graphics.FillRectangle($brush, $X, $Y, $Width, $Height)
    } finally {
        $brush.Dispose()
    }
}

function Fill-Polygon {
    param(
        [System.Drawing.Graphics]$Graphics,
        [string]$Color,
        [int[][]]$Coordinates
    )

    $points = [System.Drawing.Point[]]($Coordinates | ForEach-Object {
        [System.Drawing.Point]::new($_[0], $_[1])
    })
    $brush = [System.Drawing.SolidBrush]::new([System.Drawing.ColorTranslator]::FromHtml($Color))
    try {
        $Graphics.FillPolygon($brush, $points)
    } finally {
        $brush.Dispose()
    }
}

function Save-VoidRingIcon {
    param([string]$Destination)

    $bitmap = New-PixelIcon
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    try {
        $graphics.Clear([System.Drawing.Color]::Transparent)
        $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::None
        $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor

        # Segmented black ring behind the red-black ceremonial chest piece.
        Fill-Rect $graphics '#121014' 11 1 10 3
        Fill-Rect $graphics '#1E1E1E' 7 3 6 3
        Fill-Rect $graphics '#1E1E1E' 19 3 6 3
        Fill-Rect $graphics '#121014' 4 6 5 7
        Fill-Rect $graphics '#121014' 23 6 5 7
        Fill-Rect $graphics '#1E1E1E' 3 11 4 10
        Fill-Rect $graphics '#1E1E1E' 25 11 4 10
        Fill-Rect $graphics '#121014' 5 20 5 7
        Fill-Rect $graphics '#121014' 22 20 5 7
        Fill-Rect $graphics '#1E1E1E' 9 26 14 4
        Fill-Rect $graphics '#550707' 12 2 8 1
        Fill-Rect $graphics '#7F0909' 5 10 2 8
        Fill-Rect $graphics '#7F0909' 25 10 2 8

        Fill-Polygon $graphics '#160F12' @(
            @(9, 9), @(13, 7), @(16, 10), @(19, 7), @(23, 9),
            @(25, 14), @(22, 17), @(21, 27), @(11, 27), @(10, 17), @(7, 14)
        )
        Fill-Polygon $graphics '#7F0909' @(
            @(9, 10), @(13, 8), @(15, 11), @(12, 16), @(9, 14)
        )
        Fill-Polygon $graphics '#9E0D0D' @(
            @(23, 10), @(19, 8), @(17, 11), @(20, 16), @(23, 14)
        )
        Fill-Polygon $graphics '#262626' @(
            @(15, 11), @(17, 11), @(20, 16), @(19, 25), @(13, 25), @(12, 16)
        )
        Fill-Rect $graphics '#B51A22' 15 13 2 9
        Fill-Rect $graphics '#550707' 11 18 3 7
        Fill-Rect $graphics '#7F0909' 18 18 3 7
        Fill-Rect $graphics '#292020' 12 26 8 2
        $bitmap.Save($Destination, [System.Drawing.Imaging.ImageFormat]::Png)
    } finally {
        $graphics.Dispose()
        $bitmap.Dispose()
    }
}

function Save-CursedArmorIcon {
    param([string]$Destination)

    $bitmap = New-PixelIcon
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    try {
        $graphics.Clear([System.Drawing.Color]::Transparent)
        $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::None
        $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor

        Fill-Polygon $graphics '#111014' @(
            @(8, 7), @(13, 5), @(16, 9), @(19, 5), @(24, 7),
            @(27, 13), @(23, 16), @(21, 29), @(11, 29), @(9, 16), @(5, 13)
        )
        Fill-Polygon $graphics '#1E1E1E' @(
            @(13, 7), @(16, 10), @(19, 7), @(21, 14), @(19, 27), @(13, 27), @(11, 14)
        )
        Fill-Rect $graphics '#7F0909' 8 10 3 6
        Fill-Rect $graphics '#9E0D0D' 21 10 3 6
        Fill-Rect $graphics '#550707' 12 17 3 10
        Fill-Rect $graphics '#7F0909' 18 15 3 12
        Fill-Rect $graphics '#B51A22' 15 11 2 13
        Fill-Rect $graphics '#292020' 10 28 12 2
        $bitmap.Save($Destination, [System.Drawing.Imaging.ImageFormat]::Png)
    } finally {
        $graphics.Dispose()
        $bitmap.Dispose()
    }
}

Save-VoidRingIcon (Join-Path $OutputDirectory 'void_ring_regalia.png')
Save-CursedArmorIcon (Join-Path $OutputDirectory 'cursed_armor_render.png')
Write-Host "Generated armor item icons in $OutputDirectory"

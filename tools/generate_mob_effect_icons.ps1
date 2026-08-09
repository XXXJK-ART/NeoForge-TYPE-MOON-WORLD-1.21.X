param(
    [string]$OutputDirectory = (Join-Path $PSScriptRoot "../src/main/resources/assets/typemoonworld/textures/mob_effect")
)

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

$transparent = [System.Drawing.Color]::Transparent

function Convert-HexColor([string]$hex) {
    return [System.Drawing.ColorTranslator]::FromHtml($hex)
}

function Write-PixelIcon {
    param(
        [Parameter(Mandatory)][string]$Name,
        [Parameter(Mandatory)][hashtable]$Palette,
        [Parameter(Mandatory)][string[]]$Rows
    )

    if ($Rows.Count -ne 16) {
        throw "$Name must contain exactly 16 rows, found $($Rows.Count)"
    }

    $bitmap = [System.Drawing.Bitmap]::new(18, 18, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    try {
        for ($y = 0; $y -lt 16; $y++) {
            if ($Rows[$y].Length -ne 16) {
                throw "$Name row $($y + 1) must contain exactly 16 pixels, found $($Rows[$y].Length)"
            }

            for ($x = 0; $x -lt 16; $x++) {
                $symbol = [string]$Rows[$y][$x]
                if ($symbol -eq ".") {
                    $bitmap.SetPixel($x + 1, $y + 1, $transparent)
                } elseif ($Palette.ContainsKey($symbol)) {
                    $bitmap.SetPixel($x + 1, $y + 1, (Convert-HexColor $Palette[$symbol]))
                } else {
                    throw "$Name uses undefined palette symbol '$symbol'"
                }
            }
        }

        $path = Join-Path $OutputDirectory "$Name.png"
        $bitmap.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    } finally {
        $bitmap.Dispose()
    }
}

function Convert-NineLivesIcon {
    $sourcePath = Join-Path $OutputDirectory "nine_lives.jpg"
    $targetPath = Join-Path $OutputDirectory "nine_lives.png"
    $source = [System.Drawing.Image]::FromFile($sourcePath)
    $bitmap = [System.Drawing.Bitmap]::new(18, 18, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    try {
        $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
        try {
            $graphics.Clear($transparent)
            $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
            $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
            $graphics.DrawImage($source, [System.Drawing.Rectangle]::new(0, 0, 18, 18), 0, 0, $source.Width, $source.Height, [System.Drawing.GraphicsUnit]::Pixel)
        } finally {
            $graphics.Dispose()
        }
        $bitmap.Save($targetPath, [System.Drawing.Imaging.ImageFormat]::Png)
    } finally {
        $bitmap.Dispose()
        $source.Dispose()
    }
}

New-Item -ItemType Directory -Force -Path $OutputDirectory | Out-Null

Convert-NineLivesIcon

Write-PixelIcon "petrified" @{ K = "#25272A"; M = "#858B8F"; L = "#C4C8C6" } @(
    "................",
    ".....KKKK.......",
    "....KMMMMK......",
    "...KMMLMMMK.....",
    "...KMMMMMMK.....",
    "...KMMKKMMK.....",
    "...KMMMMMMK.....",
    "....KMMMMK......",
    ".....KMMK.......",
    "...KKKMMKKK.....",
    "..KMMMMMMMMK....",
    ".KMMMMMMMMMMK...",
    ".KMMKMMMMKMMK...",
    ".KMMMMMMMMMMK...",
    "..KKKKKKKKKK....",
    "................"
)

Write-PixelIcon "binding" @{ K = "#4A3210"; M = "#D69A25"; L = "#FFE07A" } @(
    "................",
    "..........KKK...",
    ".........KMMMK..",
    "........KMKLMK..",
    ".......KMK.KMK..",
    "....KKKMLK.KMK..",
    "...KMMMLK..KMK..",
    "..KMLK...KMMK...",
    "..KMK...KMMK....",
    "..KMK..KMLKKK...",
    "..KMK.KMLKMMMK..",
    "..KMK.KMK.KMLK..",
    "..KMLKMK..KMK...",
    "...KMMMK.KMMK...",
    "....KKK...KK....",
    "................"
)

Write-PixelIcon "suggestion" @{ K = "#29153A"; M = "#7636A8"; L = "#C079FF"; V = "#9A4FE0"; W = "#F4DBFF" } @(
    "................",
    ".....KKKKKK.....",
    "...KKMMMMMMKK...",
    "..KMMMLLLLMMMK..",
    ".KMMLKKKKKLMMK..",
    ".KMLK......KLMK.",
    "KMK...VVV....KMK",
    "KMK..VWLWV...KMK",
    "KMK..VLKLV...KMK",
    "KMK...VVV....KMK",
    ".KMLK......KLMK.",
    ".KMMLKKKKKLMMK..",
    "..KMMMLLLMMMK...",
    "...KKMMMMKK.....",
    ".....KKKK.......",
    "................"
)

Write-PixelIcon "reverse_movement" @{ K = "#241B43"; M = "#704FD2"; L = "#BCA7FF" } @(
    "................",
    ".....K....K.....",
    "....KMK..KMK....",
    "...KMMK..KMMK...",
    "..KMMMK..KMMMK..",
    ".KMMMK....KMMMK.",
    "KMMMK......KMMMK",
    ".KMMKKKKKKKKMMK.",
    "..KLLLLLLLLLLK..",
    ".KMMKKKKKKKKMMK.",
    "KMMMK......KMMMK",
    ".KMMMK....KMMMK.",
    "..KMMMK..KMMMK..",
    "...KMMK..KMMK...",
    "....KK....KK....",
    "................"
)

Write-PixelIcon "stagger" @{ K = "#3D3420"; M = "#C2A755"; L = "#FFE59A"; W = "#FFFBE4" } @(
    ".......K........",
    ".......K........",
    "...K...M...K....",
    "....K..M..K.....",
    ".....K.M.K......",
    ".KMMMMMMMMMMMK..",
    "..KMMMMMMMMMK...",
    "...KMLLWWLMK....",
    "KKKMLWWWWLMKKK..",
    "...KMLLWWLMK....",
    "..KMMMMMMMMMK...",
    ".KMMMMMMMMMMMK..",
    ".....K.M.K......",
    "....K..M..K.....",
    "...K...M...K....",
    ".......K........"
)

Write-PixelIcon "off_balance" @{ K = "#263124"; M = "#66845E"; L = "#B6D09D" } @(
    "................",
    ".........KK.....",
    "........KMMK....",
    ".......KMMMK....",
    "......KMMMMK....",
    ".....KMMMMK.....",
    "....KMMMMK......",
    "...KMMMMK.......",
    "..KMMMMMKK......",
    "..KMMMMMMMK.....",
    "...KMMMMMMMK....",
    "....KKMMMMMK....",
    "......KMMMKK....",
    ".KKK...KKK......",
    "KLLLKKKK........",
    "KKKKKK.........."
)

Write-PixelIcon "pale_rider_infection" @{ K = "#171717"; D = "#393939"; M = "#7D846C"; L = "#C9D0A2" } @(
    "................",
    "......KKK.......",
    ".....KDDDK......",
    "..KK.KDMDK.KK...",
    ".KDDKKMMMKKDDK..",
    ".KDMDMMMMMDMDK..",
    "..KMMMMMMMMMK...",
    "...KMMMLMMMMK...",
    ".KKMMMLLLMMMK...",
    "KDDMMMMMMMMMDDK.",
    "KDMDMMMMMMMDMDK.",
    "KDDKMMMMMMMKDDK.",
    ".KK.KMMMMMK.KK..",
    "....KDDMDDK.....",
    ".....KKKKK......",
    "................"
)

Write-PixelIcon "pale_rider_fear" @{ K = "#161616"; D = "#45454B"; M = "#9A9AA2" } @(
    "................",
    "......KKKK......",
    "....KKDDDDKK....",
    "...KDDDDDDDDK...",
    "..KDDDMMMMDDDK..",
    "..KDDMMMMMMDDK..",
    ".KDDMMKMMKMMDDK.",
    ".KDDMMKMMKMMDDK.",
    ".KDDMMMMMMMMDDK.",
    ".KDDMMKMMKMMDDK.",
    "..KDDMKMMKMDDK..",
    "..KDDDMMMMDDDK..",
    "...KDDDDDDDDK...",
    "....KDKKKKDK....",
    ".....KK..KK.....",
    "................"
)

Write-PixelIcon "fanatic_wounded" @{ K = "#3B080C"; M = "#A51F2B"; L = "#F05A64" } @(
    "................",
    "...KKK....KKK...",
    "..KMMMK..KMMMK..",
    ".KMLLMMKKMMLLMK.",
    ".KMLLMMMMMMLLMK.",
    ".KMMMMMMMMMMMMK.",
    "..KMMMMKMMMMMK..",
    "...KMMK.KMMMMK..",
    "....KK..KMMMK...",
    ".......KMMMK....",
    "......KMMMK.....",
    ".....KMMMK......",
    "....KMMMK.......",
    ".....KMK........",
    "......K.........",
    "................"
)

Write-PixelIcon "fanatic_circuit_disruption" @{ K = "#28163B"; M = "#7546A5"; L = "#CE8EFF" } @(
    "................",
    ".KK..........KK.",
    ".KMK........KMK.",
    ".KMK..KKKK..KMK.",
    ".KMMKKMLLMKKMMK.",
    "..KMMMLLLLMMMK..",
    "...KMLK..KLMK...",
    "KKKMLK.KK.KLMKKK",
    "KLLLLKKMMKKLLLLK",
    "KKKMLK.KK.KLMKKK",
    "...KMLK..KLMK...",
    "..KMMMLLLLMMMK..",
    ".KMMKKMLLMKKMMK.",
    ".KMK..KKKK..KMK.",
    ".KK..........KK.",
    "................"
)

Write-PixelIcon "fanatic_toxin" @{ K = "#173514"; M = "#3F9138"; L = "#9FEA67" } @(
    ".......KK.......",
    "......KMMK......",
    ".....KMLLMK.....",
    "....KMLLLLMK....",
    "...KMLLLLLLMK...",
    "..KMLLLLLLLLMK..",
    "..KLLLLLLLLLLK..",
    ".KLLLLLLLLLLLLK.",
    ".KLLLLKLLKLLLLK.",
    ".KLLLLKLLKLLLLK.",
    ".KLLLLLLLLLLLLK.",
    "..KLLLKKKKLLLK..",
    "..KLLLLLLLLLLK..",
    "...KLLLLLLLLK...",
    "....KKKKKKKK....",
    "................"
)

Write-PixelIcon "demon_god_curse" @{ K = "#250307"; D = "#5A0712"; M = "#A51C2D"; L = "#F06067" } @(
    ".KK..........KK.",
    ".KMK........KMK.",
    "..KMMK......KMMK",
    "...KMMK....KMMK.",
    "....KMMKKKKMMK..",
    "...KDDMMMMMMDDK.",
    "..KDMMLLMMMLLMDK",
    ".KDMMLKMMMMKLMMK",
    ".KDMMMKMMMMKMMMK",
    ".KDMMMMKMMKMMMMK",
    "..KDMMLLLLLLMDK.",
    "...KDMMLLLLMDK..",
    "....KDMMMMMDK...",
    ".....KDMMDK.....",
    "......KKKK......",
    "................"
)

Write-Host "Generated 13 mob effect icons in $([System.IO.Path]::GetFullPath($OutputDirectory))"

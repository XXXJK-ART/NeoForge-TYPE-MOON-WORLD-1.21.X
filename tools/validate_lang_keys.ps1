param(
    [string]$ZhPath = "src/main/resources/assets/typemoonworld/lang/zh_cn.json",
    [string]$EnPath = "src/main/resources/assets/typemoonworld/lang/en_us.json"
)

function To-KeyMap($obj) {
    $map = @{}
    foreach ($p in $obj.PSObject.Properties) {
        $map[[string]$p.Name] = [string]$p.Value
    }
    return $map
}

function Prefix-Of([string]$key) {
    if ($key -match "^[^.]+") {
        return $Matches[0]
    }
    return "<unknown>"
}

if (-not (Test-Path $ZhPath)) {
    throw "Missing file: $ZhPath"
}
if (-not (Test-Path $EnPath)) {
    throw "Missing file: $EnPath"
}

$utf8 = New-Object System.Text.UTF8Encoding($false)
$zhObj = ([System.IO.File]::ReadAllText((Resolve-Path $ZhPath), $utf8)) | ConvertFrom-Json
$enObj = ([System.IO.File]::ReadAllText((Resolve-Path $EnPath), $utf8)) | ConvertFrom-Json

$zh = To-KeyMap $zhObj
$en = To-KeyMap $enObj

$zhKeys = @($zh.Keys | Sort-Object)
$enKeys = @($en.Keys | Sort-Object)

$missingInEn = @($zhKeys | Where-Object { -not $en.ContainsKey($_) })
$missingInZh = @($enKeys | Where-Object { -not $zh.ContainsKey($_) })

Write-Host "zh key count: $($zhKeys.Count)"
Write-Host "en key count: $($enKeys.Count)"
Write-Host ""

Write-Host "Prefix distribution (zh):"
$zhKeys |
    Group-Object { Prefix-Of $_ } |
    Sort-Object Name |
    ForEach-Object { Write-Host ("  {0}: {1}" -f $_.Name, $_.Count) }

Write-Host ""
Write-Host "Prefix distribution (en):"
$enKeys |
    Group-Object { Prefix-Of $_ } |
    Sort-Object Name |
    ForEach-Object { Write-Host ("  {0}: {1}" -f $_.Name, $_.Count) }

$hasError = $false
if ($missingInEn.Count -gt 0) {
    $hasError = $true
    Write-Host ""
    Write-Host "Missing in en_us.json ($($missingInEn.Count)):" -ForegroundColor Red
    $missingInEn | ForEach-Object { Write-Host "  $_" }
}

if ($missingInZh.Count -gt 0) {
    $hasError = $true
    Write-Host ""
    Write-Host "Missing in zh_cn.json ($($missingInZh.Count)):" -ForegroundColor Red
    $missingInZh | ForEach-Object { Write-Host "  $_" }
}

if ($hasError) {
    exit 1
}

Write-Host ""
Write-Host "Lang key validation passed." -ForegroundColor Green

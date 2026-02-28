param(
    [string]$ZhPath = "src/main/resources/assets/typemoonworld/lang/zh_cn.json",
    [string]$EnPath = "src/main/resources/assets/typemoonworld/lang/en_us.json"
)

function Get-GroupOrder([string]$key) {
    if ($key -match '^item\.') { return 1 }
    if ($key -match '^block\.') { return 2 }
    if ($key -match '^effect\.') { return 3 }
    if ($key -match '^attribute\.') { return 4 }
    if ($key -match '^key\.') { return 5 }
    if ($key -match '^gui\.') { return 6 }
    if ($key -match '^magic\.') { return 7 }
    if ($key -match '^message\.') { return 8 }
    if ($key -match '^entity\.') { return 9 }
    if ($key -match '^tooltip\.') { return 10 }
    if ($key -match '^command\.') { return 11 }
    if ($key -match '^adv\.') { return 12 }
    if ($key -match '^creativetab\.') { return 13 }
    return 99
}

function Escape-JsonString([string]$s) {
    if ($null -eq $s) { return "" }

    $sb = New-Object System.Text.StringBuilder
    foreach ($ch in $s.ToCharArray()) {
        switch ($ch) {
            '"'  { [void]$sb.Append('\"'); continue }
            '\'  { [void]$sb.Append('\\'); continue }
            "`b" { [void]$sb.Append('\b'); continue }
            "`f" { [void]$sb.Append('\f'); continue }
            "`n" { [void]$sb.Append('\n'); continue }
            "`r" { [void]$sb.Append('\r'); continue }
            "`t" { [void]$sb.Append('\t'); continue }
        }

        $code = [int][char]$ch
        if ($code -lt 32) {
            [void]$sb.AppendFormat('\u{0:x4}', $code)
        } else {
            [void]$sb.Append($ch)
        }
    }
    return $sb.ToString()
}

function Sort-LangFile([string]$path) {
    if (-not (Test-Path $path)) {
        throw "Missing file: $path"
    }

    $utf8 = New-Object System.Text.UTF8Encoding($false)
    $raw = [System.IO.File]::ReadAllText((Resolve-Path $path), $utf8)
    $json = $raw | ConvertFrom-Json
    if ($null -eq $json) {
        throw "JSON parse failed: $path"
    }

    $props = @($json.PSObject.Properties)
    if ($props.Count -lt 100) {
        throw "Suspicious key count in ${path}: $($props.Count)"
    }

    $entries = foreach ($p in $props) {
        $k = [string]$p.Name
        [PSCustomObject]@{
            Key        = $k
            Value      = [string]$p.Value
            GroupOrder = Get-GroupOrder $k
            GroupName  = ($k -split '\.')[0]
        }
    }

    $sorted = @($entries | Sort-Object GroupOrder, Key)

    $lines = New-Object System.Collections.Generic.List[string]
    $lines.Add('{')

    $prevGroup = $null
    for ($i = 0; $i -lt $sorted.Count; $i++) {
        $entry = $sorted[$i]
        if ($null -ne $prevGroup -and $entry.GroupName -ne $prevGroup) {
            $lines.Add("")
        }

        $keyEsc = Escape-JsonString $entry.Key
        $valEsc = Escape-JsonString $entry.Value
        $comma = if ($i -lt $sorted.Count - 1) { "," } else { "" }
        $lines.Add("  ""$keyEsc"": ""$valEsc""$comma")
        $prevGroup = $entry.GroupName
    }

    $lines.Add('}')

    $output = [string]::Join("`n", $lines)
    # Normalize accidental explicit LF unicode escapes to avoid duplicated blank lines in UI.
    $output = $output -replace '\\u000a', ''
    [System.IO.File]::WriteAllText((Resolve-Path $path), $output, $utf8)
}

Sort-LangFile $ZhPath
Sort-LangFile $EnPath

Write-Host "Sorted lang files:"
Write-Host "  $ZhPath"
Write-Host "  $EnPath"

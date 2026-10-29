$ErrorActionPreference = "Stop"
$f = "app/src/main/res/values-hi/strings_guide_content.xml"
$raw = [System.IO.File]::ReadAllBytes($f)
$needle = [System.Text.Encoding]::UTF8.GetBytes("guide_situations_remaining")
$idx = [System.Array]::IndexOf($raw, $needle[0])
while ($idx -ge 0 -and $idx -lt $raw.Length) {
    $ok = $true
    for ($k = 1; $k -lt $needle.Length; $k++) {
        if ($raw[$idx + $k] -ne $needle[$k]) { $ok = $false; break }
    }
    if ($ok) { break }
    $idx = [System.Array]::IndexOf($raw, $needle[0], $idx + 1)
}
Write-Host "idx:" $idx
if ($idx -ge 0) {
    $start = [Math]::Max(0, $idx - 40)
    $end = [Math]::Min($raw.Length, $idx + 220)
    $seg = $raw[$start..($end - 1)]
    [System.IO.File]::WriteAllBytes("logs/hi_segment.bin", $seg)
    Write-Host "segment written length:" $seg.Length
} else {
    Write-Host "NOT FOUND"
}

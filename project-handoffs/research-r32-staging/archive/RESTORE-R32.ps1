# Restore the exact R32 research staging archive from the GitHub Base64 blob.
$ErrorActionPreference = 'Stop'
$InFile = Join-Path $PSScriptRoot 'BURROW-RESEARCH-DISPATCH-R32.zip.b64'
$OutDir = Join-Path $PSScriptRoot 'restored-original-r32'
$Expected = '36176521201338156334898582ee12077baa929fe42bf8c3846ef327440b1d0a'
if (!(Test-Path -LiteralPath $InFile -PathType Leaf)) { throw "Missing original R32 GitHub Base64 source" }
New-Item -ItemType Directory -Path $OutDir -Force | Out-Null
$OutFile = Join-Path $OutDir 'BURROW-RESEARCH-DISPATCH-R32.zip'
[System.IO.File]::WriteAllBytes($OutFile,[Convert]::FromBase64String(([IO.File]::ReadAllText($InFile)).Trim()))
if ((Get-FileHash -LiteralPath $OutFile -Algorithm SHA256).Hash.ToLowerInvariant() -ne $Expected) { Remove-Item $OutFile -Force; throw "R32 SHA256 mismatch" }
Add-Type -AssemblyName System.IO.Compression.FileSystem
$z=[IO.Compression.ZipFile]::OpenRead($OutFile)
try { if ($z.Entries.Count -ne 84) { throw "Wrong R32 entry count" } }
finally {$z.Dispose()}
Write-Host "PASS: original R32 ZIP restored and SHA256 verified (84 entries)." -ForegroundColor Green
Write-Host "R32 staging only; no canonical R31 overwrite." -ForegroundColor Yellow

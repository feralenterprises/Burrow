# Recover original immutable R32 research staging ZIP from GitHub-tracked base64 parts.
# No Git operations, research promotion, or plugin source modifications.
$ErrorActionPreference='Stop'
Set-StrictMode -Version Latest
Add-Type -AssemblyName System.IO.Compression.FileSystem
$partsDir=Join-Path $PSScriptRoot 'parts'
$targetRoot=Join-Path $PSScriptRoot '..\generated'
New-Item -Path $targetRoot -ItemType Directory -Force | Out-Null
$zipPath=Join-Path $targetRoot 'BURROW-RESEARCH-DISPATCH-R32.zip'
$expectedHash='36176521201338156334898582ee12077baa929fe42bf8c3846ef327440b1d0a'
$joined=New-Object System.Text.StringBuilder
for ($i=1;$i -le 4;$i++) {
  $part=Join-Path $partsDir ('BURROW-RESEARCH-DISPATCH-R32__part-'+$i.ToString('000')+'.b64')
  if (!(Test-Path -LiteralPath $part -PathType Leaf)) { throw "Missing R32 part $i" }
  [void]$joined.Append([System.IO.File]::ReadAllText($part).Trim())
}
$bytes=[System.Convert]::FromBase64String($joined.ToString())
$sha=[System.Security.Cryptography.SHA256]::Create()
try { $actual=([System.BitConverter]::ToString($sha.ComputeHash($bytes))).Replace('-','').ToLowerInvariant() }
finally { $sha.Dispose() }
if ($actual -ne $expectedHash -or $bytes.Length -ne 351885) { throw 'R32 ZIP size/SHA256 mismatch' }
if (Test-Path -LiteralPath $zipPath -PathType Leaf) {
  $existing=(Get-FileHash -LiteralPath $zipPath -Algorithm SHA256).Hash.ToLowerInvariant()
  if ($existing -ne $expectedHash) { throw "Refusing to overwrite different existing R32 ZIP: $zipPath" }
} else {
  [System.IO.File]::WriteAllBytes($zipPath,$bytes)
}
$archive=[System.IO.Compression.ZipFile]::OpenRead($zipPath)
try {
  $entries=@($archive.Entries | Where-Object { $_.Name -ne '' })
  if ($entries.Count -ne 84) { throw "R32 ZIP entry count mismatch: $($entries.Count)" }
  $manifestEntry=$archive.GetEntry('14-INTEGRITY-MANIFEST.json')
  if ($null -eq $manifestEntry) { throw 'R32 integrity manifest missing' }
  $reader=New-Object System.IO.StreamReader($manifestEntry.Open())
  try { $manifest=$reader.ReadToEnd() | ConvertFrom-Json }
  finally { $reader.Dispose() }
  if ($manifest.revision -ne 'R32' -or $manifest.files.Count -ne 83) { throw 'Wrong R32 manifest revision/entry count' }
  $hasher=[System.Security.Cryptography.SHA256]::Create()
  try {
    foreach ($file in $manifest.files) {
      $entry=$archive.GetEntry([string]$file.name)
      if ($null -eq $entry -or $entry.Length -ne [long]$file.size_bytes) { throw "R32 manifest entry missing / size wrong: $($file.name)" }
      $stream=$entry.Open()
      try { $digest=([System.BitConverter]::ToString($hasher.ComputeHash($stream))).Replace('-','').ToLowerInvariant() }
      finally { $stream.Dispose() }
      if ($digest -ne $file.sha256) { throw "R32 SHA mismatch: $($file.name)" }
    }
  } finally { $hasher.Dispose() }
} finally { $archive.Dispose() }
Write-Host 'VERIFIED original R32 archive and all 83 manifest files. RELEASE HOLD.' -ForegroundColor Green
Write-Host "Original research-only source: $zipPath" -ForegroundColor Cyan

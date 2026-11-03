# TrashPanda Tracker archive restoration from GitHub-tracked base64 shards.
# Read-only regarding source, main, and GitHub. Restores two original ZIPs locally.
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$SourceDir = Join-Path $PSScriptRoot 'parts'
$OutputDir = Join-Path $PSScriptRoot 'restored-original-zips'
if (-not (Test-Path -LiteralPath $SourceDir -PathType Container)) { throw "Archive parts folder missing: $SourceDir" }
New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null
$Archives = @(
    @{ BaseName = 'TPT_COMPLETE_INTELLIGENCE_HANDOFF_R03_2026-11-03'; Parts = 15; Bytes = 7073780; SHA256 = '708c5a26d902426af9edc9c65339fb49711a53f37d607ae8b6f06641cd11eaad' },
    @{ BaseName = 'TPT-PATHFINDER-AI-SELF-READ-MASTER-R01-2026-11-03'; Parts = 21; Bytes = 10111186; SHA256 = '94d135bd88db436c0f952df4683786deb09aff75737be15a6bd6ddbb44bcebac' }
)
foreach ($archive in $Archives) {
    $output = Join-Path $OutputDir ($archive.BaseName + '.zip')
    Write-Host ('Restoring: ' + $archive.BaseName) -ForegroundColor Cyan
    $stream = [System.IO.File]::Open($output, [System.IO.FileMode]::Create, [System.IO.FileAccess]::Write, [System.IO.FileShare]::None)
    try {
        for ($part = 1; $part -le $archive.Parts; $part++) {
            $fileName = $archive.BaseName + '__part-' + $part.ToString('000') + '.b64'
            $partPath = Join-Path $SourceDir $fileName
            if (-not (Test-Path -LiteralPath $partPath -PathType Leaf)) { throw ('Missing part: ' + $fileName) }
            $base64 = [System.IO.File]::ReadAllText($partPath).Trim()
            $bytes = [System.Convert]::FromBase64String($base64)
            $stream.Write($bytes, 0, $bytes.Length)
        }
    }
    finally { $stream.Dispose() }
    $info = Get-Item -LiteralPath $output
    $sha = (Get-FileHash -LiteralPath $output -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($info.Length -ne $archive.Bytes -or $sha -ne $archive.SHA256) {
        Remove-Item -LiteralPath $output -Force
        throw ('INTEGRITY FAILURE: ' + $archive.BaseName)
    }
    Write-Host ('VERIFIED original ZIP: ' + $output) -ForegroundColor Green
}
Write-Host 'Both original archives restored. Archival sources only; NOT integrated or release-ready plugin code.' -ForegroundColor Green

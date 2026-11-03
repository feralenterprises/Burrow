# TrashPanda Tracker: restore and verify complete outer archive file trees locally.
# NEVER writes to main, GitHub, or plugin source. Never overwrites an existing extraction.
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$restore = Join-Path $PSScriptRoot 'RESTORE-ORIGINAL-ARCHIVES.ps1'
if (-not (Test-Path -LiteralPath $restore -PathType Leaf)) { throw 'Missing archive restoration script.' }
& $restore

Add-Type -AssemblyName System.IO.Compression.FileSystem
$zipDir = Join-Path $PSScriptRoot 'restored-original-zips'
$fullDir = Join-Path $PSScriptRoot 'restored-full'
New-Item -ItemType Directory -Path $fullDir -Force | Out-Null
$specs = @(
    @{
        Base = 'TPT_COMPLETE_INTELLIGENCE_HANDOFF_R03_2026-11-03'
        Folder = 'intelligence-r03'
        Sha = '708c5a26d902426af9edc9c65339fb49711a53f37d607ae8b6f06641cd11eaad'
        Files = 53
        NestedZips = 13
    },
    @{
        Base = 'TPT-PATHFINDER-AI-SELF-READ-MASTER-R01-2026-11-03'
        Folder = 'pathfinder-r01'
        Sha = '94d135bd88db436c0f952df4683786deb09aff75737be15a6bd6ddbb44bcebac'
        Files = 39
        NestedZips = 14
    }
)

function Assert-ExtractedFileTree([string]$zipPath, [string]$destination, [int]$expectedFiles, [int]$expectedNestedZips) {
    $archive = [System.IO.Compression.ZipFile]::OpenRead($zipPath)
    try {
        $entries = @($archive.Entries | Where-Object { $_.Name -ne '' })
        if ($entries.Count -ne $expectedFiles) {
            throw "Incorrect ZIP entry count: expected $expectedFiles, found $($entries.Count)."
        }
        $nested = @($entries | Where-Object { $_.FullName.EndsWith('.zip', [System.StringComparison]::OrdinalIgnoreCase) }).Count
        if ($nested -ne $expectedNestedZips) {
            throw "Incorrect nested ZIP count: expected $expectedNestedZips, found $nested."
        }

        $root = [System.IO.Path]::GetFullPath($destination).TrimEnd([System.IO.Path]::DirectorySeparatorChar) + [System.IO.Path]::DirectorySeparatorChar
        $hasher = [System.Security.Cryptography.SHA256]::Create()
        try {
            foreach ($entry in $entries) {
                $relativePath = $entry.FullName.Replace('/', [System.IO.Path]::DirectorySeparatorChar)
                $localPath = [System.IO.Path]::GetFullPath((Join-Path $destination $relativePath))
                if (-not $localPath.StartsWith($root, [System.StringComparison]::OrdinalIgnoreCase)) {
                    throw "Unsafe archive path: $($entry.FullName)"
                }
                if (-not (Test-Path -LiteralPath $localPath -PathType Leaf)) {
                    throw "Missing extracted file: $($entry.FullName)"
                }
                $file = Get-Item -LiteralPath $localPath
                if ($file.Length -ne $entry.Length) {
                    throw "File size mismatch: $($entry.FullName)"
                }

                # Verify extracted content against decompressed ZIP entry bytes.
                $sourceStream = $entry.Open()
                try { $sourceHash = $hasher.ComputeHash($sourceStream) }
                finally { $sourceStream.Dispose() }
                $fileStream = [System.IO.File]::OpenRead($localPath)
                try { $fileHash = $hasher.ComputeHash($fileStream) }
                finally { $fileStream.Dispose() }
                if ([System.BitConverter]::ToString($sourceHash) -ne [System.BitConverter]::ToString($fileHash)) {
                    throw "Content hash mismatch: $($entry.FullName)"
                }
            }
        }
        finally { $hasher.Dispose() }
    }
    finally { $archive.Dispose() }
}

foreach ($spec in $specs) {
    $zip = Join-Path $zipDir ($spec.Base + '.zip')
    $sha = (Get-FileHash -LiteralPath $zip -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($sha -ne $spec.Sha) { throw "Original ZIP SHA-256 mismatch: $($spec.Base)" }

    $destination = Join-Path $fullDir $spec.Folder
    if (Test-Path -LiteralPath $destination) {
        Write-Host "Rechecking existing extraction: $($spec.Folder)" -ForegroundColor Cyan
        Assert-ExtractedFileTree $zip $destination $spec.Files $spec.NestedZips
        Write-Host "VERIFIED: $($spec.Folder) ($($spec.Files) files, $($spec.NestedZips) nested ZIPs)" -ForegroundColor Green
        continue
    }

    $staging = Join-Path $fullDir ('.staging-' + $spec.Folder + '-' + [Guid]::NewGuid().ToString('N'))
    New-Item -ItemType Directory -Path $staging -Force | Out-Null
    try {
        Write-Host "Extracting verified archive: $($spec.Folder)" -ForegroundColor Cyan
        [System.IO.Compression.ZipFile]::ExtractToDirectory($zip, $staging)
        Assert-ExtractedFileTree $zip $staging $spec.Files $spec.NestedZips
        [System.IO.File]::WriteAllText((Join-Path $staging '_ORIGINAL_ZIP_SHA256.txt'), $sha + [Environment]::NewLine)
        Move-Item -LiteralPath $staging -Destination $destination
        Write-Host "VERIFIED: $($spec.Folder) ($($spec.Files) files, $($spec.NestedZips) nested ZIPs)" -ForegroundColor Green
    }
    catch {
        if (Test-Path -LiteralPath $staging) {
            Remove-Item -LiteralPath $staging -Recurse -Force -ErrorAction SilentlyContinue
        }
        throw
    }
}

Write-Host 'PASS: both full outer archive trees verified; nested ZIPs preserved unchanged.' -ForegroundColor Green
Write-Host 'These are isolated historical artifacts, NOT merged or current production plugin source.' -ForegroundColor Yellow

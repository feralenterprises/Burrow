# QA integration source-preparation: isolated, SHA-verified inputs.
# Nothing is merged or promoted to production. All output under generated/.
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
Add-Type -AssemblyName System.IO.Compression.FileSystem

$Repo = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$Restore = Join-Path $Repo 'project-handoffs\archive-recovery\EXPAND-VERIFIED-MASTER-ARCHIVES.ps1'
if (!(Test-Path -LiteralPath $Restore -PathType Leaf)) { throw 'Missing archive restore/expand script' }
& $Restore
$Archives = Join-Path $Repo 'project-handoffs\archive-recovery\restored-full'
$Gen = Join-Path $PSScriptRoot 'generated'
New-Item -Path $Gen -ItemType Directory -Force | Out-Null

$Inputs = @(
    @{Name='owner-b38'; Path=(Join-Path $Repo 'development-snapshots\B38\38-FERAL-DUAL-DEV-BANK-R05-O-FREEZE-HOVERLITE-v0.2.8-WINDOWS-QA-LEAN.zip'); Sha='711dd92392d63cde669d646e195610e2d0ca12a17683249874f9231e943ca0ec'},
    @{Name='host-w075'; Path=(Join-Path $Archives 'intelligence-r03\TPT_R02_UNCHANGED_REFERENCE\source\TPT-INTELLIGENCE-W075-MULTISTYLE-GEAR-CLEAN-DUAL-DEV-OWNER-HOLD.zip'); Sha='eea049b7ad56d12de7f3354333579783325c6785f2b4d2932496cd237bfb696c'},
    @{Name='host-w0751'; Path=(Join-Path $Archives 'pathfinder-r01\02-PINNED-SOURCE\TPT-W0751-RUNELITE-135-ONE-CLICK-DUAL-DEV-FIX.zip'); Sha='79e7d54a78a0bb76b6b3eca0e6804067b02b40a35e88339ee83a20408aa7ef3f'},
    @{Name='pathfinder-w09'; Path=(Join-Path $Archives 'pathfinder-r01\02-PINNED-SOURCE\W09-ISOLATED-CANDIDATE-SOURCE-RECONSTRUCTED-NOT-PRODUCTION.zip'); Sha='3e696901cc14d04a04c58473410f8548fcb47485f63baf596ba41d54e3c3ec13'},
    @{Name='cursor-wave4'; Path=(Join-Path $Archives 'pathfinder-r01\03-PARALLEL-CURSOR\pathfinder_wave4_complete.zip'); Sha='ae019db6888b5870770a0c4458ef6b12089718977d4dcc92ea262d22d42570be'},
    @{Name='gear-research'; Path=(Join-Path $Archives 'intelligence-r03\TPT_R02_UNCHANGED_REFERENCE\references\TPT-SPOONS-RESEARCH-COMPENDIUM-2026-11-02.zip'); Sha='d99f06386783b8784108f56fbeb1d8d90c6bafd01c6b4d50fd40e35cd99f38b5'}
)

foreach ($Input in $Inputs) {
    $Name=$Input.Name
    $Path=$Input.Path
    if (!(Test-Path -LiteralPath $Path -PathType Leaf)) { throw "Missing source ZIP: $Path" }
    $Actual=(Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($Actual -ne $Input.Sha) { throw "Source SHA mismatch: $Name ($Actual)" }
    $Target=Join-Path $Gen $Name
    $Stamp=Join-Path $Target '_SOURCE_SHA256.txt'
    if (Test-Path -LiteralPath $Target) {
        if (!(Test-Path -LiteralPath $Stamp)) { throw "Refusing to overwrite unverified target: $Target" }
        $Existing=[System.IO.File]::ReadAllText($Stamp).Trim().ToLowerInvariant()
        if ($Existing -ne $Actual) { throw "Source version differs in target: $Target" }
        Write-Host "REUSE (SHA verified): $Name" -ForegroundColor Cyan
        continue
    }
    $Staging=Join-Path $Gen ('.stage-'+$Name+'-'+[Guid]::NewGuid().ToString('N'))
    try {
        [System.IO.Compression.ZipFile]::ExtractToDirectory($Path,$Staging)
        [System.IO.File]::WriteAllText((Join-Path $Staging '_SOURCE_SHA256.txt'),$Actual+[Environment]::NewLine)
        Move-Item -LiteralPath $Staging -Destination $Target
        Write-Host "RESTORED (SHA verified): $Name" -ForegroundColor Green
    } catch {
        if (Test-Path -LiteralPath $Staging) { Remove-Item -LiteralPath $Staging -Recurse -Force -ErrorAction SilentlyContinue }
        throw
    }
}
Write-Host 'QA source staging finished. No integration/merge performed. R32 and newest owner runtime remain external blockers.' -ForegroundColor Yellow

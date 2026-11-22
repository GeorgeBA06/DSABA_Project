# Run from PowerShell with Python 3.9+ available as python.
param(
    [string]$ApiUrl = 'http://127.0.0.1:8081/api/v1',
    [string]$AdminLogin = 'admin',
    [string]$Batch = ''
)

$ErrorActionPreference = 'Stop'
$scriptArguments = @(
    (Join-Path $PSScriptRoot 'seed-demo.py'),
    '--api', $ApiUrl,
    '--admin-login', $AdminLogin
)
if ($Batch) {
    $scriptArguments += @('--batch', $Batch)
}
& python @scriptArguments
exit $LASTEXITCODE

$ErrorActionPreference = 'Stop'
$python = Get-Command python -ErrorAction SilentlyContinue
if (-not $python) { $python = Get-Command py -ErrorAction SilentlyContinue }
if (-not $python) { throw 'Install Python 3, then rerun this script.' }
& $python.Source "$PSScriptRoot/setup_signing.py" @args
exit $LASTEXITCODE

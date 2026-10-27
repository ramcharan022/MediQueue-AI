$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
npm run dev -- --host 127.0.0.1 --port 5174 --strictPort
exit $LASTEXITCODE

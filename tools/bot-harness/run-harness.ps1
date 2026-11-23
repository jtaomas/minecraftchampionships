# Runs the full MCM staging bot harness. Extra args are passed through, e.g.
#   .\run-harness.ps1 --only=meltdown,bingo --no-pass2
$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$busy = Get-NetTCPConnection -LocalPort 25570 -State Listen -ErrorAction SilentlyContinue
if ($busy) { Write-Error "Port 25570 already in use (staging server running?)"; exit 1 }
Push-Location $here
try { & node harness.js @args } finally { Pop-Location }

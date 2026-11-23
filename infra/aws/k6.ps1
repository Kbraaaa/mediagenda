# Corre las pruebas de k6 EN el servidor (con Docker, sin instalar nada) y trae los resultados.
# Desde el propio servidor se mide la aplicacion sin el retraso de internet Colombia-Ohio.
# Uso: correr-k6-aws.bat [concurrencia|carga|estres|todas]   (por defecto: todas, ~15 minutos)
param([string]$Prueba = "todas", [string]$Ip = "3.139.58.88")
$ErrorActionPreference = "Continue"
$raiz = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
Set-Location $raiz
$Clave = @("$env:USERPROFILE\.ssh\mediagenda-clave.pem", "$env:USERPROFILE\Downloads\mediagenda-clave.pem",
           "$env:USERPROFILE\Descargas\mediagenda-clave.pem", "$env:USERPROFILE\Documents\mediagenda-clave.pem") |
         Where-Object { Test-Path $_ } | Select-Object -First 1
if (-not $Clave) { Write-Host "No encontre mediagenda-clave.pem"; exit 1 }
$ssh = @("-i", $Clave, "-o", "StrictHostKeyChecking=accept-new", "-o", "ServerAliveInterval=30")
$destino = "ec2-user@$Ip"
$pruebas = if ($Prueba -eq "todas") { @("concurrencia", "carga", "estres") } else { @($Prueba) }

$local = Join-Path $raiz "pruebas\k6\resultados"
New-Item -ItemType Directory -Force $local | Out-Null
& ssh @ssh $destino "rm -rf k6 && mkdir -p k6/resultados"
foreach ($f in Get-ChildItem (Join-Path $raiz "pruebas\k6") -Filter *.js) { & scp @ssh $f.FullName "${destino}:k6/" }

foreach ($p in $pruebas) {
    Write-Host "`n===== Prueba de $p (en el servidor) =====" -ForegroundColor Cyan
    $cmd = 'docker run --rm --network host -u $(id -u):$(id -g) -v ~/k6:/scripts -e BASE_URL=http://localhost ' +
           "grafana/k6 run --quiet /scripts/$p.js"
    & ssh @ssh $destino $cmd 2>&1 | Out-Host
    & scp @ssh "${destino}:k6/resultados/$p.*" $local
}
# Uso de CPU y memoria del servidor al terminar (para el articulo)
& ssh @ssh $destino "docker stats --no-stream --format 'table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}'" 2>&1 |
    Out-File (Join-Path $local "recursos-servidor.txt") -Encoding utf8
Write-Host "`nResultados en $local" -ForegroundColor Green
Get-ChildItem $local -Filter *.txt | ForEach-Object { Write-Host "`n--- $($_.Name)"; Get-Content $_.FullName }

# Cobertura de codigo con las APIs DESPLEGADAS en AWS (requisito: 90 % probadas con las APIs desplegadas).
# 1) Pone en cero los contadores del agente de JaCoCo que corre dentro de cada microservicio en la EC2.
# 2) Corre la coleccion de Postman (Newman) desde este PC contra http://IP (peticiones reales por internet).
# 3) Descarga lo que registro cada agente y arma el reporte HTML en el servidor (jacococli).
# 4) Trae los reportes a pruebas\cobertura-aws\ y muestra el porcentaje por microservicio.
param([string]$Ip = "3.139.58.88")
$ErrorActionPreference = "Continue"
$raiz = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
Set-Location $raiz
$Clave = @("$env:USERPROFILE\.ssh\mediagenda-clave.pem", "$env:USERPROFILE\Downloads\mediagenda-clave.pem",
           "$env:USERPROFILE\Descargas\mediagenda-clave.pem", "$env:USERPROFILE\Documents\mediagenda-clave.pem") |
         Where-Object { Test-Path $_ } | Select-Object -First 1
if (-not $Clave) { Write-Host "No encontre mediagenda-clave.pem"; exit 1 }
$ssh = @("-i", $Clave, "-o", "StrictHostKeyChecking=accept-new", "-o", "ServerAliveInterval=30")
$destino = "ec2-user@$Ip"
$servicios = @("ms-pacientes", "ms-citas", "ms-farmacia", "ms-notificaciones")

# jacococli dentro de un contenedor Java conectado a la red interna de Docker (el puerto 6300 no es publico)
$cli = 'docker run --rm --network mediagenda_default -u $(id -u):$(id -g) -v $HOME:/h -w /h eclipse-temurin:21-jre java -jar /h/jacoco/jacococli.jar'

Write-Host "1/4 Preparando jacococli en el servidor y poniendo los contadores en cero..." -ForegroundColor Cyan
& ssh @ssh $destino ("mkdir -p jacoco && test -f jacoco/jacococli.jar || curl -fsSL -o jacoco/jacococli.jar " +
    "https://repo1.maven.org/maven2/org/jacoco/org.jacoco.cli/0.8.13/org.jacoco.cli-0.8.13-nodeps.jar")
$reset = ($servicios | ForEach-Object { "$cli dump --address $_ --port 6300 --destfile /tmp/descartar.exec --reset --quiet" }) -join " && "
& ssh @ssh $destino $reset 2>&1 | Out-Host

Write-Host "2/4 Corriendo la coleccion de Postman contra http://$Ip ..." -ForegroundColor Cyan
Push-Location (Join-Path $raiz "pruebas\postman")
& npx --yes -p newman newman run MediAgenda.postman_collection.json -e MediAgenda-AWS.postman_environment.json -r cli 2>&1 |
    Select-String -Pattern "assertions|average response|failure" | Out-Host
Pop-Location

Write-Host "3/4 Leyendo la cobertura de cada microservicio y armando el reporte..." -ForegroundColor Cyan
$pasos = @("rm -rf cobertura && mkdir -p cobertura/clases")
foreach ($s in $servicios) {
    $pasos += "docker cp mediagenda-$s-1:/opt/app/classes cobertura/clases/$s"
    $pasos += "$cli dump --address $s --port 6300 --destfile cobertura/$s.exec --quiet"
    $pasos += "$cli report cobertura/$s.exec --classfiles cobertura/clases/$s --sourcefiles mediagenda/$s/src/main/java " +
              "--name '$s (AWS)' --html cobertura/html/$s --csv cobertura/$s.csv --quiet"
}
$pasos += "tar czf cobertura.tar.gz cobertura/html cobertura/*.csv"
& ssh @ssh $destino ($pasos -join " && ") 2>&1 | Out-Host

Write-Host "4/4 Descargando reportes..." -ForegroundColor Cyan
$local = Join-Path $raiz "pruebas\cobertura-aws"
if (Test-Path $local) { Remove-Item -Recurse -Force $local }
New-Item -ItemType Directory -Force $local | Out-Null
& scp @ssh "${destino}:cobertura.tar.gz" (Join-Path $env:TEMP "cobertura.tar.gz")
tar xzf (Join-Path $env:TEMP "cobertura.tar.gz") -C $local --strip-components 1

$resumen = @("Cobertura con las APIs desplegadas en AWS - $(Get-Date -Format 'yyyy-MM-dd HH:mm') - http://$Ip", "")
$totalL = 0; $cubL = 0; $totalI = 0; $cubI = 0
foreach ($s in $servicios) {
    $filas = Import-Csv (Join-Path $local "$s.csv")
    $lc = ($filas | Measure-Object LINE_COVERED -Sum).Sum; $lm = ($filas | Measure-Object LINE_MISSED -Sum).Sum
    $ic = ($filas | Measure-Object INSTRUCTION_COVERED -Sum).Sum; $im = ($filas | Measure-Object INSTRUCTION_MISSED -Sum).Sum
    $totalL += $lc + $lm; $cubL += $lc; $totalI += $ic + $im; $cubI += $ic
    $resumen += "{0,-18} lineas {1,5:N1} %   instrucciones {2,5:N1} %" -f $s, (100 * $lc / ($lc + $lm)), (100 * $ic / ($ic + $im))
}
$resumen += ""
$resumen += "{0,-18} lineas {1,5:N1} %   instrucciones {2,5:N1} %" -f "TOTAL", (100 * $cubL / $totalL), (100 * $cubI / $totalI)
$resumen | Out-File (Join-Path $local "resumen.txt") -Encoding utf8
$resumen | Out-Host
Write-Host "`nReportes HTML: $local\html\<servicio>\index.html" -ForegroundColor Green
Start-Process (Join-Path $local "html\ms-citas\index.html")

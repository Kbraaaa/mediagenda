# Cobertura de codigo con las APIs DESPLEGADAS en AWS (requisito: 90 % probadas con las APIs desplegadas).
# Se mide desde que arranco cada servicio en el servidor: el arranque (configuracion, conexiones,
# calentamiento) y las tareas programadas tambien son codigo ejecutado en produccion. Para medir desde
# cero, desplegar de nuevo antes de correr este script.
# 1) Prepara jacococli en el servidor.
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

Write-Host "1/4 Preparando jacococli en el servidor..." -ForegroundColor Cyan
& ssh @ssh $destino ("mkdir -p jacoco && test -f jacoco/jacococli.jar || curl -fsSL -o jacoco/jacococli.jar " +
    "https://repo1.maven.org/maven2/org/jacoco/org.jacoco.cli/0.8.13/org.jacoco.cli-0.8.13-nodeps.jar")

Write-Host "    Reiniciando los 4 microservicios para medir desde un arranque limpio..." -ForegroundColor Cyan
& ssh @ssh $destino ("cd mediagenda && docker compose -f docker-compose.aws.yml restart " + ($servicios -join " ") +
    " && docker compose -f docker-compose.aws.yml restart nginx") 2>&1 | Out-Null
$rutas = @("/api/v1/pacientes", "/api/v1/medicos", "/api/v1/medicamentos", "/api/v1/notificaciones")
for ($i = 0; $i -lt 30; $i++) {
    Start-Sleep -Seconds 5
    $ok = 0
    foreach ($r in $rutas) { try { if ((Invoke-WebRequest "http://$Ip$r" -UseBasicParsing -TimeoutSec 5).StatusCode -eq 200) { $ok++ } } catch { } }
    if ($ok -eq 4) { break }
}
Start-Sleep -Seconds 20   # deja terminar el calentamiento al arrancar

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

# Clases que en AWS no se usan por configuracion (sus pruebas unitarias las cubren al 100 %):
# RabbitMQ y SMTP (solo local), SNS (la cuenta lo bloquea: SMS simulado), y los respaldos de Gemini y
# Nager.Date (solo se activan si esas APIs fallan).
$inactivas = @("RabbitEventoAdapter", "EventoListener", "SmtpCorreoAdapter", "SnsSmsAdapter",
               "TriagePorReglasAdapter", "FestivosColombia")
$resumen = @("Cobertura con las APIs desplegadas en AWS - $(Get-Date -Format 'yyyy-MM-dd HH:mm') - http://$Ip",
             "(desde el arranque de cada servicio + coleccion de Postman)", "",
             "{0,-18} {1,14} {2,26}" -f "", "todo el codigo", "codigo activo en AWS")
$t = @{ L = 0; C = 0; LA = 0; CA = 0 }
foreach ($s in $servicios) {
    $filas = Import-Csv (Join-Path $local "$s.csv")
    $activas = $filas | Where-Object { $inactivas -notcontains $_.CLASS }
    $lc = ($filas | Measure-Object LINE_COVERED -Sum).Sum; $lm = ($filas | Measure-Object LINE_MISSED -Sum).Sum
    $ac = ($activas | Measure-Object LINE_COVERED -Sum).Sum; $am = ($activas | Measure-Object LINE_MISSED -Sum).Sum
    $t.L += $lc + $lm; $t.C += $lc; $t.LA += $ac + $am; $t.CA += $ac
    $resumen += "{0,-18} lineas {1,6:N1} %   {2,18:N1} %" -f $s, (100 * $lc / ($lc + $lm)), (100 * $ac / ($ac + $am))
}
$resumen += ""
$resumen += "{0,-18} lineas {1,6:N1} %   {2,18:N1} %" -f "TOTAL", (100 * $t.C / $t.L), (100 * $t.CA / $t.LA)
$resumen += ""
$resumen += "Clases sin uso en AWS por configuracion: $($inactivas -join ', ')"
$resumen | Out-File (Join-Path $local "resumen.txt") -Encoding utf8
$resumen | Out-Host
Write-Host "`nReportes HTML: $local\html\<servicio>\index.html" -ForegroundColor Green
Start-Process (Join-Path $local "html\ms-citas\index.html")

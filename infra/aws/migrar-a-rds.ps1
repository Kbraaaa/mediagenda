# Migra las 4 bases de MediAgenda del contenedor postgres de la EC2 a Amazon RDS, sin perder datos.
# Uso: migrar-a-rds.bat <endpoint de RDS>   (el endpoint lo imprime infra/aws/crear-rds.sh)
param([Parameter(Mandatory = $true)][string]$Endpoint, [string]$Ip = "3.139.58.88")
$ErrorActionPreference = "Continue"
$raiz = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
Set-Location $raiz
$salida = Join-Path $raiz "pruebas\resultado-migracion-rds.txt"
function Anotar($t) { $t | Out-File $salida -Append -Encoding utf8; Write-Host $t }
"Migracion a RDS - $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss') - $Endpoint" | Out-File $salida -Encoding utf8

$Clave = @("$env:USERPROFILE\.ssh\mediagenda-clave.pem", "$env:USERPROFILE\Downloads\mediagenda-clave.pem",
           "$env:USERPROFILE\Descargas\mediagenda-clave.pem", "$env:USERPROFILE\Documents\mediagenda-clave.pem") |
         Where-Object { Test-Path $_ } | Select-Object -First 1
$ssh = @("-i", $Clave, "-o", "StrictHostKeyChecking=accept-new", "-o", "ServerAliveInterval=30")
$destino = "ec2-user@$Ip"
$envServidor = "$env:USERPROFILE\.ssh\mediagenda-servidor.env"
$clave = (Get-Content $envServidor | Where-Object { $_ -match '^DB_PASSWORD=' }) -replace '^DB_PASSWORD=', ''

# 1. En el servidor: detener los servicios (nadie escribe), crear las bases en RDS, copiar y comparar
$bases = "pacientes_db citas_db farmacia_db notificaciones_db"
$remoto = @"
# Todo va dentro de una funcion: bash lee el script completo antes de ejecutar. Si no, los
# "docker run -i" leerian el resto del script desde la entrada estandar y se lo comerian.
migrar() {
set -e
cd mediagenda
DC='docker compose -f docker-compose.aws.yml'
`$DC stop ms-pacientes ms-citas ms-farmacia ms-notificaciones
PSQL='docker run -i --rm -e PGPASSWORD=$clave -e PGSSLMODE=require postgres:17 psql -h $Endpoint -U postgres -v ON_ERROR_STOP=1 -q'
for db in $bases; do
  echo "== `$db"
  `$PSQL -d postgres -tc "SELECT 1 FROM pg_database WHERE datname='`$db'" | grep -q 1 || `$PSQL -d postgres -c "CREATE DATABASE `$db"
  docker exec mediagenda-postgres-1 pg_dump -U postgres --no-owner --no-privileges --clean --if-exists `$db | `$PSQL -d `$db >/dev/null
  for t in `$(docker exec mediagenda-postgres-1 psql -U postgres -d `$db -Atc "SELECT tablename FROM pg_tables WHERE schemaname='public' ORDER BY 1"); do
    a=`$(docker exec mediagenda-postgres-1 psql -U postgres -d `$db -Atc "SELECT count(*) FROM `$t")
    b=`$(`$PSQL -d `$db -Atc "SELECT count(*) FROM `$t")
    [ "`$a" = "`$b" ] && echo "   `$t: `$a filas OK" || { echo "   `$t: contenedor `$a, RDS `$b  DIFERENTE"; exit 1; }
  done
done
echo COPIA OK
}
# El "#" final absorbe el \r que PowerShell agrega al terminar de enviar el texto
migrar #
"@
Anotar "1/3 Copiando las 4 bases al RDS (los servicios quedan detenidos unos minutos)..."
# El script viaja por la entrada estandar (bash -s): PowerShell 5.1 se come las comillas dobles
# cuando el texto va como parametro de ssh, y las consultas SQL las necesitan.
$log = ($remoto -replace "`r", "") | & ssh @ssh $destino "bash -s" 2>&1
$log | ForEach-Object { Anotar "$_" }
if (-not ($log -match "COPIA OK")) { Anotar "FALLO la copia: no se cambio nada. Arranco los servicios de nuevo con el contenedor."
    & ssh @ssh $destino "cd mediagenda && docker compose -f docker-compose.aws.yml start ms-pacientes ms-citas ms-farmacia ms-notificaciones" | Out-Null
    exit 1 }

# 2. Cambiar la conexion: DB_HOST=RDS y sin el perfil postgres-local
Anotar "2/3 Cambiando la conexion a RDS en $envServidor"
$lineas = @(Get-Content $envServidor | Where-Object { $_ -notmatch '^(DB_HOST|COMPOSE_PROFILES)=' }) + "DB_HOST=$Endpoint"
[IO.File]::WriteAllText($envServidor, (($lineas -join "`n") + "`n"), (New-Object Text.UTF8Encoding $false))

# 3. Desplegar con la conexion nueva y apagar el contenedor viejo (sin borrar su volumen, por si hay que volver)
Anotar "3/3 Desplegando con RDS..."
& powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot "desplegar.ps1") -Ip $Ip | Out-Host
& ssh @ssh $destino "docker stop mediagenda-postgres-1 >/dev/null 2>&1; docker ps --format '{{.Names}}'" 2>&1 | ForEach-Object { Anotar "   corriendo: $_" }
Anotar "Listo. Para volver al contenedor: quitar DB_HOST del .env del servidor y correr desplegar-aws.bat."

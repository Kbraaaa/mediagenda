# Despliega MediAgenda en la EC2 desde este PC (PowerShell 5.1, Windows 10/11).
# Empaqueta el ultimo commit, arma el .env del servidor (sin pasar por GitHub), lo sube por SSH
# y arranca docker-compose.aws.yml. Uso: desplegar-aws.bat (pide lo que falte).
param(
    [string]$Ip = "3.139.58.88",
    [string]$Clave = "",
    [string]$Remitente = ""
)
# Continue: en PowerShell 5.1 la salida de error de ssh/scp no debe cortar el script
$ErrorActionPreference = "Continue"
$raiz = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
Set-Location $raiz
$salida = Join-Path $raiz "pruebas\resultado-despliegue.txt"
function Anotar($t) { $t | Out-File $salida -Append -Encoding utf8; Write-Host $t }
"Despliegue MediAgenda - $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss') - $Ip" | Out-File $salida -Encoding utf8

# 1. Llave .pem: se busca en las carpetas habituales y se dejan permisos que SSH acepte
if (-not $Clave) {
    $candidatas = @("$env:USERPROFILE\.ssh\mediagenda-clave.pem", "$env:USERPROFILE\Downloads\mediagenda-clave.pem",
                    "$env:USERPROFILE\Descargas\mediagenda-clave.pem", "$env:USERPROFILE\Documents\mediagenda-clave.pem")
    $Clave = $candidatas | Where-Object { Test-Path $_ } | Select-Object -First 1
}
if (-not $Clave) { Anotar "FALLO no encontre mediagenda-clave.pem. Copiala en $env:USERPROFILE\.ssh\"; exit 1 }
icacls $Clave /inheritance:r | Out-Null
icacls $Clave /grant:r "$($env:USERNAME):(R)" | Out-Null
Anotar "Llave: $Clave"
$ssh = @("-i", $Clave, "-o", "StrictHostKeyChecking=accept-new", "-o", "ServerAliveInterval=30", "-o", "ConnectTimeout=15")
$destino = "ec2-user@$Ip"

# 2. .env del servidor. Se guarda en .ssh y se reutiliza (la clave de la base NO debe cambiar entre despliegues)
$envServidor = "$env:USERPROFILE\.ssh\mediagenda-servidor.env"
if (-not (Test-Path $envServidor)) {
    $local = @{}
    Get-Content (Join-Path $raiz ".env") | Where-Object { $_ -match '^\s*([A-Z_]+)=(.*)$' } |
        ForEach-Object { $local[$Matches[1]] = $Matches[2].Trim() }
    if (-not $Remitente) { $Remitente = Read-Host "Correo verificado en SES (remitente)" }
    $claveDb = -join ((48..57) + (65..90) + (97..122) | Get-Random -Count 24 | ForEach-Object { [char]$_ })
    $lineas = @(
        "DB_USER=postgres", "DB_PASSWORD=$claveDb", "AWS_REGION=us-east-2",
        "SQS_COLA_URL=https://sqs.us-east-2.amazonaws.com/991839263115/mediagenda-eventos",
        "CORREO_REMITENTE=$Remitente", "SMS=simulado",
        "GEMINI_API_KEY=$($local['GEMINI_API_KEY'])",
        "GEMINI_MODELO=$(if ($local['GEMINI_MODELO']) { $local['GEMINI_MODELO'] } else { 'gemini-3.5-flash-lite' })")
    # Saltos de linea LF y sin BOM: Docker Compose lee mal los \r de Windows
    [IO.File]::WriteAllText($envServidor, (($lineas -join "`n") + "`n"), (New-Object Text.UTF8Encoding $false))
    Anotar "Se creo $envServidor (clave de base de datos nueva)"
} else { Anotar "Se reutiliza $envServidor" }
# Nombre de las imagenes en ECR (el mismo que usa el despliegue automatico de GitHub Actions)
if (-not (Select-String -Path $envServidor -Pattern '^ECR_REGISTRO=' -Quiet)) {
    [IO.File]::AppendAllText($envServidor, "ECR_REGISTRO=991839263115.dkr.ecr.us-east-2.amazonaws.com/mediagenda`n")
}

# Se despliega el ultimo commit: avisar si hay cambios sin guardar
if (git status --porcelain) { Anotar "AVISO: hay cambios sin commit; no se despliegan. Corre subir-cambios.bat primero si los necesitas." }

# 3. Codigo: el ultimo commit de git (lo mismo que esta en GitHub, sin target ni .env)
$paquete = Join-Path $env:TEMP "mediagenda.tar.gz"
git archive --format=tar.gz -o $paquete HEAD
Anotar "Paquete: $([math]::Round((Get-Item $paquete).Length / 1KB)) KB del commit $(git rev-parse --short HEAD)"

# 4. Esperar a que el servidor termine de instalar Docker (user-data)
Anotar "Conectando con $destino ..."
for ($i = 0; $i -lt 30; $i++) {
    & ssh @ssh $destino "test -f instalacion-terminada.txt && docker compose version" 2>$null
    if ($LASTEXITCODE -eq 0) { break }
    Start-Sleep -Seconds 10
}
if ($LASTEXITCODE -ne 0) { Anotar "FALLO el servidor no responde o Docker no termino de instalarse"; exit 1 }

# 5. Subir y arrancar (la primera vez compila las 4 imagenes: 5 a 10 minutos)
& scp @ssh $paquete "${destino}:mediagenda.tar.gz"
& scp @ssh $envServidor "${destino}:mediagenda.env"
$remoto = "mkdir -p mediagenda && tar xzf mediagenda.tar.gz -C mediagenda && cp mediagenda.env mediagenda/.env && " +
          "cd mediagenda && docker compose -f docker-compose.aws.yml up -d --build 2>&1 | tail -20 && " +
          # nginx resuelve las direcciones de los servicios solo al arrancar: si un contenedor se recreo,
          # tiene otra IP interna y nginx devolveria 502. Reiniciarlo le hace buscar las direcciones nuevas.
          "docker compose -f docker-compose.aws.yml restart nginx && " +
          "docker compose -f docker-compose.aws.yml ps"
Anotar "Compilando y arrancando en el servidor (5 a 10 minutos la primera vez)..."
& ssh @ssh $destino $remoto 2>&1 | Tee-Object -Variable log | Out-Host
$log | Out-File $salida -Append -Encoding utf8

# 6. Esperar a que respondan los 4 servicios por nginx
Anotar "Esperando a que respondan las APIs en http://$Ip ..."
$rutas = @("/api/v1/pacientes", "/api/v1/medicos", "/api/v1/medicamentos", "/api/v1/notificaciones")
for ($i = 0; $i -lt 40; $i++) {
    $ok = 0
    foreach ($r in $rutas) {
        try { if ((Invoke-WebRequest "http://$Ip$r" -UseBasicParsing -TimeoutSec 10).StatusCode -eq 200) { $ok++ } } catch { }
    }
    if ($ok -eq 4) { break }
    Start-Sleep -Seconds 15
}
if ($ok -eq 4) { Anotar "RESULTADO: DESPLEGADO OK - http://$Ip/api/v1/pacientes" }
else {
    Anotar "RESULTADO: solo $ok de 4 servicios responden. Logs:"
    & ssh @ssh $destino "cd mediagenda && docker compose -f docker-compose.aws.yml logs --tail 40" 2>&1 | Out-File $salida -Append -Encoding utf8
}

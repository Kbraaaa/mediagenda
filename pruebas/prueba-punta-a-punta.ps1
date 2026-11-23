# Prueba de punta a punta de MediAgenda contra los servicios levantados con Docker.
# Uso: .\prueba-punta-a-punta.ps1 [-Base http://localhost:8000]
param([string]$Base = "http://localhost:8000")

$ErrorActionPreference = "Stop"
$salida = Join-Path $PSScriptRoot "resultado-punta-a-punta.txt"
"Prueba punta a punta MediAgenda - $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss') - $Base" | Out-File $salida -Encoding utf8
$script:fallos = 0

function Anotar([string]$texto) { $texto | Out-File $salida -Append -Encoding utf8; Write-Host $texto }

function Llamar([string]$metodo, [string]$ruta, $cuerpo = $null) {
    $url = "$Base$ruta"
    $json = if ($cuerpo -ne $null) { $cuerpo | ConvertTo-Json -Depth 6 -Compress } else { $null }
    $reloj = [System.Diagnostics.Stopwatch]::StartNew()
    try {
        $p = @{ Uri = $url; Method = $metodo; UseBasicParsing = $true; TimeoutSec = 30 }
        if ($json) { $p.Body = [System.Text.Encoding]::UTF8.GetBytes($json); $p.ContentType = "application/json" }
        $r = Invoke-WebRequest @p
        $reloj.Stop()
        $datos = $null
        if ($r.Content) { try { $datos = $r.Content | ConvertFrom-Json } catch { $datos = $r.Content } }
        return @{ estado = [int]$r.StatusCode; datos = $datos; ms = $reloj.ElapsedMilliseconds }
    } catch {
        $reloj.Stop()
        $estado = 0
        $texto = $_.Exception.Message
        if ($_.Exception.Response) {
            $estado = [int]$_.Exception.Response.StatusCode
            try {
                $lector = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
                $texto = $lector.ReadToEnd()
            } catch { }
        }
        $datos = $null
        try { $datos = $texto | ConvertFrom-Json } catch { $datos = $texto }
        return @{ estado = $estado; datos = $datos; ms = $reloj.ElapsedMilliseconds }
    }
}

function Verificar([string]$paso, $respuesta, [int]$esperado, [string]$detalle = "") {
    $ok = $respuesta.estado -eq $esperado
    if (-not $ok) { $script:fallos++ }
    $marca = if ($ok) { "OK   " } else { "FALLO" }
    $lento = if ($respuesta.ms -gt 500) { " (LENTO > 500 ms)" } else { "" }
    $extra = $detalle
    if (-not $ok) { $extra = "$detalle | respuesta: $(($respuesta.datos | ConvertTo-Json -Depth 4 -Compress))" }
    Anotar ("{0} {1,-58} esperado {2}, obtuvo {3}, {4} ms{5}  {6}" -f $marca, $paso, $esperado, $respuesta.estado, $respuesta.ms, $lento, $extra)
    return $ok
}

# ---------- 0. Esperar a que los 4 servicios respondan (primera vez puede tardar varios minutos) ----------
Anotar "Esperando a que los servicios esten listos..."
$rutas = @("/api/v1/pacientes", "/api/v1/medicos", "/api/v1/medicamentos", "/api/v1/notificaciones")
$listo = $false
for ($i = 0; $i -lt 90 -and -not $listo; $i++) {
    $listo = $true
    foreach ($r in $rutas) { if ((Llamar GET $r).estado -ne 200) { $listo = $false } }
    if (-not $listo) { Start-Sleep -Seconds 10 }
}
if (-not $listo) { Anotar "FALLO los servicios no respondieron en 15 minutos. Revisa docker-log.txt"; exit 1 }
Anotar "Servicios listos.`n"

# Calentamiento: la primera peticion a cada servicio es mas lenta (la JVM aun esta compilando)
foreach ($r in $rutas) { 1..5 | ForEach-Object { Llamar GET $r | Out-Null } }

$sufijo = Get-Random -Minimum 100000 -Maximum 999999

# ---------- 1. Paciente ----------
$r = Llamar POST "/api/v1/pacientes" @{ tipoDocumento = "CC"; numeroDocumento = "10$sufijo"; nombres = "Laura Valentina";
    apellidos = "Gomez Rojas"; correo = "laura.$sufijo@correo.com"; telefono = "3101234567"; fechaNacimiento = "1998-05-14";
    tipoSangre = "O+"; alergias = "Penicilina" }
Verificar "1. Registrar paciente (ms-pacientes)" $r 201 $r.datos.codigo | Out-Null
$paciente = $r.datos.codigo
$r = Llamar POST "/api/v1/pacientes" @{ tipoDocumento = "CC"; numeroDocumento = "10$sufijo"; nombres = "Otra"; apellidos = "Persona";
    correo = "otra.$sufijo@correo.com"; fechaNacimiento = "1990-01-01" }
Verificar "1b. Mismo documento otra vez -> 409" $r 409 $r.datos.codigo | Out-Null
$r = Llamar GET "/api/v1/pacientes/$paciente"
Verificar "1c. Consultar paciente" $r 200 | Out-Null

# ---------- 2. Medico y agenda ----------
$r = Llamar POST "/api/v1/medicos" @{ nombre = "Andrea Lopez"; especialidad = "MEDICINA_GENERAL"; correo = "andrea.$sufijo@mediagenda.co" }
Verificar "2. Registrar medico (ms-citas)" $r 201 $r.datos.codigo | Out-Null
$medico = $r.datos.codigo
$franjas = $null
for ($d = 2; $d -le 8 -and -not $franjas; $d++) {
    $fecha = (Get-Date).AddDays($d).ToString("yyyy-MM-dd")
    $r = Llamar POST "/api/v1/medicos/$medico/franjas" @{ fecha = $fecha; horaInicio = "08:00"; horaFin = "10:00"; duracionMinutos = 30 }
    if ($r.estado -eq 422) { Anotar "      $fecha es festivo (Nager.Date), se prueba el dia siguiente" }
    elseif (Verificar "2b. Abrir agenda $fecha 08:00-10:00 cada 30 min" $r 201 "$(@($r.datos).Count) franjas") { $franjas = @($r.datos) }
}
$franja1 = $franjas[0].idFranja
$franja2 = $franjas[1].idFranja
$r = Llamar GET "/api/v1/franjas?medico=$medico"
Verificar "2c. Listar franjas disponibles" $r 200 "$($r.datos.totalElementos) disponibles" | Out-Null

# ---------- 3. Cita, concurrencia y triage con IA ----------
$r = Llamar POST "/api/v1/citas" @{ codigoPaciente = $paciente; idFranja = $franja1;
    sintomas = "Tengo dolor en el pecho y me falta el aire desde ayer en la noche" }
Verificar "3. Agendar cita (llama a ms-pacientes)" $r 201 "$($r.datos.codigo) triage=$($r.datos.triage.estado)" | Out-Null
$cita = $r.datos.codigo
$r = Llamar POST "/api/v1/citas" @{ codigoPaciente = $paciente; idFranja = $franja1; sintomas = "Otra persona quiere la misma franja" }
Verificar "3b. Misma franja otra vez -> 409 FRANJA_OCUPADA" $r 409 $r.datos.codigo | Out-Null
$r = Llamar POST "/api/v1/citas" @{ codigoPaciente = "PAC-999999"; idFranja = $franja2; sintomas = "Paciente que no existe" }
Verificar "3c. Paciente inexistente -> 404" $r 404 $r.datos.codigo | Out-Null

Anotar "      Esperando el triage con IA (segundo plano)..."
$triage = $null
for ($i = 0; $i -lt 12; $i++) {
    Start-Sleep -Seconds 5
    $r = Llamar GET "/api/v1/citas/$cita"
    if ($r.datos.triage.estado -ne "PENDIENTE") { $triage = $r.datos.triage; break }
}
if ($triage) {
    Anotar ("      Triage: estado={0} especialidad={1} prioridad={2} motivo={3}" -f $triage.estado, $triage.especialidadSugerida, $triage.prioridad, $triage.motivo)
    if ($triage.estado -ne "COMPLETADO") { $script:fallos++ }
} else { Anotar "FALLO el triage siguio PENDIENTE despues de 60 s"; $script:fallos++ }

$r = Llamar PUT "/api/v1/citas/$cita/reprogramacion" @{ idFranja = $franja2 }
Verificar "3d. Reprogramar a la segunda franja" $r 200 | Out-Null
$r = Llamar GET "/api/v1/citas?paciente=$paciente"
Verificar "3e. Listar citas del paciente" $r 200 "$($r.datos.totalElementos) cita(s)" | Out-Null
$r = Llamar PATCH "/api/v1/citas/$cita/atencion"
Verificar "3f. Marcar cita atendida" $r 200 $r.datos.estado | Out-Null

# ---------- 4. Farmacia ----------
$r = Llamar GET "/api/v1/medicamentos?nombre=acetaminofen"
Verificar "4. Buscar medicamento (ms-farmacia)" $r 200 | Out-Null
$stockAntes = $r.datos.contenido[0].stock
$r = Llamar POST "/api/v1/formulas" @{ codigoCita = $cita; items = @(
        @{ codigoMedicamento = "MDT-0001"; cantidad = 10; indicaciones = "1 tableta cada 8 horas" },
        @{ codigoMedicamento = "MDT-0004"; cantidad = 5 }) }
Verificar "4b. Emitir formula (llama a ms-citas)" $r 201 $r.datos.codigo | Out-Null
$formula = $r.datos.codigo
$r = Llamar POST "/api/v1/formulas" @{ codigoCita = $cita; items = @(@{ codigoMedicamento = "MDT-0001"; cantidad = 1 }) }
Verificar "4c. Segunda formula para la misma cita -> 409" $r 409 $r.datos.codigo | Out-Null
$r = Llamar POST "/api/v1/formulas/$formula/dispensacion"
Verificar "4d. Dispensar formula" $r 200 $r.datos.estado | Out-Null
$r = Llamar POST "/api/v1/formulas/$formula/dispensacion"
Verificar "4e. Dispensar otra vez -> 409" $r 409 $r.datos.codigo | Out-Null
$r = Llamar GET "/api/v1/medicamentos/MDT-0001"
Verificar "4f. Stock descontado" $r 200 "antes $stockAntes, ahora $($r.datos.stock)" | Out-Null
if ($r.datos.stock -ne ($stockAntes - 10)) { Anotar "FALLO el stock no bajo 10 unidades"; $script:fallos++ }

# ---------- 5. Notificaciones ----------
Start-Sleep -Seconds 5
$r = Llamar GET "/api/v1/notificaciones?paciente=$paciente&size=20"
Verificar "5. Historial de notificaciones (ms-notificaciones)" $r 200 "$($r.datos.totalElementos) avisos" | Out-Null
foreach ($n in $r.datos.contenido) { Anotar ("      {0} {1,-24} {2,-8} {3}" -f $n.codigo, $n.tipo, $n.estado, $n.asunto) }
if ($r.datos.contenido.Count -gt 0) {
    $una = $r.datos.contenido[0].codigo
    $r2 = Llamar PATCH "/api/v1/notificaciones/$una/lectura"
    Verificar "5b. Marcar aviso como leido" $r2 200 | Out-Null
}
$r = Llamar POST "/api/v1/notificaciones" @{ destinatario = "laura.$sufijo@correo.com"; asunto = "Prueba manual"; cuerpo = "Hola desde la prueba"; codigoPaciente = $paciente }
Verificar "5c. Aviso manual -> 202" $r 202 $r.datos.codigo | Out-Null
try {
    $correos = Invoke-RestMethod -Uri "http://localhost:8025/api/v1/messages" -UseBasicParsing
    Anotar "      Mailpit recibio $($correos.total) correos en total (ver http://localhost:8025)"
} catch { Anotar "      No se pudo consultar Mailpit" }

Anotar ""
if ($script:fallos -eq 0) { Anotar "RESULTADO: TODO OK" } else { Anotar "RESULTADO: $($script:fallos) FALLO(S)" }

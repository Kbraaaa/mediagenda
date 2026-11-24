# Configura el bot de Telegram de MediAgenda sin copiar valores a mano.
# 1) Pide el token que entrego @BotFather (no se muestra en pantalla) y lo valida.
# 2) Espera a que le escribas /start al bot y toma el id de ese chat.
# 3) Manda un mensaje de prueba.
# 4) Guarda TELEGRAM_TOKEN, TELEGRAM_CHAT_ID y SMS=telegram en el .env local y en el .env del servidor.
$ErrorActionPreference = "Stop"
$raiz = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$seguro = Read-Host "Pega el token de @BotFather (no se vera)" -AsSecureString
$token = [Runtime.InteropServices.Marshal]::PtrToStringBSTR([Runtime.InteropServices.Marshal]::SecureStringToBSTR($seguro)).Trim()
$api = "https://api.telegram.org/bot$token"
try { $bot = Invoke-RestMethod "$api/getMe" -TimeoutSec 15 } catch { Write-Host "Token invalido o sin internet." -ForegroundColor Red; exit 1 }
Write-Host "Bot encontrado: @$($bot.result.username)" -ForegroundColor Green
Write-Host "Ahora abre Telegram, busca @$($bot.result.username) y enviale /start (esperando hasta 2 minutos)..."
$chat = $null
for ($i = 0; $i -lt 24 -and -not $chat; $i++) {
    $upd = Invoke-RestMethod "$api/getUpdates?timeout=5" -TimeoutSec 15
    $msg = $upd.result | Where-Object { $_.message } | Select-Object -Last 1
    if ($msg) { $chat = [string]$msg.message.chat.id; $nombre = $msg.message.chat.first_name }
}
if (-not $chat) { Write-Host "No llego el /start. Vuelve a correr este archivo." -ForegroundColor Red; exit 1 }
Write-Host "Chat encontrado: $nombre ($chat)" -ForegroundColor Green
$cuerpo = @{ chat_id = $chat; text = "MediAgenda conectado. Aqui llegaran los avisos de citas y formulas." } | ConvertTo-Json
Invoke-RestMethod "$api/sendMessage" -Method Post -ContentType "application/json; charset=utf-8" `
    -Body ([Text.Encoding]::UTF8.GetBytes($cuerpo)) | Out-Null

function Fijar($archivo, $valores) {
    if (-not (Test-Path $archivo)) { return }
    $lineas = @(Get-Content $archivo | Where-Object { $_ -notmatch '^(SMS|TELEGRAM_TOKEN|TELEGRAM_CHAT_ID)=' })
    $lineas += $valores
    # LF y sin BOM: Docker Compose lee mal los \r de Windows
    [IO.File]::WriteAllText($archivo, (($lineas -join "`n") + "`n"), (New-Object Text.UTF8Encoding $false))
    Write-Host "Actualizado: $archivo"
}
$valores = @("SMS=telegram", "TELEGRAM_TOKEN=$token", "TELEGRAM_CHAT_ID=$chat")
Fijar (Join-Path $raiz ".env") $valores
Fijar "$env:USERPROFILE\.ssh\mediagenda-servidor.env" $valores
Write-Host "`nListo: revisa Telegram, debio llegarte el mensaje de prueba." -ForegroundColor Green
Write-Host "Para usarlo en AWS corre desplegar-aws.bat (lleva el .env nuevo al servidor)."

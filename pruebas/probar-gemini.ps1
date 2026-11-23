# Diagnostico de la clave de Gemini: lista los modelos disponibles y prueba el modelo configurado.
# Lee GEMINI_API_KEY y GEMINI_MODELO del archivo .env. Nunca escribe la clave en el resultado.
$ErrorActionPreference = "Stop"
$raiz = Split-Path $PSScriptRoot -Parent
$salida = Join-Path $PSScriptRoot "resultado-gemini.txt"
$env_ = @{}
Get-Content (Join-Path $raiz ".env") | ForEach-Object {
    if ($_ -match '^\s*([A-Z_]+)\s*=\s*(.*)\s*$') { $env_[$Matches[1]] = $Matches[2].Trim() }
}
$clave = $env_["GEMINI_API_KEY"]
$modelo = if ($env_["GEMINI_MODELO"]) { $env_["GEMINI_MODELO"] } else { "gemini-3.5-flash-lite" }
"Diagnostico Gemini - $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')" | Out-File $salida -Encoding utf8
"Clave: $($clave.Length) caracteres, empieza por '$($clave.Substring(0, [Math]::Min(3, $clave.Length)))'" | Out-File $salida -Append -Encoding utf8
"Modelo configurado: $modelo" | Out-File $salida -Append -Encoding utf8
$cabeceras = @{ "x-goog-api-key" = $clave }
$api = "https://generativelanguage.googleapis.com/v1beta"

function Error-Texto($e) {
    $t = $e.Exception.Message
    if ($e.Exception.Response) {
        try { $t = "HTTP $([int]$e.Exception.Response.StatusCode): " + (New-Object System.IO.StreamReader($e.Exception.Response.GetResponseStream())).ReadToEnd() } catch { }
    }
    return $t
}

try {
    $modelos = Invoke-RestMethod -Uri "$api/models?pageSize=200" -Headers $cabeceras -UseBasicParsing -TimeoutSec 30
    $nombres = $modelos.models | Where-Object { $_.supportedGenerationMethods -contains "generateContent" } |
            ForEach-Object { $_.name -replace '^models/', '' }
    "Modelos con generateContent ($(@($nombres).Count)):" | Out-File $salida -Append -Encoding utf8
    $nombres | Where-Object { $_ -match 'flash' } | ForEach-Object { "  $_" } | Out-File $salida -Append -Encoding utf8
} catch {
    "ERROR al listar modelos: $(Error-Texto $_)" | Out-File $salida -Append -Encoding utf8
}

$cuerpo = @{ contents = @(@{ parts = @(@{ text = "Responde solo con un JSON: {""ok"": true}" }) });
             generationConfig = @{ responseMimeType = "application/json"; temperature = 0 } } | ConvertTo-Json -Depth 6
# Se prueba el modelo configurado y dos alternativas livianas, cada una con limite de 30 segundos
foreach ($m in @($modelo, "gemini-flash-lite-latest", "gemini-3.8-flash") | Select-Object -Unique) {
    try {
        $reloj = [System.Diagnostics.Stopwatch]::StartNew()
        $r = Invoke-RestMethod -Uri "$api/models/${m}:generateContent" -Method Post -Headers $cabeceras `
                -ContentType "application/json" -Body $cuerpo -UseBasicParsing -TimeoutSec 30
        $reloj.Stop()
        "PRUEBA OK con $m en $($reloj.ElapsedMilliseconds) ms. Respuesta: $($r.candidates[0].content.parts[0].text)" |
                Out-File $salida -Append -Encoding utf8
    } catch {
        "PRUEBA FALLIDA con ${m}: $(Error-Texto $_)" | Out-File $salida -Append -Encoding utf8
    }
}
Get-Content $salida

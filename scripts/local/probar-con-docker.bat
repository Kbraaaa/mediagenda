@echo off
REM Levanta MediAgenda con Docker y corre la prueba de punta a punta.
REM Resultado: docker-log.txt (construccion) y pruebas\resultado-punta-a-punta.txt (prueba)
cd /d "%~dp0..\.."
echo Revisando Docker...
docker info >nul 2>&1
if errorlevel 1 (
  echo Abriendo Docker Desktop...
  start "" "C:\Program Files\Docker\Docker\Docker Desktop.exe"
  for /l %%i in (1,1,30) do (
    timeout /t 5 /nobreak >nul
    docker info >nul 2>&1 && goto :docker_listo
  )
  echo Docker no arranco > docker-log.txt
  exit /b 1
)
:docker_listo
echo Probando la clave de Gemini...
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0..\..\pruebas\probar-gemini.ps1"
echo Construyendo y levantando (la primera vez tarda varios minutos)...
docker compose up -d --build > docker-log.txt 2>&1
docker compose ps >> docker-log.txt 2>&1
echo Corriendo la prueba de punta a punta...
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0..\..\pruebas\prueba-punta-a-punta.ps1"
docker compose logs ms-citas --tail 300 > pruebas\log-ms-citas.txt 2>&1
echo.
echo Listo. Revisa pruebas\resultado-punta-a-punta.txt

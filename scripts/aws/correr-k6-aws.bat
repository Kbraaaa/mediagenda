@echo off
REM Pruebas de carga, estres y concurrencia con k6, ejecutadas en el servidor de AWS.
REM Uso: doble clic (todas, ~15 min) o: correr-k6-aws.bat concurrencia ^| carga ^| estres
cd /d "%~dp0..\.."
powershell -NoProfile -ExecutionPolicy Bypass -File "infra\aws\k6.ps1" %*
pause

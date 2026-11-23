@echo off
REM Despliega MediAgenda en AWS (EC2) desde este PC. Ver infra\aws\desplegar.ps1
cd /d "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "infra\aws\desplegar.ps1" %*
echo.
echo Resultado guardado en pruebas\resultado-despliegue.txt
pause

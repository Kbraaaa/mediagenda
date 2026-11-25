@echo off
REM Migra la base de datos del contenedor a Amazon RDS. Uso: migrar-a-rds.bat ENDPOINT
cd /d "%~dp0..\.."
powershell -NoProfile -ExecutionPolicy Bypass -File "infra\aws\migrar-a-rds.ps1" -Endpoint %1
pause

@echo off
REM Borra los datos que dejaron las pruebas (Postman, k6) en Amazon RDS y carga datos de demostracion.
REM La evidencia de las pruebas queda en pruebas\postman, pruebas\k6 y pruebas\cobertura-aws.
cd /d "%~dp0..\.."
echo Esto BORRA todos los pacientes, medicos, citas, formulas y notificaciones de la base en AWS.
set /p OK=Escribe SI para continuar: 
if /i not "%OK%"=="SI" (echo Cancelado. & pause & exit /b 0)
powershell -NoProfile -ExecutionPolicy Bypass -File "infra\aws\reiniciar-datos.ps1"
pause

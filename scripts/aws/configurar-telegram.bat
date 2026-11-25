@echo off
REM Conecta el bot de Telegram de MediAgenda (ver infra\telegram\configurar.ps1)
cd /d "%~dp0..\.."
powershell -NoProfile -ExecutionPolicy Bypass -File "infra\telegram\configurar.ps1"
pause

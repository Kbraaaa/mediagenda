@echo off
REM Cobertura del codigo con las APIs desplegadas en AWS (JaCoCo en el servidor + Postman desde este PC).
cd /d "%~dp0..\.."
powershell -NoProfile -ExecutionPolicy Bypass -File "infra\aws\cobertura.ps1" %*
pause

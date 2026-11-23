@echo off
REM Solo prueba la clave de Gemini (tarda maximo 2 minutos). Resultado: pruebas\resultado-gemini.txt
cd /d "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0pruebas\probar-gemini.ps1"

@echo off
REM Corre la coleccion de Postman con Newman y genera un reporte HTML.
REM Uso: doble clic (entorno Local) o desde consola:  correr-postman.bat AWS
setlocal
cd /d "%~dp0"
set ENTORNO=%1
if "%ENTORNO%"=="" set ENTORNO=Local

where npx >nul 2>nul
if errorlevel 1 (
  echo No se encontro Node.js. Instala Node LTS desde https://nodejs.org o usa el Runner de Postman.
  pause
  exit /b 1
)

echo Corriendo la coleccion contra el entorno %ENTORNO% ...
call npx --yes -p newman -p newman-reporter-htmlextra newman run MediAgenda.postman_collection.json ^
  -e MediAgenda-%ENTORNO%.postman_environment.json ^
  -r cli,htmlextra --reporter-htmlextra-export reporte-%ENTORNO%.html ^
  --reporter-htmlextra-title "MediAgenda - pruebas de API (%ENTORNO%)" > resultado-postman-%ENTORNO%.txt 2>&1
set CODIGO=%ERRORLEVEL%
type resultado-postman-%ENTORNO%.txt
echo CODIGO_SALIDA=%CODIGO%>> resultado-postman-%ENTORNO%.txt
echo.
echo Reporte: %~dp0reporte-%ENTORNO%.html
start "" "reporte-%ENTORNO%.html"
pause

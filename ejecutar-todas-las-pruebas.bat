@echo off
REM Compila y prueba TODOS los microservicios, uno tras otro.
REM Cada uno deja su detalle en <servicio>\resultado-pruebas.txt; aqui queda el resumen.
cd /d "%~dp0"
echo Resumen de pruebas > resumen-pruebas.txt
for %%S in (ms-pacientes ms-citas ms-farmacia ms-notificaciones) do (
  if exist "%%S\ejecutar-pruebas.bat" (
    echo Probando %%S ...
    call "%%S\ejecutar-pruebas.bat" >nul
    cd /d "%~dp0"
    findstr /C:"Tests run:" /C:"BUILD" /C:"CODIGO_SALIDA" "%%S\resultado-pruebas.txt" > "%%S\resumen.tmp"
    echo ==== %%S >> resumen-pruebas.txt
    type "%%S\resumen.tmp" >> resumen-pruebas.txt
  )
)
cd /d "%~dp0"
echo.
echo Listo. Revisa resumen-pruebas.txt

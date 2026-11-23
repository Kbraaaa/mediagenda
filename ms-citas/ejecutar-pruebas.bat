@echo off
REM Compila ms-citas, corre las pruebas y verifica la cobertura (JaCoCo >= 90 %).
REM El resultado queda en resultado-pruebas.txt y el reporte en target\site\jacoco\index.html
cd /d "%~dp0"

if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" goto :tienejava
for /d %%J in ("%USERPROFILE%\.jdks\*") do if exist "%%J\bin\java.exe" set "JAVA_HOME=%%J"
for /d %%J in ("C:\Program Files\Java\jdk-2*") do if exist "%%J\bin\java.exe" set "JAVA_HOME=%%J"
for /d %%J in ("C:\Program Files\Eclipse Adoptium\jdk-2*") do if exist "%%J\bin\java.exe" set "JAVA_HOME=%%J"
:tienejava
echo JAVA_HOME=%JAVA_HOME% > resultado-pruebas.txt
call mvnw.cmd -B verify >> resultado-pruebas.txt 2>&1
echo CODIGO_SALIDA=%ERRORLEVEL% >> resultado-pruebas.txt
echo.
echo Listo. Revisa resultado-pruebas.txt

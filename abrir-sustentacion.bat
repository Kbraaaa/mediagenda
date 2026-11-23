@echo off
rem Abre todo lo necesario para la sustentacion de pruebas de MediAgenda
set "RAIZ=C:\proyecto 803\mediagenda"
set "T=src\test\java\com\mediagenda\citas"

rem 1) Reportes JaCoCo de los 4 microservicios en Chrome
start "" chrome --new-window "file:///C:/proyecto 803/mediagenda/ms-pacientes/target/site/jacoco/index.html" "file:///C:/proyecto 803/mediagenda/ms-citas/target/site/jacoco/index.html" "file:///C:/proyecto 803/mediagenda/ms-farmacia/target/site/jacoco/index.html" "file:///C:/proyecto 803/mediagenda/ms-notificaciones/target/site/jacoco/index.html"

rem 2) Resumen de la ultima corrida (respaldo) en el Bloc de notas
start "" notepad "%RAIZ%\resumen-pruebas.txt"

rem 3) Pruebas representativas y pom.xml en IntelliJ (instancia abierta)
set "IDEA="
for /f "delims=" %%i in ('powershell -NoProfile -Command "(Get-Process idea64 -ErrorAction SilentlyContinue | Select-Object -First 1).Path"') do set "IDEA=%%i"
if defined IDEA (
  "%IDEA%" "%RAIZ%\ms-citas\pom.xml"
  timeout /t 2 /nobreak >nul
  "%IDEA%" "%RAIZ%\ms-citas\%T%\domain\model\ModeloTest.java"
  "%IDEA%" "%RAIZ%\ms-citas\%T%\infrastructure\rest_client\ClientesRestTest.java"
  "%IDEA%" "%RAIZ%\ms-citas\%T%\infrastructure\entry_points\ControladoresTest.java"
  "%IDEA%" "%RAIZ%\ms-citas\%T%\domain\usecase\AgendarCitaUseCaseTest.java"
) else (
  echo IntelliJ no esta abierto: abre el proyecto y luego vuelve a correr este archivo.
)

rem 4) Terminal lista para correr las pruebas en vivo
start "Pruebas MediAgenda" cmd /k "cd /d "%RAIZ%" && echo. && echo Para correr las 239 pruebas en vivo escribe:  ejecutar-todas-las-pruebas.bat && echo."

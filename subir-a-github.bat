@echo off
REM Crea el repositorio git local, hace el primer commit y lo sube a GitHub.
REM Antes: crea en github.com un repositorio VACIO (sin README) llamado mediagenda.
setlocal
cd /d "%~dp0"
where git >nul 2>nul || (echo No se encontro Git. Instalalo desde https://git-scm.com & pause & exit /b 1)

if not exist .git git init -b main
git add -A

REM Seguridad: el .env (claves) nunca debe quedar en el commit
git diff --cached --name-only | findstr /x /c:".env" >nul && (
  echo ERROR: el archivo .env quedo incluido. No se sube nada.
  git reset -q
  pause & exit /b 1
)
echo Archivos que se van a subir:
git diff --cached --name-only | find /c /v ""
git commit -q -m "MediAgenda: 4 microservicios, pruebas, Docker y adaptadores de AWS" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>" -m "Claude-Session: https://claude.ai/code/session_01CuvT6cDcV3H2gZt3ish4EJ"

git remote get-url origin >nul 2>nul || (
  set /p URL=Pega la URL del repositorio de GitHub ^(https://github.com/USUARIO/mediagenda.git^): 
  call git remote add origin %%URL%%
)
git push -u origin main
echo.
echo Listo. Revisa la pestana Actions del repositorio: deben correr las pruebas de los 4 servicios.
pause

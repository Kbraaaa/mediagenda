@echo off
REM Guarda los cambios en git y los sube a GitHub (GitHub Actions vuelve a correr las pruebas).
cd /d "%~dp0"
git add -A
git diff --cached --name-only | findstr /x /c:".env" >nul && (echo ERROR: .env incluido, no se sube nada & git reset -q & pause & exit /b 1)
set MENSAJE=%*
if "%MENSAJE%"=="" set /p MENSAJE=Describe el cambio: 
git commit -m "%MENSAJE%" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>" -m "Claude-Session: https://claude.ai/code/session_01CuvT6cDcV3H2gZt3ish4EJ"
git push
pause

@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"

:: Verification que javadoc est accessible
where javadoc >nul 2>nul
if !errorlevel! neq 0 (
    echo [!] Erreur : 'javadoc' est introuvable. Verifie ton PATH.
    pause
    exit /b
)

if not exist javadoc mkdir javadoc

echo Generation de la documentation (Javadoc)...
:: Generation
javadoc -d javadoc ^
    -cp "lib\jade.jar" ^
    -encoding UTF-8 -charset UTF-8 ^
    -private ^
    -author -version ^
    -windowtitle "Documentation SMA Atelier" ^
    src\Main.java src\agents\*.java src\model\*.java src\utils\*.java

if !errorlevel! equ 0 (
    echo.
    echo [OK] Documentation generee dans le dossier 'javadoc'.
) else (
    echo [X] Erreur lors de la generation.
)
pause
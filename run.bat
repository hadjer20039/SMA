@echo off
setlocal enabledelayedexpansion
:: On se place proprement dans le dossier du script meme s'il y a des espaces
cd /d "%~dp0"

:: Verification de Java
where javac >nul 2>nul
if !errorlevel! neq 0 (
    echo [!] Erreur : 'javac' est introuvable. Verifie ton PATH.
    pause
    exit /b
)

:: Parametres par defaut
set "NB=%~1"
if "!NB!"=="" set "NB=3"
set "L1=%~2"
if "!L1!"=="" set "L1=1000"
set "L2=%~3"
if "!L2!"=="" set "L2=2000"
set "L3=%~4"
if "!L3!"=="" set "L3=500"

if not exist classes mkdir classes

echo [INFO] Compilation des sources en cours...
javac -d classes -cp "lib\jade.jar" src\Main.java src\agents\*.java src\model\*.java src\utils\*.java

if !errorlevel! equ 0 (
    echo.
    echo [OK] Lancement de JADE - Robots : !NB!
    java -cp "lib\jade.jar;classes" Main !NB! !L1! !L2! !L3!
) else (
    echo [X] Erreur : La compilation a echoue.
)

pause
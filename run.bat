@echo off
pushd "%~dp0"

echo [INFO] Configuration...

REM 
REM 
set JAVA_HOME=C:\Program Files\Java\jdk-19

if not exist "%JAVA_HOME%\bin\javac.exe" (
    echo [ERREUR] JDK introuvable dans %JAVA_HOME%
    pause
    exit /b
)

set JAVAC_CMD="%JAVA_HOME%\bin\javac.exe"
set JAVA_CMD="%JAVA_HOME%\bin\java.exe"

REM 
set NB_ROBOTS=3
set LAMBDA1=1000
set LAMBDA2=2000
set LAMBDA3=500

REM 
if not exist classes mkdir classes

REM 
echo [INFO] Entree dans le dossier src...
cd src

echo [INFO] Compilation en cours...
REM 
REM

if %ERRORLEVEL% NEQ 0 (
    echo [ERREUR] La compilation a echoue.
    cd ..
    pause
    exit /b
)

REM 
cd ..

REM 
echo [INFO] Lancement de la simulation...
REM 
%JAVA_CMD% -cp "lib\jade.jar;classes" Main %NB_ROBOTS% %LAMBDA1% %LAMBDA2% %LAMBDA3%

pause
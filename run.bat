@echo off
pushd "%~dp0"

echo [INFO] Configuration...

REM --- 1. CONFIGURATION DU JDK ---
REM On utilise ton JDK-19 qui fonctionne
set JAVA_HOME=C:\Program Files\Java\jdk-19

if not exist "%JAVA_HOME%\bin\javac.exe" (
    echo [ERREUR] JDK introuvable dans %JAVA_HOME%
    pause
    exit /b
)

set JAVAC_CMD="%JAVA_HOME%\bin\javac.exe"
set JAVA_CMD="%JAVA_HOME%\bin\java.exe"

REM --- 2. PARAMETRES ---
set NB_ROBOTS=3
set LAMBDA1=1000
set LAMBDA2=2000
set LAMBDA3=500

REM --- 3. NETTOYAGE ET PREPARATION ---
if not exist classes mkdir classes

REM --- 4. COMPILATION (METHODE SIMPLE) ---
echo [INFO] Entree dans le dossier src...
cd src

echo [INFO] Compilation en cours...
REM L'astuce : on compile depuis l'interieur, donc pas de chemins compliques avec espaces !
REM On remonte d'un cran (..) pour trouver la lib et le dossier de sortie (classes)

%JAVAC_CMD% -cp "..\lib\jade.jar" -d "..\classes" Main.java agents\*.java model\*.java utils\*.java

if %ERRORLEVEL% NEQ 0 (
    echo [ERREUR] La compilation a echoue.
    cd ..
    pause
    exit /b
)

REM On ressort du dossier src
cd ..

REM --- 5. EXECUTION ---
echo [INFO] Lancement de la simulation...
REM Note : sous Windows le separateur de classpath est ;
%JAVA_CMD% -cp "lib\jade.jar;classes" Main %NB_ROBOTS% %LAMBDA1% %LAMBDA2% %LAMBDA3%

pause
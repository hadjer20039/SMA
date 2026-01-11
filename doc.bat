@echo off
echo [INFO] Generation de la Javadoc...

REM --- CONFIGURATION ---
REM On s'assure que le dossier de sortie existe
if not exist javadoc mkdir javadoc

REM --- LOCALISATION DU JDK (Comme dans run.bat) ---
set JAVA_HOME=C:\Program Files\Java\jdk-19
set JAVADOC_CMD="%JAVA_HOME%\bin\javadoc.exe"

REM --- EXECUTION ---
REM Options expliquées :
REM -d javadoc : dossier de sortie
REM -cp ... : inclure JADE pour ne pas avoir d'erreurs
REM -private : inclure meme les methodes privees (utile pour ton rapport)
REM -encoding UTF-8 : pour gérer les accents
REM -author -version : inclure les tags @author et @version

%JAVADOC_CMD% -d javadoc ^
    -cp "lib\jade.jar" ^
    -encoding UTF-8 -charset UTF-8 ^
    -private ^
    -author -version ^
    -windowtitle "Documentation SMA Atelier" ^
    src\Main.java src\agents\*.java src\model\*.java src\utils\*.java

if %ERRORLEVEL% EQU 0 (
    echo [SUCCES] Documentation generee dans le dossier 'javadoc' !
    echo Ouvrez 'javadoc\index.html' pour la consulter.
) else (
    echo [ERREUR] Echec de la generation.
)

pause
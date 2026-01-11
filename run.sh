#!/bin/bash

# par défaut 3 robots L1=1000 L2=2000 L3=500
NB=${1:-3}
L1=${2:-1000}
L2=${3:-2000}
L3=${4:-500}

# vérifier javac
if ! command -v javac &> /dev/null; then
    echo "[!] Erreur : 'javac' est introuvable. Verifie ton installation."
    exit 1
fi

# créer dossier de sortie si absent
mkdir -p classes

echo "[INFO] Compilation des sources..."
javac -d classes -cp "lib/jade.jar" src/Main.java src/agents/*.java src/model/*.java src/utils/*.java


if [ $? -eq 0 ]; then
    echo -e "\n[OK] Lancement de JADE ($NB robots)..."

    java -cp "lib/jade.jar:classes" Main "$NB" "$L1" "$L2" "$L3"
else
    echo "[X] Erreur : La compilation a echoue."
fi
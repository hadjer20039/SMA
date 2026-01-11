#!/bin/bash
# Création du dossier de sortie
mkdir -p javadoc

echo "[INFO] Génération de la Javadoc..."

# Exécution de javadoc
javadoc -d javadoc \
    -cp "lib/jade.jar" \
    -encoding UTF-8 -charset UTF-8 \
    -private \
    -author -version \
    -windowtitle "Documentation SMA Atelier" \
    src/Main.java src/agents/*.java src/model/*.java src/utils/*.java

if [ $? -eq 0 ]; then
    echo "[OK] Documentation générée dans le dossier 'javadoc'."
else
    echo "[X] Erreur lors de la génération."
fi
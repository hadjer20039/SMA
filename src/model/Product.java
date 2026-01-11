package model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;


/**
 * Représente un produit circulant dans l'atelier.
 * Ce fichier définit les compétences nécessaires pour fabriquer le produit
 * et suit son avancement ainsi que ses statistiques (temps, échecs).
 * @author Hadjer CHEDJARI EL MEUR et Etienne BOSSU
 * @version 1.0 (Janvier 2026)
 */
public class Product implements Serializable {
    private String id;
    // Stocke les compétences nécessaires : l'ID de la compétence et si elle est faite (true/false)
    private HashMap<String, Boolean> requiredSkills;
    private long startTime;
    private int nbFailures = 0;
    
    /**
     * Constructeur d'un produit.
     * @param id Identifiant unique (ex: "P-1")
     * @param totalAvailableSkills Nombre total de compétences existantes dans l'atelier
     * @param skillsNeeded Nombre de compétences requises pour ce produit spécifique
     */
    public Product(String id, int totalAvailableSkills, int skillsNeeded) {
        this.id = id;
        this.startTime = System.currentTimeMillis();
        this.requiredSkills = new HashMap<>();

        // On crée une liste de toutes les compétences possibles pour en tirer au sort
        List<Integer> allSkills = new ArrayList<>();
        for (int i = 0; i < totalAvailableSkills; i++) {
            allSkills.add(i);
        }

        // Mélange aléatoire pour sélectionner des compétences uniques
        Collections.shuffle(allSkills);
        for (int i = 0; i < skillsNeeded && i < totalAvailableSkills; i++) {
            this.requiredSkills.put(String.valueOf(allSkills.get(i)), false);
        }
    }
    
    /**
     * Vérifie si le produit a reçu toutes les compétences nécessaires.
     */
    public boolean isFinished() {
        return !requiredSkills.containsValue(false);
    }

    /**
     * Identifie la prochaine compétence à traiter.
     * @return L'ID de la compétence manquante, ou null si tout est fini.
     */
    public String getNextMissingSkill() {
        for (String skill : requiredSkills.keySet()) {
            if (!requiredSkills.get(skill)) return skill;
        }
        return null;
    }
    
    /** Marque une compétence comme étant validée sur le produit.*/
    public void setSkillDone(String skill) {
        if (requiredSkills.containsKey(skill)) {
            requiredSkills.put(skill, true);
        }
    }
    



    // Getters et Setters

    /** @return L'identifiant du produit. */
    public String getId() { return id; }
    
    /** @return Le temps de début de la production du produit. */
    public long getStartTime() { return startTime; }

    /** Incrémente le nombre d'échecs rencontrés lors de la production. */
    public void addFailure() { this.nbFailures++; }
    
    /** @return Le nombre total d'échecs rencontrés. */
    public int getFailures() { return nbFailures; }
}
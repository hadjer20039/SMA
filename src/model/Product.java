package model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Product implements Serializable {
    private String id;
    private HashMap<String, Boolean> requiredSkills;

    public Product(String id, int totalAvailableSkills, int skillsNeeded) {
        this.id = id;
        this.requiredSkills = new HashMap<>();
        List<Integer> allSkills = new ArrayList<>();
        for (int i = 0; i < totalAvailableSkills; i++) {
            allSkills.add(i);
        }
        Collections.shuffle(allSkills);
        for (int i = 0; i < skillsNeeded && i < totalAvailableSkills; i++) {
            this.requiredSkills.put(String.valueOf(allSkills.get(i)), false);
        }
    }

    public boolean isFinished() {
        return !requiredSkills.containsValue(false);
    }

    public String getNextMissingSkill() {
        for (String skill : requiredSkills.keySet()) {
            if (!requiredSkills.get(skill)) return skill;
        }
        return null;
    }

    public void setSkillDone(String skill) {
        if (requiredSkills.containsKey(skill)) {
            requiredSkills.put(skill, true);
        }
    }
    
    public String getId() { return id; }
}
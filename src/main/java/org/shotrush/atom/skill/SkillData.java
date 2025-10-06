package org.shotrush.atom.skill;

import java.util.HashMap;
import java.util.Map;

public class SkillData {
    private final Map<String, Integer> experience = new HashMap<>();
    private final Map<String, Long> lastActionTime = new HashMap<>();
    
    public int getExperience(String itemKey) {
        return experience.getOrDefault(itemKey, 0);
    }
    
    public void addExperience(String itemKey, int amount, int maxExperience) {
        int currentExp = getExperience(itemKey);
        int newExp = Math.min(currentExp + amount, maxExperience);
        experience.put(itemKey, newExp);
    }
    
    public void setExperience(String itemKey, int amount) {
        experience.put(itemKey, amount);
    }
    
    public long getLastActionTime(String itemKey) {
        return lastActionTime.getOrDefault(itemKey, 0L);
    }
    
    public void setLastActionTime(String itemKey, long time) {
        lastActionTime.put(itemKey, time);
    }
    
    public Map<String, Integer> getAllExperience() {
        return new HashMap<>(experience);
    }
    
    public double getSuccessRate(String itemKey, double baseRate, double maxRate, int expPerLevel) {
        int exp = getExperience(itemKey);
        double rate = baseRate + (exp / (double) expPerLevel) * (maxRate - baseRate) / 100.0;
        return Math.min(rate, maxRate);
    }
    
    public double getTimeMultiplier(String itemKey, int expPerLevel) {
        int exp = getExperience(itemKey);
        double multiplier = 1.0 - (exp / (double) expPerLevel) * 0.008;
        return Math.max(multiplier, 0.2);
    }
}

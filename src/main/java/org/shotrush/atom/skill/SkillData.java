package org.shotrush.atom.skill;

import java.util.HashMap;
import java.util.Map;

public class SkillData {
    private final Map<String, Integer> experience = new HashMap<>();
    private final Map<String, Long> lastActionTime = new HashMap<>();
    private final Map<String, Integer> repetitionCount = new HashMap<>();
    
    public int getExperience(String itemKey) {
        return experience.getOrDefault(itemKey, 0);
    }
    
    public void addExperience(String itemKey, int amount, int maxExperience) {
        int currentExp = getExperience(itemKey);
        int newExp = Math.min(currentExp + amount, maxExperience);
        experience.put(itemKey, newExp);
        incrementRepetitionCount(itemKey);
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

    public int getRepetitionCount(String itemKey) {
        return repetitionCount.getOrDefault(itemKey, 0);
    }

    public void incrementRepetitionCount(String itemKey) {
        repetitionCount.put(itemKey, getRepetitionCount(itemKey) + 1);
    }

    public void setRepetitionCount(String itemKey, int count) {
        repetitionCount.put(itemKey, count);
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

    public void applyDecay(String itemKey, double initialHalfLifeHours, double stabilityIncrease, double maxStabilityMultiplier, double minRetention) {
        long lastTime = getLastActionTime(itemKey);
        if (lastTime == 0) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        double hoursElapsed = (currentTime - lastTime) / (1000.0 * 60.0 * 60.0);

        if (hoursElapsed <= 0) {
            return;
        }

        int reps = getRepetitionCount(itemKey);
        double stabilityMultiplier = Math.min(1.0 + (reps * stabilityIncrease), maxStabilityMultiplier);
        double effectiveHalfLife = initialHalfLifeHours * stabilityMultiplier;

        double retention = Math.pow(0.5, hoursElapsed / effectiveHalfLife);
        retention = Math.max(retention, minRetention);

        int currentExp = getExperience(itemKey);
        int decayedExp = (int) Math.ceil(currentExp * retention);
        experience.put(itemKey, decayedExp);
    }

    public Map<String, Integer> getAllRepetitionCounts() {
        return new HashMap<>(repetitionCount);
    }
}

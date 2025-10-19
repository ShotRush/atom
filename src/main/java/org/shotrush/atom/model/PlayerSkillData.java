package org.shotrush.atom.model;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerSkillData {
    
    private final UUID playerId;
    private final Map<String, Long> intrinsicXp;
    private volatile long lastModified;
    private volatile boolean dirty;
    
    public PlayerSkillData(UUID playerId) {
        this.playerId = playerId;
        this.intrinsicXp = new ConcurrentHashMap<>();
        this.lastModified = System.currentTimeMillis();
        this.dirty = false;
    }
    
    public PlayerSkillData(UUID playerId, Map<String, Long> intrinsicXp) {
        this.playerId = playerId;
        this.intrinsicXp = new ConcurrentHashMap<>(intrinsicXp);
        this.lastModified = System.currentTimeMillis();
        this.dirty = false;
    }
    
    public UUID playerId() {
        return playerId;
    }
    
    public long getIntrinsicXp(String skillId) {
        return intrinsicXp.getOrDefault(skillId, 0L);
    }
    
    public void setIntrinsicXp(String skillId, long xp) {
        if (xp < 0) {
            throw new IllegalArgumentException("XP cannot be negative");
        }
        intrinsicXp.put(skillId, xp);
        markDirty();
    }
    
    public void addIntrinsicXp(String skillId, long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
        intrinsicXp.merge(skillId, amount, Long::sum);
        markDirty();
    }
    
    public boolean hasXp(String skillId) {
        return intrinsicXp.containsKey(skillId);
    }
    
    public Map<String, Long> getAllIntrinsicXp() {
        return Map.copyOf(intrinsicXp);
    }
    
    public long lastModified() {
        return lastModified;
    }
    
    public boolean isDirty() {
        return dirty;
    }
    
    public void markClean() {
        this.dirty = false;
    }
    
    private void markDirty() {
        this.dirty = true;
        this.lastModified = System.currentTimeMillis();
    }
    
    @Override
    public String toString() {
        return "PlayerSkillData{playerId=" + playerId + ", skills=" + intrinsicXp.size() + ", dirty=" + dirty + "}";
    }
}

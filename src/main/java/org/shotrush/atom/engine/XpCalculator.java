package org.shotrush.atom.engine;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.shotrush.atom.model.EffectiveXp;
import org.shotrush.atom.model.PlayerSkillData;
import org.shotrush.atom.model.SkillNode;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class XpCalculator {
    
    private static final double HONORARY_XP_MULTIPLIER = 0.5;
    
    private final Cache<CacheKey, EffectiveXp> effectiveXpCache;
    
    public XpCalculator() {
        this.effectiveXpCache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(Duration.ofMinutes(5))
            .build();
    }
    
    public EffectiveXp calculateEffectiveXp(PlayerSkillData playerData, SkillNode node) {
        Objects.requireNonNull(playerData, "playerData cannot be null");
        Objects.requireNonNull(node, "node cannot be null");
        
        CacheKey key = new CacheKey(playerData.playerId(), node.id(), playerData.lastModified());
        
        EffectiveXp cached = effectiveXpCache.getIfPresent(key);
        if (cached != null) {
            return cached;
        }
        
        EffectiveXp calculated = computeEffectiveXp(playerData, node);
        effectiveXpCache.put(key, calculated);
        return calculated;
    }
    
    private EffectiveXp computeEffectiveXp(PlayerSkillData playerData, SkillNode node) {
        long intrinsicXp = playerData.getIntrinsicXp(node.id());
        
        if (node.isRoot()) {
            return EffectiveXp.of(intrinsicXp, 0, node.maxXp());
        }
        
        long honoraryXp = calculateHonoraryXp(playerData, node, intrinsicXp);
        
        return EffectiveXp.of(intrinsicXp, honoraryXp, node.maxXp());
    }
    
    private long calculateHonoraryXp(PlayerSkillData playerData, SkillNode node, long intrinsicXp) {
        if (node.isRoot()) {
            return 0;
        }
        
        SkillNode parent = node.parent().orElse(null);
        if (parent == null) {
            return 0;
        }
        
        EffectiveXp parentEffectiveXp = calculateEffectiveXp(playerData, parent);
        
        double parentProgress = parentEffectiveXp.progressPercent();
        
        long remainingCapacity = Math.max(0, node.maxXp() - intrinsicXp);
        
        long honoraryXp = (long) (remainingCapacity * parentProgress * HONORARY_XP_MULTIPLIER);
        
        return honoraryXp;
    }
    
    public long calculateParentXp(PlayerSkillData playerData, SkillNode parentNode) {
        Objects.requireNonNull(playerData, "playerData cannot be null");
        Objects.requireNonNull(parentNode, "parentNode cannot be null");
        
        if (parentNode.children().isEmpty()) {
            return playerData.getIntrinsicXp(parentNode.id());
        }
        
        long totalChildrenXp = 0;
        for (SkillNode child : parentNode.children().values()) {
            totalChildrenXp += playerData.getIntrinsicXp(child.id());
        }
        
        long ownIntrinsicXp = playerData.getIntrinsicXp(parentNode.id());
        
        return Math.max(ownIntrinsicXp, totalChildrenXp);
    }
    
    public Map<String, EffectiveXp> calculateAllEffectiveXp(PlayerSkillData playerData, Map<String, SkillNode> nodes) {
        Objects.requireNonNull(playerData, "playerData cannot be null");
        Objects.requireNonNull(nodes, "nodes cannot be null");
        
        Map<String, EffectiveXp> results = new HashMap<>();
        for (Map.Entry<String, SkillNode> entry : nodes.entrySet()) {
            results.put(entry.getKey(), calculateEffectiveXp(playerData, entry.getValue()));
        }
        return results;
    }
    
    public void invalidateCache(UUID playerId) {
        effectiveXpCache.asMap().keySet().removeIf(key -> key.playerId.equals(playerId));
    }
    
    public void invalidateCache(UUID playerId, String skillId) {
        effectiveXpCache.asMap().keySet().removeIf(key -> 
            key.playerId.equals(playerId) && key.skillId.equals(skillId));
    }
    
    public void clearCache() {
        effectiveXpCache.invalidateAll();
    }
    
    public long cacheSize() {
        return effectiveXpCache.estimatedSize();
    }
    
    private record CacheKey(UUID playerId, String skillId, long timestamp) {}
}

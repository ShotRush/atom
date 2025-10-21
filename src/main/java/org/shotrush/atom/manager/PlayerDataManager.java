package org.shotrush.atom.manager;

import org.shotrush.atom.model.PlayerSkillData;
import org.shotrush.atom.storage.StorageProvider;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerDataManager {
    
    private final StorageProvider storage;
    private final Map<UUID, PlayerSkillData> cache;
    
    public PlayerDataManager(StorageProvider storage) {
        this.storage = Objects.requireNonNull(storage, "storage cannot be null");
        this.cache = new ConcurrentHashMap<>();
    }
    
    public CompletableFuture<PlayerSkillData> loadPlayerData(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId cannot be null");
        
        PlayerSkillData cached = cache.get(playerId);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }
        
        return storage.loadPlayerData(playerId)
            .thenApply(optData -> {
                PlayerSkillData data = optData.orElseGet(() -> new PlayerSkillData(playerId));
                cache.put(playerId, data);
                return data;
            });
    }
    
    public CompletableFuture<Void> savePlayerData(UUID playerId) {
        PlayerSkillData data = cache.get(playerId);
        if (data == null || !data.isDirty()) {
            return CompletableFuture.completedFuture(null);
        }
        
        return storage.savePlayerData(data);
    }
    
    public CompletableFuture<Void> saveAllPlayerData() {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        for (PlayerSkillData data : cache.values()) {
            if (data.isDirty()) {
                futures.add(storage.savePlayerData(data));
            }
        }
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }
    
    public void unloadPlayerData(UUID playerId) {
        cache.remove(playerId);
    }
    
    public Optional<PlayerSkillData> getCachedPlayerData(UUID playerId) {
        return Optional.ofNullable(cache.get(playerId));
    }
    
    public PlayerSkillData getOrCreatePlayerData(UUID playerId) {
        return cache.computeIfAbsent(playerId, PlayerSkillData::new);
    }
    
    public Set<UUID> getCachedPlayers() {
        return Collections.unmodifiableSet(cache.keySet());
    }
    
    public int getCacheSize() {
        return cache.size();
    }
    
    public void clearCache() {
        cache.clear();
    }
    
    public Collection<PlayerSkillData> getAllCachedData() {
        return Collections.unmodifiableCollection(cache.values());
    }
}

package org.shotrush.atom.storage;

import org.shotrush.atom.model.PlayerSkillData;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface StorageProvider {
    
    CompletableFuture<Void> initialize();
    
    CompletableFuture<Void> shutdown();
    
    CompletableFuture<Optional<PlayerSkillData>> loadPlayerData(UUID playerId);
    
    CompletableFuture<Void> savePlayerData(PlayerSkillData playerData);
    
    CompletableFuture<Void> deletePlayerData(UUID playerId);
    
    CompletableFuture<Boolean> playerDataExists(UUID playerId);
}

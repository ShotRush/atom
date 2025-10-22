package org.shotrush.atom.storage;

import org.shotrush.atom.model.PlayerSkillData;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class Storage {
    
    private Storage() {}

    public interface Provider {
        CompletableFuture<Void> initialize();
        CompletableFuture<Void> shutdown();
        CompletableFuture<Optional<PlayerSkillData>> loadPlayerData(UUID playerId);
        CompletableFuture<Void> savePlayerData(PlayerSkillData playerData);
        CompletableFuture<Void> deletePlayerData(UUID playerId);
        CompletableFuture<Boolean> playerDataExists(UUID playerId);
    }

    public static final class Exception extends RuntimeException {
        public Exception(String message) {
            super(message);
        }
        
        public Exception(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

package org.shotrush.atom.util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public abstract class ManagerTemplate<T> {
    protected final Map<UUID, T> playerData = new HashMap<>();
    
    public T getData(UUID playerId) {
        return playerData.computeIfAbsent(playerId, this::createDefault);
    }
    
    public void setData(UUID playerId, T data) {
        playerData.put(playerId, data);
    }
    
    public void removeData(UUID playerId) {
        playerData.remove(playerId);
    }
    
    public Map<UUID, T> getAllData() {
        return new HashMap<>(playerData);
    }
    
    public void clearAll() {
        playerData.clear();
    }
    
    protected abstract T createDefault(UUID playerId);
}

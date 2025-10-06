package org.shotrush.atom.util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class QuickManager {
    
    public static class SimpleCounter {
        private final Map<UUID, Map<String, Integer>> counters = new HashMap<>();
        
        public void increment(UUID playerId, String key) {
            counters.computeIfAbsent(playerId, k -> new HashMap<>()).merge(key, 1, Integer::sum);
        }
        
        public int get(UUID playerId, String key) {
            return counters.getOrDefault(playerId, new HashMap<>()).getOrDefault(key, 0);
        }
        
        public void set(UUID playerId, String key, int value) {
            counters.computeIfAbsent(playerId, k -> new HashMap<>()).put(key, value);
        }
        
        public void reset(UUID playerId, String key) {
            Map<String, Integer> playerCounters = counters.get(playerId);
            if (playerCounters != null) {
                playerCounters.remove(key);
            }
        }
        
        public Map<String, Integer> getAll(UUID playerId) {
            return new HashMap<>(counters.getOrDefault(playerId, new HashMap<>()));
        }
    }
    
    public static class SimpleTimer {
        private final Map<UUID, Map<String, Long>> timers = new HashMap<>();
        
        public void start(UUID playerId, String key) {
            timers.computeIfAbsent(playerId, k -> new HashMap<>()).put(key, System.currentTimeMillis());
        }
        
        public boolean hasExpired(UUID playerId, String key, long durationMillis) {
            Map<String, Long> playerTimers = timers.get(playerId);
            if (playerTimers == null) return true;
            
            Long startTime = playerTimers.get(key);
            if (startTime == null) return true;
            
            return System.currentTimeMillis() - startTime >= durationMillis;
        }
        
        public void remove(UUID playerId, String key) {
            Map<String, Long> playerTimers = timers.get(playerId);
            if (playerTimers != null) {
                playerTimers.remove(key);
            }
        }
    }
    
    public static class SimpleFlag {
        private final Map<UUID, Map<String, Boolean>> flags = new HashMap<>();
        
        public void set(UUID playerId, String key, boolean value) {
            flags.computeIfAbsent(playerId, k -> new HashMap<>()).put(key, value);
        }
        
        public boolean get(UUID playerId, String key) {
            return flags.getOrDefault(playerId, new HashMap<>()).getOrDefault(key, false);
        }
        
        public void toggle(UUID playerId, String key) {
            set(playerId, key, !get(playerId, key));
        }
    }
}

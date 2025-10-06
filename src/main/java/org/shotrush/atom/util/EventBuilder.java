package org.shotrush.atom.util;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class EventBuilder {
    
    public static class SimplePlayerEvent extends Event implements Cancellable {
        private static final HandlerList HANDLERS = new HandlerList();
        private boolean cancelled = false;
        private final Player player;
        private final String eventType;
        private final Object data;
        
        public SimplePlayerEvent(Player player, String eventType, Object data) {
            this.player = player;
            this.eventType = eventType;
            this.data = data;
        }
        
        public Player getPlayer() { return player; }
        public String getEventType() { return eventType; }
        public Object getData() { return data; }
        
        @Override
        public boolean isCancelled() { return cancelled; }
        
        @Override
        public void setCancelled(boolean cancel) { this.cancelled = cancel; }
        
        @Override
        public HandlerList getHandlers() { return HANDLERS; }
        
        public static HandlerList getHandlerList() { return HANDLERS; }
    }
    
    public static SimplePlayerEvent createEvent(Player player, String eventType, Object data) {
        return new SimplePlayerEvent(player, eventType, data);
    }
}

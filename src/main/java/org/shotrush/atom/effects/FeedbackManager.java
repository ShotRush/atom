package org.shotrush.atom.effects;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.shotrush.atom.config.AtomConfig;

public final class FeedbackManager {
    
    private final AtomConfig config;
    
    public FeedbackManager(AtomConfig config) {
        this.config = config;
    }
    
    public void sendXpGain(Player player, String skillName, long xpGained) {
        if (!config.enableFeedback()) return;
        
        Component message = Component.text("+" + xpGained + " ", NamedTextColor.GREEN)
            .append(Component.text(skillName, NamedTextColor.GOLD))
            .append(Component.text(" XP", NamedTextColor.GRAY));
        
        player.sendActionBar(message);
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.2f);
    }
    
    public void sendLevelUp(Player player, String skillName, int level) {
        if (!config.enableFeedback()) return;
        
        Component message = Component.text("✦ ", NamedTextColor.GOLD, TextDecoration.BOLD)
            .append(Component.text(skillName, NamedTextColor.YELLOW))
            .append(Component.text(" Level " + level, NamedTextColor.GREEN))
            .append(Component.text(" ✦", NamedTextColor.GOLD, TextDecoration.BOLD));
        
        player.sendMessage(message);
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
    }
    
    public void sendMilestoneReached(Player player, String milestoneName) {
        if (!config.enableFeedback()) return;
        
        Component message = Component.text("⚡ Milestone Reached: ", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD)
            .append(Component.text(milestoneName, NamedTextColor.AQUA));
        
        player.sendMessage(message);
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 0.8f);
    }
    
    public void sendCraftFailed(Player player, String reason) {
        if (!config.enableFeedback()) return;
        
        Component message = Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
            .append(Component.text(reason, NamedTextColor.GRAY));
        
        player.sendActionBar(message);
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
    }
    
    public void sendPenaltyWarning(Player player, String category) {
        if (!config.enableFeedback()) return;
        
        Component message = Component.text("⚠ Low ", NamedTextColor.YELLOW)
            .append(Component.text(category, NamedTextColor.GOLD))
            .append(Component.text(" skill - penalties active", NamedTextColor.GRAY));
        
        player.sendMessage(message);
    }
    
    public void sendBonusActivated(Player player, String category) {
        if (!config.enableFeedback()) return;
        
        Component message = Component.text("★ ", NamedTextColor.GOLD)
            .append(Component.text(category, NamedTextColor.YELLOW))
            .append(Component.text(" bonus activated!", NamedTextColor.GREEN));
        
        player.sendMessage(message);
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.5f, 1.5f);
    }
}

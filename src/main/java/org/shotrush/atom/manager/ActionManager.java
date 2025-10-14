package org.shotrush.atom.manager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.shotrush.atom.Atom;
import org.shotrush.atom.model.Milestone;
import org.shotrush.atom.model.PlayerData;
import org.shotrush.atom.model.TrackedAction;

import java.util.Map;
import java.util.Set;

public class ActionManager {
    private final Atom plugin;
    private final ConfigManager configManager;
    private final PlayerDataManager playerDataManager;

    public ActionManager(Atom plugin, ConfigManager configManager, PlayerDataManager playerDataManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.playerDataManager = playerDataManager;
    }

    public void grantExperience(Player player, String actionId, double baseExperience) {
        TrackedAction action = configManager.getAction(actionId);
        if (action == null) return;

        double multiplier = calculateMultiplier(player);
        double finalExperience = baseExperience * multiplier;

        PlayerData data = playerDataManager.getPlayerData(player.getUniqueId());
        double oldExperience = data.getExperience(actionId);
        data.addExperience(actionId, finalExperience);
        
        applySkillTransfer(player, actionId, finalExperience);

        if (plugin.getConfig().getBoolean("display.action-bar-updates", true)) {
            double efficiency = plugin.getEmergentBonusManager().getSpeedMultiplier(player, actionId);
            String message = String.format("§7%s §8+§f%.1f §7XP §8(§f%.2fx§8)", 
                action.getDisplayName(), 
                finalExperience, 
                efficiency);
            
            Component component = LegacyComponentSerializer.legacyAmpersand().deserialize(message);
            player.sendActionBar(component);
        }

        checkMilestones(player, data);
    }
    
    private void applySkillTransfer(Player player, String actionId, double experience) {
        if (!plugin.getSkillTransferManager().isEnabled()) return;
        
        Map<String, Set<String>> skillGroups = plugin.getSkillTransferManager().getSkillGroups();
        double transferRate = plugin.getSkillTransferManager().getTransferRate();
        
        for (Set<String> group : skillGroups.values()) {
            if (group.contains(actionId)) {
                PlayerData data = playerDataManager.getPlayerData(player.getUniqueId());
                double transferAmount = experience * transferRate;
                
                for (String relatedSkill : group) {
                    if (!relatedSkill.equals(actionId)) {
                        data.addExperience(relatedSkill, transferAmount);
                    }
                }
                break;
            }
        }
    }

    private double calculateMultiplier(Player player) {
        double multiplier = 1.0;

        if (plugin.getConfig().getBoolean("multipliers.group_bonus.enabled", false)) {
            int radius = plugin.getConfig().getInt("multipliers.group_bonus.radius", 10);
            double perPlayer = plugin.getConfig().getDouble("multipliers.group_bonus.per-player", 0.1);
            int maxPlayers = plugin.getConfig().getInt("multipliers.group_bonus.max-players", 5);

            long nearbyPlayers = player.getWorld().getNearbyPlayers(player.getLocation(), radius).stream()
                    .filter(p -> !p.equals(player))
                    .count();

            multiplier += Math.min(nearbyPlayers, maxPlayers) * perPlayer;
        }

        return multiplier;
    }

    private void checkMilestones(Player player, PlayerData data) {
        for (Milestone milestone : configManager.getMilestones().values()) {
            if (data.hasMilestone(milestone.getId())) continue;

            boolean meetsRequirements = true;
            for (Map.Entry<String, Double> requirement : milestone.getActionRequirements().entrySet()) {
                if (data.getExperience(requirement.getKey()) < requirement.getValue()) {
                    meetsRequirements = false;
                    break;
                }
            }

            if (meetsRequirements) {
                data.completeMilestone(milestone.getId());
                grantMilestoneRewards(player, milestone);
            }
        }
    }

    private void grantMilestoneRewards(Player player, Milestone milestone) {
        for (Milestone.Reward reward : milestone.getRewards()) {
            switch (reward.getType()) {
                case MESSAGE:
                    Component message = LegacyComponentSerializer.legacyAmpersand().deserialize(reward.getValue());
                    player.sendActionBar(message);
                    break;
                case TITLE:
                    break;
                case EFFECT:
                    break;
                case COMMAND:
                    plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(),
                            reward.getValue().replace("{player}", player.getName()));
                    break;
            }
        }
    }

    public boolean meetsRequirements(Player player, Map<String, Double> requirements) {
        PlayerData data = playerDataManager.getPlayerData(player.getUniqueId());
        for (Map.Entry<String, Double> requirement : requirements.entrySet()) {
            if (data.getExperience(requirement.getKey()) < requirement.getValue()) {
                return false;
            }
        }
        return true;
    }
}

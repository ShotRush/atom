package org.shotrush.atom.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.shotrush.atom.Atom;
import org.shotrush.atom.gui.AtomMainGui;
import org.shotrush.atom.manager.ActionManager;
import org.shotrush.atom.manager.ConfigManager;
import org.shotrush.atom.manager.PlayerDataManager;
import org.shotrush.atom.model.PlayerData;
import org.shotrush.atom.model.TrackedAction;

import java.util.Map;

@CommandAlias("atom")
public class AtomCommand extends BaseCommand {
    private final Atom plugin;
    private final ConfigManager configManager;
    private final PlayerDataManager playerDataManager;
    private final ActionManager actionManager;

    public AtomCommand(Atom plugin, ConfigManager configManager, 
                      PlayerDataManager playerDataManager, ActionManager actionManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.playerDataManager = playerDataManager;
        this.actionManager = actionManager;
    }

    @Subcommand("reload")
    @CommandPermission("atom.admin.reload")
    @Description("Reload the plugin configuration")
    public void onReload(CommandSender sender) {
        try {
            configManager.loadConfig();
            sender.sendMessage(Component.text("Atom configuration reloaded successfully!", NamedTextColor.GREEN));
        } catch (Exception e) {
            sender.sendMessage(Component.text("Failed to reload configuration: " + e.getMessage(), NamedTextColor.RED));
            e.printStackTrace();
        }
    }

    @Default
    @CommandPermission("atom.use")
    @Description("Open the Atom GUI")
    public void onDefault(Player player) {
        new AtomMainGui(plugin).open(player);
    }

    @Subcommand("stats")
    @CommandPermission("atom.stats")
    @Description("View your specialization statistics")
    public void onStats(Player player) {
        PlayerData data = playerDataManager.getPlayerData(player.getUniqueId());
        Map<String, Double> experience = data.getActionExperience();

        player.sendMessage(Component.text("=== Your Specializations ===", NamedTextColor.GOLD));
        
        if (experience.isEmpty()) {
            player.sendMessage(Component.text("You haven't specialized in anything yet!", NamedTextColor.GRAY));
            return;
        }

        experience.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .forEach(entry -> {
                    TrackedAction action = configManager.getAction(entry.getKey());
                    String displayName = action != null ? action.getDisplayName() : entry.getKey();
                    double efficiency = plugin.getEmergentBonusManager().getSpeedMultiplier(player, entry.getKey());
                    player.sendMessage(Component.text(displayName + ": ", NamedTextColor.YELLOW)
                            .append(Component.text(String.format("%.1f XP", entry.getValue()), NamedTextColor.GREEN))
                            .append(Component.text(String.format(" (%.1fx efficiency)", efficiency), NamedTextColor.AQUA)));
                });
    }

    @Subcommand("stats")
    @CommandPermission("atom.stats.others")
    @CommandCompletion("@players")
    @Description("View another player's statistics")
    public void onStatsOther(CommandSender sender, @Flags("other") Player target) {
        PlayerData data = playerDataManager.getPlayerData(target.getUniqueId());
        Map<String, Double> experience = data.getActionExperience();

        sender.sendMessage(Component.text("=== " + target.getName() + "'s Specializations ===", NamedTextColor.GOLD));
        
        if (experience.isEmpty()) {
            sender.sendMessage(Component.text("This player hasn't specialized in anything yet!", NamedTextColor.GRAY));
            return;
        }

        experience.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .forEach(entry -> {
                    TrackedAction action = configManager.getAction(entry.getKey());
                    String displayName = action != null ? action.getDisplayName() : entry.getKey();
                    sender.sendMessage(Component.text(displayName + ": ", NamedTextColor.YELLOW)
                            .append(Component.text(String.format("%.1f XP", entry.getValue()), NamedTextColor.GREEN)));
                });
    }

    @Subcommand("set")
    @CommandPermission("atom.admin.set")
    @CommandCompletion("@players @actions @nothing")
    @Description("Set a player's experience in an action")
    @Syntax("<player> <action> <amount>")
    public void onSet(CommandSender sender, @Flags("other") Player target, String actionId, double amount) {
        if (configManager.getAction(actionId) == null) {
            sender.sendMessage(Component.text("Unknown action: " + actionId, NamedTextColor.RED));
            return;
        }

        PlayerData data = playerDataManager.getPlayerData(target.getUniqueId());
        data.setExperience(actionId, amount);
        
        sender.sendMessage(Component.text("Set " + target.getName() + "'s " + actionId + " to " + amount, NamedTextColor.GREEN));
    }

    @Subcommand("add")
    @CommandPermission("atom.admin.add")
    @CommandCompletion("@players @actions @nothing")
    @Description("Add experience to a player's action")
    @Syntax("<player> <action> <amount>")
    public void onAdd(CommandSender sender, @Flags("other") Player target, String actionId, double amount) {
        if (configManager.getAction(actionId) == null) {
            sender.sendMessage(Component.text("Unknown action: " + actionId, NamedTextColor.RED));
            return;
        }

        PlayerData data = playerDataManager.getPlayerData(target.getUniqueId());
        data.addExperience(actionId, amount);
        
        sender.sendMessage(Component.text("Added " + amount + " to " + target.getName() + "'s " + actionId, NamedTextColor.GREEN));
    }

    @Subcommand("actions")
    @CommandPermission("atom.actions")
    @Description("List all available actions")
    public void onActions(CommandSender sender) {
        sender.sendMessage(Component.text("=== Available Actions ===", NamedTextColor.GOLD));
        
        for (TrackedAction action : configManager.getActions().values()) {
            sender.sendMessage(Component.text(action.getId() + ": ", NamedTextColor.YELLOW)
                    .append(Component.text(action.getDisplayName(), NamedTextColor.WHITE))
                    .append(Component.text(" (" + action.getExperience() + " XP)", NamedTextColor.GRAY)));
        }
    }

    @Subcommand("milestones")
    @CommandPermission("atom.milestones")
    @Description("View your completed milestones")
    public void onMilestones(Player player) {
        PlayerData data = playerDataManager.getPlayerData(player.getUniqueId());
        
        player.sendMessage(Component.text("=== Your Milestones ===", NamedTextColor.GOLD));
        
        if (data.getCompletedMilestones().isEmpty()) {
            player.sendMessage(Component.text("You haven't completed any milestones yet!", NamedTextColor.GRAY));
            return;
        }

        for (String milestoneId : data.getCompletedMilestones()) {
            var milestone = configManager.getMilestone(milestoneId);
            if (milestone != null) {
                player.sendMessage(Component.text("✓ ", NamedTextColor.GREEN)
                        .append(Component.text(milestone.getDisplayName(), NamedTextColor.YELLOW)));
            }
        }
    }

    @Subcommand("help")
    @Description("Show help information")
    public void onHelp(CommandSender sender) {
        sender.sendMessage(Component.text("=== Atom Commands ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/atom stats", NamedTextColor.YELLOW)
                .append(Component.text(" - View your statistics", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/atom actions", NamedTextColor.YELLOW)
                .append(Component.text(" - List all actions", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/atom milestones", NamedTextColor.YELLOW)
                .append(Component.text(" - View your milestones", NamedTextColor.GRAY)));
        
        if (sender.hasPermission("atom.admin.reload")) {
            sender.sendMessage(Component.text("/atom reload", NamedTextColor.YELLOW)
                    .append(Component.text(" - Reload configuration", NamedTextColor.GRAY)));
        }
    }
}

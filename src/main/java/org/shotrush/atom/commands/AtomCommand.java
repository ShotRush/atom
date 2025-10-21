package org.shotrush.atom.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.shotrush.atom.Atom;
import org.shotrush.atom.engine.XpEngine;
import org.shotrush.atom.manager.PlayerDataManager;
import org.shotrush.atom.model.EffectiveXp;
import org.shotrush.atom.model.PlayerSkillData;
import org.shotrush.atom.tree.SkillTreeRegistry;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@CommandAlias("atom")
@Description("Atom skill system commands")
public final class AtomCommand extends BaseCommand {
    
    private final Atom plugin;
    private final PlayerDataManager dataManager;
    private final XpEngine xpEngine;
    private final SkillTreeRegistry treeRegistry;
    
    public AtomCommand(Atom plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();
        this.xpEngine = plugin.getXpEngine();
        this.treeRegistry = plugin.getTreeRegistry();
    }
    
    @Default
    @CommandPermission("atom.use")
    @Description("View your skill statistics")
    public void onDefault(Player player) {
        showStats(player, player);
    }
    
    @Subcommand("stats")
    @CommandPermission("atom.stats")
    @Description("View your skill statistics")
    public void onStats(Player player) {
        showStats(player, player);
    }
    
    @Subcommand("stats")
    @CommandPermission("atom.stats.others")
    @CommandCompletion("@players")
    @Description("View another player's statistics")
    public void onStatsOther(Player sender, Player target) {
        showStats(sender, target);
    }
    
    private void showStats(Player viewer, Player target) {
        Optional<PlayerSkillData> dataOpt = dataManager.getCachedPlayerData(target.getUniqueId());
        
        if (dataOpt.isEmpty()) {
            viewer.sendMessage(Component.text("No data found for " + target.getName(), NamedTextColor.RED));
            return;
        }
        
        PlayerSkillData data = dataOpt.get();
        
        viewer.sendMessage(Component.text("═══════════════════════════", NamedTextColor.GOLD));
        viewer.sendMessage(Component.text(target.getName() + "'s Skills", NamedTextColor.YELLOW));
        viewer.sendMessage(Component.text("═══════════════════════════", NamedTextColor.GOLD));
        
        String[] mainSkills = {"farmer", "guardsman", "miner", "healer", "blacksmith", "builder", "librarian"};
        
        for (String skillId : mainSkills) {
            EffectiveXp effectiveXp = xpEngine.getEffectiveXp(data, skillId);
            double level = effectiveXp.progressPercent() * 100.0;
            
            Component skillLine = Component.text("  " + capitalize(skillId) + ": ", NamedTextColor.GRAY)
                .append(Component.text(String.format("%.1f%%", level), getColorForLevel(level)))
                .append(Component.text(" (", NamedTextColor.DARK_GRAY))
                .append(Component.text(effectiveXp.intrinsicXp(), NamedTextColor.GREEN))
                .append(Component.text("+", NamedTextColor.DARK_GRAY))
                .append(Component.text(effectiveXp.honoraryXp(), NamedTextColor.YELLOW))
                .append(Component.text(" XP)", NamedTextColor.DARK_GRAY));
            
            if (effectiveXp.honoraryXp() > effectiveXp.intrinsicXp() * 0.5) {
                skillLine = skillLine.append(Component.text(" ⚡", NamedTextColor.GOLD));
            }
            
            viewer.sendMessage(skillLine);
        }
        
        viewer.sendMessage(Component.text("═══════════════════════════", NamedTextColor.GOLD));
    }
    
    @Subcommand("admin reload")
    @CommandPermission("atom.admin.reload")
    @Description("Reload the plugin configuration")
    public void onReload(Player player) {
        plugin.reloadConfig();
        plugin.loadConfiguration();
        player.sendMessage(Component.text("✓ Configuration reloaded!", NamedTextColor.GREEN));
    }
    
    @Subcommand("admin set")
    @CommandPermission("atom.admin.set")
    @CommandCompletion("@players @skills @nothing")
    @Description("Set a player's XP in a skill")
    @Syntax("<player> <skill> <amount>")
    public void onSet(Player sender, String targetName, String skillId, String amountStr) {
        Player target = plugin.getServer().getPlayer(targetName);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found: " + targetName, NamedTextColor.RED));
            return;
        }
        
        long amount;
        try {
            amount = Long.parseLong(amountStr);
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Invalid amount: " + amountStr, NamedTextColor.RED));
            return;
        }
        
        Optional<PlayerSkillData> dataOpt = dataManager.getCachedPlayerData(target.getUniqueId());
        
        if (dataOpt.isEmpty()) {
            sender.sendMessage(Component.text("Player data not loaded", NamedTextColor.RED));
            return;
        }
        
        if (!treeRegistry.findNode(skillId).isPresent()) {
            sender.sendMessage(Component.text("Unknown skill: " + skillId, NamedTextColor.RED));
            return;
        }
        
        PlayerSkillData data = dataOpt.get();
        xpEngine.setXp(data, skillId, amount);
        
        sender.sendMessage(Component.text("✓ Set ", NamedTextColor.GREEN)
            .append(Component.text(target.getName(), NamedTextColor.YELLOW))
            .append(Component.text("'s ", NamedTextColor.GREEN))
            .append(Component.text(skillId, NamedTextColor.GOLD))
            .append(Component.text(" to ", NamedTextColor.GREEN))
            .append(Component.text(amount + " XP", NamedTextColor.AQUA)));
    }
    
    @Subcommand("admin add")
    @CommandPermission("atom.admin.add")
    @CommandCompletion("@players @skills @nothing")
    @Description("Add XP to a player's skill")
    @Syntax("<player> <skill> <amount>")
    public void onAdd(Player sender, String targetName, String skillId, String amountStr) {
        Player target = plugin.getServer().getPlayer(targetName);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found: " + targetName, NamedTextColor.RED));
            return;
        }
        
        long amount;
        try {
            amount = Long.parseLong(amountStr);
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Invalid amount: " + amountStr, NamedTextColor.RED));
            return;
        }
        
        Optional<PlayerSkillData> dataOpt = dataManager.getCachedPlayerData(target.getUniqueId());
        
        if (dataOpt.isEmpty()) {
            sender.sendMessage(Component.text("Player data not loaded", NamedTextColor.RED));
            return;
        }
        
        if (!treeRegistry.findNode(skillId).isPresent()) {
            sender.sendMessage(Component.text("Unknown skill: " + skillId, NamedTextColor.RED));
            return;
        }
        
        PlayerSkillData data = dataOpt.get();
        xpEngine.awardXp(data, skillId, amount);
        
        sender.sendMessage(Component.text("✓ Added ", NamedTextColor.GREEN)
            .append(Component.text(amount + " XP", NamedTextColor.AQUA))
            .append(Component.text(" to ", NamedTextColor.GREEN))
            .append(Component.text(target.getName(), NamedTextColor.YELLOW))
            .append(Component.text("'s ", NamedTextColor.GREEN))
            .append(Component.text(skillId, NamedTextColor.GOLD)));
    }
    
    @Subcommand("admin save")
    @CommandPermission("atom.admin.reload")
    @Description("Force save all player data")
    public void onSave(Player sender) {
        sender.sendMessage(Component.text("Saving all player data...", NamedTextColor.YELLOW));
        
        dataManager.saveAllPlayerData().thenRun(() -> {
            sender.sendMessage(Component.text("✓ Saved data for " + dataManager.getCacheSize() + " players", NamedTextColor.GREEN));
        });
    }
    
    @Subcommand("debug")
    @CommandPermission("atom.admin.reload")
    @Description("View detailed XP breakdown")
    public void onDebug(Player player) {
        Optional<PlayerSkillData> dataOpt = dataManager.getCachedPlayerData(player.getUniqueId());
        
        if (dataOpt.isEmpty()) {
            player.sendMessage(Component.text("No data loaded", NamedTextColor.RED));
            return;
        }
        
        PlayerSkillData data = dataOpt.get();
        Map<String, Long> allXp = data.getAllIntrinsicXp();
        
        if (allXp.isEmpty()) {
            player.sendMessage(Component.text("No XP earned yet!", NamedTextColor.RED));
            return;
        }
        
        player.sendMessage(Component.text("═══════ XP Debug ═══════", NamedTextColor.GOLD));
        
        allXp.entrySet().stream()
            .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
            .limit(20)
            .forEach(entry -> {
                String skillId = entry.getKey();
                EffectiveXp effectiveXp = xpEngine.getEffectiveXp(data, skillId);
                
                Component line = Component.text("  " + skillId + ": ", NamedTextColor.GRAY)
                    .append(Component.text(effectiveXp.intrinsicXp(), NamedTextColor.GREEN))
                    .append(Component.text(" (+", NamedTextColor.DARK_GRAY))
                    .append(Component.text(effectiveXp.honoraryXp(), NamedTextColor.YELLOW))
                    .append(Component.text(") = ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(String.format("%.1f%%", effectiveXp.progressPercent() * 100), NamedTextColor.AQUA));
                
                player.sendMessage(line);
            });
        
        player.sendMessage(Component.text("Cache size: " + xpEngine.calculator().cacheSize(), NamedTextColor.GRAY));
    }
    
    @Subcommand("admin ml regenerate")
    @CommandPermission("atom.admin.ml")
    @Description("Regenerate skill tree branches using ML clustering")
    public void onMlRegenerate(Player sender) {
        if (plugin.getDynamicTreeManager() == null) {
            sender.sendMessage(Component.text("✗ Dynamic tree generation is not enabled!", NamedTextColor.RED));
            sender.sendMessage(Component.text("  Enable it in config.yml: features.dynamic-tree-generation: true", NamedTextColor.GRAY));
            return;
        }
        
        sender.sendMessage(Component.text("⚙ Regenerating skill tree branches using ML clustering...", NamedTextColor.YELLOW));
        
        plugin.getServer().getGlobalRegionScheduler().run(plugin, task -> {
            plugin.getDynamicTreeManager().regenerateAllBranches();
            
            sender.sendMessage(Component.text("✓ Tree regeneration complete!", NamedTextColor.GREEN));
            sender.sendMessage(Component.text("  Note: Server restart required to apply changes to advancements", NamedTextColor.GRAY));
        });
    }
    
    @Subcommand("admin ml generate")
    @CommandPermission("atom.admin.ml")
    @CommandCompletion("farmer|guardsman|miner|healer|blacksmith|builder|librarian")
    @Description("Generate branches for a specific skill cluster")
    @Syntax("<cluster>")
    public void onMlGenerate(Player sender, String clusterId) {
        if (plugin.getDynamicTreeManager() == null) {
            sender.sendMessage(Component.text("✗ Dynamic tree generation is not enabled!", NamedTextColor.RED));
            return;
        }
        
        String[] validClusters = {"farmer", "guardsman", "miner", "healer", "blacksmith", "builder", "librarian"};
        boolean isValid = false;
        for (String valid : validClusters) {
            if (valid.equalsIgnoreCase(clusterId)) {
                isValid = true;
                clusterId = valid;
                break;
            }
        }
        
        if (!isValid) {
            sender.sendMessage(Component.text("✗ Invalid cluster. Valid options: farmer, guardsman, miner, healer, blacksmith, builder, librarian", NamedTextColor.RED));
            return;
        }
        
        String finalClusterId = clusterId;
        sender.sendMessage(Component.text("⚙ Generating branches for " + clusterId + "...", NamedTextColor.YELLOW));
        
        plugin.getServer().getGlobalRegionScheduler().run(plugin, task -> {
            plugin.getDynamicTreeManager().generateBranchesForCluster(finalClusterId);
            sender.sendMessage(Component.text("✓ Generated branches for " + finalClusterId, NamedTextColor.GREEN));
        });
    }
    
    @Subcommand("admin ml restructure")
    @CommandPermission("atom.admin.ml")
    @Description("Restructure trees based on player usage patterns")
    public void onMlRestructure(Player sender) {
        if (plugin.getDynamicTreeManager() == null) {
            sender.sendMessage(Component.text("✗ Dynamic tree generation is not enabled!", NamedTextColor.RED));
            return;
        }
        
        sender.sendMessage(Component.text("⚙ Analyzing player usage patterns and restructuring trees...", NamedTextColor.YELLOW));
        
        plugin.getServer().getGlobalRegionScheduler().run(plugin, task -> {
            plugin.getDynamicTreeManager().autoRestructureIfNeeded();
            sender.sendMessage(Component.text("✓ Tree restructure analysis complete!", NamedTextColor.GREEN));
            sender.sendMessage(Component.text("  Check console for details on which trees were restructured", NamedTextColor.GRAY));
        });
    }
    
    @Subcommand("links")
    @CommandPermission("atom.links")
    @Description("View your skill links and potential combinations")
    public void onLinks(Player player) {
        Optional<PlayerSkillData> dataOpt = dataManager.getCachedPlayerData(player.getUniqueId());
        
        if (dataOpt.isEmpty()) {
            player.sendMessage(Component.text("No data loaded", NamedTextColor.RED));
            return;
        }
        
        PlayerSkillData data = dataOpt.get();
        
        
        Map<String, Double> effectiveXpMap = new HashMap<>();
        for (String skillId : data.getAllIntrinsicXp().keySet()) {
            var effectiveXp = xpEngine.getEffectiveXp(data, skillId);
            effectiveXpMap.put(skillId, effectiveXp.progressPercent());
        }
        
        
        var currentLinks = plugin.getSkillLinkingSystem().getPlayerLinks(player.getUniqueId());
        
        
        var potentialLinks = plugin.getSkillLinkingSystem().discoverPotentialLinks(data, effectiveXpMap);
        
        player.sendMessage(Component.text("═══════════════════════════", NamedTextColor.GOLD));
        player.sendMessage(Component.text("Skill Links", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("═══════════════════════════", NamedTextColor.GOLD));
        
        if (currentLinks.isEmpty()) {
            player.sendMessage(Component.text("  No active links (max 4)", NamedTextColor.GRAY));
        } else {
            player.sendMessage(Component.text("  Active Links (" + currentLinks.size() + "/4):", NamedTextColor.GREEN));
            for (var link : currentLinks) {
                player.sendMessage(Component.text("    ✦ " + link.name(), NamedTextColor.AQUA)
                    .append(Component.text(" (" + link.linkedSkills().size() + " skills)", NamedTextColor.GRAY)));
            }
        }
        
        player.sendMessage(Component.text(""));
        
        if (potentialLinks.isEmpty()) {
            player.sendMessage(Component.text("  No potential links available", NamedTextColor.GRAY));
            player.sendMessage(Component.text("  Master more leaf skills (95%+) to unlock links!", NamedTextColor.YELLOW));
        } else {
            player.sendMessage(Component.text("  Potential Links:", NamedTextColor.GREEN));
            int shown = Math.min(5, potentialLinks.size());
            for (int i = 0; i < shown; i++) {
                var link = potentialLinks.get(i);
                player.sendMessage(Component.text("    " + (i+1) + ". " + link.name(), NamedTextColor.AQUA)
                    .append(Component.text(" - " + String.format("%.0f%%", link.compatibility() * 100) + " match", NamedTextColor.GRAY)));
            }
            if (potentialLinks.size() > 5) {
                player.sendMessage(Component.text("    ... and " + (potentialLinks.size() - 5) + " more", NamedTextColor.GRAY));
            }
        }
        
        player.sendMessage(Component.text("═══════════════════════════", NamedTextColor.GOLD));
    }
    
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
    
    private NamedTextColor getColorForLevel(double level) {
        if (level >= 75.0) return NamedTextColor.GOLD;
        if (level >= 50.0) return NamedTextColor.GREEN;
        if (level >= 25.0) return NamedTextColor.YELLOW;
        return NamedTextColor.RED;
    }
}

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
    public void onSet(Player sender, Player target, String skillId, long amount) {
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
    public void onAdd(Player sender, Player target, String skillId, long amount) {
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

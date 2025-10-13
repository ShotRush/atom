package org.shotrush.atom.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.shotrush.atom.Atom;
import org.shotrush.atom.skill.SkillData;
import org.shotrush.atom.skill.SkillType;
import org.shotrush.atom.synergy.SynergyCalculator;

import java.util.List;
import java.util.Map;

@CommandAlias("atom")
public class AtomCommand extends BaseCommand {
    private final Atom plugin;
    
    public AtomCommand(Atom plugin) {
        this.plugin = plugin;
    }
    
    @Default
    @Subcommand("skills")
    @Description("Open the skills GUI")
    public void onSkills(Player player) {
        plugin.getSkillGUI().openMainMenu(player);
    }
    
    @Subcommand("reload")
    @CommandPermission("atom.admin")
    @Description("Reload the plugin configuration")
    public void onReload(CommandSender sender) {
        plugin.reloadConfiguration();
        sender.sendMessage("§aAtom configuration reloaded!");
        sender.sendMessage("§7All configs (skills, decay, learning-boost, synergy) have been refreshed.");
    }
    
    @Subcommand("save")
    @CommandPermission("atom.admin")
    @Description("Save all player data")
    public void onSave(CommandSender sender) {
        plugin.getDataManager().saveAllData();
        sender.sendMessage("§aAll player data saved!");
    }
    
    @Subcommand("setxp")
    @CommandPermission("atom.admin")
    @Description("Set a player's XP for a specific skill and item")
    @Syntax("<player> <skill> <item> <amount>")
    public void onSetXP(CommandSender sender, String playerName, String skillName, String itemName, int amount) {
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage("§cPlayer not found!");
            return;
        }
        
        SkillType skillType;
        try {
            skillType = SkillType.valueOf(skillName.toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage("§cInvalid skill type! Valid types: " + String.join(", ", getSkillNames()));
            return;
        }
        
        String itemKey = itemName.toUpperCase().replace(" ", "_");
        SkillData skillData = plugin.getSkillManager().getSkillData(target.getUniqueId(), skillType);
        skillData.setExperience(itemKey, amount);
        
        sender.sendMessage("§aSet " + target.getName() + "'s " + skillType.name() + " XP for " + itemKey + " to " + amount);
        target.sendMessage("§eYour " + skillType.name() + " XP for " + itemKey + " has been set to " + amount);
    }
    
    @Subcommand("addxp")
    @CommandPermission("atom.admin")
    @Description("Add XP to a player for a specific skill and item")
    @Syntax("<player> <skill> <item> <amount>")
    public void onAddXP(CommandSender sender, String playerName, String skillName, String itemName, int amount) {
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage("§cPlayer not found!");
            return;
        }
        
        SkillType skillType;
        try {
            skillType = SkillType.valueOf(skillName.toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage("§cInvalid skill type! Valid types: " + String.join(", ", getSkillNames()));
            return;
        }
        
        String itemKey = itemName.toUpperCase().replace(" ", "_");
        int maxXP = plugin.getSkillConfig().getConfig(skillType).maxExperience;
        plugin.getSkillManager().addExperience(target.getUniqueId(), skillType, itemKey, amount, maxXP);
        
        int newXP = plugin.getSkillManager().getExperience(target.getUniqueId(), skillType, itemKey);
        sender.sendMessage("§aAdded " + amount + " XP to " + target.getName() + "'s " + skillType.name() + " for " + itemKey + " (New total: " + newXP + ")");
        target.sendMessage("§eYou gained " + amount + " " + skillType.name() + " XP for " + itemKey + "!");
    }
    
    @Subcommand("checkxp")
    @CommandPermission("atom.admin")
    @Description("Check a player's XP for a specific skill and item")
    @Syntax("<player> <skill> <item>")
    public void onCheckXP(CommandSender sender, String playerName, String skillName, String itemName) {
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage("§cPlayer not found!");
            return;
        }
        
        SkillType skillType;
        try {
            skillType = SkillType.valueOf(skillName.toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage("§cInvalid skill type! Valid types: " + String.join(", ", getSkillNames()));
            return;
        }
        
        String itemKey = itemName.toUpperCase().replace(" ", "_");
        int xp = plugin.getSkillManager().getExperience(target.getUniqueId(), skillType, itemKey);
        double successRate = plugin.getSkillManager().getSuccessRate(
            target.getUniqueId(),
            skillType,
            itemKey,
            plugin.getSkillConfig().getConfig(skillType).baseSuccessRate,
            plugin.getSkillConfig().getConfig(skillType).maxSuccessRate,
            plugin.getSkillConfig().getConfig(skillType).experiencePerLevel
        );
        
        sender.sendMessage("§e" + target.getName() + "'s " + skillType.name() + " stats for " + itemKey + ":");
        sender.sendMessage("§7XP: §f" + xp + " §7/ §f" + plugin.getSkillConfig().getConfig(skillType).maxExperience);
        sender.sendMessage("§7Success Rate: §f" + String.format("%.1f%%", successRate * 100));
    }
    
    @Subcommand("resetskill")
    @CommandPermission("atom.admin")
    @Description("Reset all XP for a player's specific skill")
    @Syntax("<player> <skill>")
    public void onResetSkill(CommandSender sender, String playerName, String skillName) {
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage("§cPlayer not found!");
            return;
        }
        
        SkillType skillType;
        try {
            skillType = SkillType.valueOf(skillName.toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage("§cInvalid skill type! Valid types: " + String.join(", ", getSkillNames()));
            return;
        }
        
        SkillData skillData = plugin.getSkillManager().getSkillData(target.getUniqueId(), skillType);
        Map<String, Integer> allExp = skillData.getAllExperience();
        for (String key : allExp.keySet()) {
            skillData.setExperience(key, 0);
        }
        
        sender.sendMessage("§aReset all " + skillType.name() + " XP for " + target.getName());
        target.sendMessage("§cYour " + skillType.name() + " skill has been reset!");
    }
    
    @Subcommand("resetall")
    @CommandPermission("atom.admin")
    @Description("Reset all XP for a player")
    @Syntax("<player>")
    public void onResetAll(CommandSender sender, String playerName) {
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage("§cPlayer not found!");
            return;
        }
        
        for (SkillType skillType : SkillType.values()) {
            SkillData skillData = plugin.getSkillManager().getSkillData(target.getUniqueId(), skillType);
            Map<String, Integer> allExp = skillData.getAllExperience();
            for (String key : allExp.keySet()) {
                skillData.setExperience(key, 0);
            }
        }
        
        sender.sendMessage("§aReset all skills for " + target.getName());
        target.sendMessage("§cAll your skills have been reset!");
    }
    
    @Subcommand("stats")
    @CommandPermission("atom.admin")
    @Description("View detailed stats for a player")
    @Syntax("<player>")
    public void onStats(CommandSender sender, String playerName) {
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage("§cPlayer not found!");
            return;
        }
        
        sender.sendMessage("§6§l" + target.getName() + "'s Skill Statistics:");
        sender.sendMessage("§7" + "=".repeat(40));
        
        for (SkillType skillType : SkillType.values()) {
            SkillData skillData = plugin.getSkillManager().getSkillData(target.getUniqueId(), skillType);
            Map<String, Integer> allExp = skillData.getAllExperience();
            
            if (allExp.isEmpty()) {
                continue;
            }
            
            int totalXP = allExp.values().stream().mapToInt(Integer::intValue).sum();
            int uniqueItems = allExp.size();
            
            sender.sendMessage("§e" + skillType.name() + ":");
            sender.sendMessage("  §7Total XP: §f" + totalXP);
            sender.sendMessage("  §7Unique Items: §f" + uniqueItems);
        }
        
        sender.sendMessage("§7" + "=".repeat(40));
    }
    
    @Subcommand("list")
    @CommandPermission("atom.admin")
    @Description("List all available skills")
    public void onList(CommandSender sender) {
        sender.sendMessage("§6§lAvailable Skills:");
        for (SkillType skillType : SkillType.values()) {
            boolean enabled = plugin.getSkillConfig().getConfig(skillType).enabled;
            String status = enabled ? "§a✓" : "§c✗";
            sender.sendMessage(status + " §e" + skillType.name());
        }
    }

    @Subcommand("createbook")
    @CommandPermission("atom.admin")
    @Description("Create a skill book for a specific skill and item")
    @Syntax("<skill> <item>")
    public void onCreateBook(Player player, String skillName, String itemName) {
        SkillType skillType;
        try {
            skillType = SkillType.valueOf(skillName.toUpperCase());
        } catch (IllegalArgumentException e) {
            player.sendMessage("§cInvalid skill type! Valid types: " + String.join(", ", getSkillNames()));
            return;
        }

        String itemKey = itemName.toUpperCase().replace(" ", "_");
        int authorExp = plugin.getSkillManager().getExperience(player.getUniqueId(), skillType, itemKey);

        if (authorExp == 0) {
            player.sendMessage("§cYou have no experience with " + itemKey + " in " + skillType.name() + "!");
            return;
        }

        ItemStack book = plugin.getBookManager().createSkillBook(skillType, itemKey, player.getName(), authorExp);
        player.getInventory().addItem(book);
        player.sendMessage("§aCreated skill book for " + skillType.name() + ": " + itemKey);
    }

    @Subcommand("boost")
    @Description("Check your current learning boost for a skill")
    @Syntax("<skill> <item>")
    public void onBoost(Player player, String skillName, String itemName) {
        SkillType skillType;
        try {
            skillType = SkillType.valueOf(skillName.toUpperCase());
        } catch (IllegalArgumentException e) {
            player.sendMessage("§cInvalid skill type! Valid types: " + String.join(", ", getSkillNames()));
            return;
        }

        String itemKey = itemName.toUpperCase().replace(" ", "_");
        double boost = plugin.getSkillManager().calculateBoost(player, skillType, itemKey);

        player.sendMessage("§6§lLearning Boost for " + skillType.name() + ": " + itemKey);
        player.sendMessage("§7Total Multiplier: §f" + String.format("%.2fx", boost));
        player.sendMessage("§7XP Gain: §f" + String.format("%.0f%%", boost * 100));
    }

    @Subcommand("synergy")
    @Description("Check what synergies will trigger for a skill action")
    @Syntax("<skill> <item>")
    public void onSynergy(Player player, String skillName, String itemName) {
        SkillType skillType;
        try {
            skillType = SkillType.valueOf(skillName.toUpperCase());
        } catch (IllegalArgumentException e) {
            player.sendMessage("§cInvalid skill type! Valid types: " + String.join(", ", getSkillNames()));
            return;
        }

        String itemKey = itemName.toUpperCase().replace(" ", "_");
        int baseXP = plugin.getSkillConfig().getConfig(skillType).experiencePerAction;

        List<SynergyCalculator.SynergyGrant> synergies = plugin.getSynergyCalculator().calculateSynergies(
            player.getUniqueId(), skillType, itemKey, baseXP
        );

        if (synergies.isEmpty()) {
            player.sendMessage("§eNo synergies found for " + skillType.name() + ": " + itemKey);
            return;
        }

        player.sendMessage("§6§lSynergies for " + skillType.name() + ": " + itemKey);
        player.sendMessage("§7Base XP: §f" + baseXP);
        player.sendMessage("§7Synergy Grants:");
        for (SynergyCalculator.SynergyGrant grant : synergies) {
            player.sendMessage("  §e" + grant.skill.name() + " §7- §f" + grant.itemKey + " §7(+§a" + grant.xp + " XP§7)");
        }
    }
    
    private String[] getSkillNames() {
        SkillType[] types = SkillType.values();
        String[] names = new String[types.length];
        for (int i = 0; i < types.length; i++) {
            names[i] = types[i].name();
        }
        return names;
    }
}

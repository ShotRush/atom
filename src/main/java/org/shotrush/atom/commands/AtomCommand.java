package org.shotrush.atom.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.shotrush.atom.Atom;
import org.shotrush.atom.engine.XpEngine;
import org.shotrush.atom.manager.PlayerDataManager;
import org.shotrush.atom.model.Models.EffectiveXp;
import org.shotrush.atom.model.PlayerSkillData;
import org.shotrush.atom.tree.Trees.Registry;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@CommandAlias("atom")
@Description("Atom skill system commands")
public final class AtomCommand extends BaseCommand {
    
    private final Atom plugin;
    private final PlayerDataManager dataManager;
    private final XpEngine xpEngine;
    private final Registry treeRegistry;
    
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
        Map<String, Long> allXp = data.getAllIntrinsicXp();
        
        viewer.sendMessage(Component.text("═══════════════════════════", NamedTextColor.GOLD));
        viewer.sendMessage(Component.text(target.getName() + "'s Skills", NamedTextColor.YELLOW));
        viewer.sendMessage(Component.text("═══════════════════════════", NamedTextColor.GOLD));
        
        String[] rootSkills = {"farmer", "guardsman", "miner", "healer", "blacksmith", "builder", "librarian"};
        
        viewer.sendMessage(Component.text("Root Skills:", NamedTextColor.GREEN));
        for (String skillId : rootSkills) {
            long xp = allXp.getOrDefault(skillId, 0L);
            if (xp > 0) {
                EffectiveXp effectiveXp = xpEngine.getEffectiveXp(data, skillId);
                double level = effectiveXp.progressPercent() * 100.0;
                
                Component skillLine = Component.text("  " + capitalize(skillId) + ": ", NamedTextColor.GRAY)
                    .append(Component.text(String.format("%.1f%%", level), getColorForLevel(level)))
                    .append(Component.text(" (" + effectiveXp.intrinsicXp() + " XP)", NamedTextColor.DARK_GRAY));
                
                viewer.sendMessage(skillLine);
            }
        }
        
        Map<String, Long> otherSkills = new java.util.TreeMap<>();
        for (Map.Entry<String, Long> entry : allXp.entrySet()) {
            String skillId = entry.getKey();
            if (!java.util.Arrays.asList(rootSkills).contains(skillId) && entry.getValue() > 0) {
                otherSkills.put(skillId, entry.getValue());
            }
        }
        
        if (!otherSkills.isEmpty()) {
            viewer.sendMessage(Component.text(""));
            viewer.sendMessage(Component.text("Other Skills:", NamedTextColor.GREEN));
            otherSkills.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .forEach(entry -> {
                    viewer.sendMessage(Component.text("  " + entry.getKey() + ": ", NamedTextColor.GRAY)
                        .append(Component.text(entry.getValue() + " XP", NamedTextColor.WHITE)));
                });
            
            if (otherSkills.size() > 10) {
                viewer.sendMessage(Component.text("  ... and " + (otherSkills.size() - 10) + " more", NamedTextColor.DARK_GRAY));
            }
        }
        
        viewer.sendMessage(Component.text("═══════════════════════════", NamedTextColor.GOLD));
    }
    
    @Subcommand("admin")
    @CommandPermission("atom.admin")
    @Description("Show admin commands")
    public void onAdmin(Player player) {
        player.sendMessage(Component.text("═══════════════════════════", NamedTextColor.GOLD));
        player.sendMessage(Component.text("Atom Admin Commands", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("═══════════════════════════", NamedTextColor.GOLD));
        player.sendMessage(Component.text("General:", NamedTextColor.GREEN));
        player.sendMessage(Component.text("  /atom admin reload", NamedTextColor.AQUA)
            .append(Component.text(" - Reload configuration", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("  /atom admin set <player> <skill> <xp>", NamedTextColor.AQUA)
            .append(Component.text(" - Set player XP", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("  /atom admin add <player> <skill> <xp>", NamedTextColor.AQUA)
            .append(Component.text(" - Add player XP", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("  /atom admin save", NamedTextColor.AQUA)
            .append(Component.text(" - Force save all data", NamedTextColor.GRAY)));
        player.sendMessage(Component.text(""));
        player.sendMessage(Component.text("ML Commands:", NamedTextColor.GREEN));
        player.sendMessage(Component.text("  /atom admin ml cluster", NamedTextColor.AQUA)
            .append(Component.text(" - Run k-means clustering", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("  /atom admin ml generate <cluster>", NamedTextColor.AQUA)
            .append(Component.text(" - Generate branches", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("  /atom admin ml regenerate", NamedTextColor.AQUA)
            .append(Component.text(" - Regenerate all branches", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("  /atom admin ml restructure", NamedTextColor.AQUA)
            .append(Component.text(" - Restructure trees", NamedTextColor.GRAY)));
        player.sendMessage(Component.text(""));
        player.sendMessage(Component.text("Testing:", NamedTextColor.GREEN));
        player.sendMessage(Component.text("  /atom admin test tree", NamedTextColor.AQUA)
            .append(Component.text(" - Generate example tree", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("  /atom admin test simulate <count>", NamedTextColor.AQUA)
            .append(Component.text(" - Simulate actions", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("  /atom admin test clear", NamedTextColor.AQUA)
            .append(Component.text(" - Clear all XP", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("  /atom admin test ml <context>", NamedTextColor.AQUA)
            .append(Component.text(" - Test ML inference", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("═══════════════════════════", NamedTextColor.GOLD));
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
    
    @Subcommand("admin test tree")
    @CommandPermission("atom.admin.test")
    @Description("Generate an example skill tree for testing")
    public void onTestTree(Player sender) {
        sender.sendMessage(Component.text("Generating test tree...", NamedTextColor.YELLOW));
        
        Optional<PlayerSkillData> dataOpt = dataManager.getCachedPlayerData(sender.getUniqueId());
        if (dataOpt.isEmpty()) {
            sender.sendMessage(Component.text("No player data loaded!", NamedTextColor.RED));
            return;
        }
        
        PlayerSkillData data = dataOpt.get();
        
        data.setIntrinsicXp("miner", 500);
        data.setIntrinsicXp("miner.ore_mining", 300);
        data.setIntrinsicXp("miner.ore_mining.iron", 200);
        data.setIntrinsicXp("miner.ore_mining.diamond", 100);
        data.setIntrinsicXp("farmer", 400);
        data.setIntrinsicXp("farmer.crop_farming", 250);
        data.setIntrinsicXp("farmer.crop_farming.wheat", 150);
        data.setIntrinsicXp("blacksmith", 300);
        data.setIntrinsicXp("blacksmith.tool_crafting", 200);
        data.setIntrinsicXp("blacksmith.tool_crafting.pickaxes", 100);
        
        for (var tree : treeRegistry.getAllTrees()) {
            plugin.getAdvancementGenerator().updatePlayerAdvancements(sender, data, tree);
        }
        
        sender.sendMessage(Component.text("✓ Test tree generated!", NamedTextColor.GREEN));
        sender.sendMessage(Component.text("  - Miner: 500 XP", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  - Farmer: 400 XP", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  - Blacksmith: 300 XP", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Check /advancements to see the tree!", NamedTextColor.YELLOW));
    }
    
    @Subcommand("admin test simulate")
    @CommandPermission("atom.admin.test")
    @CommandCompletion("10|50|100|500|1000")
    @Description("Simulate random player actions for ML testing")
    public void onTestSimulate(Player sender, @Default("100") int actionCount) {
        sender.sendMessage(Component.text("Simulating " + actionCount + " actions...", NamedTextColor.YELLOW));
        
        Optional<PlayerSkillData> dataOpt = dataManager.getCachedPlayerData(sender.getUniqueId());
        if (dataOpt.isEmpty()) {
            sender.sendMessage(Component.text("No player data loaded!", NamedTextColor.RED));
            return;
        }
        
        PlayerSkillData data = dataOpt.get();
        String[] materials = {"diamond_ore", "iron_ore", "coal_ore", "wheat", "carrot", "zombie", "skeleton", 
                              "iron_sword", "diamond_pickaxe", "oak_planks", "stone_bricks"};
        
        java.util.Random random = new java.util.Random();
        
        for (int i = 0; i < actionCount; i++) {
            String material = materials[random.nextInt(materials.length)];
            var actionType = org.shotrush.atom.ml.ActionAnalyzer.ActionType.values()[
                random.nextInt(org.shotrush.atom.ml.ActionAnalyzer.ActionType.values().length)];
            
            var result = plugin.getActionAnalyzer().recordAction(sender.getUniqueId(), actionType, material);
            xpEngine.awardXp(data, result.skillId(), result.xpAmount());
        }
        
        sender.sendMessage(Component.text("✓ Simulated " + actionCount + " actions!", NamedTextColor.GREEN));
        sender.sendMessage(Component.text("Use /atom stats to see results", NamedTextColor.YELLOW));
    }
    
    @Subcommand("admin test clear")
    @CommandPermission("atom.admin.test")
    @Description("Clear all XP for testing")
    public void onTestClear(Player sender) {
        Optional<PlayerSkillData> dataOpt = dataManager.getCachedPlayerData(sender.getUniqueId());
        if (dataOpt.isEmpty()) {
            sender.sendMessage(Component.text("No player data loaded!", NamedTextColor.RED));
            return;
        }
        
        PlayerSkillData data = dataOpt.get();
        for (String skillId : data.getAllIntrinsicXp().keySet()) {
            data.setIntrinsicXp(skillId, 0);
        }
        
        sender.sendMessage(Component.text("✓ All XP cleared!", NamedTextColor.GREEN));
    }
    
    @Subcommand("admin test ml")
    @CommandPermission("atom.admin.test")
    @Description("Test ML category inference")
    public void onTestMl(Player sender, String context) {
        sender.sendMessage(Component.text("═══════════════════════════", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("ML Category Test: " + context, NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("═══════════════════════════", NamedTextColor.GOLD));
        
        var actionType = plugin.getActionAnalyzer().inferActionTypeFromContext(context);
        var result = plugin.getActionAnalyzer().recordAction(sender.getUniqueId(), actionType, context);
        
        sender.sendMessage(Component.text("Action Type: ", NamedTextColor.GRAY)
            .append(Component.text(actionType.name(), NamedTextColor.AQUA)));
        sender.sendMessage(Component.text("Skill ID: ", NamedTextColor.GRAY)
            .append(Component.text(result.skillId(), NamedTextColor.GREEN)));
        sender.sendMessage(Component.text("XP Amount: ", NamedTextColor.GRAY)
            .append(Component.text(result.xpAmount() + " XP", NamedTextColor.YELLOW)));
        sender.sendMessage(Component.text("Importance: ", NamedTextColor.GRAY)
            .append(Component.text(String.format("%.2f", result.importance()), NamedTextColor.GOLD)));
        sender.sendMessage(Component.text("═══════════════════════════", NamedTextColor.GOLD));
    }
    
    @Subcommand("admin ml train")
    @CommandPermission("atom.admin.ml")
    @CommandCompletion("100|500|1000|5000|10000")
    @Description("Train the ML classifier with quiz and synthetic data")
    public void onMlTrain(Player sender, @Default("1000") int iterations) {
        sender.sendMessage(Component.text("🧠 Training ML classifier...", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("  Quiz + Synthetic training", NamedTextColor.GRAY));
        
        long startTime = System.currentTimeMillis();
        
        plugin.getServer().getGlobalRegionScheduler().run(plugin, task -> {
            int quizQuestions = Math.min(100, iterations / 10);
            int correct = plugin.getActionAnalyzer().trainWithQuiz(quizQuestions);
            double accuracy = (double) correct / quizQuestions * 100;
            
            int trained = plugin.getActionAnalyzer().trainWithSyntheticData(iterations);
            long duration = System.currentTimeMillis() - startTime;
            
            sender.sendMessage(Component.text("✓ Training complete!", NamedTextColor.GREEN));
            sender.sendMessage(Component.text("  Quiz accuracy: ", NamedTextColor.GRAY)
                .append(Component.text(String.format("%.1f%%", accuracy), 
                    accuracy >= 90 ? NamedTextColor.GREEN : accuracy >= 70 ? NamedTextColor.YELLOW : NamedTextColor.RED)));
            sender.sendMessage(Component.text("  Synthetic examples: ", NamedTextColor.GRAY)
                .append(Component.text(trained, NamedTextColor.AQUA)));
            sender.sendMessage(Component.text("  Duration: ", NamedTextColor.GRAY)
                .append(Component.text(duration + "ms", NamedTextColor.YELLOW)));
            sender.sendMessage(Component.text("  ML is now smarter!", NamedTextColor.GREEN));
        });
    }
    
    @Subcommand("admin ml cluster")
    @CommandPermission("atom.admin.ml")
    @Description("Run k-means clustering to discover player specializations")
    public void onMlCluster(Player sender) {
        int playerCount = dataManager.getCacheSize();
        
        if (playerCount < 7) {
            sender.sendMessage(Component.text("✗ Not enough players online for clustering (need 7, have " + playerCount + ")", NamedTextColor.RED));
            return;
        }
        
        sender.sendMessage(Component.text("⚙ Running k-means clustering on " + playerCount + " players...", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("  Using normalized leaf node XP vectors", NamedTextColor.GRAY));
        
        plugin.getServer().getGlobalRegionScheduler().run(plugin, task -> {
            plugin.getDynamicTreeManager().runSpecializationClustering();
            
            var clusters = plugin.getSpecializationClusterer().getClusters();
            
            sender.sendMessage(Component.text("✓ Clustering complete! Discovered " + clusters.size() + " specializations:", NamedTextColor.GREEN));
            
            for (int i = 0; i < clusters.size(); i++) {
                var cluster = clusters.get(i);
                sender.sendMessage(Component.text("  [" + i + "] ", NamedTextColor.GRAY)
                    .append(Component.text(cluster.getLabel(), NamedTextColor.AQUA))
                    .append(Component.text(" - " + cluster.getMembers().size() + " players", NamedTextColor.YELLOW)));
            }
        });
    }
    
    @Subcommand("specialization")
    @CommandPermission("atom.specialization")
    @Description("View your discovered specialization")
    public void onSpecialization(Player player) {
        var specializationOpt = plugin.getSpecializationClusterer().getPlayerSpecialization(player.getUniqueId());
        
        if (specializationOpt.isEmpty()) {
            player.sendMessage(Component.text("═══════════════════════════", NamedTextColor.GOLD));
            player.sendMessage(Component.text("Your Specialization", NamedTextColor.YELLOW));
            player.sendMessage(Component.text("═══════════════════════════", NamedTextColor.GOLD));
            player.sendMessage(Component.text("  No specialization discovered yet!", NamedTextColor.GRAY));
            player.sendMessage(Component.text("  Play more to develop your unique playstyle", NamedTextColor.YELLOW));
            player.sendMessage(Component.text("  Clustering runs every 6 hours", NamedTextColor.DARK_GRAY));
            player.sendMessage(Component.text("═══════════════════════════", NamedTextColor.GOLD));
            return;
        }
        
        var spec = specializationOpt.get();
        
        player.sendMessage(Component.text("═══════════════════════════", NamedTextColor.GOLD));
        player.sendMessage(Component.text("Your Specialization", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("═══════════════════════════", NamedTextColor.GOLD));
        player.sendMessage(Component.text("  ✦ ", NamedTextColor.GOLD)
            .append(Component.text(spec.label(), NamedTextColor.AQUA)));
        player.sendMessage(Component.text(""));
        player.sendMessage(Component.text("  Top Skills:", NamedTextColor.GREEN));
        
        int shown = Math.min(5, spec.topSkills().size());
        for (int i = 0; i < shown; i++) {
            String skillId = spec.topSkills().get(i);
            player.sendMessage(Component.text("    " + (i+1) + ". ", NamedTextColor.GRAY)
                .append(Component.text(skillId, NamedTextColor.YELLOW)));
        }
        
        player.sendMessage(Component.text(""));
        player.sendMessage(Component.text("  Cluster: ", NamedTextColor.GRAY)
            .append(Component.text("#" + spec.clusterId(), NamedTextColor.DARK_GRAY))
            .append(Component.text(" (" + spec.clusterSize() + " similar players)", NamedTextColor.DARK_GRAY)));
        player.sendMessage(Component.text("═══════════════════════════", NamedTextColor.GOLD));
    }
    
    @Subcommand("specialization")
    @CommandPermission("atom.specialization.others")
    @CommandCompletion("@players")
    @Description("View another player's specialization")
    public void onSpecializationOther(Player sender, Player target) {
        var specializationOpt = plugin.getSpecializationClusterer().getPlayerSpecialization(target.getUniqueId());
        
        if (specializationOpt.isEmpty()) {
            sender.sendMessage(Component.text(target.getName() + " has no discovered specialization yet", NamedTextColor.GRAY));
            return;
        }
        
        var spec = specializationOpt.get();
        
        sender.sendMessage(Component.text("═══════════════════════════", NamedTextColor.GOLD));
        sender.sendMessage(Component.text(target.getName() + "'s Specialization", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("═══════════════════════════", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("  ✦ ", NamedTextColor.GOLD)
            .append(Component.text(spec.label(), NamedTextColor.AQUA)));
        sender.sendMessage(Component.text(""));
        sender.sendMessage(Component.text("  Top Skills:", NamedTextColor.GREEN));
        
        int shown = Math.min(3, spec.topSkills().size());
        for (int i = 0; i < shown; i++) {
            String skillId = spec.topSkills().get(i);
            sender.sendMessage(Component.text("    " + (i+1) + ". ", NamedTextColor.GRAY)
                .append(Component.text(skillId, NamedTextColor.YELLOW)));
        }
        
        sender.sendMessage(Component.text("═══════════════════════════", NamedTextColor.GOLD));
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

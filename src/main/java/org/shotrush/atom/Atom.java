package org.shotrush.atom;

import co.aikar.commands.PaperCommandManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.shotrush.atom.advancement.AdvancementGenerator;
import org.shotrush.atom.commands.AtomCommand;
import org.shotrush.atom.config.AtomConfig;
import org.shotrush.atom.config.DefaultTrees;
import org.shotrush.atom.config.TreeBuilder;
import org.shotrush.atom.config.TreeDefinition;
import org.shotrush.atom.effects.EffectManager;
import org.shotrush.atom.effects.FeedbackManager;
import org.shotrush.atom.engine.XpEngine;
import org.shotrush.atom.features.ToolReinforcement;
import org.shotrush.atom.features.XpTransfer;
import org.shotrush.atom.listener.FeatureListener;
import org.shotrush.atom.listener.PlayerConnectionListener;
import org.shotrush.atom.listener.SkillEventListener;
import org.shotrush.atom.manager.PlayerDataManager;
import org.shotrush.atom.milestone.MilestoneManager;
import org.shotrush.atom.storage.SQLiteStorage;
import org.shotrush.atom.storage.StorageProvider;
import org.shotrush.atom.tree.SkillTree;
import org.shotrush.atom.tree.SkillTreeRegistry;

import java.nio.file.Path;
import java.util.List;

public final class Atom extends JavaPlugin {

    private AtomConfig config;
    private StorageProvider storage;
    private PlayerDataManager dataManager;
    private SkillTreeRegistry treeRegistry;
    private XpEngine xpEngine;
    private EffectManager effectManager;
    private FeedbackManager feedbackManager;
    private MilestoneManager milestoneManager;
    private AdvancementGenerator advancementGenerator;
    private ToolReinforcement toolReinforcement;
    private XpTransfer xpTransfer;
    private org.shotrush.atom.tree.MultiTreeAggregator multiTreeAggregator;
    private PaperCommandManager commandManager;
    
    @Override
    public void onEnable() {
        getLogger().info("Initializing Atom plugin...");

        try {
            saveDefaultConfig();
            loadConfiguration();
            initializeStorage();
            initializeTreeRegistry();
            initializeManagers();
            initializeFeatures();
            registerListeners();
            registerCommands();
            startTasks();

            getLogger().info("Atom has been enabled successfully!");
        } catch (Exception e) {
            getLogger().severe("Failed to enable Atom: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("Shutting down Atom plugin...");

        try {
            if (commandManager != null) {
                commandManager.unregisterCommands();
            }
            
            if (dataManager != null) {
                getLogger().info("Saving all player data...");
                dataManager.saveAllPlayerData().join();
            }

            if (storage != null) {
                getLogger().info("Closing database connection...");
                storage.shutdown().join();
            }

            getLogger().info("Atom has been disabled successfully!");
        } catch (Exception e) {
            getLogger().severe("Error during shutdown: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void loadConfiguration() {
        reloadConfig();
        this.config = AtomConfig.loadFrom(getConfig());
        getLogger().info("Configuration loaded");
    }

    private void initializeStorage() {
        Path databasePath = getDataFolder().toPath().resolve("atom.db");
        storage = new SQLiteStorage(databasePath);
        storage.initialize().join();
        getLogger().info("Database initialized at: " + databasePath);
    }

    private void initializeTreeRegistry() {
        treeRegistry = new SkillTreeRegistry();

        List<TreeDefinition> defaultTrees = DefaultTrees.createDefaultTrees();

        for (TreeDefinition treeDef : defaultTrees) {
            SkillTree tree = TreeBuilder.buildFromDefinition(treeDef);
            treeRegistry.registerTree(tree);
            getLogger().info("Registered skill tree: " + tree.name() + " (" + tree.size() + " nodes)");
        }
        
    }

    private void initializeManagers() {
        dataManager = new PlayerDataManager(storage);
        multiTreeAggregator = new org.shotrush.atom.tree.MultiTreeAggregator(treeRegistry);
        xpEngine = new XpEngine(treeRegistry, multiTreeAggregator);
        effectManager = new EffectManager(this, config, xpEngine, treeRegistry);
        feedbackManager = new FeedbackManager(config);
        milestoneManager = new MilestoneManager(xpEngine, feedbackManager);
        advancementGenerator = new AdvancementGenerator(this, xpEngine);
        
        for (SkillTree tree : treeRegistry.getAllTrees()) {
            advancementGenerator.generateAdvancementsForTree(tree);
        }
        
        advancementGenerator.generateMilestoneAdvancements(milestoneManager.getAllMilestones());
        
        getLogger().info("Managers initialized");
        getLogger().info("Advancements loaded");
    }
    
    private void initializeFeatures() {
        toolReinforcement = new ToolReinforcement(this);
        xpTransfer = new XpTransfer(this);
        
        getLogger().info("Features initialized");
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(
            new PlayerConnectionListener(dataManager),
            this
        );

        getServer().getPluginManager().registerEvents(
            new SkillEventListener(config, dataManager, xpEngine, feedbackManager, milestoneManager),
            this
        );
        
        getServer().getPluginManager().registerEvents(
            new FeatureListener(config, dataManager, xpEngine, effectManager, feedbackManager, toolReinforcement, xpTransfer),
            this
        );
        
        getServer().getPluginManager().registerEvents(
            effectManager.getRecipeManager(),
            this
        );

        getLogger().info("Event listeners registered");
    }
    
    private void registerCommands() {
        commandManager = new PaperCommandManager(this);
        
        commandManager.getCommandCompletions().registerCompletion("skills", c -> 
            treeRegistry.getAllTrees().stream()
                .flatMap(tree -> tree.getAllSkillIds().stream())
                .toList()
        );
        
        commandManager.registerCommand(new AtomCommand(this));
        
        getLogger().info("Commands registered");
    }

    private void startTasks() {
        long saveInterval = 20L * config.autoSaveInterval();

        getServer().getGlobalRegionScheduler().runAtFixedRate(this, task -> {
            dataManager.saveAllPlayerData().thenRun(() ->
                getLogger().info("Auto-saved data for " + dataManager.getCacheSize() + " players")
            );
        }, saveInterval, saveInterval);
        
        getServer().getGlobalRegionScheduler().runAtFixedRate(this, task -> {
            getServer().getOnlinePlayers().forEach(player -> {
                dataManager.getCachedPlayerData(player.getUniqueId()).ifPresent(data -> {
                    effectManager.updatePlayerEffects(player, data);
                });
            });
        }, 100L, 100L);

        getLogger().info("Background tasks started");
    }
    
    public AtomConfig getAtomConfig() {
        return config;
    }

    public PlayerDataManager getDataManager() {
        return dataManager;
    }

    public SkillTreeRegistry getTreeRegistry() {
        return treeRegistry;
    }

    public XpEngine getXpEngine() {
        return xpEngine;
    }

    public StorageProvider getStorage() {
        return storage;
    }
    
    public EffectManager getEffectManager() {
        return effectManager;
    }
    
    public FeedbackManager getFeedbackManager() {
        return feedbackManager;
    }
    
    public MilestoneManager getMilestoneManager() {
        return milestoneManager;
    }
    
    public AdvancementGenerator getAdvancementGenerator() {
        return advancementGenerator;
    }
    
    public org.shotrush.atom.tree.MultiTreeAggregator getMultiTreeAggregator() {
        return multiTreeAggregator;
    }
    
    public ToolReinforcement getToolReinforcement() {
        return toolReinforcement;
    }
    
    public XpTransfer getXpTransfer() {
        return xpTransfer;
    }
}

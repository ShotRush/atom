package org.shotrush.atom;

import co.aikar.commands.PaperCommandManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.shotrush.atom.command.AtomCommand;
import org.shotrush.atom.gui.GuiListener;
import org.shotrush.atom.listener.ActionListener;
import org.shotrush.atom.listener.CombatPenaltyListener;
import org.shotrush.atom.listener.CraftingEfficiencyListener;
import org.shotrush.atom.listener.CraftingFatigueListener;
import org.shotrush.atom.listener.CraftingProjectListener;
import org.shotrush.atom.listener.CropGrowthListener;
import org.shotrush.atom.listener.DiscoveryListener;
import org.shotrush.atom.listener.DynamicActionListener;
import org.shotrush.atom.listener.EmergentBonusListener;
import org.shotrush.atom.listener.ItemQualityListener;
import org.shotrush.atom.listener.PlayerListener;
import org.shotrush.atom.listener.PlayerTradeListener;
import org.shotrush.atom.listener.RestrictionListener;
import org.shotrush.atom.config.SocialSystemsConfig;
import org.shotrush.atom.manager.*;
import org.shotrush.atom.util.PDCKeys;

public final class Atom extends JavaPlugin {
    private ConfigManager configManager;
    private SocialSystemsConfig socialSystemsConfig;
    private PlayerDataManager playerDataManager;
    private ActionManager actionManager;
    private EmergentBonusManager emergentBonusManager;
    private SkillTransferManager skillTransferManager;
    private ReputationManager reputationManager;
    private CollectiveActionManager collectiveActionManager;
    private PoliticalManager politicalManager;
    private EnvironmentalManager environmentalManager;
    private CognitiveCapacityManager cognitiveCapacityManager;
    private DynamicActionManager dynamicActionManager;
    private RecipeDiscoveryManager recipeDiscoveryManager;
    private CraftingProjectManager craftingProjectManager;
    private PaperCommandManager commandManager;
    private PDCKeys pdcKeys;
    private org.shotrush.atom.gui.SkillGUI skillGUI;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        
        pdcKeys = new PDCKeys(this);
        configManager = new ConfigManager(this);
        socialSystemsConfig = new SocialSystemsConfig(this);
        playerDataManager = new PlayerDataManager(this);
        actionManager = new ActionManager(this, configManager, playerDataManager);
        skillTransferManager = new SkillTransferManager(this, configManager, playerDataManager);
        emergentBonusManager = new EmergentBonusManager(this, configManager, playerDataManager);
        reputationManager = new ReputationManager(this, socialSystemsConfig);
        collectiveActionManager = new CollectiveActionManager(this, socialSystemsConfig);
        politicalManager = new PoliticalManager(this, socialSystemsConfig, reputationManager, collectiveActionManager);
        environmentalManager = new EnvironmentalManager(this);
        cognitiveCapacityManager = new CognitiveCapacityManager(this, configManager);
        dynamicActionManager = new DynamicActionManager(this, playerDataManager);
        recipeDiscoveryManager = new RecipeDiscoveryManager(this, playerDataManager, emergentBonusManager);
        craftingProjectManager = new CraftingProjectManager(this, configManager, emergentBonusManager);
        skillGUI = new org.shotrush.atom.gui.SkillGUI(this);
        
        configManager.loadConfig();
        socialSystemsConfig.loadConfig();
        skillTransferManager.loadConfig();
        
        registerListeners();
        registerCommands();
        startAutoSaveTask();
        
        getLogger().info("Atom has been enabled!");
    }

    @Override
    public void onDisable() {
        if (playerDataManager != null) {
            playerDataManager.saveAllPlayerData();
        }
        getLogger().info("Atom has been disabled!");
    }

    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new PlayerListener(this, playerDataManager), this);
        Bukkit.getPluginManager().registerEvents(new ActionListener(this, configManager, actionManager), this);
        Bukkit.getPluginManager().registerEvents(new RestrictionListener(this, configManager, actionManager), this);
        Bukkit.getPluginManager().registerEvents(new EmergentBonusListener(this, configManager, emergentBonusManager, actionManager), this);
        Bukkit.getPluginManager().registerEvents(new ItemQualityListener(this, configManager, emergentBonusManager), this);
        Bukkit.getPluginManager().registerEvents(new DiscoveryListener(this, configManager, emergentBonusManager), this);
        Bukkit.getPluginManager().registerEvents(new DynamicActionListener(this, dynamicActionManager), this);
        Bukkit.getPluginManager().registerEvents(new CombatPenaltyListener(this, configManager, emergentBonusManager), this);
        Bukkit.getPluginManager().registerEvents(new CraftingFatigueListener(this, configManager, emergentBonusManager), this);
        Bukkit.getPluginManager().registerEvents(new CraftingEfficiencyListener(this, configManager, emergentBonusManager), this);
        Bukkit.getPluginManager().registerEvents(new CraftingProjectListener(this, craftingProjectManager), this);
        Bukkit.getPluginManager().registerEvents(new CropGrowthListener(this, environmentalManager), this);
        Bukkit.getPluginManager().registerEvents(new PlayerTradeListener(this, reputationManager), this);
        Bukkit.getPluginManager().registerEvents(new GuiListener(), this);
    }

    private void registerCommands() {
        commandManager = new PaperCommandManager(this);
        
        commandManager.getCommandCompletions().registerCompletion("actions", c -> 
            configManager.getActions().keySet());
        
        commandManager.registerCommand(new AtomCommand(this, configManager, playerDataManager, actionManager));
    }

    private void startAutoSaveTask() {
        long interval = getConfig().getLong("storage.auto-save-interval", 6000);
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            if (playerDataManager != null) {
                playerDataManager.saveAllPlayerData();
            }
        }, interval, interval);
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public ActionManager getActionManager() {
        return actionManager;
    }

    public EmergentBonusManager getEmergentBonusManager() {
        return emergentBonusManager;
    }

    public PDCKeys getPDCKeys() {
        return pdcKeys;
    }

    public SkillTransferManager getSkillTransferManager() {
        return skillTransferManager;
    }

    public ReputationManager getReputationManager() {
        return reputationManager;
    }

    public CollectiveActionManager getCollectiveActionManager() {
        return collectiveActionManager;
    }

    public PoliticalManager getPoliticalManager() {
        return politicalManager;
    }

    public SocialSystemsConfig getSocialSystemsConfig() {
        return socialSystemsConfig;
    }

    public EnvironmentalManager getEnvironmentalManager() {
        return environmentalManager;
    }

    public CognitiveCapacityManager getCognitiveCapacityManager() {
        return cognitiveCapacityManager;
    }

    public DynamicActionManager getDynamicActionManager() {
        return dynamicActionManager;
    }

    public RecipeDiscoveryManager getRecipeDiscoveryManager() {
        return recipeDiscoveryManager;
    }

    public CraftingProjectManager getCraftingProjectManager() {
        return craftingProjectManager;
    }

    public org.shotrush.atom.gui.SkillGUI getSkillGUI() {
        return skillGUI;
    }
}

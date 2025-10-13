package org.shotrush.atom;

import co.aikar.commands.PaperCommandManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.shotrush.atom.api.AtomAPI;
import org.shotrush.atom.boost.LearningBoostCalculator;
import org.shotrush.atom.boost.SkillBookManager;
import org.shotrush.atom.commands.AtomCommand;
import org.shotrush.atom.config.SkillConfig;
import org.shotrush.atom.data.DataManager;
import org.shotrush.atom.gui.GUIListener;
import org.shotrush.atom.gui.SkillGUI;
import org.shotrush.atom.handler.HandlerRegistry;
import org.shotrush.atom.listener.MiscListener;
import org.shotrush.atom.listener.PlayerListener;
import org.shotrush.atom.manager.SkillManager;
import org.shotrush.atom.synergy.SynergyCalculator;
import org.shotrush.atom.synergy.SynergyConfig;

public final class Atom extends JavaPlugin {
    private SkillManager skillManager;
    private SkillConfig skillConfig;
    private DataManager dataManager;
    private SkillGUI skillGUI;
    private PaperCommandManager commandManager;
    private HandlerRegistry handlerRegistry;
    private LearningBoostCalculator boostCalculator;
    private SkillBookManager bookManager;
    private SynergyConfig synergyConfig;
    private SynergyCalculator synergyCalculator;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        
        AtomAPI.initialize(this);
        
        skillManager = new SkillManager();
        skillConfig = new SkillConfig();
        skillConfig.loadFromConfig(getConfig());
        skillManager.setSkillConfig(skillConfig);
        boostCalculator = new LearningBoostCalculator(this);
        skillManager.setBoostCalculator(boostCalculator);
        bookManager = new SkillBookManager(this);
        synergyConfig = new SynergyConfig();
        synergyConfig.loadFromConfig(getConfig());
        synergyCalculator = new SynergyCalculator(this);
        skillManager.setSynergyCalculator(synergyCalculator);
        dataManager = new DataManager(this);
        skillGUI = new SkillGUI(this);
        handlerRegistry = new HandlerRegistry(this);
        
        handlerRegistry.registerAll();
        registerCommands();
        registerListeners();
        
        dataManager.loadAllData();
        
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            dataManager.saveAllData();
        }, 6000L, 6000L);
        
        getLogger().info("Atom has been enabled!");
        getLogger().info("API initialized - other plugins can now hook into Atom!");
    }

    @Override
    public void onDisable() {
        dataManager.saveAllData();
        getLogger().info("Atom has been disabled!");
    }
    
    private void registerCommands() {
        commandManager = new PaperCommandManager(this);
        commandManager.registerCommand(new AtomCommand(this));
    }
    
    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);
        Bukkit.getPluginManager().registerEvents(new GUIListener(this), this);
        Bukkit.getPluginManager().registerEvents(new MiscListener(this), this);
    }
    
    public SkillManager getSkillManager() {
        return skillManager;
    }
    
    public SkillConfig getSkillConfig() {
        return skillConfig;
    }
    
    public DataManager getDataManager() {
        return dataManager;
    }
    
    public SkillGUI getSkillGUI() {
        return skillGUI;
    }
    
    public HandlerRegistry getHandlerRegistry() {
        return handlerRegistry;
    }

    public LearningBoostCalculator getBoostCalculator() {
        return boostCalculator;
    }

    public SkillBookManager getBookManager() {
        return bookManager;
    }

    public SynergyConfig getSynergyConfig() {
        return synergyConfig;
    }

    public SynergyCalculator getSynergyCalculator() {
        return synergyCalculator;
    }
}

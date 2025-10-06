package org.shotrush.atom;

import co.aikar.commands.PaperCommandManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.shotrush.atom.api.AtomAPI;
import org.shotrush.atom.commands.AtomCommand;
import org.shotrush.atom.config.SkillConfig;
import org.shotrush.atom.data.DataManager;
import org.shotrush.atom.gui.GUIListener;
import org.shotrush.atom.gui.SkillGUI;
import org.shotrush.atom.handler.HandlerRegistry;
import org.shotrush.atom.listener.PlayerListener;
import org.shotrush.atom.manager.SkillManager;

public final class Atom extends JavaPlugin {
    private SkillManager skillManager;
    private SkillConfig skillConfig;
    private DataManager dataManager;
    private SkillGUI skillGUI;
    private PaperCommandManager commandManager;
    private HandlerRegistry handlerRegistry;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        
        AtomAPI.initialize(this);
        
        skillManager = new SkillManager();
        skillConfig = new SkillConfig();
        skillConfig.loadFromConfig(getConfig());
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
}

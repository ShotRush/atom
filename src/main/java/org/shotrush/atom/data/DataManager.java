package org.shotrush.atom.data;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.shotrush.atom.Atom;
import org.shotrush.atom.skill.PlayerSkillData;
import org.shotrush.atom.skill.SkillData;
import org.shotrush.atom.skill.SkillType;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

public class DataManager {
    private final Atom plugin;
    private final File dataFile;
    private FileConfiguration dataConfig;
    
    public DataManager(Atom plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "playerdata.yml");
        loadData();
    }
    
    private void loadData() {
        if (!dataFile.exists()) {
            try {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }
    
    public void saveAllData() {
        for (Map.Entry<UUID, PlayerSkillData> entry : plugin.getSkillManager().getAllPlayerData().entrySet()) {
            savePlayerData(entry.getKey(), entry.getValue());
        }
        
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void loadAllData() {
        for (String key : dataConfig.getKeys(false)) {
            try {
                UUID playerId = UUID.fromString(key);
                loadPlayerData(playerId);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid UUID in data file: " + key);
            }
        }
    }
    
    private void savePlayerData(UUID playerId, PlayerSkillData playerData) {
        String basePath = playerId.toString();
        
        for (SkillType type : SkillType.values()) {
            SkillData skillData = playerData.getSkillData(type);
            String skillPath = basePath + "." + type.name();
            
            for (Map.Entry<String, Integer> entry : skillData.getAllExperience().entrySet()) {
                dataConfig.set(skillPath + "." + entry.getKey(), entry.getValue());
            }
        }
    }
    
    private void loadPlayerData(UUID playerId) {
        String basePath = playerId.toString();
        PlayerSkillData playerData = plugin.getSkillManager().getPlayerData(playerId);
        
        ConfigurationSection playerSection = dataConfig.getConfigurationSection(basePath);
        if (playerSection == null) {
            return;
        }
        
        for (SkillType type : SkillType.values()) {
            ConfigurationSection skillSection = playerSection.getConfigurationSection(type.name());
            if (skillSection == null) {
                continue;
            }
            
            SkillData skillData = playerData.getSkillData(type);
            for (String itemKey : skillSection.getKeys(false)) {
                int exp = skillSection.getInt(itemKey);
                skillData.setExperience(itemKey, exp);
            }
        }
    }
    
    public void savePlayerData(UUID playerId) {
        PlayerSkillData playerData = plugin.getSkillManager().getPlayerData(playerId);
        savePlayerData(playerId, playerData);
        
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

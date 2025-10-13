package org.shotrush.atom.manager;

import org.bukkit.entity.Player;
import org.shotrush.atom.boost.LearningBoostCalculator;
import org.shotrush.atom.config.SkillConfig;
import org.shotrush.atom.skill.PlayerSkillData;
import org.shotrush.atom.skill.SkillData;
import org.shotrush.atom.skill.SkillType;
import org.shotrush.atom.synergy.SynergyCalculator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SkillManager {
    private final Map<UUID, PlayerSkillData> playerData = new HashMap<>();
    private SkillConfig skillConfig;
    private LearningBoostCalculator boostCalculator;
    private SynergyCalculator synergyCalculator;
    
    public void setSkillConfig(SkillConfig config) {
        this.skillConfig = config;
    }

    public void setBoostCalculator(LearningBoostCalculator calculator) {
        this.boostCalculator = calculator;
    }

    public void setSynergyCalculator(SynergyCalculator calculator) {
        this.synergyCalculator = calculator;
    }

    public PlayerSkillData getPlayerData(UUID playerId) {
        return playerData.computeIfAbsent(playerId, PlayerSkillData::new);
    }
    
    public SkillData getSkillData(UUID playerId, SkillType type) {
        return getPlayerData(playerId).getSkillData(type);
    }
    
    public void addExperience(UUID playerId, SkillType type, String itemKey, int amount, int maxExperience) {
        SkillData data = getSkillData(playerId, type);
        applyDecayIfEnabled(data, itemKey);
        data.addExperience(itemKey, amount, maxExperience);
        data.setLastActionTime(itemKey, System.currentTimeMillis());
    }

    public void addExperienceWithBoost(Player player, SkillType type, String itemKey, int amount, int maxExperience) {
        double boost = 1.0;
        if (boostCalculator != null) {
            boost = boostCalculator.calculateTotalBoost(player, type, itemKey);
        }
        int boostedAmount = (int) Math.ceil(amount * boost);
        addExperience(player.getUniqueId(), type, itemKey, boostedAmount, maxExperience);

        if (synergyCalculator != null) {
            List<SynergyCalculator.SynergyGrant> synergies = synergyCalculator.calculateSynergies(
                player.getUniqueId(), type, itemKey, boostedAmount
            );
            for (SynergyCalculator.SynergyGrant grant : synergies) {
                SkillConfig.SkillTypeConfig targetConfig = skillConfig.getConfig(grant.skill);
                addExperience(player.getUniqueId(), grant.skill, grant.itemKey, grant.xp, targetConfig.maxExperience);
            }
        }
    }
    
    public int getExperience(UUID playerId, SkillType type, String itemKey) {
        SkillData data = getSkillData(playerId, type);
        applyDecayIfEnabled(data, itemKey);
        return data.getExperience(itemKey);
    }
    
    public double getSuccessRate(UUID playerId, SkillType type, String itemKey, double baseRate, double maxRate, int expPerLevel) {
        SkillData data = getSkillData(playerId, type);
        applyDecayIfEnabled(data, itemKey);
        return data.getSuccessRate(itemKey, baseRate, maxRate, expPerLevel);
    }

    private void applyDecayIfEnabled(SkillData data, String itemKey) {
        if (skillConfig == null || skillConfig.getDecay() == null || !skillConfig.getDecay().enabled) {
            return;
        }
        SkillConfig.DecayConfig decay = skillConfig.getDecay();
        data.applyDecay(itemKey, decay.initialHalfLifeHours, decay.stabilityIncreasePerRepetition, decay.maxStabilityMultiplier, decay.minRetentionPercentage);
    }
    
    public void removePlayer(UUID playerId) {
        playerData.remove(playerId);
    }
    
    public Map<UUID, PlayerSkillData> getAllPlayerData() {
        return playerData;
    }

    public double calculateBoost(Player player, SkillType type, String itemKey) {
        if (boostCalculator == null) {
            return 1.0;
        }
        return boostCalculator.calculateTotalBoost(player, type, itemKey);
    }
}

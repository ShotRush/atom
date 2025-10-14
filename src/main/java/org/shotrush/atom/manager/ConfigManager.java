package org.shotrush.atom.manager;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.shotrush.atom.Atom;
import org.shotrush.atom.model.*;

import java.util.*;

public class ConfigManager {
    private final Atom plugin;
    private final Map<String, TrackedAction> actions;
    private final Map<String, Restriction> restrictions;
    private final Map<String, Milestone> milestones;
    private final Map<String, EfficiencyBonus> efficiencyBonuses;

    public ConfigManager(Atom plugin) {
        this.plugin = plugin;
        this.actions = new HashMap<>();
        this.restrictions = new HashMap<>();
        this.milestones = new HashMap<>();
        this.efficiencyBonuses = new HashMap<>();
    }

    public void loadConfig() {
        plugin.reloadConfig();
        actions.clear();
        restrictions.clear();
        milestones.clear();
        efficiencyBonuses.clear();

        loadActions();
        loadRestrictions();
        loadMilestones();
        loadEfficiencyBonuses();
    }

    private void loadActions() {
        ConfigurationSection actionsSection = plugin.getConfig().getConfigurationSection("actions");
        if (actionsSection == null) return;

        for (String actionId : actionsSection.getKeys(false)) {
            ConfigurationSection actionSection = actionsSection.getConfigurationSection(actionId);
            if (actionSection == null) continue;

            String displayName = actionSection.getString("display-name", actionId);
            String description = actionSection.getString("description", "");
            double experience = actionSection.getDouble("experience", 1.0);

            List<TrackedAction.ActionTrigger> triggers = new ArrayList<>();
            List<Map<?, ?>> triggersList = actionSection.getMapList("triggers");

            for (Map<?, ?> triggerMap : triggersList) {
                String typeStr = (String) triggerMap.get("type");
                TriggerType type = TriggerType.valueOf(typeStr);

                Set<Material> materials = new HashSet<>();
                Set<EntityType> entities = new HashSet<>();
                boolean matchAny = false;

                Object materialsObj = triggerMap.get("materials");
                if (materialsObj instanceof String && materialsObj.equals("any")) {
                    matchAny = true;
                } else if (materialsObj instanceof List) {
                    for (Object matObj : (List<?>) materialsObj) {
                        try {
                            materials.add(Material.valueOf(matObj.toString()));
                        } catch (IllegalArgumentException ignored) {}
                    }
                }

                Object entitiesObj = triggerMap.get("entities");
                if (entitiesObj instanceof List) {
                    for (Object entObj : (List<?>) entitiesObj) {
                        try {
                            entities.add(EntityType.valueOf(entObj.toString()));
                        } catch (IllegalArgumentException ignored) {}
                    }
                }

                triggers.add(new TrackedAction.ActionTrigger(type, materials, entities, matchAny));
            }

            actions.put(actionId, new TrackedAction(actionId, displayName, description, triggers, experience));
        }
    }

    private void loadRestrictions() {
        ConfigurationSection restrictionsSection = plugin.getConfig().getConfigurationSection("restrictions");
        if (restrictionsSection == null) return;

        for (String restrictionId : restrictionsSection.getKeys(false)) {
            ConfigurationSection restrictionSection = restrictionsSection.getConfigurationSection(restrictionId);
            if (restrictionSection == null) continue;

            String displayName = restrictionSection.getString("display-name", restrictionId);
            String description = restrictionSection.getString("description", "");
            String denyMessage = restrictionSection.getString("deny-message", "You cannot perform this action!");
            boolean allowFirstTime = restrictionSection.getBoolean("allow-first-time", false);
            int firstTimeMaxLevel = restrictionSection.getInt("first-time-max-level", 0);

            List<Restriction.RestrictionBlock> blocks = new ArrayList<>();
            List<Map<?, ?>> blocksList = restrictionSection.getMapList("blocks");

            for (Map<?, ?> blockMap : blocksList) {
                String typeStr = (String) blockMap.get("type");
                TriggerType type = TriggerType.valueOf(typeStr);

                Set<Material> materials = new HashSet<>();
                Set<Material> items = new HashSet<>();

                Object materialsObj = blockMap.get("materials");
                if (materialsObj instanceof List) {
                    for (Object matObj : (List<?>) materialsObj) {
                        try {
                            materials.add(Material.valueOf(matObj.toString()));
                        } catch (IllegalArgumentException ignored) {}
                    }
                }

                Object itemsObj = blockMap.get("items");
                if (itemsObj instanceof List) {
                    for (Object itemObj : (List<?>) itemsObj) {
                        try {
                            items.add(Material.valueOf(itemObj.toString()));
                        } catch (IllegalArgumentException ignored) {}
                    }
                }

                blocks.add(new Restriction.RestrictionBlock(type, materials, items));
            }

            Map<String, Double> actionRequirements = new HashMap<>();
            ConfigurationSection requirementsSection = restrictionSection.getConfigurationSection("requirements.actions");
            if (requirementsSection != null) {
                for (String actionId : requirementsSection.getKeys(false)) {
                    actionRequirements.put(actionId, requirementsSection.getDouble(actionId));
                }
            }

            restrictions.put(restrictionId, new Restriction(restrictionId, displayName, description,
                    blocks, actionRequirements, denyMessage, allowFirstTime, firstTimeMaxLevel));
        }
    }

    private void loadMilestones() {
        ConfigurationSection milestonesSection = plugin.getConfig().getConfigurationSection("milestones");
        if (milestonesSection == null) return;

        for (String milestoneId : milestonesSection.getKeys(false)) {
            ConfigurationSection milestoneSection = milestonesSection.getConfigurationSection(milestoneId);
            if (milestoneSection == null) continue;

            String displayName = milestoneSection.getString("display-name", milestoneId);
            String description = milestoneSection.getString("description", "");

            Map<String, Double> actionRequirements = new HashMap<>();
            ConfigurationSection requirementsSection = milestoneSection.getConfigurationSection("requirements.actions");
            if (requirementsSection != null) {
                for (String actionId : requirementsSection.getKeys(false)) {
                    actionRequirements.put(actionId, requirementsSection.getDouble(actionId));
                }
            }

            List<Milestone.Reward> rewards = new ArrayList<>();
            List<Map<?, ?>> rewardsList = milestoneSection.getMapList("rewards");
            for (Map<?, ?> rewardMap : rewardsList) {
                String typeStr = (String) rewardMap.get("type");
                String value = (String) rewardMap.get("value");
                try {
                    Milestone.RewardType type = Milestone.RewardType.valueOf(typeStr);
                    rewards.add(new Milestone.Reward(type, value));
                } catch (IllegalArgumentException ignored) {}
            }

            milestones.put(milestoneId, new Milestone(milestoneId, displayName, description,
                    actionRequirements, rewards));
        }
    }

    public Map<String, TrackedAction> getActions() {
        return new HashMap<>(actions);
    }

    public Map<String, Restriction> getRestrictions() {
        return new HashMap<>(restrictions);
    }

    public Map<String, Milestone> getMilestones() {
        return new HashMap<>(milestones);
    }

    public TrackedAction getAction(String id) {
        return actions.get(id);
    }

    public Restriction getRestriction(String id) {
        return restrictions.get(id);
    }

    public Milestone getMilestone(String id) {
        return milestones.get(id);
    }

    private void loadEfficiencyBonuses() {
        ConfigurationSection bonusesSection = plugin.getConfig().getConfigurationSection("efficiency-bonuses");
        if (bonusesSection == null) return;

        for (String actionId : bonusesSection.getKeys(false)) {
            ConfigurationSection bonusSection = bonusesSection.getConfigurationSection(actionId);
            if (bonusSection == null) continue;

            EfficiencyBonus bonus = new EfficiencyBonus(
                actionId,
                bonusSection.getBoolean("break-speed-multiplier", false),
                bonusSection.getBoolean("drop-multiplier", false),
                bonusSection.getBoolean("durability-cost-reduction", false),
                bonusSection.getBoolean("fortune-bonus", false),
                bonusSection.getBoolean("replant-chance", false),
                bonusSection.getBoolean("breeding-cooldown-reduction", false),
                bonusSection.getBoolean("offspring-bonus", false),
                bonusSection.getBoolean("damage-multiplier", false),
                bonusSection.getBoolean("experience-multiplier", false),
                bonusSection.getBoolean("placement-speed", false),
                bonusSection.getBoolean("craft-speed", false),
                bonusSection.getBoolean("durability-bonus", false),
                bonusSection.getBoolean("fuel-efficiency", false),
                bonusSection.getBoolean("smelt-speed", false),
                bonusSection.getBoolean("discount", false),
                bonusSection.getBoolean("reputation-bonus", false),
                bonusSection.getBoolean("level-cost-reduction", false),
                bonusSection.getBoolean("better-enchants", false),
                bonusSection.getBoolean("treasure-chance", false)
            );

            efficiencyBonuses.put(actionId, bonus);
        }
    }

    public Map<String, EfficiencyBonus> getEfficiencyBonuses() {
        return new HashMap<>(efficiencyBonuses);
    }

    public EfficiencyBonus getEfficiencyBonus(String actionId) {
        return efficiencyBonuses.get(actionId);
    }

    public double getMaxEfficiency() {
        return plugin.getConfig().getDouble("specialization.max-efficiency", 2.0);
    }

    public double getBaseEfficiency() {
        return plugin.getConfig().getDouble("specialization.base-efficiency", 0.5);
    }

    public double getExperiencePerEfficiencyPoint() {
        return plugin.getConfig().getDouble("specialization.experience-per-efficiency-point", 100);
    }

    public boolean isPenaltyForUnspecialized() {
        return plugin.getConfig().getBoolean("specialization.penalty-for-unspecialized", true);
    }
}

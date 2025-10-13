package org.shotrush.atom.synergy;

import org.shotrush.atom.Atom;
import org.shotrush.atom.skill.SkillData;
import org.shotrush.atom.skill.SkillType;

import java.util.*;
import java.util.UUID;

public class SynergyCalculator {
    private final Atom plugin;

    public SynergyCalculator(Atom plugin) {
        this.plugin = plugin;
    }

    public List<SynergyGrant> calculateSynergies(UUID playerId, SkillType sourceSkill, String sourceItem, int baseXP) {
        List<SynergyGrant> grants = new ArrayList<>();
        SynergyConfig config = plugin.getSynergyConfig();

        if (config == null || !config.isEnabled()) {
            return grants;
        }

        grants.addAll(calculateSkillSynergies(playerId, sourceSkill, sourceItem, baseXP, config));
        grants.addAll(calculateItemSynergies(playerId, sourceSkill, sourceItem, baseXP, config));

        return grants;
    }

    private List<SynergyGrant> calculateSkillSynergies(UUID playerId, SkillType sourceSkill, String sourceItem, int baseXP, SynergyConfig config) {
        List<SynergyGrant> grants = new ArrayList<>();
        List<SynergyConfig.SkillSynergy> synergies = config.getSkillSynergies().get(sourceSkill);

        if (synergies == null) {
            return grants;
        }

        for (SynergyConfig.SkillSynergy synergy : synergies) {
            if (synergy.itemPattern.matcher(sourceItem).matches()) {
                int synergyXP = (int) Math.ceil(baseXP * synergy.transferRate);
                grants.add(new SynergyGrant(synergy.targetSkill, sourceItem, synergyXP));
            }
        }

        return grants;
    }

    private List<SynergyGrant> calculateItemSynergies(UUID playerId, SkillType sourceSkill, String sourceItem, int baseXP, SynergyConfig config) {
        List<SynergyGrant> grants = new ArrayList<>();
        SynergyConfig.ItemSynergyConfig itemConfig = config.getItemSynergies();

        if (itemConfig == null || !itemConfig.enabled) {
            return grants;
        }

        SkillData sourceSkillData = plugin.getSkillManager().getSkillData(playerId, sourceSkill);
        Map<String, Integer> allItems = sourceSkillData.getAllExperience();

        for (SynergyConfig.ItemSynergyPattern pattern : itemConfig.patterns) {
            if (pattern.sourcePattern.matcher(sourceItem).matches()) {
                for (String targetItem : allItems.keySet()) {
                    if (!targetItem.equals(sourceItem) && pattern.targetPattern.matcher(targetItem).matches()) {
                        int synergyXP = (int) Math.ceil(baseXP * pattern.transferRate);
                        grants.add(new SynergyGrant(sourceSkill, targetItem, synergyXP));
                    }
                }
            }
        }

        return grants;
    }

    public static class SynergyGrant {
        public final SkillType skill;
        public final String itemKey;
        public final int xp;

        public SynergyGrant(SkillType skill, String itemKey, int xp) {
            this.skill = skill;
            this.itemKey = itemKey;
            this.xp = xp;
        }
    }
}

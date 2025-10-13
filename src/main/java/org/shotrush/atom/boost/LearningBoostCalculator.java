package org.shotrush.atom.boost;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.shotrush.atom.Atom;
import org.shotrush.atom.config.SkillConfig;
import org.shotrush.atom.skill.SkillType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LearningBoostCalculator {
    private final Atom plugin;

    public LearningBoostCalculator(Atom plugin) {
        this.plugin = plugin;
    }

    public double calculateTotalBoost(Player player, SkillType skill, String itemKey) {
        double multiplier = 1.0;
        SkillConfig.LearningBoostConfig config = plugin.getSkillConfig().getLearningBoost();

        if (config == null) {
            return multiplier;
        }

        if (config.mentorship != null && config.mentorship.enabled) {
            multiplier += calculateMentorshipBoost(player, skill, itemKey, config.mentorship);
        }

        if (config.books != null && config.books.enabled) {
            multiplier += calculateBookBoost(player, skill, itemKey, config.books);
        }

        if (config.infrastructure != null && config.infrastructure.enabled) {
            multiplier += calculateInfrastructureBoost(player, skill, config.infrastructure);
        }

        return multiplier;
    }

    private double calculateMentorshipBoost(Player player, SkillType skill, String itemKey, SkillConfig.LearningBoostConfig.MentorshipConfig config) {
        int playerExp = plugin.getSkillManager().getExperience(player.getUniqueId(), skill, itemKey);
        double bestBoost = 0.0;

        for (Player nearby : player.getWorld().getPlayers()) {
            if (nearby.equals(player)) continue;
            if (nearby.getLocation().distance(player.getLocation()) > config.radius) continue;

            int mentorExp = plugin.getSkillManager().getExperience(nearby.getUniqueId(), skill, itemKey);
            int expDifference = mentorExp - playerExp;

            if (expDifference > 0) {
                double boost = Math.min(
                    (expDifference / (double) config.skillDifferenceThreshold) * config.maxBoost,
                    config.maxBoost
                );
                bestBoost = Math.max(bestBoost, boost);
            }
        }

        return bestBoost;
    }

    private double calculateBookBoost(Player player, SkillType skill, String itemKey, SkillConfig.LearningBoostConfig.BooksConfig config) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType() != Material.WRITTEN_BOOK) continue;

            ItemMeta meta = item.getItemMeta();
            if (meta == null) continue;

            String bookSkill = meta.getPersistentDataContainer().get(
                SkillBookManager.SKILL_TYPE_KEY,
                PersistentDataType.STRING
            );
            String bookItem = meta.getPersistentDataContainer().get(
                SkillBookManager.ITEM_KEY_KEY,
                PersistentDataType.STRING
            );

            if (bookSkill != null && bookSkill.equals(skill.name()) && 
                bookItem != null && bookItem.equals(itemKey)) {
                return config.maxBoost;
            }
        }

        return 0.0;
    }

    private double calculateInfrastructureBoost(Player player, SkillType skill, SkillConfig.LearningBoostConfig.InfrastructureConfig config) {
        List<Material> requiredBlocks = config.blocks.get(skill);
        if (requiredBlocks == null || requiredBlocks.isEmpty()) {
            return 0.0;
        }

        Location loc = player.getLocation();
        int radius = config.searchRadius;
        Set<Material> foundBlocks = new HashSet<>();

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Location checkLoc = loc.clone().add(x, y, z);
                    Material blockType = checkLoc.getBlock().getType();
                    if (requiredBlocks.contains(blockType)) {
                        foundBlocks.add(blockType);
                    }
                }
            }
        }

        if (foundBlocks.isEmpty()) {
            return 0.0;
        }

        double ratio = foundBlocks.size() / (double) requiredBlocks.size();
        return ratio * config.maxBoost;
    }
}

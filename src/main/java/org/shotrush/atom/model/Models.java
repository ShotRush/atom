package org.shotrush.atom.model;

import java.util.List;

public final class Models {
    
    private Models() {}

    public record EffectiveXp(
        long intrinsicXp,
        long honoraryXp,
        long totalXp,
        double progressPercent
    ) {
        
        public EffectiveXp {
            if (intrinsicXp < 0) throw new IllegalArgumentException("Intrinsic XP cannot be negative");
            if (honoraryXp < 0) throw new IllegalArgumentException("Honorary XP cannot be negative");
            if (totalXp < 0) throw new IllegalArgumentException("Total XP cannot be negative");
            if (progressPercent < 0.0 || progressPercent > 1.0) {
                throw new IllegalArgumentException("Progress percent must be between 0.0 and 1.0");
            }
        }
        
        public static EffectiveXp of(long intrinsicXp, long honoraryXp, int maxXp) {
            long totalXp = intrinsicXp + honoraryXp;
            double progressPercent = maxXp > 0 ? Math.min(1.0, (double) totalXp / maxXp) : 0.0;
            return new EffectiveXp(intrinsicXp, honoraryXp, totalXp, progressPercent);
        }
        
        public static EffectiveXp zero() {
            return new EffectiveXp(0, 0, 0, 0.0);
        }
        
        public boolean isMaxed(int maxXp) {
            return intrinsicXp >= maxXp;
        }
        
        public long remaining(int maxXp) {
            return Math.max(0, maxXp - totalXp);
        }
    }

    public record Milestone(
        String id,
        String displayName,
        String description,
        String skillId,
        double requiredLevel,
        MilestoneReward reward
    ) {
        
        public enum MilestoneReward {
            NONE,
            BONUS_DROP_RATE,
            BONUS_XP_GAIN,
            UNLOCK_FEATURE,
            REDUCED_PENALTY
        }
    }

    public record TreeDefinition(
        String name,
        double weight,
        NodeDefinition root
    ) {
        
        public record NodeDefinition(
            String id,
            String displayName,
            int maxXp,
            String type,
            List<NodeDefinition> children
        ) {}
    }
}

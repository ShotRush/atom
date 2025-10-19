package org.shotrush.atom.milestone;

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

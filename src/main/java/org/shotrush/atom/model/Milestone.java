package org.shotrush.atom.model;

import java.util.List;
import java.util.Map;

public class Milestone {
    private final String id;
    private final String displayName;
    private final String description;
    private final Map<String, Double> actionRequirements;
    private final List<Reward> rewards;

    public Milestone(String id, String displayName, String description,
                    Map<String, Double> actionRequirements, List<Reward> rewards) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.actionRequirements = actionRequirements;
        this.rewards = rewards;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public Map<String, Double> getActionRequirements() {
        return actionRequirements;
    }

    public List<Reward> getRewards() {
        return rewards;
    }

    public static class Reward {
        private final RewardType type;
        private final String value;

        public Reward(RewardType type, String value) {
            this.type = type;
            this.value = value;
        }

        public RewardType getType() {
            return type;
        }

        public String getValue() {
            return value;
        }
    }

    public enum RewardType {
        MESSAGE,
        TITLE,
        EFFECT,
        COMMAND
    }
}

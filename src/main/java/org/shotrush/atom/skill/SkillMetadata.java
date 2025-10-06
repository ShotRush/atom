package org.shotrush.atom.skill;

import org.bukkit.Material;
import org.shotrush.atom.api.SkillInfo;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class SkillMetadata {
    private static final Map<SkillType, Material> SKILL_ICONS = new HashMap<>();
    private static final Map<SkillType, String> SKILL_DESCRIPTIONS = new HashMap<>();
    
    static {
        loadFromAnnotations();
    }
    
    private static void loadFromAnnotations() {
        for (Field field : SkillType.class.getDeclaredFields()) {
            if (field.isEnumConstant() && field.isAnnotationPresent(SkillInfo.class)) {
                try {
                    SkillType type = SkillType.valueOf(field.getName());
                    SkillInfo info = field.getAnnotation(SkillInfo.class);
                    SKILL_ICONS.put(type, info.icon());
                    SKILL_DESCRIPTIONS.put(type, info.description());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    public static Material getIcon(SkillType type) {
        return SKILL_ICONS.getOrDefault(type, Material.PAPER);
    }
    
    public static String getDescription(SkillType type) {
        return SKILL_DESCRIPTIONS.getOrDefault(type, "Unknown skill");
    }
    
    public static void setIcon(SkillType type, Material icon) {
        SKILL_ICONS.put(type, icon);
    }
    
    public static void setDescription(SkillType type, String description) {
        SKILL_DESCRIPTIONS.put(type, description);
    }
    
    public static void reload() {
        SKILL_ICONS.clear();
        SKILL_DESCRIPTIONS.clear();
        loadFromAnnotations();
    }
}

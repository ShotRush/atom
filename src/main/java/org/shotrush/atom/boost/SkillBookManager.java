package org.shotrush.atom.boost;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.persistence.PersistentDataType;
import org.shotrush.atom.Atom;
import org.shotrush.atom.skill.SkillType;

import java.util.ArrayList;
import java.util.List;

public class SkillBookManager {
    public static NamespacedKey SKILL_TYPE_KEY;
    public static NamespacedKey ITEM_KEY_KEY;
    public static NamespacedKey AUTHOR_KEY;

    private final Atom plugin;

    public SkillBookManager(Atom plugin) {
        this.plugin = plugin;
        SKILL_TYPE_KEY = new NamespacedKey(plugin, "skill_type");
        ITEM_KEY_KEY = new NamespacedKey(plugin, "item_key");
        AUTHOR_KEY = new NamespacedKey(plugin, "author");
    }

    public ItemStack createSkillBook(SkillType skill, String itemKey, String authorName, int authorExperience) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        if (meta == null) return book;

        String skillName = formatSkillName(skill);
        String itemName = formatItemName(itemKey);

        meta.setTitle("§6" + skillName + ": " + itemName);
        meta.setAuthor(authorName);

        List<String> pages = new ArrayList<>();
        pages.add(generateCoverPage(skillName, itemName, authorName));
        pages.add(generateKnowledgePage(skill, itemKey, authorExperience));
        pages.add(generateTipsPage(skill, itemKey));

        meta.setPages(pages);

        meta.getPersistentDataContainer().set(SKILL_TYPE_KEY, PersistentDataType.STRING, skill.name());
        meta.getPersistentDataContainer().set(ITEM_KEY_KEY, PersistentDataType.STRING, itemKey);
        meta.getPersistentDataContainer().set(AUTHOR_KEY, PersistentDataType.STRING, authorName);

        book.setItemMeta(meta);
        return book;
    }

    public boolean isSkillBook(ItemStack item) {
        if (item == null || item.getType() != Material.WRITTEN_BOOK) return false;
        BookMeta meta = (BookMeta) item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(SKILL_TYPE_KEY, PersistentDataType.STRING);
    }

    public SkillType getSkillType(ItemStack item) {
        if (!isSkillBook(item)) return null;
        BookMeta meta = (BookMeta) item.getItemMeta();
        String skillName = meta.getPersistentDataContainer().get(SKILL_TYPE_KEY, PersistentDataType.STRING);
        try {
            return SkillType.valueOf(skillName);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public String getItemKey(ItemStack item) {
        if (!isSkillBook(item)) return null;
        BookMeta meta = (BookMeta) item.getItemMeta();
        return meta.getPersistentDataContainer().get(ITEM_KEY_KEY, PersistentDataType.STRING);
    }

    private String formatSkillName(SkillType skill) {
        String name = skill.name().toLowerCase();
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    private String formatItemName(String itemKey) {
        return itemKey.toLowerCase().replace("_", " ");
    }

    private String generateCoverPage(String skillName, String itemName, String authorName) {
        return "§l§n" + skillName + "§r\n\n" +
               "§oA Guide to " + itemName + "§r\n\n" +
               "Written by:\n§9" + authorName + "§r\n\n" +
               "This book contains knowledge that will help you learn this skill faster.";
    }

    private String generateKnowledgePage(SkillType skill, String itemKey, int experience) {
        String level = experience >= 500 ? "Master" : experience >= 300 ? "Expert" : experience >= 100 ? "Skilled" : "Novice";
        return "§l§nKnowledge Level§r\n\n" +
               "The author has " + level + " experience with " + formatItemName(itemKey) + ".\n\n" +
               "Experience: " + experience + "\n\n" +
               "Keep this book in your inventory while practicing to gain bonus experience.";
    }

    private String generateTipsPage(SkillType skill, String itemKey) {
        return "§l§nTips§r\n\n" +
               "• Practice regularly\n" +
               "• Learn from masters\n" +
               "• Set up proper workspace\n" +
               "• Repetition builds mastery\n\n" +
               "Good luck on your journey!";
    }
}

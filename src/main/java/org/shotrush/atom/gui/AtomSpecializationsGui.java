package org.shotrush.atom.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.shotrush.atom.Atom;

import java.util.ArrayList;
import java.util.List;

public class AtomSpecializationsGui {
    private final Atom plugin;

    public AtomSpecializationsGui(Atom plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        double varietyBonus = plugin.getSkillTransferManager().getVarietyBonus(player);
        double masteryBonus = plugin.getSkillTransferManager().getMasteryBonus(player, "mine_stone");
        
        GuiFramework gui = GuiFramework.builder()
            .title("Specialization Systems")
            .size(3)
            
            .item(10, createVarietyItem(varietyBonus))
            .item(11, createMasteryItem(masteryBonus))
            .item(12, createSkillTransferItem())
            .item(13, createBonusTypesItem())
            .item(14, createLearningCurvesItem())
            .item(15, createInfoItem())
            .item(22, GuiFramework.GuiItem.builder()
                .material(Material.ARROW)
                .name("Back")
                .lore("Return to main menu")
                .onClick(p -> new AtomMainGui(plugin).open(p))
                .build())
            .build();

        gui.open(player);
    }

    private GuiFramework.GuiItem createVarietyItem(double bonus) {
        List<String> lore = new ArrayList<>();
        lore.add("Current Bonus: +" + String.format("%.0f%%", bonus * 100));
        lore.add("");
        lore.add("Do 5+ different actions");
        lore.add("to unlock variety bonuses!");
        lore.add("");
        lore.add("Max Bonus: +50%");
        
        return GuiFramework.GuiItem.builder()
            .material(Material.EXPERIENCE_BOTTLE)
            .name("Variety Bonus")
            .lore(lore)
            
            .build();
    }

    private GuiFramework.GuiItem createMasteryItem(double bonus) {
        List<String> lore = new ArrayList<>();
        lore.add("Current Bonus: +" + String.format("%.0f%%", bonus * 100));
        lore.add("");
        lore.add("Reach 2000 XP in one skill");
        lore.add("to unlock mastery bonuses!");
        lore.add("");
        lore.add("Max Bonus: +100%");
        
        return GuiFramework.GuiItem.builder()
            .material(Material.NETHER_STAR)
            .name("Mastery Bonus")
            .lore(lore)
            
            .build();
    }

    private GuiFramework.GuiItem createSkillTransferItem() {
        List<String> lore = new ArrayList<>();
        lore.add("30% of XP from related skills");
        lore.add("transfers to each other!");
        lore.add("");
        lore.add("Example:");
        lore.add("  Mining stone helps mining ores");
        lore.add("  Killing zombies helps all combat");
        lore.add("");
        lore.add("This creates organic solidarity!");
        
        return GuiFramework.GuiItem.builder()
            .material(Material.ENCHANTED_BOOK)
            .name("Skill Transfer")
            .lore(lore)
            .build();
    }

    private GuiFramework.GuiItem createBonusTypesItem() {
        List<String> lore = new ArrayList<>();
        lore.add("Different bonuses unlock at");
        lore.add("different experience levels:");
        lore.add("");
        lore.add("Speed Bonus (0 XP)");
        lore.add("  Break blocks faster");
        lore.add("  Attack faster");
        lore.add("");
        lore.add("Yield Bonus (500 XP)");
        lore.add("  Extra drops from blocks");
        lore.add("");
        lore.add("Quality Bonus (1000 XP)");
        lore.add("  Better crafted items");
        
        return GuiFramework.GuiItem.builder()
            .material(Material.DIAMOND)
            .name("Bonus Types")
            .lore(lore)
            .build();
    }

    private GuiFramework.GuiItem createLearningCurvesItem() {
        List<String> lore = new ArrayList<>();
        lore.add("Efficiency scales with XP:");
        lore.add("");
        lore.add("0 XP: 0.5x efficiency");
        lore.add("100 XP: 1.0x efficiency");
        lore.add("500 XP: 1.5x efficiency");
        lore.add("1000 XP: 2.0x efficiency");
        lore.add("");
        lore.add("Plus variety/mastery bonuses!");
        
        return GuiFramework.GuiItem.builder()
            .material(Material.BOOK)
            .name("Learning Curves")
            .lore(lore)
            .build();
    }

    private GuiFramework.GuiItem createInfoItem() {
        List<String> lore = new ArrayList<>();
        lore.add("This system is based on");
        lore.add("15 real academic theories:");
        lore.add("");
        lore.add("  Durkheim's Organic Solidarity");
        lore.add("  Adam Smith's Division of Labor");
        lore.add("  Weber's Social Stratification");
        lore.add("  Carneiro's Circumscription Theory");
        lore.add("  And 11 more!");
        lore.add("");
        lore.add("See SOCIOLOGY.md for details");
        
        return GuiFramework.GuiItem.builder()
            .material(Material.WRITABLE_BOOK)
            .name("Academic Background")
            .lore(lore)
            .build();
    }
}

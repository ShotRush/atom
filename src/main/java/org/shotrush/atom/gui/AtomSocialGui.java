package org.shotrush.atom.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.shotrush.atom.Atom;
import org.shotrush.atom.manager.ReputationManager;

import java.util.*;

public class AtomSocialGui {
    private final Atom plugin;

    public AtomSocialGui(Atom plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        ReputationManager repManager = plugin.getReputationManager();
        
        double socialCapital = repManager.getSocialCapital(player);
        Map<UUID, Integer> tradingPartners = repManager.getTradingPartnersWithCounts(player);
        double reputationBonus = repManager.getReputationBonus(player);
        
        GuiFramework.Builder guiBuilder = GuiFramework.builder()
            .title("Social Networks")
            .size(3)
            ;
        
        guiBuilder.item(4, createSocialCapitalItem(socialCapital, reputationBonus));
        
        List<Map.Entry<UUID, Integer>> sortedPartners = new ArrayList<>(tradingPartners.entrySet());
        sortedPartners.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        
        int slot = 9;
        for (int i = 0; i < Math.min(sortedPartners.size(), 18); i++) {
            Map.Entry<UUID, Integer> entry = sortedPartners.get(i);
            Player partner = Bukkit.getPlayer(entry.getKey());
            
            if (slot >= 27) break;
            
            guiBuilder.item(slot++, createPartnerItem(partner, entry.getValue()));
        }
        
        guiBuilder.item(22, createBackItem());
        
        guiBuilder.build().open(player);
    }

    private GuiFramework.GuiItem createSocialCapitalItem(double socialCapital, double bonus) {
        List<String> lore = new ArrayList<>();
        lore.add("Your influence in the community");
        lore.add("");
        lore.add("Social Capital: " + String.format("%.1f", socialCapital));
        lore.add("Reputation Bonus: +" + String.format("%.0f%%", bonus * 100));
        lore.add("");
        lore.add("Trade with others to build");
        lore.add("your social network!");
        
        return GuiFramework.GuiItem.builder()
            .material(Material.NETHER_STAR)
            .name("Social Capital")
            .lore(lore)
            
            .build();
    }

    private GuiFramework.GuiItem createPartnerItem(Player partner, int tradeCount) {
        String name = partner != null ? partner.getName() : "Unknown Player";
        boolean online = partner != null && partner.isOnline();
        
        List<String> lore = new ArrayList<>();
        lore.add("Status: " + (online ? "Online" : "Offline"));
        lore.add("Trades: " + tradeCount);
        lore.add("");
        
        if (tradeCount >= 50) {
            lore.add("★ TRUSTED PARTNER ★");
        } else if (tradeCount >= 20) {
            lore.add("Regular Partner");
        } else if (tradeCount >= 5) {
            lore.add("Trading Partner");
        } else {
            lore.add("New Contact");
        }
        
        return GuiFramework.GuiItem.builder()
            .material(online ? Material.EMERALD : Material.GRAY_DYE)
            .name(name)
            .lore(lore)
            .build();
    }

    private GuiFramework.GuiItem createBackItem() {
        return GuiFramework.GuiItem.builder()
            .material(Material.ARROW)
            .name("Back to Main Menu")
            .lore("Click to go back")
            .onClick(p -> new AtomMainGui(plugin).open(p))
            .build();
    }
}

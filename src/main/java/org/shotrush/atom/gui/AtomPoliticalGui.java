package org.shotrush.atom.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.shotrush.atom.Atom;
import org.shotrush.atom.manager.PoliticalManager;

import java.util.ArrayList;
import java.util.List;

public class AtomPoliticalGui {
    private final Atom plugin;

    public AtomPoliticalGui(Atom plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        PoliticalManager polManager = plugin.getPoliticalManager();
        
        boolean isBigMan = polManager.isBigMan(player);
        PoliticalManager.AuthorityType authority = polManager.getAuthorityType(player);
        double legitimacy = polManager.getLegitimacy(player);
        double bigManScore = polManager.getBigManScore(player);
        
        GuiFramework.Builder guiBuilder = GuiFramework.builder()
            .title("Political System")
            .size(3)
            ;
        
        guiBuilder.item(4, createStatusItem(isBigMan, authority, legitimacy));
        guiBuilder.item(10, createBigManScoreItem(bigManScore, isBigMan));
        guiBuilder.item(11, createAuthorityItem(authority));
        guiBuilder.item(12, createLegitimacyItem(legitimacy, isBigMan));
        guiBuilder.item(13, createLeadersItem());
        guiBuilder.item(14, createChallengeItem(isBigMan));
        guiBuilder.item(22, createBackItem());
        
        guiBuilder.build().open(player);
    }

    private GuiFramework.GuiItem createStatusItem(boolean isBigMan, PoliticalManager.AuthorityType authority, double legitimacy) {
        List<String> lore = new ArrayList<>();
        
        if (isBigMan) {
            lore.add("YOU ARE A BIG MAN");
            lore.add("");
            lore.add("Authority Type: " + authority.name());
            lore.add("Legitimacy: " + String.format("%.0f%%", legitimacy * 100));
            lore.add("");
            lore.add("You are recognized as a leader!");
            lore.add("Maintain legitimacy through:");
            lore.add("Redistribution");
            lore.add("Social capital");
            lore.add("Resource surplus");
        } else {
            lore.add("Status: Citizen");
            lore.add("");
            lore.add("Become a Big Man by:");
            lore.add("Gaining social capital (10+)");
            lore.add("Building trade networks");
            lore.add("Accumulating surplus");
            lore.add("");
            lore.add("Big Men gain influence and");
            lore.add("can shape the community!");
        }
        
        return GuiFramework.GuiItem.builder()
            .material(isBigMan ? Material.GOLDEN_HELMET : Material.IRON_HELMET)
            .name("Your Political Status")
            .lore(lore)
            
            .build();
    }

    private GuiFramework.GuiItem createBigManScoreItem(double score, boolean isBigMan) {
        List<String> lore = new ArrayList<>();
        lore.add("Your influence score");
        lore.add("");
        lore.add("Score: " + String.format("%.1f", score));
        lore.add("Required: 10.0");
        lore.add("");
        
        if (score >= 10.0) {
            lore.add("Big Man threshold reached!");
        } else {
            double needed = 10.0 - score;
            lore.add("Need: " + String.format("%.1f", needed) + " more");
        }
        
        lore.add("");
        lore.add("Calculated from:");
        lore.add("Social Capital (40%)");
        lore.add("Trading Partners (30%)");
        lore.add("Resource Surplus (30%)");
        
        return GuiFramework.GuiItem.builder()
            .material(Material.NETHER_STAR)
            .name("Big Man Score")
            .lore(lore)
            
            .build();
    }

    private GuiFramework.GuiItem createAuthorityItem(PoliticalManager.AuthorityType authority) {
        List<String> lore = new ArrayList<>();
        lore.add("Your type of leadership");
        lore.add("");
        
        switch (authority) {
            case CHARISMATIC:
                lore.add("CHARISMATIC AUTHORITY");
                lore.add("Based on personal appeal");
                lore.add("Legitimacy: 50%");
                lore.add("Required Score: 10+");
                break;
                
            case TRADITIONAL:
                lore.add("TRADITIONAL AUTHORITY");
                lore.add("Based on established customs");
                lore.add("Legitimacy: 70%");
                lore.add("Required Score: 30+");
                break;
                
            case RATIONAL_LEGAL:
                lore.add("RATIONAL-LEGAL AUTHORITY");
                lore.add("Based on rules and merit");
                lore.add("Legitimacy: 90%");
                lore.add("Required Score: 50+");
                break;
                
            default:
                lore.add("No authority yet");
                lore.add("Become a Big Man first!");
                break;
        }
        
        lore.add("");
        lore.add("Based on Weber's theory");
        
        return GuiFramework.GuiItem.builder()
            .material(Material.WRITABLE_BOOK)
            .name("Authority Type")
            .lore(lore)
            .build();
    }

    private GuiFramework.GuiItem createLegitimacyItem(double legitimacy, boolean isBigMan) {
        List<String> lore = new ArrayList<>();
        lore.add("How accepted your rule is");
        lore.add("");
        lore.add("Legitimacy: " + getLegitimacyColor(legitimacy) + String.format("%.0f%%", legitimacy * 100));
        lore.add("");
        
        if (isBigMan) {
            lore.add("Increase legitimacy by:");
            lore.add("Redistributing resources");
            lore.add("Winning challenges");
            lore.add("Maintaining surplus");
            lore.add("");
            
            if (legitimacy >= 0.8) {
                lore.add("STRONG LEGITIMACY");
            } else if (legitimacy >= 0.5) {
                lore.add("Stable legitimacy");
            } else {
                lore.add(" WEAK LEGITIMACY");
                lore.add("Vulnerable to challenge!");
            }
        } else {
            lore.add("Not applicable");
            lore.add("(Not a Big Man)");
        }
        
        return GuiFramework.GuiItem.builder()
            .material(Material.GOLDEN_APPLE)
            .name("Legitimacy")
            .lore(lore)
            
            .build();
    }

    private GuiFramework.GuiItem createLeadersItem() {
        PoliticalManager polManager = plugin.getPoliticalManager();
        
        List<String> lore = new ArrayList<>();
        lore.add("Current Big Men:");
        lore.add("");
        
        int count = 0;
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (polManager.isBigMan(onlinePlayer)) {
                PoliticalManager.AuthorityType auth = polManager.getAuthorityType(onlinePlayer);
                lore.add("" + onlinePlayer.getName() + " (" + auth.name() + ")");
                count++;
            }
        }
        
        if (count == 0) {
            lore.add("No Big Men online");
        }
        
        lore.add("");
        lore.add("Total Leaders: " + count);
        
        return GuiFramework.GuiItem.builder()
            .material(Material.PLAYER_HEAD)
            .name("Current Leaders")
            .lore(lore)
            .build();
    }

    private GuiFramework.GuiItem createChallengeItem(boolean isBigMan) {
        List<String> lore = new ArrayList<>();
        
        if (isBigMan) {
            lore.add("Defend your position!");
            lore.add("");
            lore.add("Other players can challenge");
            lore.add("your authority if they have");
            lore.add("a higher Big Man score.");
            lore.add("");
            lore.add("Maintain your legitimacy to");
            lore.add("resist challenges!");
        } else {
            lore.add("Challenge a Big Man!");
            lore.add("");
            lore.add("If your Big Man score is 20%");
            lore.add("higher than theirs, you can");
            lore.add("challenge their authority!");
            lore.add("");
            lore.add("Coming Soon!");
        }
        
        return GuiFramework.GuiItem.builder()
            .material(Material.IRON_SWORD)
            .name("Challenge System")
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

    private String getLegitimacyColor(double legitimacy) {
        if (legitimacy >= 0.8) return "";
        if (legitimacy >= 0.5) return "";
        if (legitimacy >= 0.3) return "";
        return "";
    }
}

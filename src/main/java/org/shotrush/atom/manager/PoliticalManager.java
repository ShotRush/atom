package org.shotrush.atom.manager;

import org.bukkit.entity.Player;
import org.shotrush.atom.Atom;
import org.shotrush.atom.config.SocialSystemsConfig;
import org.shotrush.atom.model.PlayerData;

import java.util.*;

public class PoliticalManager {
    private final Atom plugin;
    private final SocialSystemsConfig config;
    private final ReputationManager reputationManager;
    private final CollectiveActionManager collectiveActionManager;
    
    private final Map<UUID, BigManStatus> bigMen;
    private final Map<UUID, AuthorityType> authorityTypes;
    private final Map<UUID, Double> legitimacy;

    public PoliticalManager(Atom plugin, SocialSystemsConfig config, ReputationManager reputationManager, CollectiveActionManager collectiveActionManager) {
        this.plugin = plugin;
        this.config = config;
        this.reputationManager = reputationManager;
        this.collectiveActionManager = collectiveActionManager;
        this.bigMen = new HashMap<>();
        this.authorityTypes = new HashMap<>();
        this.legitimacy = new HashMap<>();
    }
    
    public boolean isEnabled() {
        return config.isPoliticalEnabled();
    }

    public void evaluateBigManStatus(Player player) {
        if (!isEnabled()) return;
        
        UUID uuid = player.getUniqueId();
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(uuid);
        
        double socialCapital = reputationManager.getSocialCapital(player);
        int tradingPartners = reputationManager.getTradingPartners(player).size();
        double surplus = calculateSurplus(player);
        
        double bigManScore = (socialCapital * config.getSocialCapitalWeight()) + 
                            (tradingPartners * config.getTradingPartnersWeight()) + 
                            (surplus * config.getSurplusWeight());
        
        if (bigManScore > config.getBigManThreshold()) {
            BigManStatus status = bigMen.getOrDefault(uuid, new BigManStatus(player.getName()));
            status.score = bigManScore;
            status.followers = tradingPartners;
            bigMen.put(uuid, status);
            
            data.setBigMan(true);
            data.setBigManScore(bigManScore);
            data.setBigManFollowers(tradingPartners);
            
            determineAuthorityType(player, status);
        } else {
            bigMen.remove(uuid);
            authorityTypes.remove(uuid);
            data.setBigMan(false);
            data.setBigManScore(0.0);
            data.setAuthorityType("NONE");
            data.setLegitimacy(0.0);
        }
    }

    private void determineAuthorityType(Player player, BigManStatus status) {
        UUID uuid = player.getUniqueId();
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(uuid);
        
        if (status.score > config.getRationalLegalThreshold() && status.followers > 20) {
            authorityTypes.put(uuid, AuthorityType.RATIONAL_LEGAL);
            legitimacy.put(uuid, config.getRationalLegalLegitimacy());
            data.setAuthorityType("RATIONAL_LEGAL");
            data.setLegitimacy(config.getRationalLegalLegitimacy());
        } else if (status.score > config.getTraditionalThreshold()) {
            authorityTypes.put(uuid, AuthorityType.TRADITIONAL);
            legitimacy.put(uuid, config.getTraditionalLegitimacy());
            data.setAuthorityType("TRADITIONAL");
            data.setLegitimacy(config.getTraditionalLegitimacy());
        } else {
            authorityTypes.put(uuid, AuthorityType.CHARISMATIC);
            double charismaticLegitimacy = config.getCharismaticLegitimacy() + (status.score / 100.0);
            legitimacy.put(uuid, charismaticLegitimacy);
            data.setAuthorityType("CHARISMATIC");
            data.setLegitimacy(charismaticLegitimacy);
        }
    }

    private double calculateSurplus(Player player) {
        double efficiency = plugin.getEmergentBonusManager().getSpeedMultiplier(player, "mine_stone");
        if (efficiency > 2.0) {
            return (efficiency - 1.0) * 10.0;
        }
        return 0.0;
    }

    public boolean isBigMan(Player player) {
        return bigMen.containsKey(player.getUniqueId());
    }

    public BigManStatus getBigManStatus(Player player) {
        return bigMen.get(player.getUniqueId());
    }

    public AuthorityType getAuthorityType(Player player) {
        return authorityTypes.getOrDefault(player.getUniqueId(), AuthorityType.NONE);
    }

    public double getLegitimacy(Player player) {
        return legitimacy.getOrDefault(player.getUniqueId(), 0.0);
    }

    public double getBigManScore(Player player) {
        BigManStatus status = bigMen.get(player.getUniqueId());
        if (status != null) {
            return status.score;
        }
        return calculateBigManScore(player);
    }

    public void redistributeResources(Player bigMan, List<Player> followers) {
        if (!isEnabled() || !isBigMan(bigMan)) return;
        
        UUID uuid = bigMan.getUniqueId();
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(uuid);
        BigManStatus status = bigMen.get(uuid);
        status.redistributions++;
        data.incrementRedistributions();
        
        for (Player follower : followers) {
            reputationManager.recordGift(bigMan, follower);
        }
        
        double newLegitimacy = legitimacy.getOrDefault(uuid, 0.0) + 
                              config.getRedistributionLegitimacyGain();
        newLegitimacy = Math.min(newLegitimacy, config.getMaxLegitimacy());
        legitimacy.put(uuid, newLegitimacy);
        data.setLegitimacy(newLegitimacy);
    }

    public void challengeAuthority(Player challenger, Player incumbent) {
        if (!isEnabled() || !isBigMan(incumbent)) return;
        
        double challengerScore = calculateBigManScore(challenger);
        double incumbentScore = bigMen.get(incumbent.getUniqueId()).score;
        
        if (challengerScore > incumbentScore * config.getChallengeAdvantage()) {
            bigMen.remove(incumbent.getUniqueId());
            authorityTypes.remove(incumbent.getUniqueId());
            legitimacy.remove(incumbent.getUniqueId());
            
            evaluateBigManStatus(challenger);
            
            challenger.sendMessage("§6You have successfully challenged " + incumbent.getName() + "'s authority!");
            incumbent.sendMessage("§cYour authority has been challenged and lost!");
        } else {
            double currentLegitimacy = legitimacy.getOrDefault(incumbent.getUniqueId(), 0.5);
            legitimacy.put(incumbent.getUniqueId(), currentLegitimacy + config.getSuccessLegitimacyGain());
            
            incumbent.sendMessage("§aYou have successfully defended your authority!");
            challenger.sendMessage("§cYour challenge failed!");
        }
    }

    private double calculateBigManScore(Player player) {
        double socialCapital = reputationManager.getSocialCapital(player);
        int tradingPartners = reputationManager.getTradingPartners(player).size();
        double surplus = calculateSurplus(player);
        return (socialCapital * config.getSocialCapitalWeight()) + 
               (tradingPartners * config.getTradingPartnersWeight()) + 
               (surplus * config.getSurplusWeight());
    }

    public List<Player> getRankedLeaders() {
        List<Player> leaders = new ArrayList<>();
        List<Map.Entry<UUID, BigManStatus>> sorted = new ArrayList<>(bigMen.entrySet());
        sorted.sort((a, b) -> Double.compare(b.getValue().score, a.getValue().score));
        
        for (Map.Entry<UUID, BigManStatus> entry : sorted) {
            Player player = plugin.getServer().getPlayer(entry.getKey());
            if (player != null) {
                leaders.add(player);
            }
        }
        return leaders;
    }

    public enum AuthorityType {
        NONE,
        CHARISMATIC,
        TRADITIONAL,
        RATIONAL_LEGAL
    }

    public static class BigManStatus {
        public String name;
        public double score;
        public int followers;
        public int redistributions;

        public BigManStatus(String name) {
            this.name = name;
            this.score = 0.0;
            this.followers = 0;
            this.redistributions = 0;
        }
    }
}

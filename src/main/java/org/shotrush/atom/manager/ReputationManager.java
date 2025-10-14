package org.shotrush.atom.manager;

import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.shotrush.atom.Atom;
import org.shotrush.atom.config.SocialSystemsConfig;
import org.shotrush.atom.util.PDCKeys;

import java.util.*;

public class ReputationManager {
    private final Atom plugin;
    private final PDCKeys pdcKeys;
    private final SocialSystemsConfig config;
    private final Map<UUID, Map<UUID, Integer>> tradeNetwork;
    private final Map<UUID, Double> socialCapital;

    public ReputationManager(Atom plugin, SocialSystemsConfig config) {
        this.plugin = plugin;
        this.pdcKeys = plugin.getPDCKeys();
        this.config = config;
        this.tradeNetwork = new HashMap<>();
        this.socialCapital = new HashMap<>();
    }
    
    public boolean isEnabled() {
        return config.isReputationEnabled();
    }

    public void recordTrade(Player player1, Player player2, int value) {
        if (!isEnabled()) return;
        
        UUID uuid1 = player1.getUniqueId();
        UUID uuid2 = player2.getUniqueId();
        
        tradeNetwork.computeIfAbsent(uuid1, k -> new HashMap<>())
            .merge(uuid2, 1, Integer::sum);
        tradeNetwork.computeIfAbsent(uuid2, k -> new HashMap<>())
            .merge(uuid1, 1, Integer::sum);
        
        plugin.getPlayerDataManager().getPlayerData(uuid1).recordTrade(uuid2);
        plugin.getPlayerDataManager().getPlayerData(uuid2).recordTrade(uuid1);
        
        updateSocialCapital(player1);
        updateSocialCapital(player2);
    }
    
    public void recordGift(Player giver, Player receiver) {
        if (!isEnabled()) return;
        
        UUID giverUUID = giver.getUniqueId();
        UUID receiverUUID = receiver.getUniqueId();
        
        double giverCapital = socialCapital.getOrDefault(giverUUID, 0.0);
        socialCapital.put(giverUUID, giverCapital + config.getGiftBondStrength());
        plugin.getPlayerDataManager().getPlayerData(giverUUID).setSocialCapital(giverCapital + config.getGiftBondStrength());
        
        double receiverCapital = socialCapital.getOrDefault(receiverUUID, 0.0);
        socialCapital.put(receiverUUID, receiverCapital + config.getTradeBondStrength());
        plugin.getPlayerDataManager().getPlayerData(receiverUUID).setSocialCapital(receiverCapital + config.getTradeBondStrength());
    }

    private void updateSocialCapital(Player player) {
        UUID uuid = player.getUniqueId();
        Map<UUID, Integer> trades = tradeNetwork.get(uuid);
        
        if (trades == null || trades.isEmpty()) {
            socialCapital.put(uuid, 0.0);
            plugin.getPlayerDataManager().getPlayerData(uuid).setSocialCapital(0.0);
            return;
        }
        
        int uniquePartners = trades.size();
        int totalTrades = trades.values().stream().mapToInt(Integer::intValue).sum();
        
        double capital = (Math.log1p(uniquePartners) * config.getUniquePartnersWeight()) + 
                        (Math.log1p(totalTrades) * config.getTotalTradesWeight());
        
        socialCapital.put(uuid, capital);
        plugin.getPlayerDataManager().getPlayerData(uuid).setSocialCapital(capital);
    }

    public double getSocialCapital(Player player) {
        return socialCapital.getOrDefault(player.getUniqueId(), 0.0);
    }
    public double getReputationBonus(Player player) {
        if (!isEnabled()) return 0.0;
        double capital = getSocialCapital(player);
        return Math.min(capital * config.getSocialCapitalBonusMultiplier(), config.getMaxSocialCapitalBonus());
    }

    public int getTradeCount(Player player, Player partner) {
        Map<UUID, Integer> trades = tradeNetwork.get(player.getUniqueId());
        if (trades == null) return 0;
        return trades.getOrDefault(partner.getUniqueId(), 0);
    }

    public Set<UUID> getTradingPartners(Player player) {
        Map<UUID, Integer> trades = tradeNetwork.get(player.getUniqueId());
        return trades != null ? new HashSet<>(trades.keySet()) : new HashSet<>();
    }

    public Map<UUID, Integer> getTradingPartnersWithCounts(Player player) {
        Map<UUID, Integer> trades = tradeNetwork.get(player.getUniqueId());
        return trades != null ? new HashMap<>(trades) : new HashMap<>();
    }

    public boolean hasReciprocityDebt(Player player, Player partner) {
        int given = getTradeCount(player, partner);
        int received = getTradeCount(partner, player);
        return Math.abs(given - received) > config.getDebtThreshold();
    }

    public double getReciprocityBalance(Player player, Player partner) {
        int given = getTradeCount(player, partner);
        int received = getTradeCount(partner, player);
        if (given + received == 0) return 0.0;
        return (double) (given - received) / (given + received);
    }
}

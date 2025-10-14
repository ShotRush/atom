package org.shotrush.atom.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

public class SocialSystemsConfig {
    private final Plugin plugin;
    
    private boolean reputationEnabled;
    private double uniquePartnersWeight;
    private double totalTradesWeight;
    private double maxSocialCapitalBonus;
    private double socialCapitalBonusMultiplier;
    private int debtThreshold;
    private double giftBondStrength;
    private double tradeBondStrength;
    
    private boolean collectiveActionEnabled;
    private double freeRiderThreshold;
    private double contributionBonusMax;
    private double contributionBonusMultiplier;
    
    private boolean politicalEnabled;
    private double bigManThreshold;
    private double charismaticThreshold;
    private double traditionalThreshold;
    private double rationalLegalThreshold;
    private double charismaticLegitimacy;
    private double traditionalLegitimacy;
    private double rationalLegalLegitimacy;
    private double socialCapitalWeight;
    private double tradingPartnersWeight;
    private double surplusWeight;
    private double challengeAdvantage;
    private double successLegitimacyGain;
    private double redistributionLegitimacyGain;
    private double maxLegitimacy;

    public SocialSystemsConfig(Plugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        ConfigurationSection social = plugin.getConfig().getConfigurationSection("social-systems");
        if (social == null) {
            setDefaults();
            return;
        }

        ConfigurationSection reputation = social.getConfigurationSection("reputation");
        if (reputation != null) {
            reputationEnabled = reputation.getBoolean("enabled", true);
            
            ConfigurationSection formula = reputation.getConfigurationSection("social-capital-formula");
            if (formula != null) {
                uniquePartnersWeight = formula.getDouble("unique-partners-weight", 2.0);
                totalTradesWeight = formula.getDouble("total-trades-weight", 0.5);
                maxSocialCapitalBonus = formula.getDouble("max-bonus", 0.3);
                socialCapitalBonusMultiplier = formula.getDouble("bonus-multiplier", 0.05);
            }
            
            ConfigurationSection reciprocity = reputation.getConfigurationSection("reciprocity");
            if (reciprocity != null) {
                debtThreshold = reciprocity.getInt("debt-threshold", 5);
                giftBondStrength = reciprocity.getDouble("gift-bond-strength", 0.5);
                tradeBondStrength = reciprocity.getDouble("trade-bond-strength", 0.25);
            }
        }

        ConfigurationSection collective = social.getConfigurationSection("collective-action");
        if (collective != null) {
            collectiveActionEnabled = collective.getBoolean("enabled", true);
            freeRiderThreshold = collective.getDouble("free-rider-threshold", 0.3);
            contributionBonusMax = collective.getDouble("contribution-bonus-max", 0.2);
            contributionBonusMultiplier = collective.getDouble("contribution-bonus-multiplier", 0.5);
        }

        ConfigurationSection political = social.getConfigurationSection("political");
        if (political != null) {
            politicalEnabled = political.getBoolean("enabled", true);
            bigManThreshold = political.getDouble("big-man-threshold", 10.0);
            
            ConfigurationSection thresholds = political.getConfigurationSection("authority-thresholds");
            if (thresholds != null) {
                charismaticThreshold = thresholds.getDouble("charismatic", 10.0);
                traditionalThreshold = thresholds.getDouble("traditional", 30.0);
                rationalLegalThreshold = thresholds.getDouble("rational-legal", 50.0);
            }
            
            ConfigurationSection legitimacy = political.getConfigurationSection("legitimacy-values");
            if (legitimacy != null) {
                charismaticLegitimacy = legitimacy.getDouble("charismatic", 0.5);
                traditionalLegitimacy = legitimacy.getDouble("traditional", 0.7);
                rationalLegalLegitimacy = legitimacy.getDouble("rational-legal", 0.9);
            }
            
            ConfigurationSection weights = political.getConfigurationSection("score-weights");
            if (weights != null) {
                socialCapitalWeight = weights.getDouble("social-capital", 0.4);
                tradingPartnersWeight = weights.getDouble("trading-partners", 0.3);
                surplusWeight = weights.getDouble("surplus", 0.3);
            }
            
            ConfigurationSection challenge = political.getConfigurationSection("challenge");
            if (challenge != null) {
                challengeAdvantage = challenge.getDouble("required-advantage", 1.2);
                successLegitimacyGain = challenge.getDouble("success-legitimacy-gain", 0.1);
                redistributionLegitimacyGain = challenge.getDouble("redistribution-legitimacy-gain", 0.05);
                maxLegitimacy = challenge.getDouble("max-legitimacy", 1.0);
            }
        }
    }

    private void setDefaults() {
        reputationEnabled = true;
        uniquePartnersWeight = 2.0;
        totalTradesWeight = 0.5;
        maxSocialCapitalBonus = 0.3;
        socialCapitalBonusMultiplier = 0.05;
        debtThreshold = 5;
        giftBondStrength = 0.5;
        tradeBondStrength = 0.25;
        
        collectiveActionEnabled = true;
        freeRiderThreshold = 0.3;
        contributionBonusMax = 0.2;
        contributionBonusMultiplier = 0.5;
        
        politicalEnabled = true;
        bigManThreshold = 10.0;
        charismaticThreshold = 10.0;
        traditionalThreshold = 30.0;
        rationalLegalThreshold = 50.0;
        charismaticLegitimacy = 0.5;
        traditionalLegitimacy = 0.7;
        rationalLegalLegitimacy = 0.9;
        socialCapitalWeight = 0.4;
        tradingPartnersWeight = 0.3;
        surplusWeight = 0.3;
        challengeAdvantage = 1.2;
        successLegitimacyGain = 0.1;
        redistributionLegitimacyGain = 0.05;
        maxLegitimacy = 1.0;
    }

    public boolean isReputationEnabled() { return reputationEnabled; }
    public double getUniquePartnersWeight() { return uniquePartnersWeight; }
    public double getTotalTradesWeight() { return totalTradesWeight; }
    public double getMaxSocialCapitalBonus() { return maxSocialCapitalBonus; }
    public double getSocialCapitalBonusMultiplier() { return socialCapitalBonusMultiplier; }
    public int getDebtThreshold() { return debtThreshold; }
    public double getGiftBondStrength() { return giftBondStrength; }
    public double getTradeBondStrength() { return tradeBondStrength; }
    
    public boolean isCollectiveActionEnabled() { return collectiveActionEnabled; }
    public double getFreeRiderThreshold() { return freeRiderThreshold; }
    public double getContributionBonusMax() { return contributionBonusMax; }
    public double getContributionBonusMultiplier() { return contributionBonusMultiplier; }
    
    public boolean isPoliticalEnabled() { return politicalEnabled; }
    public double getBigManThreshold() { return bigManThreshold; }
    public double getCharismaticThreshold() { return charismaticThreshold; }
    public double getTraditionalThreshold() { return traditionalThreshold; }
    public double getRationalLegalThreshold() { return rationalLegalThreshold; }
    public double getCharismaticLegitimacy() { return charismaticLegitimacy; }
    public double getTraditionalLegitimacy() { return traditionalLegitimacy; }
    public double getRationalLegalLegitimacy() { return rationalLegalLegitimacy; }
    public double getSocialCapitalWeight() { return socialCapitalWeight; }
    public double getTradingPartnersWeight() { return tradingPartnersWeight; }
    public double getSurplusWeight() { return surplusWeight; }
    public double getChallengeAdvantage() { return challengeAdvantage; }
    public double getSuccessLegitimacyGain() { return successLegitimacyGain; }
    public double getRedistributionLegitimacyGain() { return redistributionLegitimacyGain; }
    public double getMaxLegitimacy() { return maxLegitimacy; }
}

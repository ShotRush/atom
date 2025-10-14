package org.shotrush.atom.manager;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.shotrush.atom.Atom;
import org.shotrush.atom.config.CropGrowthConfig;

import java.util.*;

public class EnvironmentalManager {
    private final Atom plugin;
    private final CropGrowthConfig cropGrowthConfig;
    private final Map<UUID, EnvironmentalContext> playerEnvironments;
    private final Map<String, Double> biomeProductivity;
    private final Map<String, Set<Material>> biomeResources;
    private final Map<UUID, Double> playerFoodSurplus;

    public EnvironmentalManager(Atom plugin) {
        this.plugin = plugin;
        this.cropGrowthConfig = new CropGrowthConfig(plugin);
        this.playerEnvironments = new HashMap<>();
        this.biomeProductivity = new HashMap<>();
        this.biomeResources = new HashMap<>();
        this.playerFoodSurplus = new HashMap<>();
        initializeBiomeData();
    }

    private void initializeBiomeData() {
        biomeProductivity.put("PLAINS", 1.5);
        biomeProductivity.put("FOREST", 1.3);
        biomeProductivity.put("JUNGLE", 1.4);
        biomeProductivity.put("DESERT", 0.6);
        biomeProductivity.put("MOUNTAINS", 0.7);
        biomeProductivity.put("OCEAN", 0.8);
        biomeProductivity.put("RIVER", 1.2);
        biomeProductivity.put("SWAMP", 1.1);
        biomeProductivity.put("TAIGA", 0.9);
        biomeProductivity.put("SAVANNA", 1.0);
        
        biomeResources.put("DESERT", new HashSet<>(Arrays.asList(
            Material.SAND, Material.SANDSTONE, Material.CACTUS, Material.DEAD_BUSH
        )));
        biomeResources.put("MOUNTAINS", new HashSet<>(Arrays.asList(
            Material.STONE, Material.IRON_ORE, Material.COAL_ORE, Material.EMERALD_ORE
        )));
        biomeResources.put("FOREST", new HashSet<>(Arrays.asList(
            Material.OAK_LOG, Material.BIRCH_LOG, Material.SPRUCE_LOG, Material.MUSHROOM_STEM
        )));
        biomeResources.put("OCEAN", new HashSet<>(Arrays.asList(
            Material.KELP, Material.PRISMARINE, Material.SEA_LANTERN, Material.SPONGE
        )));
        biomeResources.put("PLAINS", new HashSet<>(Arrays.asList(
            Material.WHEAT, Material.CARROT, Material.POTATO, Material.GRASS_BLOCK
        )));
    }

    public void updatePlayerEnvironment(Player player) {
        Location loc = player.getLocation();
        Biome biome = loc.getBlock().getBiome();
        String biomeCategory = categorizeBiome(biome);
        
        EnvironmentalContext context = playerEnvironments.getOrDefault(
            player.getUniqueId(),
            new EnvironmentalContext()
        );
        
        context.currentBiome = biomeCategory;
        context.productivity = biomeProductivity.getOrDefault(biomeCategory, 1.0);
        context.availableResources = biomeResources.getOrDefault(biomeCategory, new HashSet<>());
        context.isCircumscribed = checkCircumscription(loc);
        context.populationDensity = calculatePopulationDensity(loc);
        
        playerEnvironments.put(player.getUniqueId(), context);
    }

    private String categorizeBiome(Biome biome) {
        String name = biome.getKey().getKey().toUpperCase();
        if (name.contains("DESERT")) return "DESERT";
        if (name.contains("MOUNTAIN") || name.contains("PEAK") || name.contains("HILL")) return "MOUNTAINS";
        if (name.contains("FOREST")) return "FOREST";
        if (name.contains("OCEAN") || name.contains("DEEP")) return "OCEAN";
        if (name.contains("PLAINS")) return "PLAINS";
        if (name.contains("RIVER")) return "RIVER";
        if (name.contains("SWAMP")) return "SWAMP";
        if (name.contains("TAIGA")) return "TAIGA";
        if (name.contains("SAVANNA")) return "SAVANNA";
        if (name.contains("JUNGLE")) return "JUNGLE";
        return "PLAINS";
    }

    private boolean checkCircumscription(Location loc) {
        int radius = 100;
        int waterBlocks = 0;
        int mountainBlocks = 0;
        int totalBlocks = 0;
        
        for (int x = -radius; x <= radius; x += 10) {
            for (int z = -radius; z <= radius; z += 10) {
                Location check = loc.clone().add(x, 0, z);
                Biome biome = check.getBlock().getBiome();
                String category = categorizeBiome(biome);
                
                if (category.equals("OCEAN")) waterBlocks++;
                if (category.equals("MOUNTAINS")) mountainBlocks++;
                totalBlocks++;
            }
        }
        
        double circumscriptionRatio = (double)(waterBlocks + mountainBlocks) / totalBlocks;
        return circumscriptionRatio > 0.4;
    }

    private double calculatePopulationDensity(Location loc) {
        int radius = 200;
        int playerCount = 0;
        
        for (Player other : loc.getWorld().getPlayers()) {
            if (other.getLocation().distance(loc) < radius) {
                playerCount++;
            }
        }
        
        return playerCount / (Math.PI * radius * radius / 1000000.0);
    }

    public double getEnvironmentalBonus(Player player) {
        EnvironmentalContext context = playerEnvironments.get(player.getUniqueId());
        if (context == null) {
            updatePlayerEnvironment(player);
            context = playerEnvironments.get(player.getUniqueId());
        }
        
        double bonus = context.productivity - 1.0;
        
        if (context.isCircumscribed) {
            bonus += 0.2;
        }
        
        if (context.populationDensity > 0.5) {
            bonus -= 0.1 * (context.populationDensity - 0.5);
        }
        
        return Math.max(-0.5, Math.min(bonus, 0.5));
    }

    public boolean hasResourceAccess(Player player, Material material) {
        EnvironmentalContext context = playerEnvironments.get(player.getUniqueId());
        if (context == null) return true;
        
        return context.availableResources.contains(material);
    }

    public double getResourceScarcityPressure(Player player) {
        EnvironmentalContext context = playerEnvironments.get(player.getUniqueId());
        if (context == null) return 0.0;
        
        double pressure = 0.0;
        
        if (context.isCircumscribed) {
            pressure += 0.3;
        }
        
        if (context.populationDensity > 1.0) {
            pressure += 0.2 * context.populationDensity;
        }
        
        if (context.productivity < 0.8) {
            pressure += (0.8 - context.productivity) * 0.5;
        }
        
        return Math.min(pressure, 1.0);
    }

    public boolean shouldTriggerConflict(Player player) {
        double pressure = getResourceScarcityPressure(player);
        EnvironmentalContext context = playerEnvironments.get(player.getUniqueId());
        
        if (context == null) return false;
        
        return pressure > 0.6 && context.isCircumscribed && context.populationDensity > 0.8;
    }

    public List<Player> getNearbyCompetitors(Player player, int radius) {
        List<Player> competitors = new ArrayList<>();
        Location loc = player.getLocation();
        
        for (Player other : loc.getWorld().getPlayers()) {
            if (other.equals(player)) continue;
            if (other.getLocation().distance(loc) < radius) {
                competitors.add(other);
            }
        }
        
        return competitors;
    }

    public double getCropGrowthMultiplier(Block cropBlock) {
        if (!cropGrowthConfig.isEnabled()) return 1.0;

        Material cropType = cropBlock.getType();
        Biome biome = cropBlock.getBiome();
        String biomeCategory = categorizeBiome(biome);

        return cropGrowthConfig.getGrowthMultiplier(cropType, biomeCategory);
    }

    public void updatePlayerFoodSurplus(Player player, double cropProductivity) {
        double surplus = cropGrowthConfig.getFoodSurplusBonus(cropProductivity);
        playerFoodSurplus.put(player.getUniqueId(), surplus);
    }

    public double getFoodSurplusBonus(Player player) {
        return playerFoodSurplus.getOrDefault(player.getUniqueId(), 0.0);
    }

    public double getTotalEnvironmentalBonus(Player player) {
        double baseBonus = getEnvironmentalBonus(player);
        double foodBonus = getFoodSurplusBonus(player);
        return baseBonus + foodBonus;
    }

    public CropGrowthConfig getCropGrowthConfig() {
        return cropGrowthConfig;
    }

    public static class EnvironmentalContext {
        public String currentBiome;
        public double productivity;
        public Set<Material> availableResources;
        public boolean isCircumscribed;
        public double populationDensity;
        public double cropProductivity;

        public EnvironmentalContext() {
            this.currentBiome = "PLAINS";
            this.productivity = 1.0;
            this.availableResources = new HashSet<>();
            this.isCircumscribed = false;
            this.populationDensity = 0.0;
            this.cropProductivity = 1.0;
        }
    }
}


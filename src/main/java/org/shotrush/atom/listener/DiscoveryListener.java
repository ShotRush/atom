package org.shotrush.atom.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.shotrush.atom.Atom;
import org.shotrush.atom.manager.ConfigManager;
import org.shotrush.atom.manager.EmergentBonusManager;
import org.shotrush.atom.model.TrackedAction;
import org.shotrush.atom.model.TriggerType;

import java.util.*;

public class DiscoveryListener implements Listener {
    private final Atom plugin;
    private final ConfigManager configManager;
    private final EmergentBonusManager emergentBonusManager;
    private final Random random;
    private final Map<String, List<DiscoveryItem>> discoveryPools;
    
    public DiscoveryListener(Atom plugin, ConfigManager configManager, EmergentBonusManager emergentBonusManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.emergentBonusManager = emergentBonusManager;
        this.random = new Random();
        this.discoveryPools = new HashMap<>();
        
        initializeDiscoveryPools();
    }
    
    private void initializeDiscoveryPools() {
        List<DiscoveryItem> universalDiscoveries = new ArrayList<>();
        universalDiscoveries.add(new DiscoveryItem(Material.DIAMOND, 1, 0.001, "Legendary Diamond"));
        universalDiscoveries.add(new DiscoveryItem(Material.EMERALD, 1, 0.002, "Precious Emerald"));
        universalDiscoveries.add(new DiscoveryItem(Material.NETHERITE_SCRAP, 1, 0.0005, "Ancient Debris Fragment"));
        universalDiscoveries.add(new DiscoveryItem(Material.TOTEM_OF_UNDYING, 1, 0.0001, "Mythical Totem"));
        universalDiscoveries.add(new DiscoveryItem(Material.ENCHANTED_GOLDEN_APPLE, 1, 0.0003, "Divine Apple"));
        universalDiscoveries.add(new DiscoveryItem(Material.NETHER_STAR, 1, 0.00005, "Celestial Star"));
        universalDiscoveries.add(new DiscoveryItem(Material.ELYTRA, 1, 0.00001, "Wings of Fortune"));
        universalDiscoveries.add(new DiscoveryItem(Material.TRIDENT, 1, 0.00008, "Oceanic Trident"));
        universalDiscoveries.add(new DiscoveryItem(Material.HEART_OF_THE_SEA, 1, 0.0001, "Heart of the Deep"));
        universalDiscoveries.add(new DiscoveryItem(Material.ENCHANTED_BOOK, 1, 0.005, "Ancient Knowledge"));
        universalDiscoveries.add(new DiscoveryItem(Material.EXPERIENCE_BOTTLE, 5, 0.01, "Essence Cluster"));
        universalDiscoveries.add(new DiscoveryItem(Material.GOLD_INGOT, 3, 0.008, "Gold Cache"));
        universalDiscoveries.add(new DiscoveryItem(Material.ENDER_PEARL, 2, 0.006, "Void Pearls"));
        universalDiscoveries.add(new DiscoveryItem(Material.GOLDEN_CARROT, 3, 0.012, "Golden Harvest"));
        universalDiscoveries.add(new DiscoveryItem(Material.NAUTILUS_SHELL, 1, 0.004, "Ocean Relic"));
        
        discoveryPools.put("universal", universalDiscoveries);
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreakDiscovery(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Material material = event.getBlock().getType();
        
        for (TrackedAction action : configManager.getActions().values()) {
            for (TrackedAction.ActionTrigger trigger : action.getTriggers()) {
                if (trigger.getType() == TriggerType.BLOCK_BREAK && trigger.matches(material)) {
                    attemptDiscovery(player, action.getId(), event.getBlock().getLocation());
                    return;
                }
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDeathDiscovery(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        
        EntityType entityType = event.getEntity().getType();
        
        for (TrackedAction action : configManager.getActions().values()) {
            for (TrackedAction.ActionTrigger trigger : action.getTriggers()) {
                if (trigger.getType() == TriggerType.ENTITY_DEATH && trigger.matches(entityType)) {
                    List<ItemStack> discoveries = attemptDiscovery(killer, action.getId(), event.getEntity().getLocation());
                    if (!discoveries.isEmpty()) {
                        event.getDrops().addAll(discoveries);
                    }
                    return;
                }
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFishingDiscovery(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        
        Player player = event.getPlayer();
        
        for (TrackedAction action : configManager.getActions().values()) {
            if (action.getId().equals("catch_fish")) {
                List<ItemStack> discoveries = attemptDiscovery(player, action.getId(), event.getHook().getLocation());
                if (!discoveries.isEmpty() && event.getCaught() != null) {
                    for (ItemStack discovery : discoveries) {
                        player.getWorld().dropItemNaturally(event.getHook().getLocation(), discovery);
                    }
                }
                return;
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onVillagerTradeDiscovery(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof org.bukkit.entity.Villager)) return;
        
        Player player = event.getPlayer();
        
        for (TrackedAction action : configManager.getActions().values()) {
            if (action.getId().equals("trade_villager")) {
                double discoveryChance = emergentBonusManager.getDiscoveryChance(player, action.getId());
                if (discoveryChance > 0 && random.nextDouble() < discoveryChance * 0.5) {
                    List<ItemStack> discoveries = attemptDiscovery(player, action.getId(), event.getRightClicked().getLocation());
                    if (!discoveries.isEmpty()) {
                        for (ItemStack discovery : discoveries) {
                            player.getInventory().addItem(discovery);
                        }
                    }
                }
                return;
            }
        }
    }
    
    private List<ItemStack> attemptDiscovery(Player player, String actionId, org.bukkit.Location location) {
        List<ItemStack> discoveries = new ArrayList<>();
        
        double discoveryChance = emergentBonusManager.getDiscoveryChance(player, actionId);
        if (discoveryChance <= 0) return discoveries;
        
        List<DiscoveryItem> pool = discoveryPools.get("universal");
        if (pool == null || pool.isEmpty()) return discoveries;
        
        if (random.nextDouble() > discoveryChance) return discoveries;
        
        DiscoveryItem discovered = selectDiscovery(pool, discoveryChance);
        if (discovered == null) return discoveries;
        
        ItemStack item = new ItemStack(discovered.material, discovered.amount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(discovered.name)
                .color(NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));
            
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Discovered through experience")
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, true));
            lore.add(Component.text("Discovery Chance: " + String.format("%.1f%%", discoveryChance * 100))
                .color(NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            
            if (discovered.rarity < 0.10 && item.getType().getMaxDurability() > 0) {
                Enchantment[] enchantments = {Enchantment.UNBREAKING, Enchantment.MENDING};
                Enchantment randomEnchant = enchantments[random.nextInt(enchantments.length)];
                meta.addEnchant(randomEnchant, 1, true);
            }
            
            item.setItemMeta(meta);
        }
        
        discoveries.add(item);
        
        player.sendActionBar(Component.text("Discovered: " + discovered.name));
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.3f, 1.5f);
        
        if (location != null) {
            player.getWorld().dropItemNaturally(location, item);
            discoveries.clear();
        }
        
        return discoveries;
    }
    
    private DiscoveryItem selectDiscovery(List<DiscoveryItem> pool, double discoveryChance) {
        double totalWeight = 0;
        List<WeightedDiscovery> weighted = new ArrayList<>();
        
        for (DiscoveryItem item : pool) {
            double weight = (1.0 / item.rarity) * (1.0 + discoveryChance);
            weighted.add(new WeightedDiscovery(item, weight));
            totalWeight += weight;
        }
        
        double roll = random.nextDouble() * totalWeight;
        double current = 0;
        
        for (WeightedDiscovery wd : weighted) {
            current += wd.weight;
            if (roll <= current) {
                return wd.item;
            }
        }
        
        return weighted.isEmpty() ? null : weighted.get(0).item;
    }
    
    private static class DiscoveryItem {
        final Material material;
        final int amount;
        final double rarity;
        final String name;
        
        DiscoveryItem(Material material, int amount, double rarity, String name) {
            this.material = material;
            this.amount = amount;
            this.rarity = rarity;
            this.name = name;
        }
    }
    
    private static class WeightedDiscovery {
        final DiscoveryItem item;
        final double weight;
        
        WeightedDiscovery(DiscoveryItem item, double weight) {
            this.item = item;
            this.weight = weight;
        }
    }
}

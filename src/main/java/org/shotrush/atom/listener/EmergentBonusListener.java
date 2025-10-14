package org.shotrush.atom.listener;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.BrewingStandFuelEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.shotrush.atom.Atom;
import org.shotrush.atom.manager.ActionManager;
import org.shotrush.atom.manager.ConfigManager;
import org.shotrush.atom.manager.EmergentBonusManager;
import org.shotrush.atom.model.ActionContext;
import org.shotrush.atom.model.TrackedAction;
import org.shotrush.atom.model.TriggerType;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class EmergentBonusListener implements Listener {
    private final Atom plugin;
    private final ConfigManager configManager;
    private final EmergentBonusManager emergentBonusManager;
    private final ActionManager actionManager;
    private final Random random;
    private final Map<UUID, NamespacedKey> activeModifiers;

    public EmergentBonusListener(Atom plugin, ConfigManager configManager, EmergentBonusManager emergentBonusManager, ActionManager actionManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.emergentBonusManager = emergentBonusManager;
        this.actionManager = actionManager;
        this.random = new Random();
        this.activeModifiers = new HashMap<>();
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockDamageSpeed(BlockDamageEvent event) {
        Player player = event.getPlayer();
        Material material = event.getBlock().getType();

        for (TrackedAction action : configManager.getActions().values()) {
            for (TrackedAction.ActionTrigger trigger : action.getTriggers()) {
                if (trigger.getType() == TriggerType.BLOCK_BREAK && trigger.matches(material)) {
                    double speedMultiplier = emergentBonusManager.getSpeedMultiplier(player, action.getId());
                    applyMiningSpeed(player, speedMultiplier);
                    return;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreakBonus(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Material material = event.getBlock().getType();

        for (TrackedAction action : configManager.getActions().values()) {
            for (TrackedAction.ActionTrigger trigger : action.getTriggers()) {
                if (trigger.getType() == TriggerType.BLOCK_BREAK && trigger.matches(material)) {
                    ActionContext context = new ActionContext(
                        action.getId(),
                        TriggerType.BLOCK_BREAK,
                        material,
                        null,
                        player.getInventory().getItemInMainHand()
                    );

                    applyYieldBonus(event, player, context);
                    break;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityDamageBonus(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        Player player = (Player) event.getDamager();

        for (TrackedAction action : configManager.getActions().values()) {
            for (TrackedAction.ActionTrigger trigger : action.getTriggers()) {
                if (trigger.getType() == TriggerType.ENTITY_DEATH || trigger.getType() == TriggerType.ENTITY_DAMAGE) {
                    double speedMultiplier = emergentBonusManager.getSpeedMultiplier(player, action.getId());
                    applyCombatAttributes(player, speedMultiplier);
                    return;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockPlaceSpeed(org.bukkit.event.block.BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Material material = event.getBlock().getType();

        for (TrackedAction action : configManager.getActions().values()) {
            for (TrackedAction.ActionTrigger trigger : action.getTriggers()) {
                if (trigger.getType() == TriggerType.BLOCK_PLACE && trigger.matches(material)) {
                    double speedMultiplier = emergentBonusManager.getSpeedMultiplier(player, action.getId());
                    applyPlacementSpeed(player, speedMultiplier);
                    return;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCraftSpeed(org.bukkit.event.inventory.CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        for (TrackedAction action : configManager.getActions().values()) {
            for (TrackedAction.ActionTrigger trigger : action.getTriggers()) {
                if (trigger.getType() == TriggerType.CRAFT_ITEM) {
                    double speedMultiplier = emergentBonusManager.getSpeedMultiplier(player, action.getId());
                    applyCraftingSpeed(player, speedMultiplier);
                    return;
                }
            }
        }
    }

    private void applyMiningSpeed(Player player, double speedMultiplier) {
        AttributeInstance blockBreakSpeed = player.getAttribute(Attribute.BLOCK_BREAK_SPEED);
        AttributeInstance miningEfficiency = player.getAttribute(Attribute.MINING_EFFICIENCY);
        
        if (blockBreakSpeed == null || miningEfficiency == null) return;
        
        NamespacedKey key = new NamespacedKey(plugin, "atom_mining_" + player.getUniqueId());
        
        blockBreakSpeed.getModifiers().stream()
            .filter(mod -> mod.getKey().getNamespace().equals("atom"))
            .toList()
            .forEach(blockBreakSpeed::removeModifier);
        
        miningEfficiency.getModifiers().stream()
            .filter(mod -> mod.getKey().getNamespace().equals("atom"))
            .toList()
            .forEach(miningEfficiency::removeModifier);
        
        double clampedMultiplier = Math.max(0.08, speedMultiplier);
        double baseValue = blockBreakSpeed.getBaseValue();
        double targetValue = baseValue * clampedMultiplier;
        double addAmount = targetValue - baseValue;
        
        AttributeModifier breakModifier = new AttributeModifier(
            key,
            addAmount,
            AttributeModifier.Operation.ADD_NUMBER
        );
        
        double miningModifier = clampedMultiplier - 1.0;
        AttributeModifier mineModifier = new AttributeModifier(
            key,
            miningModifier,
            AttributeModifier.Operation.MULTIPLY_SCALAR_1
        );
        
        blockBreakSpeed.addModifier(breakModifier);
        miningEfficiency.addModifier(mineModifier);
        activeModifiers.put(player.getUniqueId(), key);
    }
    
    private void applyCombatAttributes(Player player, double speedMultiplier) {
        AttributeInstance attackDamage = player.getAttribute(Attribute.ATTACK_DAMAGE);
        AttributeInstance attackSpeed = player.getAttribute(Attribute.ATTACK_SPEED);
        AttributeInstance movementSpeed = player.getAttribute(Attribute.MOVEMENT_SPEED);
        
        if (attackDamage == null || attackSpeed == null || movementSpeed == null) return;
        
        NamespacedKey key = new NamespacedKey(plugin, "atom_combat_" + player.getUniqueId());
        
        attackDamage.getModifiers().stream()
            .filter(mod -> mod.getKey().getNamespace().equals("atom"))
            .toList()
            .forEach(attackDamage::removeModifier);
        
        attackSpeed.getModifiers().stream()
            .filter(mod -> mod.getKey().getNamespace().equals("atom"))
            .toList()
            .forEach(attackSpeed::removeModifier);
        
        movementSpeed.getModifiers().stream()
            .filter(mod -> mod.getKey().getNamespace().equals("atom"))
            .toList()
            .forEach(movementSpeed::removeModifier);
        
        double clampedMultiplier = Math.max(0.08, speedMultiplier);
        double damageBonus = (clampedMultiplier - 1.0) * 2.0;
        double speedBonus = (clampedMultiplier - 1.0) * 1.0;
        double moveBonus = (clampedMultiplier - 1.0) * 0.05;
        
        AttributeModifier damageModifier = new AttributeModifier(
            key,
            damageBonus,
            AttributeModifier.Operation.ADD_NUMBER
        );
        
        AttributeModifier speedModifier = new AttributeModifier(
            key,
            speedBonus,
            AttributeModifier.Operation.ADD_NUMBER
        );
        
        AttributeModifier moveModifier = new AttributeModifier(
            key,
            moveBonus,
            AttributeModifier.Operation.ADD_NUMBER
        );
        
        attackDamage.addModifier(damageModifier);
        attackSpeed.addModifier(speedModifier);
        movementSpeed.addModifier(moveModifier);
        activeModifiers.put(player.getUniqueId(), key);
    }

    private void applyPlacementSpeed(Player player, double speedMultiplier) {
        AttributeInstance blockInteractionRange = player.getAttribute(Attribute.BLOCK_INTERACTION_RANGE);
        AttributeInstance movementSpeed = player.getAttribute(Attribute.MOVEMENT_SPEED);
        AttributeInstance stepHeight = player.getAttribute(Attribute.STEP_HEIGHT);
        AttributeInstance sneakingSpeed = player.getAttribute(Attribute.SNEAKING_SPEED);
        
        if (blockInteractionRange == null || movementSpeed == null) return;
        
        NamespacedKey key = new NamespacedKey(plugin, "atom_placement_" + player.getUniqueId());
        
        blockInteractionRange.getModifiers().stream()
            .filter(mod -> mod.getKey().getNamespace().equals("atom"))
            .toList()
            .forEach(blockInteractionRange::removeModifier);
        
        movementSpeed.getModifiers().stream()
            .filter(mod -> mod.getKey().getNamespace().equals("atom"))
            .toList()
            .forEach(movementSpeed::removeModifier);
        
        if (stepHeight != null) {
            stepHeight.getModifiers().stream()
                .filter(mod -> mod.getKey().getNamespace().equals("atom"))
                .toList()
                .forEach(stepHeight::removeModifier);
        }
        
        if (sneakingSpeed != null) {
            sneakingSpeed.getModifiers().stream()
                .filter(mod -> mod.getKey().getNamespace().equals("atom"))
                .toList()
                .forEach(sneakingSpeed::removeModifier);
        }
        
        double clampedMultiplier = Math.max(0.08, speedMultiplier);
        double rangeBonus = (clampedMultiplier - 1.0) * 1.5;
        double moveBonus = (clampedMultiplier - 1.0) * 0.05;
        double stepBonus = (clampedMultiplier - 1.0) * 0.5;
        double sneakBonus = (clampedMultiplier - 1.0) * 0.3;
        
        AttributeModifier rangeModifier = new AttributeModifier(
            key,
            rangeBonus,
            AttributeModifier.Operation.ADD_NUMBER
        );
        blockInteractionRange.addModifier(rangeModifier);
        
        AttributeModifier moveModifier = new AttributeModifier(
            key,
            moveBonus,
            AttributeModifier.Operation.ADD_NUMBER
        );
        movementSpeed.addModifier(moveModifier);
        
        if (stepHeight != null) {
            AttributeModifier stepModifier = new AttributeModifier(
                key,
                stepBonus,
                AttributeModifier.Operation.ADD_NUMBER
            );
            stepHeight.addModifier(stepModifier);
        }
        
        if (sneakingSpeed != null) {
            AttributeModifier sneakModifier = new AttributeModifier(
                key,
                sneakBonus,
                AttributeModifier.Operation.ADD_NUMBER
            );
            sneakingSpeed.addModifier(sneakModifier);
        }
        
        activeModifiers.put(player.getUniqueId(), key);
    }

    private void applyCraftingSpeed(Player player, double speedMultiplier) {
        AttributeInstance movementSpeed = player.getAttribute(Attribute.MOVEMENT_SPEED);
        
        if (movementSpeed == null) return;
        
        NamespacedKey key = new NamespacedKey(plugin, "atom_crafting_" + player.getUniqueId());
        
        movementSpeed.getModifiers().stream()
            .filter(mod -> mod.getKey().getNamespace().equals("atom"))
            .toList()
            .forEach(movementSpeed::removeModifier);
        
        double clampedMultiplier = Math.max(0.08, speedMultiplier);
        double moveBonus = (clampedMultiplier - 1.0) * 0.02;
        
        AttributeModifier moveModifier = new AttributeModifier(
            key,
            moveBonus,
            AttributeModifier.Operation.ADD_NUMBER
        );
        
        movementSpeed.addModifier(moveModifier);
        activeModifiers.put(player.getUniqueId(), key);
    }
    
    private void removeExistingModifier(Player player, AttributeInstance attribute) {
        NamespacedKey oldKey = activeModifiers.get(player.getUniqueId());
        if (oldKey != null) {
            attribute.getModifiers().stream()
                .filter(mod -> mod.getKey().equals(oldKey))
                .toList()
                .forEach(attribute::removeModifier);
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        activeModifiers.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemDamage(PlayerItemDamageEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        for (TrackedAction action : configManager.getActions().values()) {
            for (TrackedAction.ActionTrigger trigger : action.getTriggers()) {
                if ((trigger.getType() == TriggerType.BLOCK_BREAK || 
                     trigger.getType() == TriggerType.ENTITY_DEATH ||
                     trigger.getType() == TriggerType.ENTITY_DAMAGE) && 
                    item.equals(player.getInventory().getItemInMainHand())) {
                    
                    double reduction = emergentBonusManager.getDurabilityReduction(player, action.getId());
                    if (reduction > 0) {
                        int originalDamage = event.getDamage();
                        int newDamage = (int) (originalDamage * (1.0 - reduction));
                        event.setDamage(Math.max(0, newDamage));
                    }
                    break;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBrew(BrewEvent event) {
        if (!(event.getContents().getHolder() instanceof org.bukkit.block.BrewingStand)) return;
        
        org.bukkit.block.BrewingStand stand = (org.bukkit.block.BrewingStand) event.getContents().getHolder();
        Player nearestPlayer = stand.getLocation().getNearbyPlayers(5).stream().findFirst().orElse(null);
        
        if (nearestPlayer != null) {
            for (TrackedAction action : configManager.getActions().values()) {
                if (action.getId().contains("brew") || action.getId().contains("potion")) {
                    actionManager.grantExperience(nearestPlayer, action.getId(), action.getExperience());
                    break;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBrewingFuel(BrewingStandFuelEvent event) {
        Player nearestPlayer = event.getBlock().getLocation().getNearbyPlayers(5).stream().findFirst().orElse(null);
        
        if (nearestPlayer != null) {
            for (TrackedAction action : configManager.getActions().values()) {
                if (action.getId().contains("brew") || action.getId().contains("potion")) {
                    double efficiency = emergentBonusManager.getSpeedMultiplier(nearestPlayer, action.getId());
                    if (efficiency > 1.0) {
                        int bonusFuel = (int) ((efficiency - 1.0) * 10);
                        event.setFuelPower(event.getFuelPower() + bonusFuel);
                    }
                    break;
                }
            }
        }
    }

    private void applyYieldBonus(BlockBreakEvent event, Player player, ActionContext context) {
        double multiplier = emergentBonusManager.getYieldMultiplier(player, context.getActionId());
        if (multiplier <= 1.0) return;

        Block block = event.getBlock();
        Collection<ItemStack> drops = block.getDrops(player.getInventory().getItemInMainHand());
        
        if (random.nextDouble() < (multiplier - 1.0)) {
            for (ItemStack drop : drops) {
                block.getWorld().dropItemNaturally(block.getLocation(), drop.clone());
            }
        }
    }
}

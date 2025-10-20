package org.shotrush.atom.detection;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public final class CraftDetection {
    
    private CraftDetection() {}
    
    public static int calculateCraftedAmount(CraftItemEvent event) {
        if (isShiftClickCraft(event)) {
            ItemStack result = event.getRecipe().getResult();
            int maxCraftable = getMaxCraftableAmount(event, result);
            int maxFittable = getMaxFittableAmount(event, result);
            return Math.min(maxCraftable, maxFittable);
        }
        
        if (isDragCraft(event)) {
            return 1;
        }
        
        return 1;
    }
    
    public static boolean isShiftClickCraft(CraftItemEvent event) {
        return event.isShiftClick();
    }
    
    public static boolean isLeftClickCraft(CraftItemEvent event) {
        return event.getClick() == ClickType.LEFT;
    }
    
    public static boolean isRightClickCraft(CraftItemEvent event) {
        return event.getClick() == ClickType.RIGHT;
    }
    
    public static boolean isDragCraft(CraftItemEvent event) {
        ClickType click = event.getClick();
        return click == ClickType.DROP || 
               click == ClickType.CONTROL_DROP ||
               click == ClickType.CREATIVE;
    }
    
    public static boolean isNumberKeyCraft(CraftItemEvent event) {
        ClickType click = event.getClick();
        return click == ClickType.NUMBER_KEY;
    }
    
    public static boolean canCraftSucceed(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return false;
        
        ItemStack result = event.getRecipe().getResult();
        PlayerInventory inv = player.getInventory();
        ItemStack cursor = event.getCursor();
        
        if (cursor != null && !cursor.getType().isAir()) {
            if (isLeftClickCraft(event) || isRightClickCraft(event)) {
                if (!cursor.isSimilar(result)) {
                    return false;
                }
                
                if (cursor.getAmount() + result.getAmount() > cursor.getMaxStackSize()) {
                    return false;
                }
            }
        }
        
        if (isNumberKeyCraft(event)) {
            int hotbarSlot = event.getHotbarButton();
            if (hotbarSlot < 0 || hotbarSlot > 8) return true;
            
            ItemStack slotItem = inv.getItem(hotbarSlot);
            if (slotItem == null || slotItem.getType().isAir()) return true;
            
            if (!slotItem.isSimilar(result)) return false;
            
            if (slotItem.getAmount() + result.getAmount() > slotItem.getMaxStackSize()) {
                return false;
            }
        }
        
        return true;
    }
    
    public static int getMaxCraftableAmount(CraftItemEvent event, ItemStack result) {
        CraftingInventory craftingInv = event.getInventory();
        ItemStack[] matrix = craftingInv.getMatrix();
        
        int minStackSize = Integer.MAX_VALUE;
        for (ItemStack item : matrix) {
            if (item != null && !item.getType().isAir()) {
                minStackSize = Math.min(minStackSize, item.getAmount());
            }
        }
        
        return minStackSize == Integer.MAX_VALUE ? 1 : minStackSize;
    }
    
    public static int getMaxFittableAmount(CraftItemEvent event, ItemStack result) {
        if (!(event.getWhoClicked() instanceof Player player)) return 0;
        
        PlayerInventory inv = player.getInventory();
        int resultAmount = result.getAmount();
        int maxStack = result.getMaxStackSize();
        int totalSpace = 0;
        
        for (ItemStack item : inv.getStorageContents()) {
            if (item == null || item.getType().isAir()) {
                totalSpace += maxStack;
            } else if (item.isSimilar(result)) {
                int spaceInStack = maxStack - item.getAmount();
                totalSpace += spaceInStack;
            }
        }
        
        if (totalSpace == 0) return 0;
        
        return (int) Math.ceil((double) totalSpace / resultAmount);
    }
    
    public static boolean hasMaterialInIngredients(CraftItemEvent event, Material material) {
        for (ItemStack ingredient : event.getInventory().getMatrix()) {
            if (ingredient != null && ingredient.getType() == material) {
                return true;
            }
        }
        return false;
    }
    
    public static boolean hasIronInIngredients(CraftItemEvent event) {
        return hasMaterialInIngredients(event, Material.IRON_INGOT);
    }
    
    public static ItemStack[] getCraftingMatrix(CraftItemEvent event) {
        return event.getInventory().getMatrix();
    }
    
    public static int countMaterialInMatrix(CraftItemEvent event, Material material) {
        int count = 0;
        for (ItemStack ingredient : event.getInventory().getMatrix()) {
            if (ingredient != null && ingredient.getType() == material) {
                count += ingredient.getAmount();
            }
        }
        return count;
    }
    
    public static boolean isToolCraft(Material material) {
        String name = material.name();
        return name.contains("PICKAXE") || name.contains("AXE") || 
               name.contains("SHOVEL") || name.contains("HOE") ||
               name.contains("SWORD");
    }
    
    public static boolean isArmorCraft(Material material) {
        String name = material.name();
        return name.contains("HELMET") || name.contains("CHESTPLATE") || 
               name.contains("LEGGINGS") || name.contains("BOOTS");
    }
    
    public static boolean isStoneToolCraft(Material material) {
        return material == Material.STONE_PICKAXE || material == Material.STONE_AXE ||
               material == Material.STONE_SHOVEL || material == Material.STONE_HOE ||
               material == Material.STONE_SWORD;
    }
    
    public static boolean isWoodenToolCraft(Material material) {
        return material == Material.WOODEN_PICKAXE || material == Material.WOODEN_AXE ||
               material == Material.WOODEN_SHOVEL || material == Material.WOODEN_HOE ||
               material == Material.WOODEN_SWORD;
    }
    
    public static boolean isIronToolCraft(Material material) {
        return material == Material.IRON_PICKAXE || material == Material.IRON_AXE ||
               material == Material.IRON_SHOVEL || material == Material.IRON_HOE ||
               material == Material.IRON_SWORD;
    }
    
    public static boolean isDiamondToolCraft(Material material) {
        return material == Material.DIAMOND_PICKAXE || material == Material.DIAMOND_AXE ||
               material == Material.DIAMOND_SHOVEL || material == Material.DIAMOND_HOE ||
               material == Material.DIAMOND_SWORD;
    }
    
    public static boolean isGoldenToolCraft(Material material) {
        return material == Material.GOLDEN_PICKAXE || material == Material.GOLDEN_AXE ||
               material == Material.GOLDEN_SHOVEL || material == Material.GOLDEN_HOE ||
               material == Material.GOLDEN_SWORD;
    }
    
    public static boolean isNetheriteToolCraft(Material material) {
        return material == Material.NETHERITE_PICKAXE || material == Material.NETHERITE_AXE ||
               material == Material.NETHERITE_SHOVEL || material == Material.NETHERITE_HOE ||
               material == Material.NETHERITE_SWORD;
    }
    
    public static ToolTier getToolTier(Material material) {
        if (isWoodenToolCraft(material)) return ToolTier.WOODEN;
        if (isStoneToolCraft(material)) return ToolTier.STONE;
        if (isIronToolCraft(material)) return ToolTier.IRON;
        if (isDiamondToolCraft(material)) return ToolTier.DIAMOND;
        if (isGoldenToolCraft(material)) return ToolTier.GOLDEN;
        if (isNetheriteToolCraft(material)) return ToolTier.NETHERITE;
        return ToolTier.NONE;
    }
    
    public enum ToolTier {
        NONE, WOODEN, STONE, IRON, DIAMOND, GOLDEN, NETHERITE
    }
    
    public enum CraftType {
        NORMAL_CLICK,
        SHIFT_CLICK,
        RIGHT_CLICK,
        DRAG,
        NUMBER_KEY,
        OTHER
    }
    
    public static CraftType getCraftType(CraftItemEvent event) {
        if (isShiftClickCraft(event)) return CraftType.SHIFT_CLICK;
        if (isLeftClickCraft(event)) return CraftType.NORMAL_CLICK;
        if (isRightClickCraft(event)) return CraftType.RIGHT_CLICK;
        if (isDragCraft(event)) return CraftType.DRAG;
        if (isNumberKeyCraft(event)) return CraftType.NUMBER_KEY;
        return CraftType.OTHER;
    }
}

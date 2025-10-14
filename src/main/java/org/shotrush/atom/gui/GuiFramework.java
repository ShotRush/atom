package org.shotrush.atom.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class GuiFramework {
    private final String title;
    private final int size;
    private final Map<Integer, GuiItem> items;
    private Consumer<Player> onClose;
    private boolean cancelAllClicks;

    private GuiFramework(Builder builder) {
        this.title = builder.title;
        this.size = builder.size;
        this.items = builder.items;
        this.onClose = builder.onClose;
        this.cancelAllClicks = builder.cancelAllClicks;
    }

    public void open(Player player) {
        Inventory inventory = Bukkit.createInventory(new GuiHolder(this), size, title);
        
        for (Map.Entry<Integer, GuiItem> entry : items.entrySet()) {
            inventory.setItem(entry.getKey(), entry.getValue().getItemStack());
        }
        
        player.openInventory(inventory);
    }

    public void handleClick(InventoryClickEvent event) {
        if (cancelAllClicks) {
            event.setCancelled(true);
        }
        
        int slot = event.getSlot();
        GuiItem item = items.get(slot);
        
        if (item != null && item.getClickAction() != null) {
            item.getClickAction().accept((Player) event.getWhoClicked());
        }
    }

    public void handleClose(Player player) {
        if (onClose != null) {
            onClose.accept(player);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String title = "GUI";
        private int size = 27;
        private Map<Integer, GuiItem> items = new HashMap<>();
        private Consumer<Player> onClose;
        private boolean cancelAllClicks = true;

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder size(int rows) {
            this.size = rows * 9;
            return this;
        }

        public Builder item(int slot, GuiItem item) {
            this.items.put(slot, item);
            return this;
        }

        public Builder fillBorder(Material material, String name) {
            GuiItem borderItem = GuiItem.builder()
                .material(material)
                .name(name)
                .build();
            
            for (int i = 0; i < 9; i++) {
                items.put(i, borderItem);
                items.put(size - 9 + i, borderItem);
            }
            
            for (int i = 1; i < (size / 9) - 1; i++) {
                items.put(i * 9, borderItem);
                items.put(i * 9 + 8, borderItem);
            }
            
            return this;
        }

        public Builder onClose(Consumer<Player> onClose) {
            this.onClose = onClose;
            return this;
        }

        public Builder cancelAllClicks(boolean cancel) {
            this.cancelAllClicks = cancel;
            return this;
        }

        public GuiFramework build() {
            return new GuiFramework(this);
        }
    }

    public static class GuiItem {
        private final ItemStack itemStack;
        private final Consumer<Player> clickAction;

        private GuiItem(ItemBuilder builder) {
            this.itemStack = builder.buildItemStack();
            this.clickAction = builder.clickAction;
        }

        public ItemStack getItemStack() {
            return itemStack;
        }

        public Consumer<Player> getClickAction() {
            return clickAction;
        }

        public static ItemBuilder builder() {
            return new ItemBuilder();
        }

        public static class ItemBuilder {
            private Material material = Material.STONE;
            private String name = "";
            private List<String> lore = new ArrayList<>();
            private int amount = 1;
            private Consumer<Player> clickAction;
            private boolean glowing = false;

            public ItemBuilder material(Material material) {
                this.material = material;
                return this;
            }

            public ItemBuilder name(String name) {
                this.name = name;
                return this;
            }

            public ItemBuilder lore(String... lines) {
                for (String line : lines) {
                    this.lore.add(line);
                }
                return this;
            }

            public ItemBuilder lore(List<String> lore) {
                this.lore.addAll(lore);
                return this;
            }

            public ItemBuilder amount(int amount) {
                this.amount = amount;
                return this;
            }

            public ItemBuilder onClick(Consumer<Player> action) {
                this.clickAction = action;
                return this;
            }

            public ItemBuilder glowing(boolean glowing) {
                this.glowing = glowing;
                return this;
            }

            private ItemStack buildItemStack() {
                ItemStack item = new ItemStack(material, amount);
                ItemMeta meta = item.getItemMeta();
                
                if (meta != null) {
                    if (!name.isEmpty()) {
                        meta.setDisplayName(name);
                    }
                    if (!lore.isEmpty()) {
                        meta.setLore(lore);
                    }
                    if (glowing) {
                        meta.setEnchantmentGlintOverride(true);
                    }
                    item.setItemMeta(meta);
                }
                
                return item;
            }

            public GuiItem build() {
                return new GuiItem(this);
            }
        }
    }
}

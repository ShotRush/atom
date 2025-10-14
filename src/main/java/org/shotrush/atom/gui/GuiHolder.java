package org.shotrush.atom.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public class GuiHolder implements InventoryHolder {
    private final GuiFramework gui;

    public GuiHolder(GuiFramework gui) {
        this.gui = gui;
    }

    public GuiFramework getGui() {
        return gui;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return null;
    }
}

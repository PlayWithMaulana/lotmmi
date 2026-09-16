package com.Maul.lotmmi.gui;

import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class StaffItemCaptureMenuProvider implements MenuProvider {

    private final boolean mainHand;
    private final ItemStack staffStack;

    public StaffItemCaptureMenuProvider(boolean mainHand, ItemStack staffStack) {
        this.mainHand = mainHand;
        this.staffStack = staffStack;
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.literal("Staff of the Stars - Record Item");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new StaffItemCaptureMenu(containerId, playerInventory, mainHand, staffStack);
    }
}

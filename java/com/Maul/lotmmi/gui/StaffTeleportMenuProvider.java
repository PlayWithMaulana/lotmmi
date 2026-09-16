package com.Maul.lotmmi.gui;

import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class StaffTeleportMenuProvider implements MenuProvider {

    private final boolean mainHand;
    private final List<String> slotLabels;
    private final List<String> slotNames;

    public StaffTeleportMenuProvider(boolean mainHand, List<String> slotLabels, List<String> slotNames) {
        this.mainHand = mainHand;
        this.slotLabels = slotLabels;
        this.slotNames = slotNames;
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.literal("Staff of the Stars - Teleport");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new StaffTeleportMenu(containerId, mainHand, slotLabels, slotNames);
    }
}

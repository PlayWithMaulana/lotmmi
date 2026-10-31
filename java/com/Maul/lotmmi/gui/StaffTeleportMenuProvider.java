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
    private final List<String> slotDetails;

    public StaffTeleportMenuProvider(boolean mainHand, List<String> slotDetails) {
        this.mainHand = mainHand;
        this.slotDetails = slotDetails;
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.literal("Staff of the Stars - Teleport");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new StaffTeleportMenu(containerId, mainHand, slotDetails);
    }
}

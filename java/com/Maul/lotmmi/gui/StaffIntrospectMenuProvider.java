package com.Maul.lotmmi.gui;

import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class StaffIntrospectMenuProvider implements MenuProvider {

    private final boolean mainHand;
    private final List<String> libraryIds, libraryLabels, wheelIds, wheelLabels;

    public StaffIntrospectMenuProvider(boolean mainHand,
                                        List<String> libraryIds, List<String> libraryLabels,
                                        List<String> wheelIds, List<String> wheelLabels) {
        this.mainHand = mainHand;
        this.libraryIds = libraryIds;
        this.libraryLabels = libraryLabels;
        this.wheelIds = wheelIds;
        this.wheelLabels = wheelLabels;
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.literal("Staff of the Stars");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new StaffIntrospectMenu(containerId, mainHand, libraryIds, libraryLabels, wheelIds, wheelLabels);
    }
}

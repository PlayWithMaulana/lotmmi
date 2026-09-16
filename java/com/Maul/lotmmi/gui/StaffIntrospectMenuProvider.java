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
    private final List<String> entityWatchIds, entityWatchLabels;
    private final List<Boolean> entityEligible;
    private final List<String> entityActiveIds, entityActiveLabels;
    private final List<String> itemLibraryLabels;
    private final List<String> itemActiveIds, itemActiveLabels;

    public StaffIntrospectMenuProvider(boolean mainHand,
                                        List<String> libraryIds, List<String> libraryLabels,
                                        List<String> wheelIds, List<String> wheelLabels,
                                        List<String> entityWatchIds, List<String> entityWatchLabels, List<Boolean> entityEligible,
                                        List<String> entityActiveIds, List<String> entityActiveLabels,
                                        List<String> itemLibraryLabels,
                                        List<String> itemActiveIds, List<String> itemActiveLabels) {
        this.mainHand = mainHand;
        this.libraryIds = libraryIds;
        this.libraryLabels = libraryLabels;
        this.wheelIds = wheelIds;
        this.wheelLabels = wheelLabels;
        this.entityWatchIds = entityWatchIds;
        this.entityWatchLabels = entityWatchLabels;
        this.entityEligible = entityEligible;
        this.entityActiveIds = entityActiveIds;
        this.entityActiveLabels = entityActiveLabels;
        this.itemLibraryLabels = itemLibraryLabels;
        this.itemActiveIds = itemActiveIds;
        this.itemActiveLabels = itemActiveLabels;
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.literal("Staff of the Stars");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new StaffIntrospectMenu(containerId, mainHand, libraryIds, libraryLabels, wheelIds, wheelLabels,
                entityWatchIds, entityWatchLabels, entityEligible, entityActiveIds, entityActiveLabels,
                itemLibraryLabels, itemActiveIds, itemActiveLabels);
    }
}

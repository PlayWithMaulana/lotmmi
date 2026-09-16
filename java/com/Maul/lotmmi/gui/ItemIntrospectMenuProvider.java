package com.Maul.lotmmi.gui;

import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.NotNull;

public class ItemIntrospectMenuProvider implements MenuProvider {

    private final boolean mainHand;
    private final int maxSlots;
    private final String soulsRaw;
    private final String foodRaw;
    private final String abilityOrderRaw;
    private final long lastFedTick;

    public ItemIntrospectMenuProvider(boolean mainHand, int maxSlots, String soulsRaw, String foodRaw,
                                       String abilityOrderRaw, long lastFedTick) {
        this.mainHand = mainHand;
        this.maxSlots = maxSlots;
        this.soulsRaw = soulsRaw;
        this.foodRaw = foodRaw;
        this.abilityOrderRaw = abilityOrderRaw;
        this.lastFedTick = lastFedTick;
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.empty();
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ItemIntrospectMenu(containerId, mainHand, maxSlots, soulsRaw, foodRaw, abilityOrderRaw, lastFedTick);
    }
}

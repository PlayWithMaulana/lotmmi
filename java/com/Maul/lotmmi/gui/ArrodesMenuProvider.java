package com.Maul.lotmmi.gui;

import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.NotNull;

public class ArrodesMenuProvider implements MenuProvider {

    private final boolean owner;
    private final boolean answering;
    private final String opening;

    public ArrodesMenuProvider(boolean owner, boolean answering, String opening) {
        this.owner = owner;
        this.answering = answering;
        this.opening = opening;
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.literal("Arrodes");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ArrodesMenu(containerId, owner, answering, opening);
    }
}

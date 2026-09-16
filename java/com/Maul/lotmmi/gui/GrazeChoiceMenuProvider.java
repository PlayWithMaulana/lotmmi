package com.Maul.lotmmi.gui;

import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class GrazeChoiceMenuProvider implements MenuProvider {

    private final String targetName;
    private final String pathway;
    private final int sequence;
    private final List<String> abilityIds;

    public GrazeChoiceMenuProvider(String targetName, String pathway, int sequence, List<String> abilityIds) {
        this.targetName = targetName;
        this.pathway = pathway;
        this.sequence = sequence;
        this.abilityIds = abilityIds;
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.empty();
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new GrazeChoiceMenu(containerId, targetName, pathway, sequence, abilityIds);
    }
}

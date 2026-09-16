package com.Maul.lotmmi.gui;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class GrazeChoiceMenu extends AbstractContainerMenu {

    private final String targetName;
    private final String pathway;
    private final int sequence;
    private final List<String> abilityIds;

    public GrazeChoiceMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, buf.readUtf(), buf.readUtf(), buf.readVarInt(), readStringList(buf));
    }

    public GrazeChoiceMenu(int containerId, String targetName, String pathway, int sequence, List<String> abilityIds) {
        super(ModMenuTypes.GRAZE_CHOICE_MENU.get(), containerId);
        this.targetName = targetName;
        this.pathway = pathway;
        this.sequence = sequence;
        this.abilityIds = abilityIds;
    }

    private static List<String> readStringList(RegistryFriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<String> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) list.add(buf.readUtf());
        return list;
    }

    public String getTargetName() {
        return targetName;
    }

    public String getPathway() {
        return pathway;
    }

    public int getSequence() {
        return sequence;
    }

    public List<String> getAbilityIds() {
        return abilityIds;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}

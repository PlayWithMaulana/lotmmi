package com.Maul.lotmmi.gui;

import com.Maul.lotmmi.item.custom.StaffOfStarsItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class StaffTeleportMenu extends AbstractContainerMenu {

    private final boolean mainHand;
    private final List<String> slotLabels;
    private final List<String> slotNames;

    public StaffTeleportMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, buf.readBoolean(), readLabelsThenNames(buf));
    }

    private StaffTeleportMenu(int containerId, boolean mainHand, List<List<String>> labelsAndNames) {
        this(containerId, mainHand, labelsAndNames.get(0), labelsAndNames.get(1));
    }

    public StaffTeleportMenu(int containerId, boolean mainHand, List<String> slotLabels, List<String> slotNames) {
        super(ModMenuTypes.STAFF_TELEPORT_MENU.get(), containerId);
        this.mainHand = mainHand;
        this.slotLabels = slotLabels;
        this.slotNames = slotNames;
    }

    private static List<List<String>> readLabelsThenNames(RegistryFriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<String> labels = new ArrayList<>(size);
        for (int i = 0; i < size; i++) labels.add(buf.readUtf());
        List<String> names = new ArrayList<>(size);
        for (int i = 0; i < size; i++) names.add(buf.readUtf());
        return List.of(labels, names);
    }

    public boolean isMainHand() {
        return mainHand;
    }

    public List<String> getSlotLabels() {
        return slotLabels;
    }

    public List<String> getSlotNames() {
        return slotNames;
    }

    public InteractionHand getHand() {
        return mainHand ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getItemInHand(getHand()).getItem() instanceof StaffOfStarsItem;
    }
}

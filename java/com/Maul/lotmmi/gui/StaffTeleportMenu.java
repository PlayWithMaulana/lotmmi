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
    private List<String> slotDetails;
    private int syncVersion = 0;

    public StaffTeleportMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, buf.readBoolean(), readDetails(buf));
    }

    public StaffTeleportMenu(int containerId, boolean mainHand, List<String> slotDetails) {
        super(ModMenuTypes.STAFF_TELEPORT_MENU.get(), containerId);
        this.mainHand = mainHand;
        applySync(slotDetails);
    }

    public void applySync(List<String> slotDetails) {
        this.slotDetails = slotDetails;
        this.syncVersion++;
    }

    public int getSyncVersion() {
        return syncVersion;
    }

    private static List<String> readDetails(RegistryFriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<String> details = new ArrayList<>(size);
        for (int i = 0; i < size; i++) details.add(buf.readUtf());
        return details;
    }

    public boolean isMainHand() {
        return mainHand;
    }

    public List<String> getSlotDetails() {
        return slotDetails;
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

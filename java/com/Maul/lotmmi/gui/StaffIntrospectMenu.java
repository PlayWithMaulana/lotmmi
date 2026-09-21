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

public class StaffIntrospectMenu extends AbstractContainerMenu {

    private final boolean mainHand;
    private final List<String> libraryIds, libraryLabels;
    private final List<String> wheelIds, wheelLabels;

    public StaffIntrospectMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, buf.readBoolean(), readStrings(buf), readStrings(buf), readStrings(buf), readStrings(buf));
    }

    public StaffIntrospectMenu(int containerId, boolean mainHand,
                                List<String> libraryIds, List<String> libraryLabels,
                                List<String> wheelIds, List<String> wheelLabels) {
        super(ModMenuTypes.STAFF_INTROSPECT_MENU.get(), containerId);
        this.mainHand = mainHand;
        this.libraryIds = libraryIds;
        this.libraryLabels = libraryLabels;
        this.wheelIds = wheelIds;
        this.wheelLabels = wheelLabels;
    }

    private static List<String> readStrings(RegistryFriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<String> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) list.add(buf.readUtf());
        return list;
    }

    public boolean isMainHand() { return mainHand; }
    public InteractionHand getHand() { return mainHand ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND; }
    public List<String> getLibraryIds() { return libraryIds; }
    public List<String> getLibraryLabels() { return libraryLabels; }
    public List<String> getWheelIds() { return wheelIds; }
    public List<String> getWheelLabels() { return wheelLabels; }

    @Override
    public @NotNull ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getItemInHand(getHand()).getItem() instanceof StaffOfStarsItem;
    }
}

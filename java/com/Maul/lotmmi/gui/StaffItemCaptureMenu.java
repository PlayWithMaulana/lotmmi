package com.Maul.lotmmi.gui;

import com.Maul.lotmmi.item.custom.StaffItemSummonUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class StaffItemCaptureMenu extends AbstractContainerMenu {

    public static final int CAPTURE_COLUMNS = 5;
    public static final int CAPTURE_ROWS = 4;
    public static final int CAPTURE_SLOTS = CAPTURE_COLUMNS * CAPTURE_ROWS;

    private final SimpleContainer captureContainer;
    private final boolean mainHand;
    private final ItemStack staffStack;

    public StaffItemCaptureMenu(int containerId, Inventory playerInventory, net.minecraft.network.RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory, buf.readBoolean(), ItemStack.EMPTY);
    }

    public StaffItemCaptureMenu(int containerId, Inventory playerInventory, boolean mainHand, ItemStack staffStack) {
        super(ModMenuTypes.STAFF_ITEM_CAPTURE_MENU.get(), containerId);
        this.mainHand = mainHand;
        this.staffStack = staffStack;
        this.captureContainer = new SimpleContainer(CAPTURE_SLOTS);

        int gridLeft = 15;
        int gridTop = 18;
        for (int row = 0; row < CAPTURE_ROWS; row++) {
            for (int col = 0; col < CAPTURE_COLUMNS; col++) {
                int index = row * CAPTURE_COLUMNS + col;
                this.addSlot(new Slot(captureContainer, index, gridLeft + col * 18, gridTop + row * 18));
            }
        }

        int invLeft = 8;
        int invTop = gridTop + CAPTURE_ROWS * 18 + 14;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, invLeft + col * 18, invTop + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, invLeft + col * 18, invTop + 58));
        }
    }

    public boolean isMainHand() {
        return mainHand;
    }

    public Container getCaptureContainer() {
        return captureContainer;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;

        ItemStack original = slot.getItem();
        ItemStack moving = original.copy();

        if (index < CAPTURE_SLOTS) {
            if (!this.moveItemStackTo(original, CAPTURE_SLOTS, this.slots.size(), true)) return ItemStack.EMPTY;
        } else {
            if (!this.moveItemStackTo(original, 0, CAPTURE_SLOTS, false)) return ItemStack.EMPTY;
        }

        if (original.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();

        return moving;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (player instanceof ServerPlayer serverPlayer && !player.level().isClientSide) {
            StaffItemSummonUtil.processCapture(serverPlayer, staffStack, captureContainer);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}

package com.Maul.lotmmi.gui;

import com.Maul.lotmmi.item.custom.CreepingHungerItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class ItemIntrospectMenu extends AbstractContainerMenu {

    private final boolean mainHand;
    private final int maxSlots;

    private java.util.List<CreepingHungerItem.SoulSlot> souls;
    private CreepingHungerItem.SoulSlot foodSoul;
    private java.util.List<CreepingHungerItem.AbilityEntry> abilityOrder;
    private long lastFedTick;
    private int syncVersion = 0;

    public ItemIntrospectMenu(int containerId, net.minecraft.world.entity.player.Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        super(ModMenuTypes.ITEM_INTROSPECT_MENU.get(), containerId);
        this.mainHand = buf.readBoolean();
        this.maxSlots = buf.readVarInt();
        applySync(buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readVarLong());
    }

    public ItemIntrospectMenu(int containerId, boolean mainHand, int maxSlots,
                               String soulsRaw, String foodRaw, String abilityOrderRaw, long lastFedTick) {
        super(ModMenuTypes.ITEM_INTROSPECT_MENU.get(), containerId);
        this.mainHand = mainHand;
        this.maxSlots = maxSlots;
        applySync(soulsRaw, foodRaw, abilityOrderRaw, lastFedTick);
    }

    public void applySync(String soulsRaw, String foodRaw, String abilityOrderRaw, long lastFedTick) {
        this.souls = CreepingHungerItem.parseSouls(soulsRaw);
        this.foodSoul = (foodRaw == null || foodRaw.isEmpty()) ? null : CreepingHungerItem.SoulSlot.deserialize(foodRaw);
        this.abilityOrder = CreepingHungerItem.parseAbilityOrder(abilityOrderRaw);
        this.lastFedTick = lastFedTick;
        this.syncVersion++;
    }

    public int getSyncVersion() {
        return syncVersion;
    }

    public InteractionHand getHand() {
        return mainHand ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
    }

    public int getMaxSlots() {
        return maxSlots;
    }

    public java.util.List<CreepingHungerItem.SoulSlot> getSouls() {
        return souls;
    }

    public CreepingHungerItem.SoulSlot getFoodSoul() {
        return foodSoul;
    }

    public java.util.List<CreepingHungerItem.AbilityEntry> getAbilityOrder() {
        return abilityOrder;
    }

    public long getLastFedTick() {
        return lastFedTick;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getItemInHand(getHand()).getItem() instanceof CreepingHungerItem;
    }
}

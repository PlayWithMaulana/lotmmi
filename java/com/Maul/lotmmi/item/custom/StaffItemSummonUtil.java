package com.Maul.lotmmi.item.custom;

import com.Maul.lotmmi.gui.StaffItemCaptureMenuProvider;
import de.jakob.lotm.LOTMCraft;
import de.jakob.lotm.beyonders.abilities.core.Ability;
import de.jakob.lotm.util.BeyonderData;
import de.jakob.lotm.util.scheduling.ServerScheduler;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.List;
import java.util.UUID;

public class StaffItemSummonUtil {

    private static final String HISTORICAL_VOID_SUMMONING_ID = "historical_void_summoning_ability";
    private static final int SUMMON_DURATION_TICKS = 20 * 20;

    public static void openCaptureMenu(ServerPlayer player, ItemStack staffStack, boolean mainHand) {
        player.openMenu(new StaffItemCaptureMenuProvider(mainHand, staffStack), buf -> buf.writeBoolean(mainHand));
    }

    public static void processCapture(ServerPlayer player, ItemStack staffStack, Container captureContainer) {
        for (int i = 0; i < captureContainer.getContainerSize(); i++) {

            ItemStack stack = captureContainer.getItem(i).copy();
            if (stack.isEmpty()) continue;

            if (StaffMemoryUtil.getRecordedItems(staffStack).size() >= StaffMemoryUtil.MAX_RECORDED_ITEMS) {
                player.sendSystemMessage(Component.literal("The staff already remembers 20 items - returned unrecorded."));
                captureContainer.setItem(i, ItemStack.EMPTY);
                if (!player.getInventory().add(stack)) player.drop(stack, false);
                continue;
            }

            CompoundTag entry = new CompoundTag();
            entry.putString("ItemId", net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
            entry.putString("DisplayName", stack.getHoverName().getString());
            entry.putInt("Count", stack.getCount());
            Tag itemNbt = ItemStack.CODEC.encodeStart(
                    player.level().registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), stack
            ).result().orElse(null);
            if (itemNbt != null) entry.put("ItemNBT", itemNbt);

            StaffMemoryUtil.addRecordedItem(staffStack, entry);

            captureContainer.setItem(i, ItemStack.EMPTY);
            if (!player.getInventory().add(stack)) player.drop(stack, false);
        }
    }

    public static void summonFromLibrary(ServerLevel level, ServerPlayer player, ItemStack staffStack, int index) {
        List<CompoundTag> library = StaffMemoryUtil.getRecordedItems(staffStack);
        if (index < 0 || index >= library.size()) return;

        if (StaffMemoryUtil.getActiveItemSummons(staffStack).size() >= StaffMemoryUtil.MAX_ACTIVE_ITEM_SUMMONS) {
            player.sendSystemMessage(Component.literal("The staff already has 20 items conjured at once."));
            return;
        }

        ItemStack stack = StaffMemoryUtil.reconstructItem(library.get(index), level.registryAccess());
        if (stack.isEmpty()) return;

        UUID trackingId = UUID.randomUUID();
        long expiry = level.getGameTime() + SUMMON_DURATION_TICKS;

        CompoundTag tag = new CompoundTag();
        tag.putLong("VoidSummonTime", expiry);
        tag.putUUID("VoidSummonOwner", player.getUUID());
        tag.putUUID("StaffTrackingId", trackingId);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

        if (!player.getInventory().add(stack)) player.drop(stack, false);

        chargeCost(level, player);
        StaffMemoryUtil.addActiveItemSummon(staffStack, new StaffMemoryUtil.ActiveItemSummon(
                trackingId, stack.getHoverName().getString(), expiry));

        ServerScheduler.scheduleDelayed(SUMMON_DURATION_TICKS, () -> {
            removeTrackedItem(player, trackingId);
            StaffVoidBlockTracker.removeAllFor(level, trackingId);
            StaffMemoryUtil.removeActiveItemSummon(staffStack, trackingId);
        }, level);
    }

    private static void removeTrackedItem(ServerPlayer player, UUID trackingId) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            CustomData data = stack.get(DataComponents.CUSTOM_DATA);
            if (data != null && data.copyTag().hasUUID("StaffTrackingId")
                    && data.copyTag().getUUID("StaffTrackingId").equals(trackingId)) {
                player.getInventory().removeItem(i, stack.getCount());
                return;
            }
        }
    }

    public static void withdraw(ServerLevel level, ServerPlayer player, ItemStack staffStack, UUID trackingId) {
        removeTrackedItem(player, trackingId);
        StaffVoidBlockTracker.removeAllFor(level, trackingId);
        StaffMemoryUtil.removeActiveItemSummon(staffStack, trackingId);
    }

    public static void forgetRecorded(ItemStack staffStack, int index) {
        StaffMemoryUtil.removeRecordedItem(staffStack, index);
    }

    private static void chargeCost(ServerLevel level, ServerPlayer player) {
        Ability historicalVoid = LOTMCraft.abilityHandler.getById(HISTORICAL_VOID_SUMMONING_ID);
        if (historicalVoid == null) return;
        float cost = historicalVoid.getInflatedSpiritualityCost(player, level);
        BeyonderData.reduceSpirituality(player, cost);
    }
}

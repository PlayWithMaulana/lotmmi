package com.Maul.lotmmi.network.packets.toServer;

import com.Maul.lotmmi.LotmMysticalItems;
import com.Maul.lotmmi.item.ModItems;
import com.Maul.lotmmi.item.custom.StaffTeleportUtil;
import com.Maul.lotmmi.item.custom.TeleportSlotUtil;
import com.Maul.lotmmi.network.packets.toClient.SyncStaffTeleportPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

public record StaffSlotActionPacket(boolean mainHand, int slotIndex, int action) implements CustomPacketPayload {

    public static final int ACTION_TELEPORT = 0;
    public static final int ACTION_SAVE = 1;
    public static final int ACTION_CLEAR = 2;

    public static final Type<StaffSlotActionPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(LotmMysticalItems.MOD_ID, "staff_slot_action"));

    public static final StreamCodec<FriendlyByteBuf, StaffSlotActionPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, StaffSlotActionPacket::mainHand,
            ByteBufCodecs.VAR_INT, StaffSlotActionPacket::slotIndex,
            ByteBufCodecs.VAR_INT, StaffSlotActionPacket::action,
            StaffSlotActionPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(StaffSlotActionPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (com.Maul.lotmmi.events.StaffCurseHandler.isCursed(player)) return;
            if (packet.slotIndex() < 0 || packet.slotIndex() >= TeleportSlotUtil.MAX_SLOTS) return;

            InteractionHand hand = packet.mainHand() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            ItemStack stack = player.getItemInHand(hand);
            if (!stack.is(ModItems.STAFF_OF_THE_STARS.get())) return;

            List<TeleportSlotUtil.Slot> slots = TeleportSlotUtil.getSlots(stack);
            TeleportSlotUtil.Slot current = packet.slotIndex() < slots.size() ? slots.get(packet.slotIndex()) : null;

            switch (packet.action()) {
                case ACTION_TELEPORT -> {
                    if (current != null) StaffTeleportUtil.teleportToSlot(player, current, slots);
                }
                case ACTION_SAVE -> {
                    StaffTeleportUtil.saveSlot(player, stack, packet.slotIndex());
                    SyncStaffTeleportPacket.sendTo(player, stack);
                }
                case ACTION_CLEAR -> {
                    TeleportSlotUtil.clearSlot(stack, packet.slotIndex());
                    SyncStaffTeleportPacket.sendTo(player, stack);
                }
                default -> {
                }
            }
        });
    }
}

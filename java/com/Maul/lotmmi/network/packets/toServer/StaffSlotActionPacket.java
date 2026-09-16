package com.Maul.lotmmi.network.packets.toServer;

import com.Maul.lotmmi.LotmMysticalItems;
import com.Maul.lotmmi.item.ModItems;
import com.Maul.lotmmi.item.custom.StaffTeleportUtil;
import com.Maul.lotmmi.item.custom.TeleportSlotUtil;
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

public record StaffSlotActionPacket(boolean mainHand, int slotIndex, boolean forceRecord) implements CustomPacketPayload {

    public static final Type<StaffSlotActionPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(LotmMysticalItems.MOD_ID, "staff_slot_action"));

    public static final StreamCodec<FriendlyByteBuf, StaffSlotActionPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, StaffSlotActionPacket::mainHand,
            ByteBufCodecs.VAR_INT, StaffSlotActionPacket::slotIndex,
            ByteBufCodecs.BOOL, StaffSlotActionPacket::forceRecord,
            StaffSlotActionPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(StaffSlotActionPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (packet.slotIndex() < 0 || packet.slotIndex() >= TeleportSlotUtil.MAX_SLOTS) return;

            InteractionHand hand = packet.mainHand() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            ItemStack stack = player.getItemInHand(hand);
            if (!stack.is(ModItems.STAFF_OF_THE_STARS.get())) return;

            List<TeleportSlotUtil.Slot> slots = TeleportSlotUtil.getSlots(stack);
            boolean occupied = packet.slotIndex() < slots.size() && slots.get(packet.slotIndex()) != null;

            if (occupied && !packet.forceRecord()) {
                StaffTeleportUtil.teleportToSlot(player, slots.get(packet.slotIndex()));
            } else {
                TeleportSlotUtil.recordSlot(stack, packet.slotIndex(), player.level().dimension(), player.position());
            }
        });
    }
}

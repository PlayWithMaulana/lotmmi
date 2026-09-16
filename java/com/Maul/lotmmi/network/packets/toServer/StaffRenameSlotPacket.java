package com.Maul.lotmmi.network.packets.toServer;

import com.Maul.lotmmi.LotmMysticalItems;
import com.Maul.lotmmi.item.ModItems;
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

public record StaffRenameSlotPacket(boolean mainHand, int slotIndex, String name) implements CustomPacketPayload {

    public static final int MAX_NAME_LENGTH = 24;

    public static final Type<StaffRenameSlotPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(LotmMysticalItems.MOD_ID, "staff_rename_slot"));

    public static final StreamCodec<FriendlyByteBuf, StaffRenameSlotPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, StaffRenameSlotPacket::mainHand,
            ByteBufCodecs.VAR_INT, StaffRenameSlotPacket::slotIndex,
            ByteBufCodecs.STRING_UTF8, StaffRenameSlotPacket::name,
            StaffRenameSlotPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(StaffRenameSlotPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (packet.slotIndex() < 0 || packet.slotIndex() >= TeleportSlotUtil.MAX_SLOTS) return;

            InteractionHand hand = packet.mainHand() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            ItemStack stack = player.getItemInHand(hand);
            if (!stack.is(ModItems.STAFF_OF_THE_STARS.get())) return;

            String name = packet.name();
            if (name.length() > MAX_NAME_LENGTH) name = name.substring(0, MAX_NAME_LENGTH);

            TeleportSlotUtil.renameSlot(stack, packet.slotIndex(), name);
        });
    }
}

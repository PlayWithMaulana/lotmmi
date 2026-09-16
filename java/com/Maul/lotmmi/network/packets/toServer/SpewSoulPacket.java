package com.Maul.lotmmi.network.packets.toServer;

import com.Maul.lotmmi.LotmMysticalItems;
import com.Maul.lotmmi.item.custom.CreepingHungerItem;
import com.Maul.lotmmi.network.packets.toClient.SyncItemIntrospectPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SpewSoulPacket(boolean mainHand, int slotIndex) implements CustomPacketPayload {

    public static final Type<SpewSoulPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(LotmMysticalItems.MOD_ID, "spew_soul"));

    public static final StreamCodec<FriendlyByteBuf, SpewSoulPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, SpewSoulPacket::mainHand,
            ByteBufCodecs.VAR_INT, SpewSoulPacket::slotIndex,
            SpewSoulPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SpewSoulPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            InteractionHand hand = packet.mainHand() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            ItemStack stack = player.getItemInHand(hand);
            if (!(stack.getItem() instanceof CreepingHungerItem)) return;

            var souls = CreepingHungerItem.getSouls(stack);
            if (packet.slotIndex() < 0 || packet.slotIndex() >= souls.size()) return;

            CreepingHungerItem.releaseSoul(player, stack, packet.slotIndex());
            SyncItemIntrospectPacket.sendTo(player, stack);
        });
    }
}

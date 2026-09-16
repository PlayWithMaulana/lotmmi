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

public record FeedReserveSoulPacket(boolean mainHand) implements CustomPacketPayload {

    public static final Type<FeedReserveSoulPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(LotmMysticalItems.MOD_ID, "feed_reserve_soul"));

    public static final StreamCodec<FriendlyByteBuf, FeedReserveSoulPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, FeedReserveSoulPacket::mainHand,
            FeedReserveSoulPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(FeedReserveSoulPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            InteractionHand hand = packet.mainHand() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            ItemStack stack = player.getItemInHand(hand);
            if (!(stack.getItem() instanceof CreepingHungerItem)) return;

            CreepingHungerItem.feedReserveNow(player, stack);
            SyncItemIntrospectPacket.sendTo(player, stack);
        });
    }
}

package com.Maul.lotmmi.network.packets.toServer;

import com.Maul.lotmmi.LotmMysticalItems;
import com.Maul.lotmmi.item.custom.SeaGodScepterItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record OpenScepterIntrospectPacket(boolean mainHand) implements CustomPacketPayload {

    public static final Type<OpenScepterIntrospectPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(LotmMysticalItems.MOD_ID, "open_scepter_introspect"));

    public static final StreamCodec<FriendlyByteBuf, OpenScepterIntrospectPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, OpenScepterIntrospectPacket::mainHand,
            OpenScepterIntrospectPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenScepterIntrospectPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (com.Maul.lotmmi.events.StaffCurseHandler.isCursed(player)) return;

            InteractionHand hand = packet.mainHand() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            ItemStack stack = player.getItemInHand(hand);
            if (!(stack.getItem() instanceof SeaGodScepterItem)) return;

            SeaGodScepterItem.openIntrospectMenu(player, stack, packet.mainHand());
        });
    }
}

package com.Maul.lotmmi.network.packets.toServer;

import com.Maul.lotmmi.LotmMysticalItems;
import com.Maul.lotmmi.item.ModItems;
import com.Maul.lotmmi.item.custom.StaffTeleportUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record StaffTypedTeleportPacket(boolean mainHand, double x, double y, double z) implements CustomPacketPayload {

    public static final Type<StaffTypedTeleportPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(LotmMysticalItems.MOD_ID, "staff_typed_teleport"));

    public static final StreamCodec<FriendlyByteBuf, StaffTypedTeleportPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, StaffTypedTeleportPacket::mainHand,
            ByteBufCodecs.DOUBLE, StaffTypedTeleportPacket::x,
            ByteBufCodecs.DOUBLE, StaffTypedTeleportPacket::y,
            ByteBufCodecs.DOUBLE, StaffTypedTeleportPacket::z,
            StaffTypedTeleportPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(StaffTypedTeleportPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            InteractionHand hand = packet.mainHand() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            ItemStack stack = player.getItemInHand(hand);
            if (!stack.is(ModItems.STAFF_OF_THE_STARS.get())) return;

            StaffTeleportUtil.Result result = StaffTeleportUtil.attemptTypedTeleport(player, packet.x(), packet.y(), packet.z());

            Component message = switch (result) {
                case SUCCESS -> Component.literal("The path was true.").withStyle(ChatFormatting.GRAY);
                case RANDOM_PLACE -> Component.literal("The stars pulled you elsewhere.").withStyle(ChatFormatting.RED);
                case SPIRIT_WORLD -> Component.literal("You slipped into the Spirit World.").withStyle(ChatFormatting.DARK_PURPLE);
            };
            player.displayClientMessage(message, true);
        });
    }
}

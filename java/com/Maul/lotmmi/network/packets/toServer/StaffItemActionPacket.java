package com.Maul.lotmmi.network.packets.toServer;

import com.Maul.lotmmi.LotmMysticalItems;
import com.Maul.lotmmi.item.ModItems;
import com.Maul.lotmmi.item.custom.StaffItemSummonUtil;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public record StaffItemActionPacket(boolean mainHand, int action, int libraryIndex, UUID trackingId) implements CustomPacketPayload {

    public static final int ACTION_OPEN_CAPTURE = 0;
    public static final int ACTION_SUMMON_FROM_LIBRARY = 1;
    public static final int ACTION_FORGET_FROM_LIBRARY = 2;
    public static final int ACTION_WITHDRAW = 3;

    public static final Type<StaffItemActionPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(LotmMysticalItems.MOD_ID, "staff_item_action"));

    public static final StreamCodec<FriendlyByteBuf, StaffItemActionPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, StaffItemActionPacket::mainHand,
            ByteBufCodecs.VAR_INT, StaffItemActionPacket::action,
            ByteBufCodecs.VAR_INT, StaffItemActionPacket::libraryIndex,
            UUIDUtil.STREAM_CODEC, StaffItemActionPacket::trackingId,
            StaffItemActionPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(StaffItemActionPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (!(player.level() instanceof ServerLevel level)) return;

            InteractionHand hand = packet.mainHand() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            ItemStack stack = player.getItemInHand(hand);
            if (!stack.is(ModItems.STAFF_OF_THE_STARS.get())) return;

            switch (packet.action()) {
                case ACTION_OPEN_CAPTURE -> StaffItemSummonUtil.openCaptureMenu(player, stack, packet.mainHand());
                case ACTION_SUMMON_FROM_LIBRARY -> StaffItemSummonUtil.summonFromLibrary(level, player, stack, packet.libraryIndex());
                case ACTION_FORGET_FROM_LIBRARY -> StaffItemSummonUtil.forgetRecorded(stack, packet.libraryIndex());
                case ACTION_WITHDRAW -> StaffItemSummonUtil.withdraw(level, player, stack, packet.trackingId());
            }
        });
    }
}

package com.Maul.lotmmi.network.packets.toServer;

import com.Maul.lotmmi.LotmMysticalItems;
import com.Maul.lotmmi.item.ModItems;
import com.Maul.lotmmi.item.custom.StaffEntitySummonUtil;
import com.Maul.lotmmi.item.custom.StaffMemoryUtil;
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

import java.util.List;
import java.util.UUID;

public record StaffEntityActionPacket(boolean mainHand, int action, UUID targetUUID) implements CustomPacketPayload {

    public static final int ACTION_SUMMON = 0;
    public static final int ACTION_WITHDRAW = 1;
    public static final int ACTION_FORGET = 2;

    public static final Type<StaffEntityActionPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(LotmMysticalItems.MOD_ID, "staff_entity_action"));

    public static final StreamCodec<FriendlyByteBuf, StaffEntityActionPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, StaffEntityActionPacket::mainHand,
            ByteBufCodecs.VAR_INT, StaffEntityActionPacket::action,
            UUIDUtil.STREAM_CODEC, StaffEntityActionPacket::targetUUID,
            StaffEntityActionPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(StaffEntityActionPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (!(player.level() instanceof ServerLevel level)) return;

            InteractionHand hand = packet.mainHand() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            ItemStack stack = player.getItemInHand(hand);
            if (!stack.is(ModItems.STAFF_OF_THE_STARS.get())) return;

            switch (packet.action()) {
                case ACTION_WITHDRAW -> StaffEntitySummonUtil.withdraw(level, stack, packet.targetUUID());
                case ACTION_FORGET -> StaffMemoryUtil.removeWatch(stack, packet.targetUUID());
                case ACTION_SUMMON -> {
                    List<StaffMemoryUtil.EntityWatch> watches = StaffMemoryUtil.getEntityWatches(stack);
                    for (StaffMemoryUtil.EntityWatch watch : watches) {
                        if (watch.sourceUUID().equals(packet.targetUUID())) {
                            StaffEntitySummonUtil.summon(level, player, stack, watch);
                            return;
                        }
                    }
                }
            }
        });
    }
}

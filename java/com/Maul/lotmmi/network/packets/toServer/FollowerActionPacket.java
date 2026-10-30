package com.Maul.lotmmi.network.packets.toServer;

import com.Maul.lotmmi.LotmMysticalItems;
import com.Maul.lotmmi.item.custom.ScepterFollowerUtil;
import com.Maul.lotmmi.item.custom.SeaGodScepterItem;
import com.Maul.lotmmi.network.packets.toClient.SyncScepterIntrospectPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public record FollowerActionPacket(boolean mainHand, UUID followerId, int action) implements CustomPacketPayload {

    public static final int ACTION_BLESS = 0;
    public static final int ACTION_ATTACK = 1;
    public static final int ACTION_TOGGLE_SUMMON = 2;
    public static final int ACTION_REMOVE = 3;

    public static final Type<FollowerActionPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(LotmMysticalItems.MOD_ID, "follower_action"));

    public static final StreamCodec<FriendlyByteBuf, FollowerActionPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, FollowerActionPacket::mainHand,
            ByteBufCodecs.STRING_UTF8.map(UUID::fromString, UUID::toString), FollowerActionPacket::followerId,
            ByteBufCodecs.VAR_INT, FollowerActionPacket::action,
            FollowerActionPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(FollowerActionPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (com.Maul.lotmmi.events.StaffCurseHandler.isCursed(player)) return;

            InteractionHand hand = packet.mainHand() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            ItemStack stack = player.getItemInHand(hand);
            if (!(stack.getItem() instanceof SeaGodScepterItem)) return;

            switch (packet.action()) {
                case ACTION_BLESS -> SeaGodScepterItem.blessFollower(player, stack, packet.followerId());
                case ACTION_ATTACK -> SeaGodScepterItem.attackFollower(player, stack, packet.followerId());
                case ACTION_TOGGLE_SUMMON -> SeaGodScepterItem.toggleSummon(player, stack, packet.followerId());
                case ACTION_REMOVE -> ScepterFollowerUtil.removeFollower(stack, packet.followerId());
                default -> {
                }
            }

            SyncScepterIntrospectPacket.sendTo(player, packet.mainHand(), stack);
        });
    }
}

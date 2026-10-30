package com.Maul.lotmmi.network.packets.toServer;

import com.Maul.lotmmi.LotmMysticalItems;
import com.Maul.lotmmi.item.custom.ScepterPrayerUtil;
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

public record PrayerActionPacket(boolean mainHand, UUID prayerId, int action) implements CustomPacketPayload {

    public static final int ACTION_GAZE = 0;
    public static final int ACTION_DISMISS = 1;
    public static final int ACTION_ANSWER = 2;

    public static final Type<PrayerActionPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(LotmMysticalItems.MOD_ID, "prayer_action"));

    public static final StreamCodec<FriendlyByteBuf, PrayerActionPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, PrayerActionPacket::mainHand,
            ByteBufCodecs.STRING_UTF8.map(UUID::fromString, UUID::toString), PrayerActionPacket::prayerId,
            ByteBufCodecs.VAR_INT, PrayerActionPacket::action,
            PrayerActionPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PrayerActionPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (com.Maul.lotmmi.events.StaffCurseHandler.isCursed(player)) return;

            InteractionHand hand = packet.mainHand() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            ItemStack stack = player.getItemInHand(hand);
            if (!(stack.getItem() instanceof SeaGodScepterItem)) return;

            switch (packet.action()) {
                case ACTION_GAZE -> SeaGodScepterItem.gazeAtPrayer(player, stack, packet.prayerId());
                case ACTION_DISMISS -> ScepterPrayerUtil.removePrayerById(stack, packet.prayerId());
                case ACTION_ANSWER -> SeaGodScepterItem.answerPrayer(player, stack, packet.prayerId());
                default -> {
                }
            }

            SyncScepterIntrospectPacket.sendTo(player, packet.mainHand(), stack);
        });
    }
}

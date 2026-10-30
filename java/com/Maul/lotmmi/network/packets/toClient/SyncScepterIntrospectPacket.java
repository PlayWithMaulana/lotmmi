package com.Maul.lotmmi.network.packets.toClient;

import com.Maul.lotmmi.LotmMysticalItems;
import com.Maul.lotmmi.gui.ScepterIntrospectMenuProvider;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

public record SyncScepterIntrospectPacket(boolean mainHand, List<String> lines, List<String> prayerLines,
                                          boolean canSummon, boolean canGaze, int charmCost)
        implements CustomPacketPayload {

    private static final StreamCodec<ByteBuf, List<String>> STRINGS =
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list());

    public static final Type<SyncScepterIntrospectPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(LotmMysticalItems.MOD_ID, "sync_scepter_introspect"));

    public static final StreamCodec<FriendlyByteBuf, SyncScepterIntrospectPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, SyncScepterIntrospectPacket::mainHand,
            STRINGS, SyncScepterIntrospectPacket::lines,
            STRINGS, SyncScepterIntrospectPacket::prayerLines,
            ByteBufCodecs.BOOL, SyncScepterIntrospectPacket::canSummon,
            ByteBufCodecs.BOOL, SyncScepterIntrospectPacket::canGaze,
            ByteBufCodecs.VAR_INT, SyncScepterIntrospectPacket::charmCost,
            SyncScepterIntrospectPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void sendTo(ServerPlayer player, boolean mainHand, ItemStack scepter) {
        ScepterIntrospectMenuProvider.Snapshot snapshot = ScepterIntrospectMenuProvider.snapshot(player, scepter, mainHand);
        PacketDistributor.sendToPlayer(player, new SyncScepterIntrospectPacket(mainHand, snapshot.followerLines(),
                snapshot.prayerLines(), snapshot.canSummon(), snapshot.canGaze(), snapshot.charmCost()));
    }

    public static void handle(SyncScepterIntrospectPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> com.Maul.lotmmi.client.ClientPacketHandler.syncScepterIntrospect(packet));
    }
}

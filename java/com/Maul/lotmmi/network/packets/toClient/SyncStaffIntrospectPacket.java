package com.Maul.lotmmi.network.packets.toClient;

import com.Maul.lotmmi.LotmMysticalItems;
import com.Maul.lotmmi.item.custom.StaffAbilityUtil;
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

public record SyncStaffIntrospectPacket(List<String> libraryIds, List<String> libraryLabels,
                                        List<String> wheelIds, List<String> wheelLabels)
        implements CustomPacketPayload {

    private static final StreamCodec<ByteBuf, List<String>> STRINGS =
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list());

    public static final Type<SyncStaffIntrospectPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(LotmMysticalItems.MOD_ID, "sync_staff_introspect"));

    public static final StreamCodec<FriendlyByteBuf, SyncStaffIntrospectPacket> STREAM_CODEC = StreamCodec.composite(
            STRINGS, SyncStaffIntrospectPacket::libraryIds,
            STRINGS, SyncStaffIntrospectPacket::libraryLabels,
            STRINGS, SyncStaffIntrospectPacket::wheelIds,
            STRINGS, SyncStaffIntrospectPacket::wheelLabels,
            SyncStaffIntrospectPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void sendTo(ServerPlayer player, ItemStack staff) {
        StaffAbilityUtil.IntrospectState state = StaffAbilityUtil.buildIntrospectState(staff);
        PacketDistributor.sendToPlayer(player, new SyncStaffIntrospectPacket(
                state.libraryIds(), state.libraryLabels(), state.wheelIds(), state.wheelLabels()));
    }

    public static void handle(SyncStaffIntrospectPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> com.Maul.lotmmi.client.ClientPacketHandler.syncStaffIntrospect(packet));
    }
}

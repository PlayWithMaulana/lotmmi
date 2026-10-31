package com.Maul.lotmmi.network.packets.toClient;

import com.Maul.lotmmi.LotmMysticalItems;
import com.Maul.lotmmi.item.custom.StaffTeleportUtil;
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

public record SyncStaffTeleportPacket(List<String> slotDetails) implements CustomPacketPayload {

    private static final StreamCodec<ByteBuf, List<String>> STRINGS =
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list());

    public static final Type<SyncStaffTeleportPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(LotmMysticalItems.MOD_ID, "sync_staff_teleport"));

    public static final StreamCodec<FriendlyByteBuf, SyncStaffTeleportPacket> STREAM_CODEC = StreamCodec.composite(
            STRINGS, SyncStaffTeleportPacket::slotDetails,
            SyncStaffTeleportPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void sendTo(ServerPlayer player, ItemStack staff) {
        PacketDistributor.sendToPlayer(player, new SyncStaffTeleportPacket(StaffTeleportUtil.buildSlotDetails(staff)));
    }

    public static void handle(SyncStaffTeleportPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> com.Maul.lotmmi.client.ClientPacketHandler.syncStaffTeleport(packet));
    }
}

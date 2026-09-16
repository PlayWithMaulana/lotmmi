package com.Maul.lotmmi.network.packets.toClient;

import com.Maul.lotmmi.LotmMysticalItems;
import com.Maul.lotmmi.data.ModDataComponents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SyncItemIntrospectPacket(String soulsRaw, String foodRaw, String abilityOrderRaw, long lastFedTick)
        implements CustomPacketPayload {

    public static final Type<SyncItemIntrospectPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(LotmMysticalItems.MOD_ID, "sync_item_introspect"));

    public static final StreamCodec<FriendlyByteBuf, SyncItemIntrospectPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, SyncItemIntrospectPacket::soulsRaw,
            ByteBufCodecs.STRING_UTF8, SyncItemIntrospectPacket::foodRaw,
            ByteBufCodecs.STRING_UTF8, SyncItemIntrospectPacket::abilityOrderRaw,
            ByteBufCodecs.VAR_LONG, SyncItemIntrospectPacket::lastFedTick,
            SyncItemIntrospectPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void sendTo(ServerPlayer player, ItemStack glove) {
        String soulsRaw = glove.getOrDefault(ModDataComponents.SOULS.get(), "");
        String foodRaw = glove.getOrDefault(ModDataComponents.FOOD_SOUL.get(), "");
        String abilityOrderRaw = glove.getOrDefault(ModDataComponents.ABILITY_ORDER.get(), "");
        long lastFedTick = glove.getOrDefault(ModDataComponents.LAST_FED_TICK.get(), player.level().getGameTime());

        PacketDistributor.sendToPlayer(player, new SyncItemIntrospectPacket(soulsRaw, foodRaw, abilityOrderRaw, lastFedTick));
    }

    public static void handle(SyncItemIntrospectPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> com.Maul.lotmmi.client.ClientPacketHandler.syncItemIntrospect(packet));
    }
}

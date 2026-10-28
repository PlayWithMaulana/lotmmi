package com.Maul.lotmmi.network.packets.toServer;

import com.Maul.lotmmi.LotmMysticalItems;
import com.Maul.lotmmi.item.custom.AwakenedCreepingHungerItem;
import com.Maul.lotmmi.item.custom.CreepingHungerItem;
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

public record CycleSoulPacket(int direction) implements CustomPacketPayload {

    public static final Type<CycleSoulPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(LotmMysticalItems.MOD_ID, "cycle_soul"));

    public static final StreamCodec<FriendlyByteBuf, CycleSoulPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, CycleSoulPacket::direction,
            CycleSoulPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CycleSoulPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            ItemStack glove = findAwakenedGlove(player);
            if (glove.isEmpty()) return;

            int before = CreepingHungerItem.getActiveSoulIndex(glove);
            int after = CreepingHungerItem.cycleSoul(glove, packet.direction());

            if (after < 0) {
                sendActionBar(player, "No soul offers a usable ability.", ChatFormatting.RED);
                return;
            }
            if (after == before) {
                sendActionBar(player, "No other soul has a usable ability.", ChatFormatting.GRAY);
                return;
            }

            CreepingHungerItem.SoulSlot soul = CreepingHungerItem.getSouls(glove).get(after);
            sendActionBar(player, "Soul " + (after + 1) + ": " + soul.ownerName() + " - "
                    + CreepingHungerItem.formatPathway(soul.pathway()) + " Sequence " + soul.sequence(),
                    ChatFormatting.LIGHT_PURPLE);
        });
    }

    private static ItemStack findAwakenedGlove(ServerPlayer player) {
        ItemStack main = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (main.getItem() instanceof AwakenedCreepingHungerItem) return main;
        ItemStack off = player.getItemInHand(InteractionHand.OFF_HAND);
        if (off.getItem() instanceof AwakenedCreepingHungerItem) return off;
        return ItemStack.EMPTY;
    }

    private static void sendActionBar(ServerPlayer player, String message, ChatFormatting color) {
        player.displayClientMessage(Component.literal(message).withStyle(color), true);
    }
}

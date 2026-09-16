package com.Maul.lotmmi.network.packets.toServer;

import com.Maul.lotmmi.LotmMysticalItems;
import com.Maul.lotmmi.item.custom.CreepingHungerItem;
import com.Maul.lotmmi.network.packets.toClient.SyncItemIntrospectPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ReorderAbilityPacket(boolean mainHand, String abilityId, int direction) implements CustomPacketPayload {

    public static final Type<ReorderAbilityPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(LotmMysticalItems.MOD_ID, "reorder_ability"));

    public static final StreamCodec<FriendlyByteBuf, ReorderAbilityPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, ReorderAbilityPacket::mainHand,
            ByteBufCodecs.STRING_UTF8, ReorderAbilityPacket::abilityId,
            ByteBufCodecs.VAR_INT, ReorderAbilityPacket::direction,
            ReorderAbilityPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ReorderAbilityPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            InteractionHand hand = packet.mainHand() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            ItemStack stack = player.getItemInHand(hand);
            if (!(stack.getItem() instanceof CreepingHungerItem)) return;

            CreepingHungerItem.reorderAbility(stack, packet.abilityId(), packet.direction());
            SyncItemIntrospectPacket.sendTo(player, stack);
        });
    }
}

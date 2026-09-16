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

public record ToggleAbilityPacket(boolean mainHand, String abilityId) implements CustomPacketPayload {

    public static final Type<ToggleAbilityPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(LotmMysticalItems.MOD_ID, "toggle_ability"));

    public static final StreamCodec<FriendlyByteBuf, ToggleAbilityPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, ToggleAbilityPacket::mainHand,
            ByteBufCodecs.STRING_UTF8, ToggleAbilityPacket::abilityId,
            ToggleAbilityPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ToggleAbilityPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            InteractionHand hand = packet.mainHand() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            ItemStack stack = player.getItemInHand(hand);
            if (!(stack.getItem() instanceof CreepingHungerItem)) return;

            CreepingHungerItem.toggleAbility(stack, packet.abilityId());
            SyncItemIntrospectPacket.sendTo(player, stack);
        });
    }
}

package com.Maul.lotmmi.network.packets.toServer;

import com.Maul.lotmmi.LotmMysticalItems;
import com.Maul.lotmmi.item.custom.AwakenedCreepingHungerItem;
import de.jakob.lotm.beyonders.abilities.core.Ability;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ChooseGrazeAbilityPacket(String abilityId) implements CustomPacketPayload {

    public static final Type<ChooseGrazeAbilityPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(LotmMysticalItems.MOD_ID, "choose_graze_ability"));

    public static final StreamCodec<FriendlyByteBuf, ChooseGrazeAbilityPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ChooseGrazeAbilityPacket::abilityId,
            ChooseGrazeAbilityPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ChooseGrazeAbilityPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            AwakenedCreepingHungerItem.PendingGraze pending = AwakenedCreepingHungerItem.PENDING.remove(player.getUUID());
            if (pending == null) return;

            Ability chosen = pending.candidates().stream()
                    .filter(a -> a.getId().equals(packet.abilityId()))
                    .findFirst()
                    .orElse(null);
            if (chosen == null) return;

            ItemStack main = player.getItemInHand(InteractionHand.MAIN_HAND);
            ItemStack off = player.getItemInHand(InteractionHand.OFF_HAND);
            ItemStack glove;
            if (main.getItem() instanceof AwakenedCreepingHungerItem) {
                glove = main;
            } else if (off.getItem() instanceof AwakenedCreepingHungerItem) {
                glove = off;
            } else {
                return;
            }

            AwakenedCreepingHungerItem.confirmGrazeChoice(player, glove, pending, chosen);
        });
    }
}

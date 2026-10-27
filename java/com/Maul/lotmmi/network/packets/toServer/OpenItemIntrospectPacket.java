package com.Maul.lotmmi.network.packets.toServer;

import com.Maul.lotmmi.LotmMysticalItems;
import com.Maul.lotmmi.data.ModDataComponents;
import com.Maul.lotmmi.gui.ItemIntrospectMenuProvider;
import com.Maul.lotmmi.item.custom.AwakenedCreepingHungerItem;
import com.Maul.lotmmi.item.custom.CreepingHungerItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record OpenItemIntrospectPacket(boolean mainHand) implements CustomPacketPayload {

    public static final Type<OpenItemIntrospectPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(LotmMysticalItems.MOD_ID, "open_item_introspect"));

    public static final StreamCodec<FriendlyByteBuf, OpenItemIntrospectPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, OpenItemIntrospectPacket::mainHand,
            OpenItemIntrospectPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenItemIntrospectPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            InteractionHand hand = packet.mainHand() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            ItemStack stack = player.getItemInHand(hand);
            if (!(stack.getItem() instanceof CreepingHungerItem item)) return;

            long now = player.level().getGameTime();
            int maxSlots = item.getMaxSlots();
            String soulsRaw = stack.getOrDefault(ModDataComponents.SOULS.get(), "");
            String foodRaw = stack.getOrDefault(ModDataComponents.FOOD_SOUL.get(), "");
            String abilityOrderRaw = CreepingHungerItem.getActiveAbilityOrderRaw(stack);
            long feedRemaining = CreepingHungerItem.getTicksUntilNextFeed(stack, now);
            long prayerRemaining = AwakenedCreepingHungerItem.prayerRemainingFor(stack, now);
            int activeSoul = CreepingHungerItem.getActiveSoulIndex(stack);

            player.openMenu(
                    new ItemIntrospectMenuProvider(packet.mainHand(), maxSlots, soulsRaw, foodRaw, abilityOrderRaw,
                            feedRemaining, prayerRemaining, activeSoul),
                    buf -> {
                        buf.writeBoolean(packet.mainHand());
                        buf.writeVarInt(maxSlots);
                        buf.writeUtf(soulsRaw);
                        buf.writeUtf(foodRaw);
                        buf.writeUtf(abilityOrderRaw);
                        buf.writeVarLong(feedRemaining);
                        buf.writeVarLong(prayerRemaining);
                        buf.writeVarInt(activeSoul);
                    });
        });
    }
}

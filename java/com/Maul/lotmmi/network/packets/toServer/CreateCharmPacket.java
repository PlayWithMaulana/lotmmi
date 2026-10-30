package com.Maul.lotmmi.network.packets.toServer;

import com.Maul.lotmmi.LotmMysticalItems;
import com.Maul.lotmmi.config.LotmmiConfig;
import com.Maul.lotmmi.data.ModDataComponents;
import com.Maul.lotmmi.item.ModItems;
import com.Maul.lotmmi.item.custom.CharmType;
import com.Maul.lotmmi.item.custom.SeaGodScepterItem;
import de.jakob.lotm.util.BeyonderData;
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

public record CreateCharmPacket(boolean mainHand, int abilityIndex) implements CustomPacketPayload {

    public static final Type<CreateCharmPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(LotmMysticalItems.MOD_ID, "create_charm"));

    public static final StreamCodec<FriendlyByteBuf, CreateCharmPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, CreateCharmPacket::mainHand,
            ByteBufCodecs.VAR_INT, CreateCharmPacket::abilityIndex,
            CreateCharmPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CreateCharmPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (com.Maul.lotmmi.events.StaffCurseHandler.isCursed(player)) return;

            CharmType[] types = CharmType.values();
            if (packet.abilityIndex() < 0 || packet.abilityIndex() >= types.length) return;
            CharmType type = types[packet.abilityIndex()];

            InteractionHand scepterHand = packet.mainHand() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            InteractionHand materialHand = packet.mainHand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;

            ItemStack scepter = player.getItemInHand(scepterHand);
            ItemStack material = player.getItemInHand(materialHand);

            if (!(scepter.getItem() instanceof SeaGodScepterItem) || material.isEmpty()) {
                player.displayClientMessage(Component.literal("Hold an item in your off hand to enchant it.")
                        .withStyle(ChatFormatting.GRAY), true);
                return;
            }
            if (material.getItem() instanceof SeaGodScepterItem) {
                return;
            }

            float cost = LotmmiConfig.CHARM_SPIRITUALITY_COST.get().floatValue();
            if (BeyonderData.getSpirituality(player) < cost) {
                player.displayClientMessage(Component.literal("You need " + Math.round(cost) + " spirituality to enchant this.")
                        .withStyle(ChatFormatting.RED), true);
                return;
            }

            BeyonderData.reduceSpirituality(player, cost);
            material.shrink(1);

            ItemStack charm = new ItemStack(ModItems.SEA_GOD_CHARM.get());
            charm.set(ModDataComponents.CHARM_ABILITY_ID.get(), type.id());
            charm.set(ModDataComponents.CHARM_CREATED_TICK.get(), player.level().getGameTime());
            charm.set(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA,
                    new net.minecraft.world.item.component.CustomModelData(type.modelIndex()));

            if (!player.getInventory().add(charm)) {
                player.drop(charm, false);
            }

            player.displayClientMessage(Component.literal(type.displayName() + " charm bound.")
                    .withStyle(ChatFormatting.LIGHT_PURPLE), true);
        });
    }
}

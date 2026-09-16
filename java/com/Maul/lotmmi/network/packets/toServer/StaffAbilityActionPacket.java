package com.Maul.lotmmi.network.packets.toServer;

import com.Maul.lotmmi.LotmMysticalItems;
import com.Maul.lotmmi.item.ModItems;
import com.Maul.lotmmi.item.custom.StaffAbilityUtil;
import com.Maul.lotmmi.item.custom.StaffMemoryUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record StaffAbilityActionPacket(boolean mainHand, int action, String abilityId, int index) implements CustomPacketPayload {

    public static final int ACTION_ADD_TO_WHEEL = 0;
    public static final int ACTION_REMOVE_FROM_WHEEL = 1;
    public static final int ACTION_MOVE_UP = 2;
    public static final int ACTION_MOVE_DOWN = 3;
    public static final int ACTION_SELECT = 4;
    public static final int ACTION_FORGET = 5;

    public static final Type<StaffAbilityActionPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(LotmMysticalItems.MOD_ID, "staff_ability_action"));

    public static final StreamCodec<FriendlyByteBuf, StaffAbilityActionPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, StaffAbilityActionPacket::mainHand,
            ByteBufCodecs.VAR_INT, StaffAbilityActionPacket::action,
            ByteBufCodecs.STRING_UTF8, StaffAbilityActionPacket::abilityId,
            ByteBufCodecs.VAR_INT, StaffAbilityActionPacket::index,
            StaffAbilityActionPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(StaffAbilityActionPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            InteractionHand hand = packet.mainHand() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            ItemStack stack = player.getItemInHand(hand);
            if (!stack.is(ModItems.STAFF_OF_THE_STARS.get())) return;

            switch (packet.action()) {
                case ACTION_ADD_TO_WHEEL -> {
                    StaffMemoryUtil.addToWheel(stack, packet.abilityId());
                    StaffAbilityUtil.rebuildSealedArtifactData(stack);
                }
                case ACTION_REMOVE_FROM_WHEEL -> {
                    StaffMemoryUtil.removeFromWheel(stack, packet.abilityId());
                    StaffAbilityUtil.rebuildSealedArtifactData(stack);
                }
                case ACTION_MOVE_UP -> {
                    StaffMemoryUtil.moveInWheel(stack, packet.index(), -1);
                    StaffAbilityUtil.rebuildSealedArtifactData(stack);
                }
                case ACTION_MOVE_DOWN -> {
                    StaffMemoryUtil.moveInWheel(stack, packet.index(), 1);
                    StaffAbilityUtil.rebuildSealedArtifactData(stack);
                }
                case ACTION_SELECT -> StaffAbilityUtil.selectAbility(stack, packet.abilityId());
                case ACTION_FORGET -> {
                    StaffMemoryUtil.forgetAbility(stack, packet.abilityId());
                    StaffAbilityUtil.rebuildSealedArtifactData(stack);
                }
            }
        });
    }
}

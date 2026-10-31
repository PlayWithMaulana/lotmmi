package com.Maul.lotmmi.network.packets.toServer;

import com.Maul.lotmmi.LotmMysticalItems;
import com.Maul.lotmmi.gui.StaffIntrospectMenuProvider;
import com.Maul.lotmmi.item.ModItems;
import com.Maul.lotmmi.item.custom.StaffAbilityUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

public record OpenStaffIntrospectPacket(boolean mainHand) implements CustomPacketPayload {

    public static final Type<OpenStaffIntrospectPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(LotmMysticalItems.MOD_ID, "open_staff_introspect"));

    public static final StreamCodec<FriendlyByteBuf, OpenStaffIntrospectPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, OpenStaffIntrospectPacket::mainHand,
            OpenStaffIntrospectPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenStaffIntrospectPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (com.Maul.lotmmi.events.StaffCurseHandler.isCursed(player)) return;

            InteractionHand hand = packet.mainHand() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            ItemStack stack = player.getItemInHand(hand);
            if (!stack.is(ModItems.STAFF_OF_THE_STARS.get())) return;

            StaffAbilityUtil.IntrospectState state = StaffAbilityUtil.buildIntrospectState(stack);

            player.openMenu(new StaffIntrospectMenuProvider(packet.mainHand(), state.libraryIds(), state.libraryLabels(),
                            state.wheelIds(), state.wheelLabels()),
                    buf -> {
                        buf.writeBoolean(packet.mainHand());
                        writeStrings(buf, state.libraryIds());
                        writeStrings(buf, state.libraryLabels());
                        writeStrings(buf, state.wheelIds());
                        writeStrings(buf, state.wheelLabels());
                    });
        });
    }

    private static void writeStrings(RegistryFriendlyByteBuf buf, List<String> list) {
        buf.writeVarInt(list.size());
        for (String s : list) buf.writeUtf(s);
    }
}

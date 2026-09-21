package com.Maul.lotmmi.network.packets.toServer;

import com.Maul.lotmmi.LotmMysticalItems;
import com.Maul.lotmmi.gui.StaffIntrospectMenuProvider;
import com.Maul.lotmmi.item.ModItems;
import com.Maul.lotmmi.item.custom.StaffMemoryUtil;
import de.jakob.lotm.LOTMCraft;
import de.jakob.lotm.beyonders.abilities.core.Ability;
import de.jakob.lotm.beyonders.artifacts.SealedArtifactData;
import de.jakob.lotm.data.ModDataComponents;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

            InteractionHand hand = packet.mainHand() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            ItemStack stack = player.getItemInHand(hand);
            if (!stack.is(ModItems.STAFF_OF_THE_STARS.get())) return;

            List<String> libraryIds = new ArrayList<>();
            List<String> libraryLabels = new ArrayList<>();
            for (String id : StaffMemoryUtil.getWitnessedAbilityIds(stack)) {
                Ability ability = LOTMCraft.abilityHandler.getById(id);
                if (ability == null) continue;
                Map<String, Integer> req = ability.getRequirements();
                int sequence = req.isEmpty() ? 9 : req.values().iterator().next();
                double accuracy = StaffMemoryUtil.getAccuracy(stack, id, sequence);
                libraryIds.add(id);
                libraryLabels.add(ability.getName().getString() + " - " + Math.round(accuracy * 100) + "% (Seq " + sequence + ")");
            }

            List<String> wheelIds = new ArrayList<>();
            List<String> wheelLabels = new ArrayList<>();
            SealedArtifactData data = stack.get(ModDataComponents.SEALED_ARTIFACT_DATA.get());
            if (data != null) {
                for (Ability ability : data.abilities()) {
                    wheelIds.add(ability.getId());
                    wheelLabels.add(ability.getName().getString());
                }
            }

            player.openMenu(new StaffIntrospectMenuProvider(packet.mainHand(), libraryIds, libraryLabels, wheelIds, wheelLabels),
                    buf -> {
                        buf.writeBoolean(packet.mainHand());
                        writeStrings(buf, libraryIds);
                        writeStrings(buf, libraryLabels);
                        writeStrings(buf, wheelIds);
                        writeStrings(buf, wheelLabels);
                    });
        });
    }

    private static void writeStrings(RegistryFriendlyByteBuf buf, List<String> list) {
        buf.writeVarInt(list.size());
        for (String s : list) buf.writeUtf(s);
    }
}

package com.Maul.lotmmi.network.packets.toServer;

import com.Maul.lotmmi.LotmMysticalItems;
import com.Maul.lotmmi.gui.StaffIntrospectMenuProvider;
import com.Maul.lotmmi.item.ModItems;
import com.Maul.lotmmi.item.custom.StaffMemoryUtil;
import de.jakob.lotm.LOTMCraft;
import de.jakob.lotm.beyonders.abilities.core.Ability;
import de.jakob.lotm.beyonders.artifacts.SealedArtifactData;
import de.jakob.lotm.data.ModDataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
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
            if (!(player.level() instanceof ServerLevel level)) return;

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

            List<String> entityWatchIds = new ArrayList<>();
            List<String> entityWatchLabels = new ArrayList<>();
            List<Boolean> entityEligible = new ArrayList<>();
            for (StaffMemoryUtil.EntityWatch watch : StaffMemoryUtil.getEntityWatches(stack)) {
                boolean eligible = watch.watchedTicks() >= StaffMemoryUtil.STARE_UNLOCK_TICKS;
                double progress = Math.min(1.0, watch.watchedTicks() / (double) StaffMemoryUtil.STARE_MAX_TICKS);
                entityWatchIds.add(watch.sourceUUID().toString());
                entityWatchLabels.add(watch.displayName() + " - " + watch.pathway() + " Seq " + watch.sequence()
                        + (eligible ? " (" + Math.round(progress * 100) + "%)" : " (watching...)"));
                entityEligible.add(eligible);
            }

            List<String> entityActiveIds = new ArrayList<>();
            List<String> entityActiveLabels = new ArrayList<>();
            for (StaffMemoryUtil.ActiveEntitySummon summon : StaffMemoryUtil.getActiveEntitySummons(stack)) {
                long remainingTicks = Math.max(0, summon.expiryTick() - level.getGameTime());
                entityActiveIds.add(summon.summonedUUID().toString());
                entityActiveLabels.add(summon.displayName() + " - " + summon.pathway() + " Seq " + summon.sequence()
                        + " (" + (remainingTicks / 20) + "s left)");
            }

            List<String> itemLibraryLabels = new ArrayList<>();
            List<CompoundTag> recordedItems = StaffMemoryUtil.getRecordedItems(stack);
            for (CompoundTag entry : recordedItems) {
                itemLibraryLabels.add(entry.getString("DisplayName") + " x" + entry.getInt("Count"));
            }

            List<String> itemActiveIds = new ArrayList<>();
            List<String> itemActiveLabels = new ArrayList<>();
            for (StaffMemoryUtil.ActiveItemSummon summon : StaffMemoryUtil.getActiveItemSummons(stack)) {
                long remainingTicks = Math.max(0, summon.expiryTick() - level.getGameTime());
                itemActiveIds.add(summon.trackingId().toString());
                itemActiveLabels.add(summon.displayName() + " (" + (remainingTicks / 20) + "s left)");
            }

            player.openMenu(new StaffIntrospectMenuProvider(packet.mainHand(),
                            libraryIds, libraryLabels, wheelIds, wheelLabels,
                            entityWatchIds, entityWatchLabels, entityEligible,
                            entityActiveIds, entityActiveLabels, itemLibraryLabels, itemActiveIds, itemActiveLabels),
                    buf -> {
                        buf.writeBoolean(packet.mainHand());
                        writeStrings(buf, libraryIds);
                        writeStrings(buf, libraryLabels);
                        writeStrings(buf, wheelIds);
                        writeStrings(buf, wheelLabels);
                        writeStrings(buf, entityWatchIds);
                        writeStrings(buf, entityWatchLabels);
                        writeBools(buf, entityEligible);
                        writeStrings(buf, entityActiveIds);
                        writeStrings(buf, entityActiveLabels);
                        writeStrings(buf, itemLibraryLabels);
                        writeStrings(buf, itemActiveIds);
                        writeStrings(buf, itemActiveLabels);
                    });
        });
    }

    private static void writeStrings(RegistryFriendlyByteBuf buf, List<String> list) {
        buf.writeVarInt(list.size());
        for (String s : list) buf.writeUtf(s);
    }

    private static void writeBools(RegistryFriendlyByteBuf buf, List<Boolean> list) {
        buf.writeVarInt(list.size());
        for (boolean b : list) buf.writeBoolean(b);
    }
}

package com.Maul.lotmmi.item.custom;

import com.Maul.lotmmi.events.StaffAdriftHandler;
import com.Maul.lotmmi.gui.StaffTeleportMenuProvider;
import de.jakob.lotm.LOTMCraft;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

public class StaffTeleportUtil {
    private static final int MAX_LIFT = 24;

    private static final Random RANDOM = new Random();
    private static final double MISRECALL_BASE_CHANCE = 0.01D;

    private static final double[] MAX_MISS_DISTANCE = {8.0D, 12.0D, 20.0D, 32.0D, 48.0D, 64.0D};

    private static final Set<ResourceLocation> BLOCKED_DIMENSIONS = Set.of(
            ResourceLocation.fromNamespaceAndPath(LOTMCraft.MOD_ID, "sefirah_castle"),
            ResourceLocation.fromNamespaceAndPath(LOTMCraft.MOD_ID, "brood_hive"),
            ResourceLocation.fromNamespaceAndPath(LOTMCraft.MOD_ID, "historical_void"),
            ResourceLocation.fromNamespaceAndPath(LOTMCraft.MOD_ID, "concealment_world")
    );

    public static boolean isBlockedDimension(ResourceLocation dimension) {
        return BLOCKED_DIMENSIONS.contains(dimension);
    }

    public static boolean saveSlot(ServerPlayer player, ItemStack staff, int index) {
        if (isBlockedDimension(player.level().dimension().location())) {
            player.displayClientMessage(Component.literal("This location can't be saved.")
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }

        TeleportSlotUtil.recordSlot(staff, index, player.level().dimension(), player.position());
        player.displayClientMessage(Component.literal("Location " + (index + 1) + " saved.")
                .withStyle(ChatFormatting.GOLD), true);
        return true;
    }

    public static double misrecallChance(LivingEntity entity) {
        return Math.min(0.95D, PanicUtil.scale(MISRECALL_BASE_CHANCE, entity));
    }

    public static boolean teleportToSlot(ServerPlayer player, TeleportSlotUtil.Slot slot, List<TeleportSlotUtil.Slot> allSlots) {
        if (RANDOM.nextDouble() < misrecallChance(player)) {
            return misrecall(player, slot, allSlots);
        }
        return arrive(player, slot);
    }

    private static void departEffect(ServerPlayer player) {
        if (player.level() instanceof ServerLevel from) {
            ArtifactEffects.staffTeleport(from, player.getX(), player.getY(), player.getZ(), false);
        }
    }

    private static boolean arrive(ServerPlayer player, TeleportSlotUtil.Slot slot) {
        MinecraftServer server = player.getServer();
        if (server == null) return false;

        ServerLevel level = server.getLevel(slot.dimension());
        if (level == null) {
            player.displayClientMessage(Component.literal("That location can't be found.")
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }

        level.getChunk(BlockPos.containing(slot.x(), slot.y(), slot.z()));
        Vec3 spot = findStandableSpot(level, player, slot.x(), slot.y(), slot.z());
        departEffect(player);
        player.teleportTo(level, spot.x, spot.y, spot.z, player.getYRot(), player.getXRot());
        ArtifactEffects.staffTeleport(level, spot.x, spot.y, spot.z, true);
        return true;
    }

    private static boolean misrecall(ServerPlayer player, TeleportSlotUtil.Slot intended, List<TeleportSlotUtil.Slot> allSlots) {
        int panic = PanicUtil.getAmplifier(player);

        List<TeleportSlotUtil.Slot> others = new ArrayList<>();
        for (TeleportSlotUtil.Slot candidate : allSlots) {
            if (candidate != null && !candidate.equals(intended)) others.add(candidate);
        }

        int nearWeight = 10;
        int otherWeight = panic >= 1 && !others.isEmpty() ? 3 : 0;
        int spiritWeight = panic >= 2 ? 2 : 0;
        int cosmosWeight = panic >= 3 ? 1 : 0;

        PanicUtil.escalate(player);

        int roll = RANDOM.nextInt(nearWeight + otherWeight + spiritWeight + cosmosWeight);
        if (roll >= nearWeight) {
            roll -= nearWeight;
            if (roll < otherWeight) {
                player.displayClientMessage(Component.literal("You remembered the wrong place.")
                        .withStyle(ChatFormatting.LIGHT_PURPLE), true);
                return arrive(player, others.get(RANDOM.nextInt(others.size())));
            }
            roll -= otherWeight;
            StaffAdriftHandler.Realm realm = roll < spiritWeight ? StaffAdriftHandler.Realm.SPIRIT_WORLD : StaffAdriftHandler.Realm.COSMOS;
            if (StaffAdriftHandler.send(player, realm)) return true;
        }

        return arriveNear(player, intended, MAX_MISS_DISTANCE[Math.max(0, Math.min(5, panic + 1))]);
    }

    private static boolean arriveNear(ServerPlayer player, TeleportSlotUtil.Slot slot, double maxDistance) {
        MinecraftServer server = player.getServer();
        ServerLevel level = server == null ? null : server.getLevel(slot.dimension());
        if (level == null) return arrive(player, slot);

        level.getChunk(BlockPos.containing(slot.x(), slot.y(), slot.z()));
        EntityDimensions dimensions = player.getDimensions(player.getPose());
        double minDistance = Math.min(4.0D, maxDistance / 2.0D);

        for (int attempt = 0; attempt < 24; attempt++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2.0;
            double distance = minDistance + RANDOM.nextDouble() * (maxDistance - minDistance);
            double x = slot.x() + Math.cos(angle) * distance;
            double z = slot.z() + Math.sin(angle) * distance;

            for (int dy = 10; dy >= -10; dy--) {
                double y = Math.floor(slot.y()) + dy;
                if (y < level.getMinBuildHeight() + 1 || y >= level.getMaxBuildHeight() - 2) continue;

                BlockPos below = BlockPos.containing(x, y - 1.0, z);
                if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) continue;
                if (!level.getBlockState(below).getFluidState().isEmpty()) continue;
                if (!level.noCollision(dimensions.makeBoundingBox(x, y, z))) continue;

                departEffect(player);
                player.teleportTo(level, x, y, z, player.getYRot(), player.getXRot());
                ArtifactEffects.staffTeleport(level, x, y, z, true);
                player.displayClientMessage(Component.literal("You misremember the place and arrive off target.")
                        .withStyle(ChatFormatting.GOLD), true);
                return true;
            }
        }
        return arrive(player, slot);
    }

    private static Vec3 findStandableSpot(ServerLevel level, ServerPlayer player, double x, double y, double z) {
        EntityDimensions dimensions = player.getDimensions(player.getPose());
        for (int lift = 0; lift <= MAX_LIFT; lift++) {
            double testY = y + lift;
            if (testY >= level.getMaxBuildHeight()) break;
            AABB box = dimensions.makeBoundingBox(x, testY, z);
            if (level.noCollision(box)) return new Vec3(x, testY, z);
        }
        return new Vec3(x, y, z);
    }

    public static String dimensionLabel(ResourceLocation dimension) {
        StringBuilder out = new StringBuilder();
        for (String word : dimension.getPath().split("_")) {
            if (word.isEmpty()) continue;
            if (out.length() > 0) out.append(' ');
            out.append(word.substring(0, 1).toUpperCase(Locale.ROOT)).append(word.substring(1));
        }
        return out.toString();
    }

    public static List<String> buildSlotDetails(ItemStack staffStack) {
        List<TeleportSlotUtil.Slot> slots = TeleportSlotUtil.getSlots(staffStack);
        List<String> details = new ArrayList<>();
        for (int i = 0; i < TeleportSlotUtil.MAX_SLOTS; i++) {
            if (i < slots.size() && slots.get(i) != null) {
                TeleportSlotUtil.Slot slot = slots.get(i);
                details.add(String.format("%.0f, %.0f, %.0f  %s", slot.x(), slot.y(), slot.z(),
                        dimensionLabel(slot.dimension().location())));
            } else {
                details.add("");
            }
        }
        return details;
    }

    public static void openMenu(ServerPlayer player, ItemStack staffStack, boolean mainHand) {
        List<String> details = buildSlotDetails(staffStack);

        player.openMenu(new StaffTeleportMenuProvider(mainHand, details), buf -> {
            buf.writeBoolean(mainHand);
            buf.writeVarInt(details.size());
            for (String detail : details) buf.writeUtf(detail);
        });
    }
}

package com.Maul.lotmmi.item.custom;

import com.Maul.lotmmi.gui.StaffTeleportMenuProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import de.jakob.lotm.LOTMCraft;
import de.jakob.lotm.dimension.SpiritWorldHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class StaffTeleportUtil {

    private static final Random RANDOM = new Random();
    private static final double TYPED_SUCCESS_CHANCE = 0.65D;
    private static final double TYPED_RANDOM_CHANCE = 0.30D;
    private static final double TYPED_SPIRIT_WORLD_CHANCE = 0.05D;

    public enum Result { SUCCESS, RANDOM_PLACE, SPIRIT_WORLD }

    public static Result attemptTypedTeleport(ServerPlayer player, ResourceKey<Level> dimension, double x, double y, double z) {
        double roll = RANDOM.nextDouble();

        double randomThreshold = PanicUtil.scale(TYPED_RANDOM_CHANCE, player);
        double spiritThreshold = PanicUtil.scale(TYPED_SPIRIT_WORLD_CHANCE, player);

        if (roll < spiritThreshold) {
            teleportToSpiritWorld(player);
            PanicUtil.escalate(player);
            return Result.SPIRIT_WORLD;
        } else if (roll < spiritThreshold + randomThreshold) {
            teleportToRandomPlace(player);
            PanicUtil.escalate(player);
            return Result.RANDOM_PLACE;
        } else {
            ServerLevel targetLevel = player.getServer().getLevel(dimension);
            if (targetLevel == null) targetLevel = (ServerLevel) player.level();
            safeTeleport(targetLevel, player, x, y, z);
            return Result.SUCCESS;
        }
    }

    public static void teleportToSlot(ServerPlayer player, TeleportSlotUtil.Slot slot) {
        ServerLevel targetLevel = player.getServer().getLevel(slot.dimension());
        if (targetLevel == null) return;
        player.teleportTo(targetLevel, slot.x(), slot.y(), slot.z(), player.getYRot(), player.getXRot());
    }

    public static void teleportToRandomPlace(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        int offsetX = RANDOM.nextInt(60_000_000) - 30_000_000;
        int offsetZ = RANDOM.nextInt(60_000_000) - 30_000_000;
        safeTeleport(level, player, offsetX, 128, offsetZ);
    }

    public static void teleportToSpiritWorld(ServerPlayer player) {
        ResourceKey<Level> spiritWorld = ResourceKey.create(Registries.DIMENSION,
                ResourceLocation.fromNamespaceAndPath(LOTMCraft.MOD_ID, "spirit_world"));
        ServerLevel targetLevel = player.getServer().getLevel(spiritWorld);
        if (targetLevel == null) return;

        Vec3 targetPos = SpiritWorldHandler.getCoordinatesInSpiritWorld(player.position(), targetLevel);
        BlockPos pos = BlockPos.containing(targetPos);

        while (!targetLevel.getBlockState(pos).isAir()) {
            pos = pos.above();
        }
        BlockPos below = pos.below();
        if (targetLevel.getBlockState(below).isAir()) {
            targetLevel.setBlockAndUpdate(below, Blocks.END_STONE.defaultBlockState());
        }
        player.teleportTo(targetLevel, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, player.getYRot(), player.getXRot());
    }

    private static void safeTeleport(ServerLevel level, ServerPlayer player, double x, double y, double z) {
        BlockPos pos = BlockPos.containing(x, y, z);
        int attempts = 0;
        while (!level.getBlockState(pos).isAir() && attempts < 384) {
            pos = pos.above();
            attempts++;
        }
        BlockPos below = pos.below();
        if (level.getBlockState(below).isAir()) {
            level.setBlockAndUpdate(below, Blocks.STONE.defaultBlockState());
        }
        player.teleportTo(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, player.getYRot(), player.getXRot());
    }

    public static void openMenu(ServerPlayer player, ItemStack staffStack, boolean mainHand) {
        List<TeleportSlotUtil.Slot> slots = TeleportSlotUtil.getSlots(staffStack);
        List<String> labels = new ArrayList<>();
        List<String> names = new ArrayList<>();
        for (int i = 0; i < TeleportSlotUtil.MAX_SLOTS; i++) {
            if (i < slots.size() && slots.get(i) != null) {
                TeleportSlotUtil.Slot slot = slots.get(i);
                String coords = String.format("%.0f, %.0f, %.0f (%s)", slot.x(), slot.y(), slot.z(),
                        slot.dimension().location().getPath());
                boolean hasName = slot.name() != null && !slot.name().isEmpty();
                labels.add(hasName ? slot.name() + " - " + coords : coords);
                names.add(hasName ? slot.name() : "");
            } else {
                labels.add("Empty - click to record");
                names.add("");
            }
        }

        player.openMenu(new StaffTeleportMenuProvider(mainHand, labels, names), buf -> {
            buf.writeBoolean(mainHand);
            buf.writeVarInt(labels.size());
            for (String label : labels) buf.writeUtf(label);
            for (String name : names) buf.writeUtf(name);
        });
    }
}

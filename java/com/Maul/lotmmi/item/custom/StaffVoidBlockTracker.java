package com.Maul.lotmmi.item.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StaffVoidBlockTracker {

    public record Placement(UUID trackingId) {}

    private static final Map<BlockPos, Placement> PLACED_BLOCKS = new HashMap<>();

    public static void track(BlockPos pos, UUID trackingId) {
        PLACED_BLOCKS.put(pos.immutable(), new Placement(trackingId));
    }

    public static boolean isTracked(BlockPos pos, UUID trackingId) {
        Placement placement = PLACED_BLOCKS.get(pos);
        return placement != null && placement.trackingId().equals(trackingId);
    }

    public static void removeAllFor(ServerLevel level, UUID trackingId) {
        PLACED_BLOCKS.entrySet().removeIf(entry -> {
            if (entry.getValue().trackingId().equals(trackingId)) {
                level.removeBlock(entry.getKey(), false);
                return true;
            }
            return false;
        });
    }
}

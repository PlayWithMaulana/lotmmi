package com.Maul.lotmmi.events;

import com.Maul.lotmmi.LotmMysticalItems;
import com.Maul.lotmmi.item.custom.ScepterFollowerUtil;
import com.Maul.lotmmi.item.custom.SeaGodScepterItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

@EventBusSubscriber(modid = LotmMysticalItems.MOD_ID)
public class ScepterFollowerTracker {
    public static final TicketType<ChunkPos> LOOKUP_TICKET =
            TicketType.create("lotmmi_follower_lookup", Comparator.comparingLong(ChunkPos::toLong), 200);

    private static final int POSITION_REFRESH_TICKS = 20;
    private static final double POSITION_UPDATE_DISTANCE_SQR = 4.0;
    private static final int MAX_WAIT_TICKS = 100;
    private static final int TICKET_RADIUS = 2;

    @FunctionalInterface
    public interface FollowerTask {
        void run(ServerPlayer wielder, ItemStack scepter, ScepterFollowerUtil.Follower follower, LivingEntity entity);
    }

    private record Pending(UUID wielderId, UUID followerId, String followerName, FollowerTask task, int startedTick) {
    }

    private static final List<Pending> PENDING = new ArrayList<>();

    public static LivingEntity findLoaded(MinecraftServer server, UUID id) {
        ServerPlayer onlinePlayer = server.getPlayerList().getPlayer(id);
        if (onlinePlayer != null) return onlinePlayer;

        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(id);
            if (entity instanceof LivingEntity living && living.isAlive()) return living;
        }
        return null;
    }

    public static void run(ServerPlayer wielder, ItemStack scepter, UUID followerId, FollowerTask task) {
        MinecraftServer server = wielder.getServer();
        if (server == null) return;

        ScepterFollowerUtil.Follower follower = ScepterFollowerUtil.getFollower(scepter, followerId);
        if (follower == null) return;

        LivingEntity entity = findLoaded(server, followerId);
        if (entity != null) {
            follower = remember(scepter, follower, entity);
            task.run(wielder, scepter, follower, entity);
            return;
        }

        for (Pending pending : PENDING) {
            if (pending.wielderId().equals(wielder.getUUID()) && pending.followerId().equals(followerId)) {
                return;
            }
        }

        ServerLevel level = levelOf(server, follower.lastKnownDimension());
        if (level == null) {
            wielder.displayClientMessage(Component.literal("Their last known location can't be found.")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        ChunkPos chunk = new ChunkPos((int) Math.floor(follower.lastKnownX()) >> 4, (int) Math.floor(follower.lastKnownZ()) >> 4);
        level.getChunkSource().addRegionTicket(LOOKUP_TICKET, chunk, TICKET_RADIUS, chunk);

        PENDING.add(new Pending(wielder.getUUID(), followerId, follower.name(), task, server.getTickCount()));
        wielder.displayClientMessage(Component.literal("Looking for " + follower.name() + "...")
                .withStyle(ChatFormatting.GRAY), true);
    }

    private static ServerLevel levelOf(MinecraftServer server, String dimensionId) {
        if (dimensionId == null || dimensionId.isEmpty()) return null;
        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(dimensionId));
        return server.getLevel(key);
    }

    private static ScepterFollowerUtil.Follower remember(ItemStack scepter, ScepterFollowerUtil.Follower follower, LivingEntity entity) {
        ScepterFollowerUtil.Follower updated = follower.withLastKnown(entity.level().dimension(), entity.getX(), entity.getY(), entity.getZ());
        ScepterFollowerUtil.updateFollower(scepter, updated);
        return updated;
    }

    private static ItemStack findScepterWithFollower(ServerPlayer player, UUID followerId) {
        for (ItemStack scepter : SeaGodScepterItem.findScepters(player)) {
            if (ScepterFollowerUtil.isFollower(scepter, followerId)) return scepter;
        }
        return ItemStack.EMPTY;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        int tick = server.getTickCount();

        if (!PENDING.isEmpty()) processPending(server, tick);

        if (tick % POSITION_REFRESH_TICKS != 0) return;
        for (ServerPlayer holder : server.getPlayerList().getPlayers()) {
            for (ItemStack scepter : SeaGodScepterItem.findScepters(holder)) {
                refreshPositions(server, scepter);
            }
        }
    }

    private static void refreshPositions(MinecraftServer server, ItemStack scepter) {
        for (ScepterFollowerUtil.Follower follower : ScepterFollowerUtil.getFollowers(scepter)) {
            LivingEntity entity = findLoaded(server, follower.id());
            if (entity == null) continue;

            String dimension = entity.level().dimension().location().toString();
            double dx = entity.getX() - follower.lastKnownX();
            double dy = entity.getY() - follower.lastKnownY();
            double dz = entity.getZ() - follower.lastKnownZ();
            if (dimension.equals(follower.lastKnownDimension()) && dx * dx + dy * dy + dz * dz < POSITION_UPDATE_DISTANCE_SQR) {
                continue;
            }
            remember(scepter, follower, entity);
        }
    }

    private static void processPending(MinecraftServer server, int tick) {
        Iterator<Pending> iterator = PENDING.iterator();
        List<Pending> ready = new ArrayList<>();

        while (iterator.hasNext()) {
            Pending pending = iterator.next();
            ServerPlayer wielder = server.getPlayerList().getPlayer(pending.wielderId());
            if (wielder == null) {
                iterator.remove();
                continue;
            }

            if (findLoaded(server, pending.followerId()) != null) {
                iterator.remove();
                ready.add(pending);
            } else if (tick - pending.startedTick() > MAX_WAIT_TICKS) {
                iterator.remove();
                wielder.displayClientMessage(Component.literal(pending.followerName()
                                + " can't be reached. If they are gone for good, use Del to remove them.")
                        .withStyle(ChatFormatting.RED), true);
            }
        }

        for (Pending pending : ready) {
            ServerPlayer wielder = server.getPlayerList().getPlayer(pending.wielderId());
            if (wielder == null) continue;

            ItemStack scepter = findScepterWithFollower(wielder, pending.followerId());
            LivingEntity entity = findLoaded(server, pending.followerId());
            if (scepter.isEmpty() || entity == null) continue;

            ScepterFollowerUtil.Follower follower = ScepterFollowerUtil.getFollower(scepter, pending.followerId());
            if (follower == null) continue;

            follower = remember(scepter, follower, entity);
            pending.task().run(wielder, scepter, follower, entity);
            SeaGodScepterItem.resync(wielder);
        }
    }
}

package com.Maul.lotmmi.events;

import com.Maul.lotmmi.LotmMysticalItems;
import de.jakob.lotm.LOTMCraft;
import de.jakob.lotm.dimension.SpiritWorldHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.Random;

@EventBusSubscriber(modid = LotmMysticalItems.MOD_ID)
public class StaffAdriftHandler {
    public enum Realm { SPIRIT_WORLD, COSMOS }

    private static final String TAG = "lotmmi_adrift";
    private static final Random RANDOM = new Random();

    private static final ResourceLocation SPIRIT_WORLD = ResourceLocation.fromNamespaceAndPath(LOTMCraft.MOD_ID, "spirit_world");
    private static final ResourceLocation DEEP_SPACE = ResourceLocation.fromNamespaceAndPath(LOTMCraft.MOD_ID, "deep_space");

    public static boolean isAdrift(ServerPlayer player) {
        return player.getPersistentData().contains(TAG);
    }

    private static boolean isRealm(ResourceKey<Level> dimension) {
        return dimension.location().equals(SPIRIT_WORLD) || dimension.location().equals(DEEP_SPACE);
    }

    public static boolean send(ServerPlayer player, Realm realm) {
        MinecraftServer server = player.getServer();
        if (server == null || isAdrift(player)) return false;

        ResourceLocation path = realm == Realm.SPIRIT_WORLD ? SPIRIT_WORLD : DEEP_SPACE;
        ServerLevel target = server.getLevel(ResourceKey.create(Registries.DIMENSION, path));
        if (target == null) return false;

        CompoundTag data = player.getPersistentData();
        data.putBoolean(TAG, true);

        if (realm == Realm.COSMOS) {
            data.putLong("ReturnTime", Long.MAX_VALUE);
            data.putString("ReturnLevel", player.level().dimension().location().toString());
            data.putDouble("ReturnX", player.getX());
            data.putDouble("ReturnY", player.getY());
            data.putDouble("ReturnZ", player.getZ());
        }

        BlockPos landing = realm == Realm.SPIRIT_WORLD ? spiritWorldLanding(player, target) : cosmosLanding(target);
        player.teleportTo(target, landing.getX() + 0.5, landing.getY(), landing.getZ() + 0.5, player.getYRot(), player.getXRot());
        player.resetFallDistance();
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 20 * 30, 0, false, false, true));

        String where = realm == Realm.SPIRIT_WORLD ? "You slip into the Spirit World!" : "You are cast out into the Cosmos!";
        player.displayClientMessage(Component.literal(where + " Only a saved location or one of your own powers can get you out.")
                .withStyle(ChatFormatting.DARK_PURPLE), true);
        return true;
    }

    private static BlockPos spiritWorldLanding(ServerPlayer player, ServerLevel target) {
        Vec3 coordinates = SpiritWorldHandler.getCoordinatesInSpiritWorld(player.position(), target);
        BlockPos pos = BlockPos.containing(coordinates);

        int guard = 0;
        while (!target.getBlockState(pos).isAir() && guard++ < 256) {
            pos = pos.above();
        }
        BlockPos below = pos.below();
        if (target.getBlockState(below).isAir()) {
            target.setBlockAndUpdate(below, Blocks.END_STONE.defaultBlockState());
        }
        return pos;
    }

    private static BlockPos cosmosLanding(ServerLevel target) {
        int x = (int) ((RANDOM.nextDouble() * 2.0 - 1.0) * 2000.0);
        int z = (int) ((RANDOM.nextDouble() * 2.0 - 1.0) * 2000.0);
        BlockPos center = new BlockPos(x, 99, z);

        target.getChunk(center);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos block = center.offset(dx, 0, dz);
                if (target.getBlockState(block).isAir()) {
                    target.setBlockAndUpdate(block, Blocks.END_STONE.defaultBlockState());
                }
            }
        }
        return center.above();
    }

    private static void clear(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        data.remove(TAG);
        data.remove("ReturnTime");
        data.remove("ReturnLevel");
        data.remove("ReturnX");
        data.remove("ReturnY");
        data.remove("ReturnZ");
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!isAdrift(player)) return;

        if (!isRealm(event.getTo())) clear(player);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) clear(player);
    }
}

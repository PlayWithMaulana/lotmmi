package com.Maul.lotmmi.events;

import com.Maul.lotmmi.LotmMysticalItems;
import com.Maul.lotmmi.item.custom.PanicUtil;
import com.Maul.lotmmi.item.custom.StaffMemoryUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

@EventBusSubscriber(modid = LotmMysticalItems.MOD_ID)
public class StaffEchoHandler {
    private record Echo(UUID entityId, UUID targetId, ResourceKey<Level> dimension, long expiryTick) {}

    private static final String TAG_ECHO = "lotmmi_echo";
    private static final long LIFETIME_TICKS = 25L * 20L;

    private static final List<Echo> ECHOES = new ArrayList<>();
    private static final Random RANDOM = new Random();

    private static boolean allowed(int panic, int sequence) {
        if (panic >= 4) return true;
        if (panic == 3) return sequence >= 3;
        return sequence >= 5;
    }

    private static int weight(int sequence) {
        if (sequence >= 5) return 4;
        if (sequence >= 3) return 2;
        return 1;
    }

    public static boolean hasCandidate(ServerPlayer player, ItemStack staff) {
        int panic = PanicUtil.getAmplifier(player);
        for (StaffMemoryUtil.EntityWatch watch : StaffMemoryUtil.getEntityWatches(staff)) {
            if (allowed(panic, watch.sequence())) return true;
        }
        return false;
    }

    public static boolean spawn(ServerLevel level, ServerPlayer player, ItemStack staff) {
        int panic = PanicUtil.getAmplifier(player);

        List<StaffMemoryUtil.EntityWatch> pool = new ArrayList<>();
        int total = 0;
        for (StaffMemoryUtil.EntityWatch watch : StaffMemoryUtil.getEntityWatches(staff)) {
            if (!allowed(panic, watch.sequence())) continue;
            pool.add(watch);
            total += weight(watch.sequence());
        }
        if (pool.isEmpty()) return false;

        int roll = RANDOM.nextInt(total);
        StaffMemoryUtil.EntityWatch chosen = pool.get(0);
        for (StaffMemoryUtil.EntityWatch watch : pool) {
            roll -= weight(watch.sequence());
            if (roll < 0) {
                chosen = watch;
                break;
            }
        }

        Optional<EntityType<?>> type = EntityType.byString(chosen.entityType());
        if (type.isEmpty()) return false;
        Entity entity = type.get().create(level);
        if (entity == null) return false;

        entity.load(chosen.entityNbt());
        entity.setUUID(UUID.randomUUID());

        BlockPos spot = findSpot(level, player, entity);
        if (spot == null) return false;
        entity.moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, RANDOM.nextFloat() * 360.0F, 0.0F);

        entity.getPersistentData().putBoolean(TAG_ECHO, true);
        level.addFreshEntity(entity);

        if (entity instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.GLOWING, (int) LIFETIME_TICKS, 0, false, false, false));
        }
        if (entity instanceof Mob mob) mob.setTarget(player);

        ECHOES.add(new Echo(entity.getUUID(), player.getUUID(), level.dimension(), level.getGameTime() + LIFETIME_TICKS));

        level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_STARE, SoundSource.HOSTILE, 1.0F, 0.5F);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, entity.getX(), entity.getY() + 1.0, entity.getZ(), 30, 0.4, 0.8, 0.4, 0.05);
        player.displayClientMessage(Component.literal("A hostile echo of " + chosen.displayName() + " appears.")
                .withStyle(ChatFormatting.DARK_RED), true);
        return true;
    }

    private static BlockPos findSpot(ServerLevel level, ServerPlayer player, Entity entity) {
        for (int attempt = 0; attempt < 16; attempt++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2.0;
            double distance = 12.0 + RANDOM.nextDouble() * 4.0;
            double x = player.getX() + Math.cos(angle) * distance;
            double z = player.getZ() + Math.sin(angle) * distance;

            for (int dy = 6; dy >= -6; dy--) {
                double y = Math.floor(player.getY()) + dy;
                if (y < level.getMinBuildHeight() + 1 || y >= level.getMaxBuildHeight() - 3) continue;

                BlockPos below = BlockPos.containing(x, y - 1.0, z);
                if (!level.getBlockState(below).isFaceSturdy(level, below, net.minecraft.core.Direction.UP)) continue;
                if (!level.noCollision(entity.getType().getDimensions().makeBoundingBox(x, y, z))) continue;
                return BlockPos.containing(x, y, z);
            }
        }
        return null;
    }

    private static void vanish(Entity entity) {
        if (entity.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.POOF, entity.getX(), entity.getY() + 1.0, entity.getZ(), 20, 0.3, 0.6, 0.3, 0.02);
        }
        entity.discard();
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (ECHOES.isEmpty()) return;
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 10 != 0) return;

        Iterator<Echo> iterator = ECHOES.iterator();
        while (iterator.hasNext()) {
            Echo echo = iterator.next();
            ServerLevel level = server.getLevel(echo.dimension());
            Entity entity = level == null ? null : level.getEntity(echo.entityId());
            if (entity == null || !entity.isAlive()) {
                iterator.remove();
                continue;
            }

            if (level.getGameTime() >= echo.expiryTick()) {
                vanish(entity);
                iterator.remove();
                continue;
            }

            if (entity instanceof Mob mob && mob.getTarget() == null) {
                ServerPlayer target = server.getPlayerList().getPlayer(echo.targetId());
                if (target != null && target.level() == level) mob.setTarget(target);
            }
        }
    }

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Post event) {
        DamageSource source = event.getSource();
        Entity attacker = source.getEntity();
        if (attacker == null || attacker.level().isClientSide) return;

        if (!attacker.getPersistentData().getBoolean(TAG_ECHO)) return;

        ECHOES.removeIf(e -> e.entityId().equals(attacker.getUUID()));
        MinecraftServer server = attacker.getServer();
        if (server != null) server.execute(() -> vanish(attacker));
    }
}

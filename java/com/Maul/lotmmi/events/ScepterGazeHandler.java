package com.Maul.lotmmi.events;

import com.Maul.lotmmi.LotmMysticalItems;
import com.Maul.lotmmi.config.LotmmiConfig;
import com.Maul.lotmmi.item.custom.ScepterPrayerUtil;
import com.Maul.lotmmi.item.custom.SeaGodScepterItem;
import de.jakob.lotm.entity.custom.ability_entities.ControlBodyDouble;
import de.jakob.lotm.util.BeyonderData;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = LotmMysticalItems.MOD_ID)
public class ScepterGazeHandler {
    private static final String TAG_ORIGIN = "lotmmi_gaze_origin";
    private static final double LEASH_RADIUS = 48.0;

    private record Session(ResourceKey<Level> originDimension, double x, double y, double z, float yRot, float xRot,
                           boolean hadMayFly, boolean wasFlying,
                           ResourceKey<Level> targetDimension, double targetX, double targetY, double targetZ,
                           long endTick, UUID bodyId, float bodyHealth) {
    }

    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();

    public static boolean isGazing(Player player) {
        return SESSIONS.containsKey(player.getUUID());
    }

    public static boolean begin(ServerPlayer player, ScepterPrayerUtil.Prayer prayer) {
        MinecraftServer server = player.getServer();
        if (server == null) return false;

        if (isGazing(player)) {
            say(player, "You are already gazing. Sneak and use the Scepter to return.", ChatFormatting.GRAY);
            return false;
        }

        if (LotmmiConfig.GAZE_REQUIRES_CASTLE.get() && !SeaGodScepterItem.isInsideOwnSefirahCastle(player)) {
            say(player, "You can only gaze from your own Sefirah Castle.", ChatFormatting.RED);
            return false;
        }

        ResourceKey<Level> targetKey = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(prayer.dimension()));
        ServerLevel targetLevel = server.getLevel(targetKey);
        if (targetLevel == null) {
            say(player, "That location can't be found.", ChatFormatting.RED);
            return false;
        }

        float drain = LotmmiConfig.GAZE_SPIRITUALITY_PER_SECOND.get().floatValue();
        if (BeyonderData.getSpirituality(player) < drain) {
            say(player, "You need at least " + Math.round(drain) + " spirituality to gaze.", ChatFormatting.RED);
            return false;
        }
        BeyonderData.reduceSpirituality(player, drain);

        ServerLevel originLevel = (ServerLevel) player.level();
        ControlBodyDouble body = ControlBodyDouble.create(originLevel, player, false);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            body.setDropChance(slot, 0.0F);
        }
        originLevel.addFreshEntity(body);

        Abilities abilities = player.getAbilities();
        long duration = LotmmiConfig.GAZE_DURATION_SECONDS.get() * 20L;
        Session session = new Session(player.level().dimension(), player.getX(), player.getY(), player.getZ(),
                player.getYRot(), player.getXRot(), abilities.mayfly, abilities.flying,
                targetKey, prayer.x(), prayer.y(), prayer.z(),
                player.level().getGameTime() + duration, body.getUUID(), body.getHealth());

        writeOrigin(player, session);
        SESSIONS.put(player.getUUID(), session);

        targetLevel.getChunk(BlockPos.containing(prayer.x(), prayer.y(), prayer.z()));
        player.teleportTo(targetLevel, prayer.x(), prayer.y(), prayer.z(), player.getYRot(), player.getXRot());

        abilities.mayfly = true;
        abilities.flying = true;
        player.onUpdateAbilities();

        int effectTicks = (int) duration + 60;
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, effectTicks, 4, false, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, effectTicks, 0, false, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, effectTicks, 0, false, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, effectTicks, 0, false, false, false));

        say(player, "You gaze at " + prayer.name() + "'s prayer. Your body stays behind. Sneak and use the Scepter to return.", ChatFormatting.LIGHT_PURPLE);
        return true;
    }

    public static void end(ServerPlayer player) {
        endWithDamage(player, 0.0F);
    }

    private static void endWithDamage(ServerPlayer player, float bodyDamage) {
        Session session = SESSIONS.remove(player.getUUID());
        if (session == null) return;

        restore(player, session.originDimension(), session.x(), session.y(), session.z(), session.yRot(), session.xRot(),
                session.hadMayFly(), session.wasFlying(), session.bodyId());

        if (bodyDamage >= session.bodyHealth()) {
            say(player, "Your body was killed while you were away!", ChatFormatting.DARK_RED);
            player.kill();
        } else if (bodyDamage > 0.0F) {
            say(player, "Your body was attacked, so your gaze ended!", ChatFormatting.RED);
            player.hurt(player.damageSources().generic(), bodyDamage);
        } else {
            say(player, "Your gaze ends.", ChatFormatting.LIGHT_PURPLE);
        }
    }

    private static float bodyDamage(MinecraftServer server, Session session) {
        ServerLevel level = server.getLevel(session.originDimension());
        if (level == null) return 0.0F;

        Entity entity = level.getEntity(session.bodyId());
        if (!(entity instanceof LivingEntity body)) return 0.0F;
        if (!body.isAlive()) return session.bodyHealth();

        return Math.max(0.0F, session.bodyHealth() - body.getHealth());
    }

    private static void discardBody(MinecraftServer server, ResourceKey<Level> dimension, UUID bodyId) {
        if (bodyId == null) return;
        ServerLevel level = server.getLevel(dimension);
        if (level == null) return;

        Entity entity = level.getEntity(bodyId);
        if (entity != null) entity.discard();
    }

    private static void restore(ServerPlayer player, ResourceKey<Level> dimension, double x, double y, double z,
                                float yRot, float xRot, boolean hadMayFly, boolean wasFlying, UUID bodyId) {
        MinecraftServer server = player.getServer();

        player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
        player.removeEffect(MobEffects.FIRE_RESISTANCE);
        player.removeEffect(MobEffects.WATER_BREATHING);
        player.removeEffect(MobEffects.INVISIBILITY);

        if (server != null) {
            discardBody(server, dimension, bodyId);

            ServerLevel level = server.getLevel(dimension);
            if (level == null) {
                level = server.overworld();
                BlockPos spawn = level.getSharedSpawnPos();
                x = spawn.getX() + 0.5;
                y = spawn.getY();
                z = spawn.getZ() + 0.5;
            }
            level.getChunk(BlockPos.containing(x, y, z));
            player.teleportTo(level, x, y, z, yRot, xRot);
        }

        Abilities abilities = player.getAbilities();
        abilities.mayfly = hadMayFly;
        abilities.flying = hadMayFly && wasFlying;
        player.onUpdateAbilities();

        player.getPersistentData().remove(TAG_ORIGIN);
    }

    private static void writeOrigin(ServerPlayer player, Session session) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Dimension", session.originDimension().location().toString());
        tag.putDouble("X", session.x());
        tag.putDouble("Y", session.y());
        tag.putDouble("Z", session.z());
        tag.putFloat("YRot", session.yRot());
        tag.putFloat("XRot", session.xRot());
        tag.putBoolean("MayFly", session.hadMayFly());
        tag.putBoolean("Flying", session.wasFlying());
        tag.putUUID("Body", session.bodyId());
        player.getPersistentData().put(TAG_ORIGIN, tag);
    }

    private static void say(ServerPlayer player, String text, ChatFormatting color) {
        player.displayClientMessage(Component.literal(text).withStyle(color), true);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (SESSIONS.isEmpty()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        Session session = SESSIONS.get(player.getUUID());
        if (session == null) return;

        MinecraftServer server = player.getServer();
        if (server != null) {
            float damage = bodyDamage(server, session);
            if (damage > 0.0F) {
                endWithDamage(player, damage);
                return;
            }
        }

        long time = player.level().getGameTime();
        if (time >= session.endTick()) {
            end(player);
            return;
        }

        if (time % 20L == 0L) {
            float drain = LotmmiConfig.GAZE_SPIRITUALITY_PER_SECOND.get().floatValue();
            if (BeyonderData.getSpirituality(player) < drain) {
                say(player, "You ran out of spirituality. Your gaze ends.", ChatFormatting.GRAY);
                end(player);
                return;
            }
            BeyonderData.reduceSpirituality(player, drain);
        }

        if (player.level().dimension().equals(session.targetDimension())) {
            double dx = player.getX() - session.targetX();
            double dy = player.getY() - session.targetY();
            double dz = player.getZ() - session.targetZ();
            if (dx * dx + dy * dy + dz * dz > LEASH_RADIUS * LEASH_RADIUS && player.level() instanceof ServerLevel level) {
                player.teleportTo(level, session.targetX(), session.targetY(), session.targetZ(), player.getYRot(), player.getXRot());
                say(player, "You can't gaze any farther.", ChatFormatting.GRAY);
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            end(player);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (SESSIONS.containsKey(player.getUUID())) return;

        CompoundTag data = player.getPersistentData();
        if (!data.contains(TAG_ORIGIN)) return;

        CompoundTag tag = data.getCompound(TAG_ORIGIN);
        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(tag.getString("Dimension")));
        UUID bodyId = tag.hasUUID("Body") ? tag.getUUID("Body") : null;
        restore(player, dimension, tag.getDouble("X"), tag.getDouble("Y"), tag.getDouble("Z"),
                tag.getFloat("YRot"), tag.getFloat("XRot"), tag.getBoolean("MayFly"), tag.getBoolean("Flying"), bodyId);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        SESSIONS.remove(player.getUUID());
        player.getPersistentData().remove(TAG_ORIGIN);
    }
}

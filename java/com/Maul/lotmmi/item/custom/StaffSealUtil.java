package com.Maul.lotmmi.item.custom;

import de.jakob.lotm.LOTMCraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Random;

public class StaffSealUtil {
    private static final Random RANDOM = new Random();

    private static final ResourceLocation SEFIRAH_CASTLE =
            ResourceLocation.fromNamespaceAndPath(LOTMCraft.MOD_ID, "sefirah_castle");

    public static boolean isSealed(Level level) {
        return level.dimension().location().equals(SEFIRAH_CASTLE);
    }

    public static boolean slipAway(ServerLevel level, ItemEntity item) {
        if (level.dimensionType().hasCeiling()) return false;

        BlockPos from = item.blockPosition();
        double angle = RANDOM.nextDouble() * Math.PI * 2.0;
        double distance = 300.0 + RANDOM.nextDouble() * 900.0;
        int x = (int) Math.floor(item.getX() + Math.cos(angle) * distance);
        int z = (int) Math.floor(item.getZ() + Math.sin(angle) * distance);

        level.getChunk(x >> 4, z >> 4);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) + 1;

        level.sendParticles(ParticleTypes.PORTAL, item.getX(), item.getY() + 0.3, item.getZ(), 40, 0.3, 0.4, 0.3, 0.4);
        level.playSound(null, from, SoundEvents.ENDERMAN_TELEPORT, SoundSource.NEUTRAL, 1.0F, 0.6F);

        item.moveTo(x + 0.5, y, z + 0.5);
        item.setDeltaMovement(0.0, 0.0, 0.0);
        item.setUnlimitedLifetime();
        return true;
    }

    public static void ambient(ServerLevel level, ItemEntity item) {
        AABB area = item.getBoundingBox().inflate(8.0);
        BlockPos pos = item.blockPosition();

        switch (RANDOM.nextInt(6)) {
            case 0 -> {
                level.playSound(null, pos, SoundEvents.ENDERMAN_STARE, SoundSource.NEUTRAL, 0.6F, 0.5F);
                level.sendParticles(ParticleTypes.SOUL, item.getX(), item.getY() + 0.5, item.getZ(), 12, 0.4, 0.5, 0.4, 0.02);
            }
            case 1 -> {
                for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area)) {
                    entity.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 60, 0, false, true, true));
                }
                level.sendParticles(ParticleTypes.END_ROD, item.getX(), item.getY() + 0.5, item.getZ(), 20, 1.0, 0.5, 1.0, 0.05);
            }
            case 2 -> {
                for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area, e -> !(e instanceof Player))) {
                    entity.addEffect(new MobEffectInstance(de.jakob.lotm.effect.ModEffects.MUTATED, 200, 0, false, true, true));
                }
            }
            case 3 -> {
                for (Player player : level.getEntitiesOfClass(Player.class, area)) {
                    player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, false, true));
                }
            }
            case 4 -> {
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
                if (bolt != null) {
                    bolt.moveTo(item.getX() + RANDOM.nextInt(9) - 4, item.getY(), item.getZ() + RANDOM.nextInt(9) - 4);
                    bolt.setVisualOnly(true);
                    level.addFreshEntity(bolt);
                }
            }
            default -> {
                List<ItemEntity> nearby = level.getEntitiesOfClass(ItemEntity.class, area);
                for (ItemEntity other : nearby) {
                    other.setDeltaMovement(other.getDeltaMovement().add(0.0, 0.4, 0.0));
                }
            }
        }
    }
}

package com.Maul.lotmmi.item.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

public class ArtifactEffects {
    private static final Random RANDOM = new Random();

    public static void scepterAura(ServerLevel level, ServerPlayer player, long gameTime) {
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();

        for (int i = 0; i < 2; i++) {
            level.sendParticles(ParticleTypes.FALLING_WATER,
                    x + (RANDOM.nextDouble() - 0.5) * 2.4, y + 2.6, z + (RANDOM.nextDouble() - 0.5) * 2.4,
                    1, 0.0, 0.0, 0.0, 0.0);
        }
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                x + (RANDOM.nextDouble() - 0.5) * 1.6, y + 0.6 + RANDOM.nextDouble() * 1.6, z + (RANDOM.nextDouble() - 0.5) * 1.6,
                1, 0.1, 0.1, 0.1, 0.4);

        if (gameTime % 20 == 0) {
            for (int i = 0; i < 16; i++) {
                double angle = Math.PI * 2.0 * i / 16.0;
                level.sendParticles(ParticleTypes.SPLASH, x + Math.cos(angle) * 1.6, y + 0.1, z + Math.sin(angle) * 1.6,
                        1, 0.0, 0.0, 0.0, 0.0);
            }
        }
        if (gameTime % 120 == 0) {
            level.playSound(null, player.blockPosition(), SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, SoundSource.PLAYERS, 0.5F, 0.6F);
        }
    }

    public static void scepterCast(ServerLevel level, ServerPlayer player) {
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();

        for (int ring = 0; ring < 3; ring++) {
            double radius = 1.5 + ring * 1.2;
            for (int i = 0; i < 28; i++) {
                double angle = Math.PI * 2.0 * i / 28.0;
                level.sendParticles(ParticleTypes.SPLASH, x + Math.cos(angle) * radius, y + 0.2 + ring * 0.4, z + Math.sin(angle) * radius,
                        2, 0.0, 0.1, 0.0, 0.1);
            }
        }
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y + 1.2, z, 60, 1.0, 1.0, 1.0, 0.6);
        level.sendParticles(ParticleTypes.FLASH, x, y + 1.5, z, 1, 0.0, 0.0, 0.0, 0.0);

        Vec3 look = player.getLookAngle();
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(x + look.x * 10.0, y, z + look.z * 10.0);
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }

        level.playSound(null, player.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.7F, 1.5F);
        level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 1.0F, 0.6F);
    }

    public static void staffAura(ServerLevel level, ServerPlayer player, int panic, boolean sealed, long gameTime) {
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();

        level.sendParticles(ParticleTypes.END_ROD,
                x + (RANDOM.nextDouble() - 0.5) * 2.0, y + 0.3 + RANDOM.nextDouble() * 2.0, z + (RANDOM.nextDouble() - 0.5) * 2.0,
                1, 0.0, 0.02, 0.0, 0.01);
        level.sendParticles(ParticleTypes.ENCHANT, x, y + 1.6, z, 2, 0.7, 0.6, 0.7, 0.4);

        if (!sealed && panic >= 0) {
            level.sendParticles(ParticleTypes.SOUL, x + (RANDOM.nextDouble() - 0.5) * 1.6, y + 0.4 + RANDOM.nextDouble() * 1.8,
                    z + (RANDOM.nextDouble() - 0.5) * 1.6, 1 + panic, 0.2, 0.2, 0.2, 0.01);
        }
        if (gameTime % 160 == 0) {
            level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.4F, 1.6F);
        }
    }

    public static void staffCast(ServerLevel level, ServerPlayer player) {
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();

        for (int ring = 0; ring < 3; ring++) {
            for (int i = 0; i < 24; i++) {
                double angle = Math.PI * 2.0 * i / 24.0;
                double radius = 1.2 + ring * 1.0;
                level.sendParticles(ParticleTypes.END_ROD, x + Math.cos(angle) * radius, y + 0.5 + ring * 0.6, z + Math.sin(angle) * radius,
                        1, 0.0, 0.05, 0.0, 0.03);
            }
        }
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, x, y + 1.0, z, 40, 0.6, 0.8, 0.6, 0.2);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.5F);
        level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.5F, 1.6F);
    }

    public static void staffTeleport(ServerLevel level, double x, double y, double z, boolean arriving) {
        level.sendParticles(ParticleTypes.PORTAL, x, y + 1.0, z, 60, 0.4, 0.9, 0.4, 0.5);
        level.sendParticles(ParticleTypes.END_ROD, x, y + 1.0, z, 25, 0.5, 0.9, 0.5, 0.1);
        if (arriving) {
            level.sendParticles(ParticleTypes.FLASH, x, y + 1.0, z, 1, 0.0, 0.0, 0.0, 0.0);
        }
        level.playSound(null, BlockPos.containing(x, y, z), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8F, arriving ? 1.2F : 0.8F);
    }

    public static void droppedGlow(ServerLevel level, ItemEntity item, boolean staff) {
        double x = item.getX();
        double y = item.getY() + 0.3;
        double z = item.getZ();

        if (staff) {
            level.sendParticles(ParticleTypes.END_ROD, x, y, z, 1, 0.2, 0.3, 0.2, 0.03);
            level.sendParticles(ParticleTypes.ENCHANT, x, y + 0.5, z, 2, 0.4, 0.4, 0.4, 0.3);
        } else {
            level.sendParticles(ParticleTypes.FALLING_WATER, x + (RANDOM.nextDouble() - 0.5), y + 1.4, z + (RANDOM.nextDouble() - 0.5), 1, 0.0, 0.0, 0.0, 0.0);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 1, 0.3, 0.3, 0.3, 0.3);
        }
    }
}

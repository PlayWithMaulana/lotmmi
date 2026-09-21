package com.Maul.lotmmi.entity;

import com.Maul.lotmmi.LotmMysticalItems;
import com.Maul.lotmmi.entity.goals.AttackEverythingGoal;
import com.Maul.lotmmi.entity.goals.QilangosSoulAbilityGoal;
import de.jakob.lotm.entity.custom.BeyonderNPCEntity;
import de.jakob.lotm.gamerule.ModGameRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

public class QilangosEntity extends BeyonderNPCEntity {

    private static final ResourceLocation SKIN =
            ResourceLocation.fromNamespaceAndPath(LotmMysticalItems.MOD_ID, "textures/entity/qilangos.png");

    private static final float LAND_SPAWN_CHANCE = 0.27F;
    private static final float COAST_SPAWN_CHANCE = 0.37F;
    private static final double EXCLUSION_RADIUS = 128.0D;
    private static final int MAX_DEPTH_BELOW_SURFACE = 4;

    private static final int HYMN_INTERVAL_TICKS = 400;
    private static final int MIMIC_DURATION_TICKS = 600;
    private static final long DREAM_PULL_COOLDOWN = 400L;
    private static final long MIMIC_COOLDOWN = 1200L;

    private final Map<String, Long> customReadyAt = new HashMap<>();
    private boolean customGoalsAdded;
    private int mimicTicks;

    public QilangosEntity(EntityType<? extends QilangosEntity> type, Level level) {
        super(type, level, true, "qilangos", "tyrant", 6, false, false);
        if (!level.isClientSide) {
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
            this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return BeyonderNPCEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, 80.0D)
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.ARMOR, 4.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.4D);
    }

    public static boolean checkQilangosSpawn(EntityType<QilangosEntity> type, ServerLevelAccessor level,
                                             MobSpawnType reason, BlockPos pos, RandomSource random) {
        if (reason != MobSpawnType.NATURAL) return true;

        if (!level.getServer().overworld().getGameRules().getBoolean(ModGameRules.ALLOW_BEYONDER_SPAWNING)) {
            return false;
        }

        int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE, pos.getX(), pos.getZ());
        if (surfaceY - pos.getY() > MAX_DEPTH_BELOW_SURFACE) return false;

        BlockPos below = pos.below();
        boolean solidGround = level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
        boolean waterSurface = level.getFluidState(below).is(net.minecraft.tags.FluidTags.WATER);
        if (!solidGround && !waterSurface) return false;
        if (!level.getFluidState(pos).isEmpty()) return false;

        Holder<Biome> biome = level.getBiome(pos);
        boolean coastal = biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_BEACH);
        if (random.nextFloat() >= (coastal ? COAST_SPAWN_CHANCE : LAND_SPAWN_CHANCE)) return false;

        AABB area = new AABB(pos).inflate(EXCLUSION_RADIUS);
        return level.getEntitiesOfClass(QilangosEntity.class, area).isEmpty();
    }

    @Override
    public ResourceLocation getSkinTexture() {
        return SKIN;
    }

    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        if (this.level().isClientSide) return;

        if (!this.customGoalsAdded) {
            this.customGoalsAdded = true;
            this.goalSelector.addGoal(5, new QilangosSoulAbilityGoal(this));
            this.targetSelector.addGoal(2, new AttackEverythingGoal(this));
        }
        if (this.mimicTicks == 0 && this.getTargetPlayerUUID().isPresent()) {
            this.setTargetPlayerUUID(null);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) return;

        if (this.mimicTicks > 0 && --this.mimicTicks == 0) {
            this.setTargetPlayerUUID(null);
        }
        if (this.tickCount % 20 == 0) {
            this.removeEffect(MobEffects.CONFUSION);
            this.removeEffect(MobEffects.BLINDNESS);
            this.removeEffect(MobEffects.DARKNESS);
        }
        if (this.tickCount % HYMN_INTERVAL_TICKS == 0 && this.getTarget() != null) {
            this.singHymn();
        }
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        this.spawnAtLocation(QilangosSouls.createCreepingHunger());
    }

    public boolean castCustom(String abilityId, LivingEntity target) {
        long now = this.level().getGameTime();
        switch (abilityId) {
            case "nightmare_ability":
                if (this.isReady("dream", now) && this.dreamPull(target)) {
                    this.customReadyAt.put("dream", now + DREAM_PULL_COOLDOWN);
                    return true;
                }
                return false;
            case "shapeshifting_ability":
                if (target instanceof Player player && this.mimicTicks == 0 && this.isReady("mimic", now)) {
                    this.mimic(player);
                    this.customReadyAt.put("mimic", now + MIMIC_COOLDOWN);
                    return true;
                }
                return false;
            default:
                return false;
        }
    }

    private boolean isReady(String key, long now) {
        return this.customReadyAt.getOrDefault(key, 0L) <= now;
    }

    private boolean dreamPull(LivingEntity target) {
        if (!(this.level() instanceof ServerLevel server)) return false;
        if (this.distanceToSqr(target) > 24.0D * 24.0D || !this.hasLineOfSight(target)) return false;

        target.addEffect(new MobEffectInstance(de.jakob.lotm.effect.ModEffects.ASLEEP, 60, 0, false, true, true), this);
        target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 120, 0, false, false, true), this);
        target.hurt(this.damageSources().magic(), 8.0F);

        DustParticleOptions dust = new DustParticleOptions(new Vector3f(0.98F, 0.16F, 0.25F), 1.5F);
        server.sendParticles(dust, target.getX(), target.getY() + 1.0D, target.getZ(), 40, 0.5D, 0.8D, 0.5D, 0.0D);
        this.playSound(SoundEvents.WITHER_AMBIENT, 1.0F, 0.5F);
        return true;
    }

    private void mimic(Player player) {
        this.setTargetPlayerUUID(player.getUUID());
        this.mimicTicks = MIMIC_DURATION_TICKS;
        if (this.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.POOF, this.getX(), this.getY() + 1.0D, this.getZ(), 25, 0.4D, 0.8D, 0.4D, 0.02D);
        }
    }

    private void singHymn() {
        this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 240, 0));
        this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 240, 0));
        if (this.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.NOTE, this.getX(), this.getY() + 2.1D, this.getZ(), 12, 0.6D, 0.4D, 0.6D, 1.0D);
        }
        this.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.5F, 0.6F);
    }
}

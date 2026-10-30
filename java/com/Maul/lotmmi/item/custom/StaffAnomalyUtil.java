package com.Maul.lotmmi.item.custom;

import com.Maul.lotmmi.LotmMysticalItems;
import com.Maul.lotmmi.config.LotmmiConfig;
import com.Maul.lotmmi.events.StaffCurseHandler;
import com.Maul.lotmmi.events.StaffEchoHandler;
import de.jakob.lotm.util.scheduling.ServerScheduler;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class StaffAnomalyUtil {
    public enum Trigger { MISFIRE, INTRUSIVE }

    private enum Category { POWER, MIND, BODY, SPACE, FORTUNE, ECHO, BOON }

    private enum Anomaly {
        FIZZLE(Category.POWER), BACKLASH(Category.POWER), WRONG_RECALL(Category.POWER), WHEEL_SHUFFLE(Category.POWER),
        BLANK_OUT(Category.MIND), MENTAL_PLAGUE(Category.MIND),
        CURSE(Category.BODY), SHRINK(Category.BODY), ENLARGE(Category.BODY), MUTATED(Category.BODY),
        DISPLACE(Category.SPACE), SWAP(Category.SPACE),
        UNLUCK(Category.FORTUNE), STATIC_SHOCK(Category.FORTUNE), CALAMITY(Category.FORTUNE),
        ECHO_FIGURE(Category.ECHO),
        BOON_VIGOR(Category.BOON), BOON_CLARITY(Category.BOON);

        private final Category category;

        Anomaly(Category category) {
            this.category = category;
        }
    }

    private record Entry(Anomaly anomaly, int minPanic, int weight) {}

    private static final Random RANDOM = new Random();

    private static final double INTRUSIVE_CHANCE_PER_SECOND = 0.002D;
    private static final long COOLDOWN_TICKS = 200L;

    private static final ResourceLocation SCALE_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(LotmMysticalItems.MOD_ID, "staff_anomaly_scale");

    private static final Map<UUID, Long> LAST_ANOMALY = new HashMap<>();

    private static final List<Entry> MISFIRE_TABLE = List.of(
            new Entry(Anomaly.FIZZLE, -1, 6),
            new Entry(Anomaly.BACKLASH, -1, 4),
            new Entry(Anomaly.WRONG_RECALL, 0, 3),
            new Entry(Anomaly.DISPLACE, 0, 3),
            new Entry(Anomaly.UNLUCK, 0, 3),
            new Entry(Anomaly.WHEEL_SHUFFLE, 0, 2),
            new Entry(Anomaly.CURSE, 1, 3),
            new Entry(Anomaly.BLANK_OUT, 1, 2),
            new Entry(Anomaly.SWAP, 1, 2),
            new Entry(Anomaly.MUTATED, 1, 2),
            new Entry(Anomaly.SHRINK, 1, 1),
            new Entry(Anomaly.ENLARGE, 1, 1),
            new Entry(Anomaly.STATIC_SHOCK, 2, 2),
            new Entry(Anomaly.ECHO_FIGURE, 2, 2),
            new Entry(Anomaly.CALAMITY, 2, 1),
            new Entry(Anomaly.MENTAL_PLAGUE, 3, 2)
    );

    private static final List<Entry> INTRUSIVE_TABLE = List.of(
            new Entry(Anomaly.WRONG_RECALL, 0, 6),
            new Entry(Anomaly.DISPLACE, -1, 3),
            new Entry(Anomaly.UNLUCK, -1, 3),
            new Entry(Anomaly.BOON_VIGOR, -1, 2),
            new Entry(Anomaly.BOON_CLARITY, 0, 2),
            new Entry(Anomaly.WHEEL_SHUFFLE, 0, 1),
            new Entry(Anomaly.SWAP, 1, 2),
            new Entry(Anomaly.BLANK_OUT, 1, 2),
            new Entry(Anomaly.MUTATED, 1, 2),
            new Entry(Anomaly.SHRINK, 1, 1),
            new Entry(Anomaly.ENLARGE, 1, 1),
            new Entry(Anomaly.CURSE, 1, 1),
            new Entry(Anomaly.STATIC_SHOCK, 2, 1),
            new Entry(Anomaly.ECHO_FIGURE, 2, 1),
            new Entry(Anomaly.CALAMITY, 2, 1),
            new Entry(Anomaly.MENTAL_PLAGUE, 3, 1)
    );

    private static boolean enabled(Category category) {
        return switch (category) {
            case POWER -> LotmmiConfig.STAFF_ANOMALY_POWER.get();
            case MIND -> LotmmiConfig.STAFF_ANOMALY_MIND.get();
            case BODY -> LotmmiConfig.STAFF_ANOMALY_BODY.get();
            case SPACE -> LotmmiConfig.STAFF_ANOMALY_SPACE.get();
            case FORTUNE -> LotmmiConfig.STAFF_ANOMALY_FORTUNE.get();
            case ECHO -> LotmmiConfig.STAFF_ANOMALY_ECHO.get();
            case BOON -> true;
        };
    }

    public static void rollMisfire(ServerLevel level, ServerPlayer player, ItemStack staff) {
        roll(Trigger.MISFIRE, level, player, staff);
    }

    public static void rollIntrusive(ServerLevel level, ServerPlayer player, ItemStack staff) {
        if (StaffSealUtil.isSealed(level)) return;
        if (RANDOM.nextDouble() >= PanicUtil.scale(INTRUSIVE_CHANCE_PER_SECOND, player) * mindFactor(level, player, staff)) return;
        roll(Trigger.INTRUSIVE, level, player, staff);
    }

    private static double mindFactor(ServerLevel level, ServerPlayer player, ItemStack staff) {
        double factor = 1.0D;

        boolean still = player.getDeltaMovement().horizontalDistanceSqr() < 1.0E-4D;
        if (player.isShiftKeyDown() && still) factor *= 0.25D;

        if (player.hurtTime > 0 || player.getLastHurtByMobTimestamp() > player.tickCount - 100) factor *= 3.0D;

        List<StaffMemoryUtil.EntityWatch> watches = StaffMemoryUtil.getEntityWatches(staff);
        if (!watches.isEmpty()) {
            AABB area = player.getBoundingBox().inflate(16.0);
            for (LivingEntity nearby : level.getEntitiesOfClass(LivingEntity.class, area, e -> e != player)) {
                String key = String.valueOf(EntityType.getKey(nearby.getType()));
                boolean recorded = false;
                for (StaffMemoryUtil.EntityWatch watch : watches) {
                    if (watch.entityType().equals(key)) {
                        recorded = true;
                        break;
                    }
                }
                if (recorded) {
                    factor *= 2.0D;
                    break;
                }
            }
        }
        return factor;
    }

    private static void roll(Trigger trigger, ServerLevel level, ServerPlayer player, ItemStack staff) {
        if (StaffSealUtil.isSealed(level)) {
            if (trigger == Trigger.MISFIRE) apply(Anomaly.FIZZLE, level, player, staff);
            return;
        }

        long now = level.getGameTime();
        Long last = LAST_ANOMALY.get(player.getUUID());
        boolean coolingDown = last != null && now - last < COOLDOWN_TICKS;

        Anomaly anomaly;
        if (coolingDown) {
            if (trigger == Trigger.INTRUSIVE) return;
            anomaly = Anomaly.FIZZLE;
        } else {
            anomaly = pick(trigger == Trigger.MISFIRE ? MISFIRE_TABLE : INTRUSIVE_TABLE, player, staff);
        }

        if (anomaly != Anomaly.FIZZLE) LAST_ANOMALY.put(player.getUUID(), now);
        apply(anomaly, level, player, staff);
    }

    private static Anomaly pick(List<Entry> table, ServerPlayer player, ItemStack staff) {
        int panic = PanicUtil.getAmplifier(player);
        List<String> wheel = StaffMemoryUtil.getWheel(staff);

        List<Entry> usable = new ArrayList<>();
        int total = 0;
        for (Entry entry : table) {
            Anomaly anomaly = entry.anomaly();
            if (panic < entry.minPanic()) continue;
            if (anomaly != Anomaly.FIZZLE && !enabled(anomaly.category)) continue;
            if (anomaly == Anomaly.WRONG_RECALL && wheel.isEmpty()) continue;
            if (anomaly == Anomaly.WHEEL_SHUFFLE && wheel.size() < 2) continue;
            if (anomaly == Anomaly.ECHO_FIGURE && !StaffEchoHandler.hasCandidate(player, staff)) continue;
            usable.add(entry);
            total += entry.weight();
        }
        if (usable.isEmpty() || total <= 0) return Anomaly.FIZZLE;

        int roll = RANDOM.nextInt(total);
        for (Entry entry : usable) {
            roll -= entry.weight();
            if (roll < 0) return entry.anomaly();
        }
        return Anomaly.FIZZLE;
    }

    private static void apply(Anomaly anomaly, ServerLevel level, ServerPlayer player, ItemStack staff) {
        int panic = Math.max(0, PanicUtil.getAmplifier(player));

        switch (anomaly) {
            case FIZZLE -> {
                level.sendParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + 1.0, player.getZ(), 12, 0.3, 0.4, 0.3, 0.02);
                say(player, "The power slips away from you.", ChatFormatting.GRAY);
            }
            case BACKLASH -> {
                player.hurt(player.damageSources().magic(), Math.min(8.0F, 3.0F + panic));
                say(player, "The power backfires on you.", ChatFormatting.RED);
            }
            case WRONG_RECALL -> {
                if (!StaffAbilityUtil.castRandomWheelAbility(level, player, staff, "The stars flicker without your command - ")) {
                    say(player, "The power slips away from you.", ChatFormatting.GRAY);
                }
            }
            case WHEEL_SHUFFLE -> shuffleWheel(player, staff);
            case DISPLACE -> displace(level, player);
            case SWAP -> swap(level, player);
            case BLANK_OUT -> {
                int ticks = 40 + 20 * panic;
                player.addEffect(new MobEffectInstance(de.jakob.lotm.effect.ModEffects.ASLEEP, ticks, 0, false, true, true), player);
                say(player, "Your mind goes blank.", ChatFormatting.DARK_PURPLE);
            }
            case MENTAL_PLAGUE -> {
                player.addEffect(new MobEffectInstance(de.jakob.lotm.effect.ModEffects.MENTAL_PLAGUE, 20 * 6, 0, false, true, true), player);
                say(player, "Something is attacking your mind.", ChatFormatting.DARK_PURPLE);
            }
            case UNLUCK -> {
                player.addEffect(new MobEffectInstance(MobEffects.UNLUCK, 20 * (60 + 20 * panic), 0, false, true, true));
                say(player, "Your luck turns bad.", ChatFormatting.DARK_GREEN);
            }
            case SHRINK -> rescale(player, -0.5D, "You feel yourself growing small.");
            case ENLARGE -> rescale(player, 0.5D, "You feel yourself swelling larger.");
            case MUTATED -> {
                player.addEffect(new MobEffectInstance(de.jakob.lotm.effect.ModEffects.MUTATED, 20 * (8 + 4 * panic), 0, false, true, true), player);
                say(player, "Your body changes shape.", ChatFormatting.DARK_RED);
            }
            case CURSE -> StaffCurseHandler.curse(player, Math.min(60, 20 + 10 * panic + RANDOM.nextInt(11)));
            case STATIC_SHOCK -> staticShock(level, player);
            case CALAMITY -> calamity(level, player, panic);
            case ECHO_FIGURE -> {
                if (!StaffEchoHandler.spawn(level, player, staff)) {
                    say(player, "The power slips away from you.", ChatFormatting.GRAY);
                }
            }
            case BOON_VIGOR -> {
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 10, 0, false, true, true));
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 20 * 20, 0, false, true, true));
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 20, 0, false, true, true));
                say(player, "The stars grant you clarity and strength.", ChatFormatting.AQUA);
            }
            case BOON_CLARITY -> {
                PanicUtil.calm(player);
                say(player, "You feel calm.", ChatFormatting.AQUA);
            }
        }
    }

    private static void say(ServerPlayer player, String text, ChatFormatting color) {
        player.displayClientMessage(Component.literal(text).withStyle(color), true);
    }

    private static void shuffleWheel(ServerPlayer player, ItemStack staff) {
        List<String> wheel = StaffMemoryUtil.getWheel(staff);
        if (wheel.size() < 2) return;

        for (int i = 0; i < 4; i++) {
            StaffMemoryUtil.moveInWheel(staff, RANDOM.nextInt(wheel.size()), RANDOM.nextBoolean() ? 1 : -1);
        }
        say(player, "Your abilities are shuffled.", ChatFormatting.LIGHT_PURPLE);
    }

    private static void displace(ServerLevel level, ServerPlayer player) {
        EntityDimensions dimensions = player.getDimensions(player.getPose());

        for (int attempt = 0; attempt < 16; attempt++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2.0;
            double distance = 8.0 + RANDOM.nextDouble() * 16.0;
            double x = player.getX() + Math.cos(angle) * distance;
            double z = player.getZ() + Math.sin(angle) * distance;

            for (int dy = 8; dy >= -8; dy--) {
                double y = Math.floor(player.getY()) + dy;
                if (y < level.getMinBuildHeight() + 1 || y >= level.getMaxBuildHeight() - 2) continue;

                BlockPos below = BlockPos.containing(x, y - 1.0, z);
                if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) continue;
                if (!level.getBlockState(below).getFluidState().isEmpty()) continue;
                if (!level.noCollision(dimensions.makeBoundingBox(x, y, z))) continue;

                level.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 1.0, player.getZ(), 30, 0.4, 0.8, 0.4, 0.3);
                player.teleportTo(level, x, y, z, player.getYRot(), player.getXRot());
                level.sendParticles(ParticleTypes.PORTAL, x, y + 1.0, z, 30, 0.4, 0.8, 0.4, 0.3);
                say(player, "You are pulled somewhere else.", ChatFormatting.LIGHT_PURPLE);
                return;
            }
        }
        say(player, "Space shivers, then settles.", ChatFormatting.GRAY);
    }

    private static void swap(ServerLevel level, ServerPlayer player) {
        AABB area = player.getBoundingBox().inflate(16.0);
        LivingEntity nearest = null;
        double best = Double.MAX_VALUE;
        for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, area,
                e -> e != player && e.isAlive() && !(e instanceof Player))) {
            double distance = candidate.distanceToSqr(player);
            if (distance < best) {
                best = distance;
                nearest = candidate;
            }
        }

        if (nearest == null) {
            say(player, "Space shivers, then settles.", ChatFormatting.GRAY);
            return;
        }

        Vec3 playerPos = player.position();
        Vec3 otherPos = nearest.position();
        nearest.moveTo(playerPos);
        nearest.setDeltaMovement(Vec3.ZERO);
        player.teleportTo(level, otherPos.x, otherPos.y, otherPos.z, player.getYRot(), player.getXRot());
        say(player, "You and " + nearest.getName().getString() + " change places.", ChatFormatting.LIGHT_PURPLE);
    }

    private static void rescale(ServerPlayer player, double amount, String message) {
        AttributeInstance scale = player.getAttribute(Attributes.SCALE);
        if (scale == null) return;

        scale.removeModifier(SCALE_MODIFIER_ID);
        scale.addTransientModifier(new AttributeModifier(SCALE_MODIFIER_ID, amount, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        say(player, message, ChatFormatting.LIGHT_PURPLE);

        UUID id = player.getUUID();
        ServerScheduler.scheduleDelayed(20 * 30, () -> {
            if (player.getServer() == null) return;
            ServerPlayer online = player.getServer().getPlayerList().getPlayer(id);
            if (online == null) return;
            AttributeInstance current = online.getAttribute(Attributes.SCALE);
            if (current != null) current.removeModifier(SCALE_MODIFIER_ID);
        });
    }

    private static void staticShock(ServerLevel level, ServerPlayer player) {
        double angle = RANDOM.nextDouble() * Math.PI * 2.0;
        double distance = 5.0 + RANDOM.nextDouble() * 4.0;
        Vec3 spot = new Vec3(player.getX() + Math.cos(angle) * distance, player.getY(), player.getZ() + Math.sin(angle) * distance);

        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(spot);
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
        player.hurt(player.damageSources().magic(), 2.0F);
        say(player, "Lightning strikes nearby.", ChatFormatting.YELLOW);
    }

    private static void calamity(ServerLevel level, ServerPlayer player, int panic) {
        level.setWeatherParameters(0, 20 * 60 * 3, true, true);

        int bolts = 2 + panic;
        for (int i = 0; i < bolts; i++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2.0;
            double distance = 8.0 + RANDOM.nextDouble() * 8.0;
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
            if (bolt == null) continue;
            bolt.moveTo(player.getX() + Math.cos(angle) * distance, player.getY(), player.getZ() + Math.sin(angle) * distance);
            level.addFreshEntity(bolt);
        }
        say(player, "Calamity strikes!", ChatFormatting.DARK_RED);
    }
}

package com.Maul.lotmmi.item.custom;

import com.Maul.lotmmi.config.LotmmiConfig;
import com.Maul.lotmmi.data.ModDataComponents;
import com.Maul.lotmmi.effect.ModEffects;
import com.Maul.lotmmi.events.ScepterFollowerTracker;
import com.Maul.lotmmi.events.ScepterGazeHandler;
import com.Maul.lotmmi.network.packets.toClient.SyncScepterIntrospectPacket;
import com.Maul.lotmmi.gui.ScepterIntrospectMenuProvider;
import de.jakob.lotm.LOTMCraft;
import de.jakob.lotm.attachments.ModAttachments;
import de.jakob.lotm.attachments.SanityComponent;
import de.jakob.lotm.attachments.SefirotData;
import de.jakob.lotm.beyonders.abilities.core.Ability;
import de.jakob.lotm.beyonders.artifacts.NegativeEffect;
import de.jakob.lotm.beyonders.artifacts.SealedArtifactData;
import de.jakob.lotm.dimension.ModDimensions;
import de.jakob.lotm.util.BeyonderData;
import de.jakob.lotm.util.helper.AbilityUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SeaGodScepterItem extends Item {
    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    public static final String PATHWAY = "tyrant";
    public static final int SEQUENCE = 3;
    public static final int DEMIGOD_SEQUENCE = 4;
    public static final int INSTANT_LOSS_SEQUENCE = 9;

    public static final List<String> ABILITY_IDS = List.of(
            "wind_manipulation_ability",
            "strong_lightning_ability",
            "tsunami_ability",
            "hurricane_ability",
            "water_mastery_ability",
            "torrential_downpour_ability",
            "lightning_storm_ability",
            "wrath_of_the_ocean_ability"
    );

    public static long bloodDrainIntervalTicks() {
        return LotmmiConfig.BLOOD_DRAIN_INTERVAL_MINUTES.get() * 60L * 20L;
    }

    public static final int BLOOD_DRAIN_RADIUS = 32;
    public static final int BLOOD_DRAIN_HEIGHT = 16;
    private static final int BLOOD_DRAIN_DURATION_TICKS = 20 * 60;

    private static final float GRADUAL_SANITY_DRAIN_PER_STEP = 0.0006F;
    private static final float INSTANT_LOSS_SANITY_CAP = 0.02F;

    private static final int HELD_EFFECT_DURATION_TICKS = 40;
    private static final int HELD_EFFECT_AMPLIFIER = 0;

    public static final int FOLLOWER_SEQUENCE_GAP = 2;
    public static final long BLESS_COOLDOWN_TICKS = 5L * 60 * 20;
    public static final long ATTACK_COOLDOWN_TICKS = 30L * 20;

    public SeaGodScepterItem(Properties properties) {
        super(properties);
    }

    private static List<NegativeEffect> tokenNegatives() {
        List<NegativeEffect> list = new ArrayList<>();
        list.add(new NegativeEffect(NegativeEffect.NegativeEffectType.CURSED, 0, null, 0));
        return list;
    }

    private static void ensureArtifactData(ItemStack stack) {
        if (stack.has(de.jakob.lotm.data.ModDataComponents.SEALED_ARTIFACT_DATA.get())) return;

        List<Ability> abilities = new ArrayList<>();
        for (String id : ABILITY_IDS) {
            Ability ability = LOTMCraft.abilityHandler.getById(id);
            if (ability != null) abilities.add(ability);
        }

        stack.set(de.jakob.lotm.data.ModDataComponents.SEALED_ARTIFACT_DATA.get(),
                new SealedArtifactData(PATHWAY, SEQUENCE, abilities, tokenNegatives()));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        if (player.isShiftKeyDown() && player instanceof ServerPlayer gazer && ScepterGazeHandler.isGazing(gazer)) {
            ScepterGazeHandler.end(gazer);
            return InteractionResultHolder.success(stack);
        }

        ensureArtifactData(stack);

        SealedArtifactData data = stack.get(de.jakob.lotm.data.ModDataComponents.SEALED_ARTIFACT_DATA.get());
        if (data == null || data.abilities().isEmpty()) {
            return InteractionResultHolder.fail(stack);
        }

        int selectedIndex = stack.getOrDefault(de.jakob.lotm.data.ModDataComponents.SEALED_ARTIFACT_SELECTED.get(), 0);
        if (selectedIndex >= data.abilities().size()) selectedIndex = 0;
        Ability ability = data.abilities().get(selectedIndex);

        AbilityUtil.setArtifactScaling(player, PATHWAY, SEQUENCE);
        ability.useAbility((ServerLevel) level, player, true, false, true, false);

        if (player instanceof ServerPlayer caster) {
            ArtifactEffects.scepterCast((ServerLevel) level, caster);
        }

        return InteractionResultHolder.success(stack);
    }

    @Override
    public net.minecraft.world.InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (player.level().isClientSide || !player.isShiftKeyDown() || !(player instanceof ServerPlayer wielder)) {
            return net.minecraft.world.InteractionResult.PASS;
        }

        ensureArtifactData(stack);

        if (ScepterFollowerUtil.isFollower(stack, target.getUUID())) {
            wielder.displayClientMessage(Component.literal(target.getName().getString() + " already follows you.")
                    .withStyle(ChatFormatting.GRAY), true);
            return net.minecraft.world.InteractionResult.SUCCESS;
        }

        if (!canMark(wielder, target)) {
            wielder.displayClientMessage(Component.literal("This target is too strong to become a follower.")
                    .withStyle(ChatFormatting.RED), true);
            return net.minecraft.world.InteractionResult.SUCCESS;
        }

        ScepterFollowerUtil.addFollower(stack, target.getUUID(), target.getName().getString(),
                target.level().dimension(), target.getX(), target.getY(), target.getZ());
        wielder.displayClientMessage(Component.literal(target.getName().getString() + " is now your follower.")
                .withStyle(ChatFormatting.LIGHT_PURPLE), true);

        return net.minecraft.world.InteractionResult.SUCCESS;
    }

    public static void openIntrospectMenu(ServerPlayer player, ItemStack stack, boolean mainHand) {
        ensureArtifactData(stack);

        ScepterIntrospectMenuProvider.Snapshot snapshot = ScepterIntrospectMenuProvider.snapshot(player, stack, mainHand);
        player.openMenu(new ScepterIntrospectMenuProvider(snapshot), snapshot::write);
    }

    public static List<ItemStack> findScepters(ServerPlayer player) {
        List<ItemStack> found = new ArrayList<>();
        int size = player.getInventory().getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemStack candidate = player.getInventory().getItem(i);
            if (candidate.getItem() instanceof SeaGodScepterItem) found.add(candidate);
        }
        return found;
    }

    public static ItemStack primaryScepter(ServerPlayer player) {
        if (player.getMainHandItem().getItem() instanceof SeaGodScepterItem) return player.getMainHandItem();
        if (player.getOffhandItem().getItem() instanceof SeaGodScepterItem) return player.getOffhandItem();

        List<ItemStack> found = findScepters(player);
        return found.isEmpty() ? ItemStack.EMPTY : found.get(0);
    }

    public static void resync(ServerPlayer player) {
        if (player.getMainHandItem().getItem() instanceof SeaGodScepterItem) {
            SyncScepterIntrospectPacket.sendTo(player, true, player.getMainHandItem());
        } else if (player.getOffhandItem().getItem() instanceof SeaGodScepterItem) {
            SyncScepterIntrospectPacket.sendTo(player, false, player.getOffhandItem());
        }
    }

    public static boolean canGaze(ServerPlayer player) {
        if (ScepterGazeHandler.isGazing(player)) return false;
        return !LotmmiConfig.GAZE_REQUIRES_CASTLE.get() || isInsideOwnSefirahCastle(player);
    }

    public static void answerPrayer(ServerPlayer wielder, ItemStack stack, UUID prayerId) {
        ScepterPrayerUtil.Prayer prayer = ScepterPrayerUtil.getPrayerById(stack, prayerId);
        if (prayer == null) {
            wielder.displayClientMessage(Component.literal("That prayer is gone.")
                    .withStyle(ChatFormatting.GRAY), true);
            return;
        }

        ServerPlayer reciter = wielder.getServer() == null ? null : wielder.getServer().getPlayerList().getPlayer(prayer.id());
        if (reciter == null) {
            wielder.displayClientMessage(Component.literal(prayer.name() + " is out of reach.")
                    .withStyle(ChatFormatting.GRAY), true);
            return;
        }

        reciter.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 20, 1, false, true, true));
        reciter.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 20 * 30, 1, false, true, true));
        reciter.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 20, 0, false, true, true));
        reciter.displayClientMessage(Component.literal("Your prayer has been answered.")
                .withStyle(ChatFormatting.AQUA), true);
        wielder.displayClientMessage(Component.literal("You answer " + prayer.name() + "'s prayer.")
                .withStyle(ChatFormatting.LIGHT_PURPLE), true);

        ScepterPrayerUtil.removePrayerById(stack, prayerId);
    }

    public static void gazeAtPrayer(ServerPlayer wielder, ItemStack stack, UUID prayerId) {
        ScepterPrayerUtil.Prayer prayer = ScepterPrayerUtil.getPrayerById(stack, prayerId);
        if (prayer == null) {
            wielder.displayClientMessage(Component.literal("That prayer is gone.")
                    .withStyle(ChatFormatting.GRAY), true);
            return;
        }
        ScepterGazeHandler.begin(wielder, prayer);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (level.isClientSide || !(entity instanceof ServerPlayer player)) return;

        ensureArtifactData(stack);

        boolean held = player.getMainHandItem() == stack || player.getOffhandItem() == stack;
        if (held) {
            applyHeldBuffs(player);
            applySequenceControl(player);
            if (level.getGameTime() % 4 == 0) {
                ArtifactEffects.scepterAura((ServerLevel) level, player, level.getGameTime());
            }
        }

        tickBloodDrain(player, stack, level.getGameTime());
    }

    private void applyHeldBuffs(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, HELD_EFFECT_DURATION_TICKS, HELD_EFFECT_AMPLIFIER, true, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, HELD_EFFECT_DURATION_TICKS, HELD_EFFECT_AMPLIFIER, true, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.CONDUIT_POWER, HELD_EFFECT_DURATION_TICKS, HELD_EFFECT_AMPLIFIER, true, false, false));
    }

    public static boolean isInsideOwnSefirahCastle(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) return false;

        SefirotData sefirotData = SefirotData.get(server);
        String claimed = sefirotData.getClaimedSefirot(player.getUUID());
        return !claimed.isEmpty() && sefirotData.isInSefirot(player, claimed);
    }

    private boolean isShelteredFromScepter(ServerPlayer player) {
        if (player.level().dimension() == ModDimensions.BROOD_HIVE_DIMENSION_KEY) return true;
        if (ScepterGazeHandler.isGazing(player)) return true;
        return isInsideOwnSefirahCastle(player);
    }

    public static boolean canMark(ServerPlayer wielder, LivingEntity target) {
        if (!BeyonderData.isBeyonder(wielder) || !BeyonderData.isBeyonder(target)) return false;
        return BeyonderData.getSequence(target) >= BeyonderData.getSequence(wielder) + FOLLOWER_SEQUENCE_GAP;
    }

    private static void relocateEntity(LivingEntity entity, ServerLevel targetLevel, double x, double y, double z) {
        targetLevel.getChunk((int) x >> 4, (int) z >> 4);

        if (entity instanceof ServerPlayer targetPlayer) {
            targetPlayer.teleportTo(targetLevel, x, y, z, targetPlayer.getYRot(), targetPlayer.getXRot());
            return;
        }

        if (entity.level() == targetLevel) {
            entity.moveTo(x, y, z, entity.getYRot(), entity.getXRot());
            return;
        }

        CompoundTag data = new CompoundTag();
        entity.saveWithoutId(data);
        Entity fresh = entity.getType().create(targetLevel);
        if (fresh == null) return;

        fresh.load(data);
        fresh.moveTo(x, y, z, entity.getYRot(), entity.getXRot());
        entity.discard();
        targetLevel.addFreshEntity(fresh);
    }

    public static void blessFollower(ServerPlayer wielder, ItemStack stack, UUID followerId) {
        ScepterFollowerUtil.Follower follower = ScepterFollowerUtil.getFollower(stack, followerId);
        if (follower == null) return;

        long now = wielder.level().getGameTime();
        if (now - follower.lastBlessTick() < BLESS_COOLDOWN_TICKS) {
            wielder.displayClientMessage(Component.literal("This follower was blessed recently.")
                    .withStyle(ChatFormatting.GRAY), true);
            return;
        }

        ScepterFollowerTracker.run(wielder, stack, followerId, (w, scepter, current, target) -> {
            target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1, false, true, true));
            target.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 1, false, true, true));
            target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 0, false, true, true));

            ScepterFollowerUtil.updateFollower(scepter, current.withCooldowns(w.level().getGameTime(), current.lastAttackTick()));
        });
    }

    public static void attackFollower(ServerPlayer wielder, ItemStack stack, UUID followerId) {
        ScepterFollowerUtil.Follower follower = ScepterFollowerUtil.getFollower(stack, followerId);
        if (follower == null) return;

        long now = wielder.level().getGameTime();
        if (now - follower.lastAttackTick() < ATTACK_COOLDOWN_TICKS) {
            wielder.displayClientMessage(Component.literal("The lightning is not ready yet.")
                    .withStyle(ChatFormatting.GRAY), true);
            return;
        }

        ScepterFollowerTracker.run(wielder, stack, followerId, (w, scepter, current, target) -> {
            if (target.level() instanceof ServerLevel targetLevel) {
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(targetLevel);
                if (bolt != null) {
                    bolt.moveTo(target.position());
                    targetLevel.addFreshEntity(bolt);
                }
            }

            ScepterFollowerUtil.updateFollower(scepter, current.withCooldowns(current.lastBlessTick(), w.level().getGameTime()));
        });
    }

    public static void toggleSummon(ServerPlayer wielder, ItemStack stack, UUID followerId) {
        if (!isInsideOwnSefirahCastle(wielder)) {
            wielder.displayClientMessage(Component.literal("You must be in your own Sefirah Castle.")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        if (ScepterFollowerUtil.getFollower(stack, followerId) == null) return;

        ScepterFollowerTracker.run(wielder, stack, followerId, SeaGodScepterItem::performSummonToggle);
    }

    private static void performSummonToggle(ServerPlayer wielder, ItemStack stack, ScepterFollowerUtil.Follower follower, LivingEntity target) {
        if (!isInsideOwnSefirahCastle(wielder)) return;
        if (!(wielder.level() instanceof ServerLevel castleLevel)) return;

        if (follower.summoned()) {
            ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION,
                    ResourceLocation.parse(follower.returnDimension()));
            ServerLevel returnLevel = wielder.getServer() == null ? null : wielder.getServer().getLevel(dimension);
            if (returnLevel == null) {
                wielder.displayClientMessage(Component.literal("Their original location can't be found.")
                        .withStyle(ChatFormatting.RED), true);
                return;
            }

            relocateEntity(target, returnLevel, follower.returnX(), follower.returnY(), follower.returnZ());
            ScepterFollowerUtil.updateFollower(stack, follower.withSummoned(false, dimension,
                    follower.returnX(), follower.returnY(), follower.returnZ())
                    .withLastKnown(dimension, follower.returnX(), follower.returnY(), follower.returnZ()));
        } else {
            ResourceKey<Level> originalDimension = target.level().dimension();
            double originalX = target.getX();
            double originalY = target.getY();
            double originalZ = target.getZ();

            relocateEntity(target, castleLevel, wielder.getX(), wielder.getY(), wielder.getZ());
            ScepterFollowerUtil.updateFollower(stack, follower.withSummoned(true, originalDimension,
                    originalX, originalY, originalZ)
                    .withLastKnown(castleLevel.dimension(), wielder.getX(), wielder.getY(), wielder.getZ()));
        }
    }

    private void applySequenceControl(ServerPlayer player) {
        if (isShelteredFromScepter(player)) return;

        boolean beyonder = BeyonderData.isBeyonder(player);
        int sequence = beyonder ? BeyonderData.getSequence(player) : INSTANT_LOSS_SEQUENCE;

        if (beyonder && sequence <= DEMIGOD_SEQUENCE) return;

        SanityComponent sanity = player.getData(ModAttachments.SANITY_COMPONENT.get());

        if (sequence >= INSTANT_LOSS_SEQUENCE) {
            sanity.setSanityAndSync(Math.min(sanity.getSanity(), INSTANT_LOSS_SANITY_CAP), player);
            return;
        }

        float drain = GRADUAL_SANITY_DRAIN_PER_STEP * (sequence - DEMIGOD_SEQUENCE);
        sanity.setSanityAndSync(Math.max(0.0F, sanity.getSanity() - drain), player);
    }

    private void tickBloodDrain(ServerPlayer player, ItemStack stack, long gameTime) {
        long last = stack.getOrDefault(ModDataComponents.SCEPTER_LAST_DRAIN_TICK.get(), gameTime);
        if (gameTime - last < bloodDrainIntervalTicks()) return;
        if (isShelteredFromScepter(player)) return;
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        stack.set(ModDataComponents.SCEPTER_LAST_DRAIN_TICK.get(), gameTime);

        AABB area = new AABB(
                player.getX() - BLOOD_DRAIN_RADIUS, player.getY() - BLOOD_DRAIN_HEIGHT, player.getZ() - BLOOD_DRAIN_RADIUS,
                player.getX() + BLOOD_DRAIN_RADIUS, player.getY() + BLOOD_DRAIN_HEIGHT, player.getZ() + BLOOD_DRAIN_RADIUS);

        for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class, area)) {
            victim.addEffect(new MobEffectInstance(ModEffects.SEA_BLOOD_DRAIN, BLOOD_DRAIN_DURATION_TICKS, 0, false, true, true));
        }
    }

}

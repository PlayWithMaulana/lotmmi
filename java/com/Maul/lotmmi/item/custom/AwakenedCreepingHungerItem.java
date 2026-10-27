package com.Maul.lotmmi.item.custom;

import com.Maul.lotmmi.data.ModDataComponents;
import de.jakob.lotm.LOTMCraft;
import de.jakob.lotm.attachments.ModAttachments;
import de.jakob.lotm.attachments.SanityComponent;
import de.jakob.lotm.beyonders.abilities.core.Ability;
import de.jakob.lotm.util.BeyonderData;
import de.jakob.lotm.util.scheduling.ServerScheduler;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class AwakenedCreepingHungerItem extends CreepingHungerItem {

    public static final int AWAKENED_MAX_SLOTS = 7;
    public static final int AWAKENED_MIN_ABILITIES = 2;
    public static final int AWAKENED_MAX_ABILITIES = 3;
    public static final long PRAYER_INTERVAL_TICKS = 8000L;
    public static final int MUSHROOM_RADIUS = 5;

    private static final float PRAYER_DAMAGE = 2.0F;
    private static final float PRAYER_SANITY_LOSS = 0.05F;
    private static final int PRAYER_LINE_DELAY_TICKS = 40;

    private static final List<String> PRAYER_LINES = List.of(
            "The Lord that Created Everything",
            "The Lord who Reigns Behind the Curtain of Shadows",
            "The Degenerate Nature of all Living Things"
    );

    private static final Set<Block> MUSHROOMS = Set.of(
            Blocks.RED_MUSHROOM,
            Blocks.BROWN_MUSHROOM,
            Blocks.RED_MUSHROOM_BLOCK,
            Blocks.BROWN_MUSHROOM_BLOCK,
            Blocks.MUSHROOM_STEM,
            Blocks.POTTED_RED_MUSHROOM,
            Blocks.POTTED_BROWN_MUSHROOM
    );

    public static final Map<UUID, PendingGraze> PENDING = new HashMap<>();

    public record PendingGraze(String targetName, String targetUUID, String pathway, int sequence,
                                List<Ability> candidates) {}

    public AwakenedCreepingHungerItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getMaxSlots() {
        return AWAKENED_MAX_SLOTS;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    public static long getTicksUntilNextPrayer(long lastPrayerTick, long currentGameTime) {
        long elapsed = Math.max(0L, currentGameTime - lastPrayerTick);
        return Math.max(0L, Math.min(PRAYER_INTERVAL_TICKS, PRAYER_INTERVAL_TICKS - elapsed));
    }

    public static long prayerRemainingFor(ItemStack stack, long currentGameTime) {
        if (!(stack.getItem() instanceof AwakenedCreepingHungerItem)) return -1L;
        return getTicksUntilNextPrayer(stack.getOrDefault(ModDataComponents.LAST_PRAYER_TICK.get(), currentGameTime), currentGameTime);
    }

    public static boolean grazeSoul(ServerPlayer player, LivingEntity target, ItemStack glove) {
        if (!BeyonderData.isBeyonder(target)) {
            player.sendSystemMessage(Component.literal("This target has no soul worth grazing.")
                    .withStyle(ChatFormatting.RED));
            return false;
        }

        String targetPathway = BeyonderData.getPathway(target);
        int targetSequence = BeyonderData.getSequence(target);
        if (!passesTierRestriction(player, glove, targetSequence, true)) return false;

        if (getSouls(glove).size() >= AWAKENED_MAX_SLOTS) {
            return stashInReserve(player, glove, target, AWAKENED_MAX_SLOTS);
        }

        List<Ability> candidates = new ArrayList<>(LOTMCraft.abilityHandler.getByPathwayAndSequence(targetPathway, targetSequence));

        if (candidates.isEmpty()) {
            player.sendSystemMessage(Component.literal("This soul's power resists being grazed.")
                    .withStyle(ChatFormatting.RED));
            return false;
        }

        PENDING.put(player.getUUID(), new PendingGraze(
                target.getName().getString(), target.getUUID().toString(),
                targetPathway, targetSequence, candidates));

        player.sendSystemMessage(Component.literal(
                "Grazed over " + target.getName().getString() + "'s soul."
        ).withStyle(ChatFormatting.LIGHT_PURPLE));

        List<String> candidateIds = candidates.stream().map(Ability::getId).toList();
        String targetName = target.getName().getString();
        player.openMenu(new com.Maul.lotmmi.gui.GrazeChoiceMenuProvider(targetName, targetPathway, targetSequence, candidateIds),
                buf -> {
                    buf.writeUtf(targetName);
                    buf.writeUtf(targetPathway);
                    buf.writeVarInt(targetSequence);
                    buf.writeVarInt(candidateIds.size());
                    for (String id : candidateIds) buf.writeUtf(id);
                });
        return true;
    }

    public static void confirmGrazeChoice(ServerPlayer player, ItemStack glove, PendingGraze pending, Ability chosen) {
        List<CreepingHungerItem.SoulSlot> souls = getSouls(glove);
        if (souls.size() >= AWAKENED_MAX_SLOTS) {
            player.sendSystemMessage(Component.literal("No open slot remains for that soul.")
                    .withStyle(ChatFormatting.RED));
            return;
        }
        if (!passesTierRestriction(player, glove, pending.sequence(), true)) return;

        List<Ability> granted = new ArrayList<>();
        granted.add(chosen);

        List<Ability> excluded = new ArrayList<>();
        excluded.add(chosen);
        int total = rollAbilityCount(AWAKENED_MIN_ABILITIES, AWAKENED_MAX_ABILITIES);
        granted.addAll(rollAbilities(pending.pathway(), pending.sequence(), total - 1, excluded));

        finalizeGraze(player, glove, pending.targetName(), pending.targetUUID(),
                pending.pathway(), pending.sequence(), souls, granted);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (mushroomNearby(level, player)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("The Awakened Creeping Hunger shrinks away from the mushrooms nearby.")
                        .withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC), true);
            }
            return InteractionResultHolder.fail(player.getItemInHand(hand));
        }
        return super.use(level, player, hand);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (level.isClientSide || !(entity instanceof ServerPlayer player) || !player.isAlive()) return;

        long now = level.getGameTime();
        if (!stack.has(ModDataComponents.LAST_PRAYER_TICK.get())) {
            stack.set(ModDataComponents.LAST_PRAYER_TICK.get(), now);
            return;
        }

        long lastPrayer = stack.getOrDefault(ModDataComponents.LAST_PRAYER_TICK.get(), now);
        if (now - lastPrayer < PRAYER_INTERVAL_TICKS) return;

        stack.set(ModDataComponents.LAST_PRAYER_TICK.get(), now);
        pray(player, stack);
    }

    private static void pray(ServerPlayer player, ItemStack glove) {
        ServerLevel level = player.serverLevel();
        for (int i = 0; i < PRAYER_LINES.size(); i++) {
            String line = PRAYER_LINES.get(i);
            ServerScheduler.scheduleDelayed(1 + i * PRAYER_LINE_DELAY_TICKS,
                    () -> player.sendSystemMessage(Component.literal(line).withStyle(ChatFormatting.RED)), level);
        }

        if (player.getMainHandItem() != glove && player.getOffhandItem() != glove) return;

        player.hurt(player.damageSources().magic(), PRAYER_DAMAGE);
        SanityComponent sanity = player.getData(ModAttachments.SANITY_COMPONENT.get());
        sanity.setSanityAndSync(Math.max(0.0F, sanity.getSanity() - PRAYER_SANITY_LOSS), player);
    }

    private static boolean mushroomNearby(Level level, Player player) {
        BlockPos origin = player.blockPosition();
        int radius = MUSHROOM_RADIUS;
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-radius, -radius, -radius), origin.offset(radius, radius, radius))) {
            if (pos.distSqr(origin) > radius * radius) continue;
            if (MUSHROOMS.contains(level.getBlockState(pos).getBlock())) return true;
        }
        return false;
    }

    @Override
    protected String getTooltipText() {
        return "Awakened mystical item corresponding to Sequence 5 Hanged Man pathway";
    }
}

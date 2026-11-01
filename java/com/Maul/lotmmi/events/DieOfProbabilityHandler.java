package com.Maul.lotmmi.events;

import com.Maul.lotmmi.LotmMysticalItems;
import com.Maul.lotmmi.config.LotmmiConfig;
import de.jakob.lotm.attachments.LuckComponent;
import de.jakob.lotm.attachments.ModAttachments;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber(modid = LotmMysticalItems.MOD_ID)
public class DieOfProbabilityHandler {

    private static final ResourceLocation DIE_ID = ResourceLocation.parse("lotmcraft:wheel_of_fortune_uniqueness");
    private static final String FACE = "lotmmi_face";
    private static final String NEXT = "lotmmi_next_roll";
    private static final String DELTA = "lotmmi_die_delta";

    private static final Set<UUID> affected = new HashSet<>();

    private static boolean isDie(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Item die = BuiltInRegistries.ITEM.get(DIE_ID);
        return stack.is(die);
    }

    public static int getFace(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return 0;
        return data.copyTag().getInt(FACE);
    }

    private static int strength(int face) {
        if (face <= 0) return 0;
        return face <= 3 ? face - 4 : face - 3;
    }

    private static ItemStack findDie(ServerPlayer player) {
        for (ItemStack stack : player.getInventory().items) if (isDie(stack)) return stack;
        for (ItemStack stack : player.getInventory().offhand) if (isDie(stack)) return stack;
        return ItemStack.EMPTY;
    }

    private static void setTag(ItemStack stack, int face, long next) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putInt(FACE, face);
            tag.putLong(NEXT, next);
        });
    }

    private static long nextRoll(ServerLevel level, long now) {
        int min = Math.min(LotmmiConfig.DIE_REROLL_MIN_MINUTES.get(), LotmmiConfig.DIE_REROLL_MAX_MINUTES.get());
        int max = Math.max(LotmmiConfig.DIE_REROLL_MIN_MINUTES.get(), LotmmiConfig.DIE_REROLL_MAX_MINUTES.get());
        int minutes = min + level.random.nextInt(max - min + 1);
        return now + minutes * 1200L;
    }

    private static void setDelta(LivingEntity entity, int wanted) {
        CompoundTag persistent = entity.getPersistentData();
        int current = persistent.getInt(DELTA);
        if (current == wanted) return;
        LuckComponent luck = entity.getData(ModAttachments.LUCK_COMPONENT);
        luck.addLuck(wanted - current);
        if (wanted == 0) persistent.remove(DELTA);
        else persistent.putInt(DELTA, wanted);
    }

    private static final String[] OMENS = {
            "The air goes still.",
            "Something above you gives way.",
            "The world turns its back on you.",
            "Every small thing leans toward your end."
    };

    private static void freakAccident(ServerPlayer player, ServerLevel level, int face) {
        double perMinute;
        if (face == 1) {
            perMinute = LotmmiConfig.DIE_FACE_ONE_DEATH_PER_MINUTE.get();
        } else if (face == 2) {
            perMinute = LotmmiConfig.DIE_FACE_TWO_ACCIDENT_PER_MINUTE.get();
        } else {
            return;
        }
        if (level.random.nextDouble() >= perMinute / 60.0D) return;

        player.sendSystemMessage(Component.literal(OMENS[level.random.nextInt(OMENS.length)]).withStyle(net.minecraft.ChatFormatting.DARK_RED));

        int[] order = {0, 1, 2};
        for (int i = order.length - 1; i > 0; i--) {
            int j = level.random.nextInt(i + 1);
            int t = order[i];
            order[i] = order[j];
            order[j] = t;
        }
        int count = face == 1 ? 3 : 1;
        for (int i = 0; i < count; i++) {
            switch (order[i]) {
                case 0 -> calamityLightning(player, level);
                case 1 -> calamityAnvil(player, level);
                default -> calamityBlast(player, level);
            }
        }
    }

    private static void calamityLightning(ServerPlayer player, ServerLevel level) {
        net.minecraft.world.entity.LightningBolt bolt = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(level);
        if (bolt == null) return;
        bolt.moveTo(player.getX(), player.getY(), player.getZ());
        level.addFreshEntity(bolt);
    }

    private static void calamityAnvil(ServerPlayer player, ServerLevel level) {
        net.minecraft.core.BlockPos.MutableBlockPos pos = new net.minecraft.core.BlockPos.MutableBlockPos(player.getBlockX(), player.getBlockY() + 12, player.getBlockZ());
        while (pos.getY() > player.getBlockY() + 2 && !level.getBlockState(pos).isAir()) pos.move(0, -1, 0);
        net.minecraft.world.entity.item.FallingBlockEntity anvil = net.minecraft.world.entity.item.FallingBlockEntity.fall(level, pos.immutable(), net.minecraft.world.level.block.Blocks.ANVIL.defaultBlockState());
        anvil.setHurtsEntities(2.0F, 80);
        anvil.dropItem = false;
    }

    private static void calamityBlast(ServerPlayer player, ServerLevel level) {
        level.explode(null, player.getX(), player.getY() + 0.5D, player.getZ(), 4.0F, net.minecraft.world.level.Level.ExplosionInteraction.NONE);
    }

    private static void rollIfDue(ServerPlayer player, ServerLevel level, ItemStack die, long now) {
        CustomData data = die.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = data == null ? new CompoundTag() : data.copyTag();
        int face = tag.getInt(FACE);
        long next = tag.getLong(NEXT);
        if (face != 0 && now < next) return;

        int rolled = 1 + level.random.nextInt(6);
        setTag(die, rolled, nextRoll(level, now));
        if (face == 0) return;

        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 0.6F + rolled * 0.1F);
        level.sendParticles(rolled <= 3 ? net.minecraft.core.particles.ParticleTypes.SMOKE : net.minecraft.core.particles.ParticleTypes.END_ROD,
                player.getX(), player.getY() + 1.0D, player.getZ(), 20, 0.4D, 0.5D, 0.4D, 0.02D);
        player.displayClientMessage(Component.literal("The die settles on " + rolled + "."), true);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 20 != 0) return;

        Map<UUID, Integer> wanted = new HashMap<>();

        if (LotmmiConfig.DIE_ENABLED.get()) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                ItemStack die = findDie(player);
                if (die.isEmpty()) continue;

                ServerLevel level = player.serverLevel();
                rollIfDue(player, level, die, server.overworld().getGameTime());

                freakAccident(player, level, getFace(die));

                int swing = strength(getFace(die)) * LotmmiConfig.DIE_LUCK_PER_FACE.get();
                wanted.merge(player.getUUID(), swing, (a, b) -> Math.abs(a) >= Math.abs(b) ? a : b);

                int radius = LotmmiConfig.DIE_LEAK_RADIUS.get();
                if (radius > 0) {
                    int leak = (int) Math.round(swing * LotmmiConfig.DIE_LEAK_FRACTION.get());
                    if (leak != 0) {
                        AABB box = player.getBoundingBox().inflate(radius);
                        for (LivingEntity near : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player && e.isAlive())) {
                            wanted.merge(near.getUUID(), leak, (a, b) -> Math.abs(a) >= Math.abs(b) ? a : b);
                        }
                    }
                }
            }
        }

        Set<UUID> now = new HashSet<>(wanted.keySet());
        for (ServerLevel level : server.getAllLevels()) {
            for (UUID id : affected) {
                if (now.contains(id)) continue;
                net.minecraft.world.entity.Entity entity = level.getEntity(id);
                if (entity instanceof LivingEntity living) setDelta(living, 0);
            }
            for (Map.Entry<UUID, Integer> entry : wanted.entrySet()) {
                net.minecraft.world.entity.Entity entity = level.getEntity(entry.getKey());
                if (entity instanceof LivingEntity living) setDelta(living, entry.getValue());
            }
        }
        affected.clear();
        affected.addAll(now);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) setDelta(player, 0);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            setDelta(player, 0);
            affected.remove(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) player.getPersistentData().remove(DELTA);
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!isDie(stack)) return;
        int face = getFace(stack);
        if (face == 0) {
            event.getToolTip().add(Component.literal("It has not settled on a face yet.").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
            return;
        }
        event.getToolTip().add(Component.literal("Face " + face).withStyle(face <= 3 ? net.minecraft.ChatFormatting.DARK_RED : net.minecraft.ChatFormatting.GOLD));
    }
}

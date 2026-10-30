package com.Maul.lotmmi.events;

import com.Maul.lotmmi.LotmMysticalItems;
import com.Maul.lotmmi.item.custom.ScepterFollowerUtil;
import com.Maul.lotmmi.item.custom.ScepterPrayerUtil;
import com.Maul.lotmmi.item.custom.SeaGodScepterItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = LotmMysticalItems.MOD_ID)
public class HonorificRecitationHandler {
    public static final List<String> HONORIFIC_LINES = List.of(
            "Blessed of the sea and spirit world",
            "Guardian of the Rorsted Archipelago",
            "Ruler of the Undersea Creatures",
            "Master of Tsunamis and Storms",
            "The Great Kalvetua"
    );

    private static final long TIMEOUT_MILLIS = 60_000L;

    private static final Map<UUID, Integer> PROGRESS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_LINE_TIME = new ConcurrentHashMap<>();

    private static String normalize(String text) {
        String trimmed = text.trim().toLowerCase(Locale.ROOT);
        int end = trimmed.length();
        while (end > 0 && ".,!;:".indexOf(trimmed.charAt(end - 1)) >= 0) end--;
        return trimmed.substring(0, end);
    }

    @SubscribeEvent
    public static void onChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        UUID id = player.getUUID();
        String text = normalize(event.getRawText());
        long now = System.currentTimeMillis();

        Long last = LAST_LINE_TIME.get(id);
        if (last != null && now - last > TIMEOUT_MILLIS) {
            PROGRESS.remove(id);
            LAST_LINE_TIME.remove(id);
        }

        int progress = PROGRESS.getOrDefault(id, 0);

        if (text.equals(normalize(HONORIFIC_LINES.get(progress)))) {
            progress++;
        } else if (text.equals(normalize(HONORIFIC_LINES.get(0)))) {
            progress = 1;
        } else {
            return;
        }

        MinecraftServer server = player.getServer();
        final int accepted = progress;

        if (progress < HONORIFIC_LINES.size()) {
            PROGRESS.put(id, progress);
            LAST_LINE_TIME.put(id, now);
            if (server != null) server.execute(() -> lineFeedback(player, accepted));
            return;
        }

        PROGRESS.remove(id);
        LAST_LINE_TIME.remove(id);

        if (server != null) {
            server.execute(() -> {
                completionFeedback(player);
                handleRecitation(player);
            });
        }
    }

    private static void lineFeedback(ServerPlayer player, int linesDone) {
        ServerLevel level = player.serverLevel();
        float pitch = 0.6F + 0.2F * linesDone;
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, pitch);
        level.sendParticles(ParticleTypes.NAUTILUS, player.getX(), player.getY() + 1.0, player.getZ(),
                8 * linesDone, 0.6, 0.8, 0.6, 0.05);
        player.displayClientMessage(Component.literal("Honorific " + linesDone + "/" + HONORIFIC_LINES.size())
                .withStyle(ChatFormatting.AQUA), true);
    }

    private static void completionFeedback(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.0F);
        level.playSound(null, player.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.6F, 1.4F);

        for (int ring = 0; ring < 3; ring++) {
            double y = player.getY() + 0.1 + ring * 0.8;
            for (int i = 0; i < 24; i++) {
                double angle = Math.PI * 2.0 * i / 24.0;
                level.sendParticles(ParticleTypes.NAUTILUS, player.getX() + Math.cos(angle) * 2.0, y,
                        player.getZ() + Math.sin(angle) * 2.0, 1, 0.0, 0.2, 0.0, 0.02);
            }
        }
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 1.0, player.getZ(),
                30, 0.5, 1.0, 0.5, 0.2);

        player.displayClientMessage(Component.literal("The honorific is complete.")
                .withStyle(ChatFormatting.LIGHT_PURPLE), true);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        PROGRESS.remove(event.getEntity().getUUID());
        LAST_LINE_TIME.remove(event.getEntity().getUUID());
    }

    private static void handleRecitation(ServerPlayer reciter) {
        MinecraftServer server = reciter.getServer();
        if (server == null) return;

        String dimension = reciter.level().dimension().location().toString();
        long now = reciter.level().getGameTime();

        List<ServerPlayer> holders = new ArrayList<>();
        boolean alreadyFollower = false;

        for (ServerPlayer candidate : server.getPlayerList().getPlayers()) {
            List<ItemStack> scepters = SeaGodScepterItem.findScepters(candidate);
            if (scepters.isEmpty()) continue;

            holders.add(candidate);
            for (ItemStack scepter : scepters) {
                ScepterPrayerUtil.recordPrayer(scepter, reciter.getUUID(), reciter.getName().getString(),
                        dimension, reciter.getX(), reciter.getY(), reciter.getZ(), now);
                if (candidate != reciter && ScepterFollowerUtil.isFollower(scepter, reciter.getUUID())) {
                    alreadyFollower = true;
                }
            }

            if (candidate == reciter) {
                candidate.displayClientMessage(Component.literal("Your own prayer echoes back.")
                        .withStyle(ChatFormatting.LIGHT_PURPLE), true);
            } else {
                candidate.sendSystemMessage(Component.literal(reciter.getName().getString()
                                + " is praying to you. Open the Scepter and check Prayers to gaze at them.")
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            SeaGodScepterItem.resync(candidate);
        }

        if (alreadyFollower) return;

        ServerPlayer best = null;
        double bestDistance = Double.MAX_VALUE;
        for (ServerPlayer holder : holders) {
            if (holder == reciter) continue;
            if (!SeaGodScepterItem.canMark(holder, reciter)) continue;

            double distance = holder.level() == reciter.level() ? holder.distanceTo(reciter) : Double.MAX_VALUE / 2;
            if (best == null || distance < bestDistance) {
                best = holder;
                bestDistance = distance;
            }
        }
        if (best == null) return;

        ItemStack target = SeaGodScepterItem.primaryScepter(best);
        if (target.isEmpty()) return;

        ScepterFollowerUtil.addFollower(target, reciter.getUUID(), reciter.getName().getString(),
                reciter.level().dimension(), reciter.getX(), reciter.getY(), reciter.getZ());
        best.displayClientMessage(Component.literal(reciter.getName().getString() + " has sworn themselves to you.")
                .withStyle(ChatFormatting.LIGHT_PURPLE), true);
        reciter.displayClientMessage(Component.literal("You have sworn yourself to the Sea God.")
                .withStyle(ChatFormatting.LIGHT_PURPLE), true);
        SeaGodScepterItem.resync(best);
    }
}

package com.Maul.lotmmi.arrodes;

import com.Maul.lotmmi.LotmMysticalItems;
import com.Maul.lotmmi.config.LotmmiConfig;
import com.Maul.lotmmi.gui.ArrodesMenuProvider;
import com.Maul.lotmmi.item.ModItems;
import com.Maul.lotmmi.network.packets.toClient.ArrodesReplyPacket;
import de.jakob.lotm.attachments.SefirotData;
import de.jakob.lotm.util.BeyonderData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = LotmMysticalItems.MOD_ID)
public final class ArrodesHandler {

    private static final int QUESTION_DELAY_TICKS = 80;
    private static final int LOGIN_GRACE_TICKS = 200;
    private static final int WEAK_STRIKE_SEQUENCE = 8;

    private static final class Pending {
        final boolean owner;
        final String question;
        volatile long askAt;
        volatile long deadline;
        volatile boolean asked;

        Pending(boolean owner, String question, long askAt) {
            this.owner = owner;
            this.question = question;
            this.askAt = askAt;
        }
    }

    private static final Map<UUID, Pending> PENDING = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> COOLDOWN = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> USED = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> RESET_AT = new ConcurrentHashMap<>();

    private ArrodesHandler() {
    }

    public static boolean isOwner(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) return false;
        String claimed = SefirotData.get(server).getClaimedSefirot(player.getUUID());
        return claimed != null && !claimed.isEmpty();
    }

    public static void resetCooldown(ServerPlayer player) {
        UUID id = player.getUUID();
        COOLDOWN.remove(id);
        USED.remove(id);
        RESET_AT.remove(id);
        player.getCooldowns().removeCooldown(ModItems.ARRODES.get());
    }

    public static void open(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        long now = server.getTickCount();
        UUID id = player.getUUID();

        Long until = COOLDOWN.get(id);
        if (until != null && until > now && !PENDING.containsKey(id)) {
            long seconds = (until - now + 19L) / 20L;
            player.displayClientMessage(Component.literal("The mirror is silent. Return in " + seconds + " seconds.")
                    .withStyle(ChatFormatting.GRAY), true);
            return;
        }

        boolean owner = isOwner(player);
        Pending pending = PENDING.get(id);
        boolean answering = pending != null && pending.asked;
        String opening = answering
                ? pending.question
                : owner ? ArrodesVoice.pick(ArrodesVoice.OWNER_GREETINGS, player.serverLevel().random) : ArrodesVoice.SILENT_GREETING;

        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.8F, 0.5F);
        player.openMenu(new ArrodesMenuProvider(owner, answering, opening), buf -> {
            buf.writeBoolean(owner);
            buf.writeBoolean(answering);
            buf.writeUtf(opening);
        });
    }

    public static void input(ServerPlayer player, String raw) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        String text = raw.trim();
        if (text.isEmpty()) return;

        long now = server.getTickCount();
        UUID id = player.getUUID();

        Pending pending = PENDING.get(id);
        if (pending != null) {
            if (!pending.asked) {
                reply(player, ArrodesReplyPacket.KIND_NOTE, List.of("Patience. I am not finished."));
                return;
            }
            answer(player, pending, text, true);
            return;
        }

        Long until = COOLDOWN.get(id);
        if (until != null && until > now) {
            long seconds = (until - now + 19L) / 20L;
            reply(player, ArrodesReplyPacket.KIND_NOTE, List.of("The mirror is silent. Return in " + seconds + " seconds."));
            return;
        }

        boolean owner = isOwner(player);
        ArrodesParser.Result result = ArrodesParser.parse(player, text, owner);
        if (result.kind() == ArrodesParser.Kind.SMITE) {
            reply(player, ArrodesReplyPacket.KIND_NOTE, result.lines());
            smite(player, result.lines().get(0));
            return;
        }
        if (result.kind() == ArrodesParser.Kind.ANSWER && !payForAnswer(player, owner)) return;
        int kind = result.kind() == ArrodesParser.Kind.ANSWER ? ArrodesReplyPacket.KIND_ANSWER : ArrodesReplyPacket.KIND_NOTE;
        reply(player, kind, result.lines());
        for (String line : result.lines()) player.sendSystemMessage(arrodes(line));

        if (result.kind() == ArrodesParser.Kind.ANSWER) {
            String question = ArrodesVoice.pick(owner ? ArrodesVoice.OWNER_QUESTIONS : ArrodesVoice.QUESTIONS, player.serverLevel().random);
            PENDING.put(id, new Pending(owner, question, now + QUESTION_DELAY_TICKS));
        }
    }

    private static boolean payForAnswer(ServerPlayer player, boolean owner) {
        float cost = LotmmiConfig.ARRODES_SPIRITUALITY_COST.get().floatValue();
        if (cost <= 0.0F || !BeyonderData.isBeyonder(player)) return true;

        if (BeyonderData.getSpirituality(player) < cost) {
            String line = ArrodesVoice.pick(owner ? ArrodesVoice.OWNER_NO_SPIRITUALITY : ArrodesVoice.NO_SPIRITUALITY, player.serverLevel().random);
            reply(player, ArrodesReplyPacket.KIND_NOTE, List.of(line));
            player.sendSystemMessage(arrodes(line));
            return false;
        }
        BeyonderData.reduceSpirituality(player, cost);
        String shown = cost == Math.rint(cost) ? Long.toString((long) cost) : String.format(Locale.ROOT, "%.1f", cost);
        player.displayClientMessage(Component.literal("Spirituality -" + shown).withStyle(ChatFormatting.DARK_PURPLE), true);
        return true;
    }

    private static void ask(ServerPlayer player, Pending pending, long now) {
        pending.deadline = now + LotmmiConfig.ARRODES_ANSWER_SECONDS.get() * 20L;
        pending.asked = true;

        String line = pending.owner
                ? pending.question
                : player.getGameProfile().getName() + ", " + lowerFirst(pending.question);
        broadcast(player, arrodes(line));
        reply(player, ArrodesReplyPacket.KIND_QUESTION, List.of(pending.question));

        ServerLevel level = player.serverLevel();
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 0.4F);
        level.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1.4D, player.getZ(), 24, 0.5D, 0.5D, 0.5D, 0.4D);
    }

    private static void answer(ServerPlayer player, Pending pending, String text, boolean echo) {
        MinecraftServer server = player.getServer();
        if (server == null) return;

        if (echo) {
            broadcast(player, Component.translatable("chat.type.text", player.getDisplayName(), Component.literal(text)));
        }

        String trimmed = text.trim();
        int words = trimmed.split("\\s+").length;
        boolean enough = pending.owner ? trimmed.length() >= 2 : words >= 2 && trimmed.length() >= 8;
        if (!enough) {
            String line = ArrodesVoice.pick(pending.owner ? ArrodesVoice.OWNER_TOO_SHORT : ArrodesVoice.TOO_SHORT, player.serverLevel().random);
            broadcast(player, arrodes(line));
            reply(player, ArrodesReplyPacket.KIND_NOTE, List.of(line));
            return;
        }

        PENDING.remove(player.getUUID());
        boolean locked = settle(player, server.getTickCount());

        String reaction = ArrodesVoice.pick(pending.owner ? ArrodesVoice.OWNER_ACCEPTED : ArrodesVoice.ACCEPTED, player.serverLevel().random);
        broadcast(player, arrodes(reaction));

        List<String> lines = new ArrayList<>();
        lines.add(reaction);
        if (pending.owner) {
            String tip = ArrodesParser.tip(player);
            player.sendSystemMessage(arrodes(tip));
            lines.add(tip);
        }
        if (locked) lines.add(lockedLine(pending.owner));
        reply(player, locked ? ArrodesReplyPacket.KIND_LOCKED : ArrodesReplyPacket.KIND_DONE, lines);
    }

    private static String lockedLine(boolean owner) {
        int seconds = LotmmiConfig.ARRODES_COOLDOWN_SECONDS.get();
        return (owner ? "Forgive your servant, Great Master. The mirror must rest for " : "The mirror clouds over. It must rest for ")
                + seconds + " seconds.";
    }

    private static boolean settle(ServerPlayer player, long now) {
        UUID id = player.getUUID();
        int ticks = LotmmiConfig.ARRODES_COOLDOWN_SECONDS.get() * 20;
        Long resetAt = RESET_AT.get(id);
        int used = (resetAt != null && resetAt > now ? USED.getOrDefault(id, 0) : 0) + 1;
        if (used >= LotmmiConfig.ARRODES_QUESTIONS_PER_USE.get()) {
            USED.remove(id);
            RESET_AT.remove(id);
            COOLDOWN.put(id, now + ticks);
            if (ticks > 0) player.getCooldowns().addCooldown(ModItems.ARRODES.get(), ticks);
            return true;
        }
        USED.put(id, used);
        RESET_AT.put(id, now + ticks);
        return false;
    }

    private static void strike(ServerPlayer player, Pending pending, long now) {
        boolean locked = settle(player, now);
        ServerLevel level = player.serverLevel();

        String line = ArrodesVoice.pick(pending.owner ? ArrodesVoice.OWNER_SILENCE : ArrodesVoice.SILENCE, level.random);
        broadcast(player, arrodes(line));
        List<String> lines = new ArrayList<>();
        lines.add(line);
        if (locked) lines.add(lockedLine(pending.owner));
        reply(player, locked ? ArrodesReplyPacket.KIND_LOCKED : ArrodesReplyPacket.KIND_DONE, lines);

        lightning(player, pending.owner);
    }

    private static void smite(ServerPlayer player, String line) {
        ServerLevel level = player.serverLevel();
        broadcast(player, arrodes(line));
        if (level.random.nextDouble() < LotmmiConfig.ARRODES_CURSE_CHANCE.get()) {
            curse(player, level);
        } else {
            lightning(player, false);
        }
    }

    private static void curse(ServerPlayer player, ServerLevel level) {
        String line = ArrodesVoice.pick(ArrodesVoice.CURSE, level.random);
        broadcast(player, arrodes(line));
        player.addEffect(new MobEffectInstance(MobEffects.UNLUCK, 12000, 1));
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 2400, 1));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 1200, 0));
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 300, 0));
        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0));
        level.playSound(null, player.blockPosition(), SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.PLAYERS, 1.0F, 0.6F);
        level.sendParticles(ParticleTypes.SOUL, player.getX(), player.getY() + 1.0D, player.getZ(), 30, 0.5D, 0.8D, 0.5D, 0.05D);
    }

    private static void lightning(ServerPlayer player, boolean owner) {
        ServerLevel level = player.serverLevel();
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(player.getX(), player.getY(), player.getZ());
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }

        if (owner) {
            float damage = (float) Math.min(LotmmiConfig.ARRODES_OWNER_STRIKE_DAMAGE.get(), Math.max(0.0D, player.getHealth() - 1.0F));
            if (damage > 0.0F) player.hurt(player.damageSources().lightningBolt(), damage);
            return;
        }

        boolean strong = BeyonderData.isBeyonder(player) && BeyonderData.getSequence(player) < WEAK_STRIKE_SEQUENCE;
        if (!strong && level.random.nextDouble() < LotmmiConfig.ARRODES_WEAK_KILL_CHANCE.get()) {
            player.hurt(player.damageSources().lightningBolt(), 1000.0F);
            return;
        }

        float full = LotmmiConfig.ARRODES_STRIKE_DAMAGE.get().floatValue();
        float damage = strong ? full : Math.min(full, Math.max(0.0F, player.getHealth() - 4.0F));
        if (!player.fireImmune()) player.setRemainingFireTicks(strong ? 100 : 40);
        if (damage > 0.0F) player.hurt(player.damageSources().lightningBolt(), damage);
    }

    private static void broadcast(ServerPlayer source, Component message) {
        MinecraftServer server = source.getServer();
        if (server == null) return;
        int radius = LotmmiConfig.ARRODES_BROADCAST_RADIUS.get();
        double limit = (double) radius * radius;
        for (ServerPlayer other : server.getPlayerList().getPlayers()) {
            if (other == source || radius == 0
                    || (other.level() == source.level() && other.distanceToSqr(source) <= limit)) {
                other.sendSystemMessage(message);
            }
        }
    }

    private static Component arrodes(String text) {
        return Component.literal("[Arrodes] ").withStyle(ChatFormatting.GRAY, ChatFormatting.BOLD)
                .append(Component.literal(text).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }

    private static void reply(ServerPlayer player, int kind, List<String> lines) {
        PacketDistributor.sendToPlayer(player, new ArrodesReplyPacket(lines, kind));
    }

    private static String lowerFirst(String text) {
        if (text.isEmpty()) return text;
        return Character.toLowerCase(text.charAt(0)) + text.substring(1);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        long now = server.getTickCount();
        if (now % 5L != 0L) return;

        Iterator<Map.Entry<UUID, Pending>> it = PENDING.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Pending> entry = it.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) continue;
            Pending pending = entry.getValue();

            if (!pending.asked) {
                if (now >= pending.askAt) ask(player, pending, now);
                continue;
            }
            if (now >= pending.deadline) {
                it.remove();
                strike(player, pending, now);
                continue;
            }
            if (now % 20L == 0L) {
                long seconds = (pending.deadline - now) / 20L;
                player.displayClientMessage(Component.literal("Answer Arrodes: " + seconds + "s")
                        .withStyle(seconds <= 10L ? ChatFormatting.RED : ChatFormatting.GRAY), true);
            }
        }
    }

    @SubscribeEvent
    public static void onChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        Pending pending = PENDING.get(player.getUUID());
        if (pending == null || !pending.asked) return;
        MinecraftServer server = player.getServer();
        if (server == null) return;

        String text = event.getRawText();
        boolean local = LotmmiConfig.ARRODES_BROADCAST_RADIUS.get() > 0;
        if (local) event.setCanceled(true);
        server.execute(() -> {
            Pending current = PENDING.get(player.getUUID());
            if (current == pending) answer(player, pending, text, local);
        });
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Pending pending = PENDING.get(player.getUUID());
        MinecraftServer server = player.getServer();
        if (pending == null || server == null || !pending.asked) return;
        pending.deadline = Math.max(pending.deadline, server.getTickCount() + LOGIN_GRACE_TICKS);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        ArrodesParser.forget(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PENDING.remove(player.getUUID());
            ArrodesParser.forget(player.getUUID());
        }
    }
}

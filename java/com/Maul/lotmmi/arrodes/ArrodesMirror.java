package com.Maul.lotmmi.arrodes;

import com.Maul.lotmmi.LotmMysticalItems;
import com.Maul.lotmmi.config.LotmmiConfig;
import com.Maul.lotmmi.events.StaffCurseHandler;
import com.Maul.lotmmi.gui.ArrodesMenu;
import de.jakob.lotm.util.BeyonderData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Summoning Arrodes through a mirror, as a small ritual. Sneak and right-click glass while holding a signed book
 * that holds Arrodes' honorific name: Arrodes answers there until the player's questions for the day are spent,
 * and the book is the price, spent when he comes. The glass is never changed. A session is only remembered here,
 * with particles to show it: Arrodes leaves when the player walks away, when the questions run out or the glass
 * sits idle, or when the glass is broken, and then it is plain glass again. While the session lasts, a right-click
 * on that same glass opens the screen again without another book.
 */
@EventBusSubscriber(modid = LotmMysticalItems.MOD_ID)
public final class ArrodesMirror {

    private static final String[] HONORIFIC = {
            "Eyes that watch All Living Beings",
            "The Stigmata from the Primordial Land",
            "The Omniscient One who serves The Fool",
            "The Great Arrodes"
    };

    private static final String HONORIFIC_KEY = ArrodesText.squash(String.join(" ", HONORIFIC));

    /** Extra blocks that count as mirrors, on top of glass. Empty by default; a pack may add to it. */
    private static final TagKey<Block> EXTRA_MIRRORS =
            TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(LotmMysticalItems.MOD_ID, "arrodes_mirrors"));

    private static final double LEAVE_DISTANCE_SQ = 16.0D * 16.0D;

    private static final class Session {
        final ResourceKey<Level> dimension;
        final BlockPos pos;
        volatile long lastActivity;

        Session(ResourceKey<Level> dimension, BlockPos pos, long now) {
            this.dimension = dimension;
            this.pos = pos;
            this.lastActivity = now;
        }
    }

    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();
    private static final Map<GlobalPos, UUID> OCCUPIED = new ConcurrentHashMap<>();

    private ArrodesMirror() {
    }

    static boolean isMirror(BlockState state) {
        if (state.is(Blocks.TINTED_GLASS)) return false;
        return state.is(Tags.Blocks.GLASS_BLOCKS) || state.is(Tags.Blocks.GLASS_PANES) || state.is(EXTRA_MIRRORS);
    }

    static boolean isHonorificBook(ItemStack stack) {
        if (!stack.is(Items.WRITTEN_BOOK)) return false;
        WrittenBookContent content = stack.get(DataComponents.WRITTEN_BOOK_CONTENT);
        if (content == null) return false;
        StringBuilder text = new StringBuilder();
        for (var page : content.pages()) text.append(page.raw().getString()).append(' ');
        // A book the player wrote holds the honorific alone. The ritual book from LOTMCraft's random loot holds it
        // among its candle and sacrifices, so the honorific only has to appear in the text, line for line.
        String squashed = ArrodesText.squash(text.toString());
        return squashed.equals(HONORIFIC_KEY) || (" " + squashed + " ").contains(" " + HONORIFIC_KEY + " ");
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        BlockPos pos = event.getPos();
        if (!isMirror(event.getLevel().getBlockState(pos))) return;

        boolean sneaking = event.getEntity().isShiftKeyDown();
        ItemStack stack = event.getItemStack();
        boolean book = sneaking && isHonorificBook(stack);
        // Arrodes is already here for this player: a plain click on the same glass opens the screen again.
        // Sneaking with something in hand is left to the block, so building against the glass still works.
        boolean reopen = !book && (!sneaking || stack.isEmpty()) && sessionAt(event.getEntity().getUUID(), event.getLevel().dimension(), pos);
        if (!book && !reopen) return;

        // With the feature switched off the book behaves like any other book.
        if (!LotmmiConfig.ARRODES_MIRROR_ENABLED.get()) return;

        // Both sides swallow the click, so the book never opens its reading screen at a mirror.
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);

        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (StaffCurseHandler.isCursed(player)) return;
        MinecraftServer server = player.getServer();
        if (server == null) return;

        summon(player, level, server, pos, ArrodesHandler.isOwner(player), book ? stack : null);
    }

    private static boolean sessionAt(UUID id, ResourceKey<Level> dimension, BlockPos pos) {
        Session session = SESSIONS.get(id);
        return session != null && session.dimension == dimension && session.pos.equals(pos);
    }

    /** The book is the price of the ritual: spent when Arrodes comes, never when he is refused. */
    private static void pay(ServerPlayer player, ServerLevel level, BlockPos pos, ItemStack book, boolean owner) {
        if (book == null) return;
        if (!player.getAbilities().instabuild) book.shrink(1);
        level.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1.0F, 0.5F);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 14, 0.3D, 0.4D, 0.3D, 0.02D);
    }

    private static void summon(ServerPlayer player, ServerLevel level, MinecraftServer server, BlockPos pos, boolean owner, ItemStack book) {
        UUID id = player.getUUID();
        GlobalPos here = GlobalPos.of(level.dimension(), pos);

        Session mine = SESSIONS.get(id);
        if (mine != null) {
            // Already speaking through a mirror: clicking that same glass again simply reopens it, and costs nothing.
            if (mine.dimension == level.dimension() && mine.pos.equals(pos)) {
                mine.lastActivity = server.getTickCount();
                ArrodesHandler.openMirrorMenu(player, owner, "...");
            }
            return;
        }
        if (OCCUPIED.containsKey(here)) return;

        long game = server.overworld().getGameTime();
        int limit = LotmmiConfig.ARRODES_MIRROR_QUESTIONS_PER_DAY.get();
        int left = ArrodesMirrorLedger.get(server).remaining(id, game, limit);
        if (left <= 0) {
            String line = ArrodesVoice.pick(owner ? ArrodesVoice.OWNER_EXHAUSTED : ArrodesVoice.EXHAUSTED, level.random);
            player.sendSystemMessage(ArrodesHandler.arrodes(line));
            return;
        }

        float cost = LotmmiConfig.ARRODES_MIRROR_SUMMON_COST.get().floatValue();
        if (cost > 0.0F && BeyonderData.isBeyonder(player)) {
            if (BeyonderData.getSpirituality(player) < cost) return;
            BeyonderData.reduceSpirituality(player, cost);
        }

        pay(player, level, pos, book, owner);

        SESSIONS.put(id, new Session(level.dimension(), pos.immutable(), server.getTickCount()));
        OCCUPIED.put(here, id);

        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 0.5D;
        double z = pos.getZ() + 0.5D;
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.0F, 0.4F);
        level.sendParticles(ParticleTypes.ENCHANT, x, y, z, 30, 0.5D, 0.5D, 0.5D, 0.4D);

        String rules = ArrodesVoice.pick(owner ? ArrodesVoice.OWNER_SUMMON_RULES : ArrodesVoice.SUMMON_RULES, level.random);
        ArrodesHandler.broadcast(player, ArrodesHandler.arrodes(rules));
        showRemaining(player, left);
        ArrodesHandler.openMirrorMenu(player, owner, rules);
    }

    public static boolean hasSession(ServerPlayer player) {
        return SESSIONS.containsKey(player.getUUID());
    }

    static void touch(ServerPlayer player) {
        Session session = SESSIONS.get(player.getUUID());
        MinecraftServer server = player.getServer();
        if (session != null && server != null) session.lastActivity = server.getTickCount();
    }

    /**
     * Counts one settled question against the player's day. Returns Arrodes' parting line when that was the
     * last question and the session has ended, or null when Arrodes stays.
     */
    static String settle(ServerPlayer player, boolean owner) {
        MinecraftServer server = player.getServer();
        if (server == null) return null;
        int left = ArrodesMirrorLedger.get(server).record(player.getUUID(), server.overworld().getGameTime(),
                LotmmiConfig.ARRODES_MIRROR_QUESTIONS_PER_DAY.get(), LotmmiConfig.ARRODES_MIRROR_COOLDOWN_TICKS.get());
        showRemaining(player, left);
        if (left > 0) return null;
        forget(player.getUUID());
        return ArrodesVoice.pick(owner ? ArrodesVoice.OWNER_SESSION_END : ArrodesVoice.SESSION_END, player.serverLevel().random);
    }

    static void reset(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server != null) ArrodesMirrorLedger.get(server).reset(player.getUUID());
        forget(player.getUUID());
    }

    /** Ends a session without a word: logging out, dying, or Arrodes having just finished. */
    static void forget(UUID id) {
        Session session = SESSIONS.remove(id);
        if (session != null) OCCUPIED.remove(GlobalPos.of(session.dimension, session.pos), id);
    }

    private static void showRemaining(ServerPlayer player, int left) {
        player.displayClientMessage(Component.literal("Questions left today: " + left).withStyle(ChatFormatting.GRAY), true);
    }

    private static void withdraw(ServerPlayer player, ServerLevel level, Session session, String line) {
        ArrodesHandler.broadcast(player, ArrodesHandler.arrodes(line));
        level.playSound(null, session.pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.8F, 0.3F);
        if (player.containerMenu instanceof ArrodesMenu) player.closeContainer();
    }

    static void tick(MinecraftServer server, long now) {
        if (SESSIONS.isEmpty()) return;
        long idleTicks = LotmmiConfig.ARRODES_MIRROR_IDLE_SECONDS.get() * 20L;
        Iterator<Map.Entry<UUID, Session>> it = SESSIONS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Session> entry = it.next();
            Session session = entry.getValue();
            GlobalPos here = GlobalPos.of(session.dimension, session.pos);
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            ServerLevel level = server.getLevel(session.dimension);
            if (player == null || level == null) {
                it.remove();
                OCCUPIED.remove(here, entry.getKey());
                continue;
            }

            boolean owner = ArrodesHandler.isOwner(player);
            String parting = null;
            double x = session.pos.getX() + 0.5D;
            double y = session.pos.getY() + 0.5D;
            double z = session.pos.getZ() + 0.5D;
            if (player.serverLevel().dimension() != session.dimension || player.distanceToSqr(x, y, z) > LEAVE_DISTANCE_SQ) {
                parting = ArrodesVoice.WALKED_AWAY;
            } else if (!isMirror(level.getBlockState(session.pos))) {
                parting = ArrodesVoice.GLASS_BROKEN;
            } else if (!ArrodesHandler.hasPending(player) && now - session.lastActivity > idleTicks) {
                parting = ArrodesVoice.pick(owner ? ArrodesVoice.OWNER_SESSION_TIMEOUT : ArrodesVoice.SESSION_TIMEOUT, level.random);
            }

            if (parting != null) {
                it.remove();
                OCCUPIED.remove(here, entry.getKey());
                withdraw(player, level, session, parting);
                continue;
            }
            if (now % 10L == 0L) {
                level.sendParticles(ParticleTypes.ENCHANT, x, y, z, 4, 0.4D, 0.4D, 0.4D, 0.3D);
            }
        }
    }
}

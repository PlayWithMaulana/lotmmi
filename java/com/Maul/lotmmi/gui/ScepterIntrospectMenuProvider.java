package com.Maul.lotmmi.gui;

import com.Maul.lotmmi.config.LotmmiConfig;
import com.Maul.lotmmi.item.custom.ScepterFollowerUtil;
import com.Maul.lotmmi.item.custom.ScepterPrayerUtil;
import com.Maul.lotmmi.item.custom.SeaGodScepterItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ScepterIntrospectMenuProvider implements MenuProvider {
    public record Snapshot(boolean mainHand, List<String> followerLines, List<String> prayerLines,
                           boolean canSummon, boolean canGaze, int charmCost) {
        public void write(FriendlyByteBuf buf) {
            buf.writeBoolean(mainHand);
            writeLines(buf, followerLines);
            writeLines(buf, prayerLines);
            buf.writeBoolean(canSummon);
            buf.writeBoolean(canGaze);
            buf.writeVarInt(charmCost);
        }

        public static Snapshot read(FriendlyByteBuf buf) {
            boolean mainHand = buf.readBoolean();
            List<String> followers = readLines(buf);
            List<String> prayers = readLines(buf);
            boolean canSummon = buf.readBoolean();
            boolean canGaze = buf.readBoolean();
            int charmCost = buf.readVarInt();
            return new Snapshot(mainHand, followers, prayers, canSummon, canGaze, charmCost);
        }

        private static void writeLines(FriendlyByteBuf buf, List<String> lines) {
            buf.writeVarInt(lines.size());
            for (String line : lines) buf.writeUtf(line);
        }

        private static List<String> readLines(FriendlyByteBuf buf) {
            int count = buf.readVarInt();
            List<String> out = new ArrayList<>();
            for (int i = 0; i < count; i++) out.add(buf.readUtf());
            return out;
        }
    }

    private final Snapshot snapshot;

    public ScepterIntrospectMenuProvider(Snapshot snapshot) {
        this.snapshot = snapshot;
    }

    public static Snapshot snapshot(ServerPlayer player, ItemStack stack, boolean mainHand) {
        long now = player.level().getGameTime();
        return new Snapshot(
                mainHand,
                buildLines(ScepterFollowerUtil.getFollowers(stack), now),
                buildPrayerLines(ScepterPrayerUtil.getPrayers(stack), now),
                SeaGodScepterItem.isInsideOwnSefirahCastle(player),
                SeaGodScepterItem.canGaze(player),
                (int) Math.round(LotmmiConfig.CHARM_SPIRITUALITY_COST.get()));
    }

    public static List<String> buildLines(List<ScepterFollowerUtil.Follower> followers, long now) {
        List<String> lines = new ArrayList<>();
        for (ScepterFollowerUtil.Follower follower : followers) {
            long blessRemaining = remaining(SeaGodScepterItem.BLESS_COOLDOWN_TICKS, follower.lastBlessTick(), now);
            long attackRemaining = remaining(SeaGodScepterItem.ATTACK_COOLDOWN_TICKS, follower.lastAttackTick(), now);
            lines.add(ScepterIntrospectMenu.packLine(follower.id(), follower.name(), follower.summoned(),
                    blessRemaining, attackRemaining));
        }
        return lines;
    }

    public static List<String> buildPrayerLines(List<ScepterPrayerUtil.Prayer> prayers, long now) {
        List<String> lines = new ArrayList<>();
        for (ScepterPrayerUtil.Prayer prayer : prayers) {
            lines.add(ScepterIntrospectMenu.packPrayer(prayer.id(), prayer.name(), prayer.dimension(),
                    (int) Math.floor(prayer.x()), (int) Math.floor(prayer.y()), (int) Math.floor(prayer.z()),
                    Math.max(0L, now - prayer.tick())));
        }
        return lines;
    }

    private static long remaining(long cooldown, long lastTick, long now) {
        return Math.max(0L, Math.min(cooldown, cooldown - (now - lastTick)));
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Sea God Scepter");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ScepterIntrospectMenu(containerId, snapshot);
    }
}

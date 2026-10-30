package com.Maul.lotmmi.gui;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ScepterIntrospectMenu extends AbstractContainerMenu {

    private static final String FIELD_DELIMITER = "\u0001";

    private final boolean mainHand;

    private List<UUID> followerIds;
    private List<String> followerNames;
    private List<Boolean> summoned;
    private List<Long> blessRemaining;
    private List<Long> attackRemaining;

    private List<UUID> prayerIds;
    private List<String> prayerNames;
    private List<String> prayerDimensions;
    private List<String> prayerCoords;
    private List<Long> prayerAges;

    private boolean canSummon;
    private boolean canGaze;
    private int charmCost;
    private int syncVersion = 0;

    public ScepterIntrospectMenu(int containerId, ScepterIntrospectMenuProvider.Snapshot snapshot) {
        super(ModMenuTypes.SCEPTER_INTROSPECT_MENU.get(), containerId);
        this.mainHand = snapshot.mainHand();
        applySync(snapshot.followerLines(), snapshot.prayerLines(), snapshot.canSummon(), snapshot.canGaze(), snapshot.charmCost());
    }

    public ScepterIntrospectMenu(int containerId, Inventory inv, FriendlyByteBuf buf) {
        this(containerId, ScepterIntrospectMenuProvider.Snapshot.read(buf));
    }

    public static String packLine(UUID id, String name, boolean summoned, long blessRemaining, long attackRemaining) {
        return id + FIELD_DELIMITER + name + FIELD_DELIMITER + summoned + FIELD_DELIMITER
                + blessRemaining + FIELD_DELIMITER + attackRemaining;
    }

    public static String packPrayer(UUID id, String name, String dimension, int x, int y, int z, long ageTicks) {
        return id + FIELD_DELIMITER + name + FIELD_DELIMITER + dimension + FIELD_DELIMITER
                + x + "," + y + "," + z + FIELD_DELIMITER + ageTicks;
    }

    public void applySync(List<String> followerLines, List<String> prayerLines, boolean canSummon, boolean canGaze, int charmCost) {
        this.followerIds = new ArrayList<>();
        this.followerNames = new ArrayList<>();
        this.summoned = new ArrayList<>();
        this.blessRemaining = new ArrayList<>();
        this.attackRemaining = new ArrayList<>();

        for (String line : followerLines) {
            String[] parts = line.split(FIELD_DELIMITER, -1);
            if (parts.length != 5) continue;
            followerIds.add(UUID.fromString(parts[0]));
            followerNames.add(parts[1]);
            this.summoned.add(Boolean.parseBoolean(parts[2]));
            blessRemaining.add(Long.parseLong(parts[3]));
            attackRemaining.add(Long.parseLong(parts[4]));
        }

        this.prayerIds = new ArrayList<>();
        this.prayerNames = new ArrayList<>();
        this.prayerDimensions = new ArrayList<>();
        this.prayerCoords = new ArrayList<>();
        this.prayerAges = new ArrayList<>();

        for (String line : prayerLines) {
            String[] parts = line.split(FIELD_DELIMITER, -1);
            if (parts.length != 5) continue;
            prayerIds.add(UUID.fromString(parts[0]));
            prayerNames.add(parts[1]);
            prayerDimensions.add(parts[2]);
            prayerCoords.add(parts[3]);
            prayerAges.add(Long.parseLong(parts[4]));
        }

        this.canSummon = canSummon;
        this.canGaze = canGaze;
        this.charmCost = charmCost;
        this.syncVersion++;
    }

    public boolean isMainHand() {
        return mainHand;
    }

    public List<UUID> getFollowerIds() {
        return followerIds;
    }

    public List<String> getFollowerNames() {
        return followerNames;
    }

    public List<Boolean> getSummoned() {
        return summoned;
    }

    public List<Long> getBlessRemaining() {
        return blessRemaining;
    }

    public List<Long> getAttackRemaining() {
        return attackRemaining;
    }

    public List<UUID> getPrayerIds() {
        return prayerIds;
    }

    public List<String> getPrayerNames() {
        return prayerNames;
    }

    public List<String> getPrayerDimensions() {
        return prayerDimensions;
    }

    public List<String> getPrayerCoords() {
        return prayerCoords;
    }

    public List<Long> getPrayerAges() {
        return prayerAges;
    }

    public boolean canSummon() {
        return canSummon;
    }

    public boolean canGaze() {
        return canGaze;
    }

    public int getCharmCost() {
        return charmCost;
    }

    public int getSyncVersion() {
        return syncVersion;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}

package com.Maul.lotmmi.gui;

import com.Maul.lotmmi.item.custom.CharmType;
import com.Maul.lotmmi.network.ModPacketHandler;
import com.Maul.lotmmi.network.packets.toServer.CreateCharmPacket;
import com.Maul.lotmmi.network.packets.toServer.FollowerActionPacket;
import com.Maul.lotmmi.network.packets.toServer.PrayerActionPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class ScepterIntrospectScreen extends AbstractContainerScreen<ScepterIntrospectMenu> {

    private enum Tab { FOLLOWERS, PRAYERS, ENCHANT }

    private static final int ROW_HEIGHT = 22;
    private static final int ROWS_PER_PAGE = 6;
    private static final int TAB_WIDTH = 80;
    private static final int NAME_BOX_WIDTH = 104;
    private static final int PRAYER_BOX_WIDTH = 160;
    private static final int DELETE_CONFIRM_MILLIS = 3000;
    private static final int TITLE_COLOR = 0xFFE8C866;
    private static final int LABEL_COLOR = 0xFFC8A028;
    private static final int DIM_COLOR = 0xFF888888;

    private Tab tab = Tab.FOLLOWERS;
    private int page = 0;
    private int lastSeenSyncVersion;
    private UUID pendingDeleteId = null;
    private long pendingDeleteUntil = 0L;

    private final List<Button> dynamicButtons = new ArrayList<>();
    private Button followersTabButton;
    private Button prayersTabButton;
    private Button enchantTabButton;

    public ScepterIntrospectScreen(ScepterIntrospectMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 322;
        int enchantRows = CharmType.values().length;
        this.imageHeight = 44 + Math.max(ROWS_PER_PAGE * ROW_HEIGHT, enchantRows * (ROW_HEIGHT - 2)) + 28;
        this.lastSeenSyncVersion = menu.getSyncVersion();
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;

        followersTabButton = Button.builder(Component.literal("Followers"), b -> switchTab(Tab.FOLLOWERS))
                .bounds(leftPos + 8, topPos + 20, TAB_WIDTH, 16).build();
        prayersTabButton = Button.builder(Component.literal("Prayers"), b -> switchTab(Tab.PRAYERS))
                .bounds(leftPos + 8 + (TAB_WIDTH + 4), topPos + 20, TAB_WIDTH, 16).build();
        enchantTabButton = Button.builder(Component.literal("Enchant"), b -> switchTab(Tab.ENCHANT))
                .bounds(leftPos + 8 + (TAB_WIDTH + 4) * 2, topPos + 20, TAB_WIDTH, 16).build();
        this.addRenderableWidget(followersTabButton);
        this.addRenderableWidget(prayersTabButton);
        this.addRenderableWidget(enchantTabButton);

        refreshWidgets();
    }

    private void switchTab(Tab newTab) {
        this.tab = newTab;
        this.page = 0;
        this.pendingDeleteId = null;
        refreshWidgets();
    }

    private int currentListSize() {
        return switch (tab) {
            case FOLLOWERS -> this.menu.getFollowerNames().size();
            case PRAYERS -> this.menu.getPrayerNames().size();
            case ENCHANT -> 0;
        };
    }

    private void refreshWidgets() {
        for (Button button : dynamicButtons) this.removeWidget(button);
        dynamicButtons.clear();

        if (followersTabButton != null) followersTabButton.active = tab != Tab.FOLLOWERS;
        if (prayersTabButton != null) prayersTabButton.active = tab != Tab.PRAYERS;
        if (enchantTabButton != null) enchantTabButton.active = tab != Tab.ENCHANT;

        switch (tab) {
            case FOLLOWERS -> buildFollowerRows();
            case PRAYERS -> buildPrayerRows();
            case ENCHANT -> buildEnchantRows();
        }
        buildPager();
    }

    private void addDynamic(Button button) {
        dynamicButtons.add(button);
        this.addRenderableWidget(button);
    }

    private void buildPager() {
        int size = currentListSize();
        if (size <= ROWS_PER_PAGE) return;

        int maxPage = (size - 1) / ROWS_PER_PAGE;
        int y = topPos + imageHeight - 22;

        Button prev = Button.builder(Component.literal("<"), b -> {
                    page = Math.max(0, page - 1);
                    refreshWidgets();
                })
                .bounds(leftPos + imageWidth - 52, y, 20, 16).build();
        prev.active = page > 0;
        addDynamic(prev);

        Button next = Button.builder(Component.literal(">"), b -> {
                    page = Math.min(maxPage, page + 1);
                    refreshWidgets();
                })
                .bounds(leftPos + imageWidth - 28, y, 20, 16).build();
        next.active = page < maxPage;
        addDynamic(next);
    }

    private void buildFollowerRows() {
        List<String> names = this.menu.getFollowerNames();
        List<Boolean> summoned = this.menu.getSummoned();
        List<Long> blessRemaining = this.menu.getBlessRemaining();
        List<Long> attackRemaining = this.menu.getAttackRemaining();

        int start = page * ROWS_PER_PAGE;
        int end = Math.min(names.size(), start + ROWS_PER_PAGE);

        for (int i = start; i < end; i++) {
            int rowIndex = i;
            int y = topPos + 44 + (i - start) * ROW_HEIGHT;
            int x = leftPos + 8 + NAME_BOX_WIDTH + 4;
            int h = ROW_HEIGHT - 4;

            Button bless = Button.builder(Component.literal("Bless"), b -> sendFollowerAction(rowIndex, FollowerActionPacket.ACTION_BLESS))
                    .bounds(x, y, 48, h).build();
            bless.active = blessRemaining.get(rowIndex) <= 0L;
            addDynamic(bless);

            Button attack = Button.builder(Component.literal("Attack"), b -> sendFollowerAction(rowIndex, FollowerActionPacket.ACTION_ATTACK))
                    .bounds(x + 51, y, 48, h).build();
            attack.active = attackRemaining.get(rowIndex) <= 0L;
            addDynamic(attack);

            boolean isSummoned = summoned.get(rowIndex);
            Button summon = Button.builder(Component.literal(isSummoned ? "Unsummon" : "Summon"),
                            b -> sendFollowerAction(rowIndex, FollowerActionPacket.ACTION_TOGGLE_SUMMON))
                    .bounds(x + 102, y, 58, h).build();
            summon.active = this.menu.canSummon() || isSummoned;
            addDynamic(summon);

            UUID id = this.menu.getFollowerIds().get(rowIndex);
            boolean confirming = id.equals(pendingDeleteId) && System.currentTimeMillis() < pendingDeleteUntil;
            Component deleteLabel = confirming
                    ? Component.literal("Sure?").withStyle(ChatFormatting.RED)
                    : Component.literal("Del").withStyle(ChatFormatting.RED);
            Button delete = Button.builder(deleteLabel, b -> onDeletePressed(rowIndex))
                    .bounds(x + 163, y, 34, h).build();
            delete.setTooltip(Tooltip.create(Component.literal("Remove this follower. Click twice to confirm.")));
            addDynamic(delete);
        }
    }

    private void onDeletePressed(int rowIndex) {
        UUID id = this.menu.getFollowerIds().get(rowIndex);
        boolean confirming = id.equals(pendingDeleteId) && System.currentTimeMillis() < pendingDeleteUntil;

        if (confirming) {
            pendingDeleteId = null;
            ModPacketHandler.sendToServer(new FollowerActionPacket(this.menu.isMainHand(), id, FollowerActionPacket.ACTION_REMOVE));
            return;
        }

        pendingDeleteId = id;
        pendingDeleteUntil = System.currentTimeMillis() + DELETE_CONFIRM_MILLIS;
        refreshWidgets();
    }

    private void buildPrayerRows() {
        List<String> names = this.menu.getPrayerNames();
        int start = page * ROWS_PER_PAGE;
        int end = Math.min(names.size(), start + ROWS_PER_PAGE);

        for (int i = start; i < end; i++) {
            int rowIndex = i;
            int y = topPos + 44 + (i - start) * ROW_HEIGHT;
            int x = leftPos + 8 + PRAYER_BOX_WIDTH + 4;
            int h = ROW_HEIGHT - 4;

            Button gaze = Button.builder(Component.literal("Gaze"), b -> {
                        ModPacketHandler.sendToServer(new PrayerActionPacket(this.menu.isMainHand(),
                                this.menu.getPrayerIds().get(rowIndex), PrayerActionPacket.ACTION_GAZE));
                        this.onClose();
                    })
                    .bounds(x, y, 44, h).build();
            gaze.active = this.menu.canGaze();
            gaze.setTooltip(Tooltip.create(Component.literal(this.menu.canGaze()
                    ? "Gaze at the place this prayer was made. Your body stays behind and spirituality drains every second. Sneak and use the Scepter to return."
                    : "You can't gaze right now.")));
            addDynamic(gaze);

            Button answer = Button.builder(Component.literal("Answer"),
                            b -> ModPacketHandler.sendToServer(new PrayerActionPacket(this.menu.isMainHand(),
                                    this.menu.getPrayerIds().get(rowIndex), PrayerActionPacket.ACTION_ANSWER)))
                    .bounds(x + 47, y, 50, h).build();
            answer.setTooltip(Tooltip.create(Component.literal("Bless whoever prayed, if they are online, and end the prayer.")));
            addDynamic(answer);

            Button clear = Button.builder(Component.literal("Clear"),
                            b -> ModPacketHandler.sendToServer(new PrayerActionPacket(this.menu.isMainHand(),
                                    this.menu.getPrayerIds().get(rowIndex), PrayerActionPacket.ACTION_DISMISS)))
                    .bounds(x + 100, y, 40, h).build();
            addDynamic(clear);
        }
    }

    private void buildEnchantRows() {
        CharmType[] types = CharmType.values();
        for (int i = 0; i < types.length; i++) {
            int index = i;
            CharmType type = types[i];

            Button button = Button.builder(Component.literal(type.displayName()), b -> {
                        ModPacketHandler.sendToServer(new CreateCharmPacket(this.menu.isMainHand(), index));
                        this.onClose();
                    })
                    .bounds(leftPos + 10, topPos + 56 + i * (ROW_HEIGHT - 2), imageWidth - 20, ROW_HEIGHT - 6).build();
            button.setTooltip(Tooltip.create(Component.literal(type.description())));
            addDynamic(button);
        }
    }

    private void sendFollowerAction(int rowIndex, int action) {
        UUID id = this.menu.getFollowerIds().get(rowIndex);
        ModPacketHandler.sendToServer(new FollowerActionPacket(this.menu.isMainHand(), id, action));
    }

    @Override
    protected void containerTick() {
        super.containerTick();

        boolean rebuild = false;

        if (pendingDeleteId != null && System.currentTimeMillis() >= pendingDeleteUntil) {
            pendingDeleteId = null;
            rebuild = true;
        }

        if (this.menu.getSyncVersion() != lastSeenSyncVersion) {
            lastSeenSyncVersion = this.menu.getSyncVersion();
            int maxPage = Math.max(0, (currentListSize() - 1) / ROWS_PER_PAGE);
            if (page > maxPage) page = maxPage;
            rebuild = true;
        }

        if (rebuild) refreshWidgets();
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.fill(0, 0, this.width, this.height, 0x90000000);
        guiGraphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xE0140A24);
        guiGraphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + 1, 0xFF6A4A9A);
        guiGraphics.fill(leftPos, topPos + imageHeight - 1, leftPos + imageWidth, topPos + imageHeight, 0xFF6A4A9A);

        guiGraphics.drawCenteredString(this.font, Component.literal("Sea God Scepter"), leftPos + imageWidth / 2, topPos + 6, TITLE_COLOR);

        switch (tab) {
            case FOLLOWERS -> renderFollowersTab(guiGraphics);
            case PRAYERS -> renderPrayersTab(guiGraphics);
            case ENCHANT -> renderEnchantTab(guiGraphics);
        }
    }

    private void renderFollowersTab(GuiGraphics guiGraphics) {
        List<String> names = this.menu.getFollowerNames();
        List<Long> blessRemaining = this.menu.getBlessRemaining();
        List<Long> attackRemaining = this.menu.getAttackRemaining();
        int start = page * ROWS_PER_PAGE;
        int end = Math.min(names.size(), start + ROWS_PER_PAGE);

        for (int i = start; i < end; i++) {
            int y = topPos + 44 + (i - start) * ROW_HEIGHT;
            guiGraphics.fill(leftPos + 8, y, leftPos + 8 + NAME_BOX_WIDTH, y + ROW_HEIGHT - 4, 0xFF221836);
            guiGraphics.drawString(this.font, this.font.plainSubstrByWidth(names.get(i), NAME_BOX_WIDTH - 8),
                    leftPos + 12, y + 4, LABEL_COLOR, false);

            if (blessRemaining.get(i) > 0L || attackRemaining.get(i) > 0L) {
                String cd = formatCooldown(Math.max(blessRemaining.get(i), attackRemaining.get(i)));
                guiGraphics.drawString(this.font, cd, leftPos + 12, y + 14, DIM_COLOR, false);
            }
        }

        if (names.isEmpty()) {
            guiGraphics.drawCenteredString(this.font, Component.literal("No followers yet"),
                    leftPos + imageWidth / 2, topPos + 60, DIM_COLOR);
        }

        if (!this.menu.canSummon()) {
            guiGraphics.drawString(this.font, "Enter your Sefirah Castle to summon",
                    leftPos + 8, topPos + imageHeight - 16, 0xFF886666, false);
        }
    }

    private void renderPrayersTab(GuiGraphics guiGraphics) {
        List<String> names = this.menu.getPrayerNames();
        List<String> dimensions = this.menu.getPrayerDimensions();
        List<String> coords = this.menu.getPrayerCoords();
        List<Long> ages = this.menu.getPrayerAges();
        int start = page * ROWS_PER_PAGE;
        int end = Math.min(names.size(), start + ROWS_PER_PAGE);

        for (int i = start; i < end; i++) {
            int y = topPos + 44 + (i - start) * ROW_HEIGHT;
            guiGraphics.fill(leftPos + 8, y, leftPos + 8 + PRAYER_BOX_WIDTH, y + ROW_HEIGHT - 4, 0xFF221836);
            guiGraphics.drawString(this.font, this.font.plainSubstrByWidth(names.get(i), 70),
                    leftPos + 12, y + 2, LABEL_COLOR, false);

            String where = dimensionLabel(dimensions.get(i)) + "  " + coords.get(i);
            guiGraphics.drawString(this.font, this.font.plainSubstrByWidth(where, 146),
                    leftPos + 12, y + 11, DIM_COLOR, false);

            String age = formatAge(ages.get(i));
            guiGraphics.drawString(this.font, age, leftPos + 8 + PRAYER_BOX_WIDTH - 4 - this.font.width(age), y + 2, DIM_COLOR, false);
        }

        if (names.isEmpty()) {
            guiGraphics.drawCenteredString(this.font, Component.literal("No one has prayed yet"),
                    leftPos + imageWidth / 2, topPos + 60, DIM_COLOR);
            guiGraphics.drawCenteredString(this.font, Component.literal("Recite the five honorifics in chat, one line at a time"),
                    leftPos + imageWidth / 2, topPos + 74, 0xFF665A7A);
        }

        if (!names.isEmpty() && !this.menu.canGaze()) {
            guiGraphics.drawString(this.font, "You can't gaze right now", leftPos + 8, topPos + imageHeight - 16, 0xFF886666, false);
        }
    }

    private void renderEnchantTab(GuiGraphics guiGraphics) {
        guiGraphics.drawString(this.font, "Binds a charm into the item in your off hand", leftPos + 10, topPos + 40, DIM_COLOR, false);
        guiGraphics.drawString(this.font, "Cost: " + this.menu.getCharmCost() + " spirituality. Fades in about three months.",
                leftPos + 10, topPos + 48, 0xFF665A7A, false);
    }

    private static String dimensionLabel(String dimension) {
        String path = dimension.contains(":") ? dimension.substring(dimension.indexOf(':') + 1) : dimension;
        StringBuilder out = new StringBuilder();
        for (String word : path.split("_")) {
            if (word.isEmpty()) continue;
            if (out.length() > 0) out.append(' ');
            out.append(word.substring(0, 1).toUpperCase(Locale.ROOT)).append(word.substring(1));
        }
        return out.toString();
    }

    private static String formatAge(long ticks) {
        long seconds = ticks / 20;
        if (seconds < 60) return seconds + "s ago";
        long minutes = seconds / 60;
        if (minutes < 60) return minutes + "m ago";
        return (minutes / 60) + "h ago";
    }

    private String formatCooldown(long ticks) {
        long seconds = ticks / 20;
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

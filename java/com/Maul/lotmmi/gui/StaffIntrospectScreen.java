package com.Maul.lotmmi.gui;

import com.Maul.lotmmi.LotmMysticalItems;
import com.Maul.lotmmi.network.ModPacketHandler;
import com.Maul.lotmmi.network.packets.toServer.StaffAbilityActionPacket;
import com.Maul.lotmmi.network.packets.toServer.StaffEntityActionPacket;
import com.Maul.lotmmi.network.packets.toServer.StaffItemActionPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.UUID;

public class StaffIntrospectScreen extends AbstractContainerScreen<StaffIntrospectMenu> {

    private enum Mode { ABILITIES, ENTITIES, ITEMS }

    private static final int ROW_HEIGHT = 18;
    private static final int ROWS_PER_PAGE = 5;
    private static final ResourceLocation PANEL_BACKGROUND = ResourceLocation.fromNamespaceAndPath(
            LotmMysticalItems.MOD_ID, "textures/gui/staff_introspect/panel_background.png");
    private static final int TITLE_COLOR = 0xFFE8C866;
    private static final int ROW_BG = 0xFF221836;
    private static final int ROW_BG_HOVER = 0xFF3A2A5A;
    private static final int ROW_BG_CONFIRM = 0xFF4A3A0A;
    private static final int TEXT_COLOR = 0xFFE8DFF5;
    private static final int LABEL_COLOR = 0xFFC8A028;

    private Mode mode = Mode.ABILITIES;
    private String pendingConfirmUUID = null;

    private int topPage = 0;
    private int bottomPage = 0;

    public StaffIntrospectScreen(StaffIntrospectMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 240;
        this.imageHeight = 360;
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
        this.clearWidgets();

        int tabWidth = imageWidth / 3;
        String[] labels = {"Abilities", "Entities", "Items"};
        for (int i = 0; i < 3; i++) {
            Mode target = Mode.values()[i];
            boolean current = target == mode;
            this.addRenderableWidget(Button.builder(Component.literal(labels[i]), b -> {
                        mode = target;
                        pendingConfirmUUID = null;
                        topPage = 0;
                        bottomPage = 0;
                        init();
                    })
                    .bounds(leftPos + i * tabWidth, topPos + 20, tabWidth, 18)
                    .build()).active = !current;
        }

        if (mode == Mode.ITEMS) {
            this.addRenderableWidget(Button.builder(Component.literal("Record New Item"), b ->
                            ModPacketHandler.sendToServer(new StaffItemActionPacket(
                                    menu.isMainHand(), StaffItemActionPacket.ACTION_OPEN_CAPTURE, 0, new UUID(0, 0))))
                    .bounds(leftPos + 10, topPos + 42, imageWidth - 20, 20)
                    .build());
        }

        switch (mode) {
            case ABILITIES -> initAbilities();
            case ENTITIES -> initEntities();
            case ITEMS -> initItems();
        }
    }

    private int sectionTopY() {
        return mode == Mode.ITEMS ? topPos + 78 : topPos + 54;
    }

    private int bottomSectionTopY() {
        return sectionTopY() + 10 + ROWS_PER_PAGE * ROW_HEIGHT + 30;
    }

    private int pagerY(int sectionTop) {
        return sectionTop + ROWS_PER_PAGE * ROW_HEIGHT + 2;
    }

    private void initAbilities() {
        List<String> wheelIds = menu.getWheelIds();
        int topY = sectionTopY();
        int topMaxPage = maxPage(wheelIds.size());
        topPage = Math.min(topPage, topMaxPage);

        int start = topPage * ROWS_PER_PAGE;
        int end = Math.min(wheelIds.size(), start + ROWS_PER_PAGE);
        for (int i = start; i < end; i++) {
            int row = i - start;
            int y = topY + row * ROW_HEIGHT;
            int rowIndex = i;
            String abilityId = wheelIds.get(i);

            this.addRenderableWidget(Button.builder(Component.literal("^"), b -> sendAbilityAction(
                            StaffAbilityActionPacket.ACTION_MOVE_UP, abilityId, rowIndex))
                    .bounds(leftPos + imageWidth - 10 - 68, y + 1, 14, ROW_HEIGHT - 4)
                    .build()).active = rowIndex > 0;
            this.addRenderableWidget(Button.builder(Component.literal("v"), b -> sendAbilityAction(
                            StaffAbilityActionPacket.ACTION_MOVE_DOWN, abilityId, rowIndex))
                    .bounds(leftPos + imageWidth - 10 - 52, y + 1, 14, ROW_HEIGHT - 4)
                    .build()).active = rowIndex < wheelIds.size() - 1;
            this.addRenderableWidget(Button.builder(Component.literal("Off"), b -> sendAbilityAction(
                            StaffAbilityActionPacket.ACTION_REMOVE_FROM_WHEEL, abilityId, rowIndex))
                    .bounds(leftPos + imageWidth - 10 - 36, y + 1, 36, ROW_HEIGHT - 4)
                    .build());
        }
        addPager(topY, topPage, topMaxPage, p -> { topPage = p; init(); });

        List<String> libraryIds = menu.getLibraryIds();
        int bottomY = bottomSectionTopY();
        int bottomMaxPage = maxPage(libraryIds.size());
        bottomPage = Math.min(bottomPage, bottomMaxPage);

        int bStart = bottomPage * ROWS_PER_PAGE;
        int bEnd = Math.min(libraryIds.size(), bStart + ROWS_PER_PAGE);
        for (int i = bStart; i < bEnd; i++) {
            int row = i - bStart;
            int y = bottomY + row * ROW_HEIGHT;
            String abilityId = libraryIds.get(i);
            boolean onWheel = wheelIds.contains(abilityId);

            Button onButton = Button.builder(Component.literal(onWheel ? "On Wheel" : "Turn On"), b ->
                            sendAbilityAction(StaffAbilityActionPacket.ACTION_ADD_TO_WHEEL, abilityId, 0))
                    .bounds(leftPos + imageWidth - 10 - 86, y + 1, 56, ROW_HEIGHT - 4)
                    .build();
            onButton.active = !onWheel;
            this.addRenderableWidget(onButton);

            this.addRenderableWidget(Button.builder(Component.literal("Del"), b ->
                            sendAbilityAction(StaffAbilityActionPacket.ACTION_FORGET, abilityId, 0))
                    .bounds(leftPos + imageWidth - 10 - 28, y + 1, 28, ROW_HEIGHT - 4)
                    .build());
        }
        addPager(bottomY, bottomPage, bottomMaxPage, p -> { bottomPage = p; init(); });
    }

    private void sendAbilityAction(int action, String abilityId, int wheelIndex) {
        ModPacketHandler.sendToServer(new StaffAbilityActionPacket(menu.isMainHand(), action, abilityId, wheelIndex));
        this.onClose();
    }

    private void initEntities() {
        int topMaxPage = maxPage(menu.getEntityActiveIds().size());
        topPage = Math.min(topPage, topMaxPage);
        addPager(sectionTopY(), topPage, topMaxPage, p -> { topPage = p; init(); });

        int bottomMaxPage = maxPage(menu.getEntityWatchIds().size());
        bottomPage = Math.min(bottomPage, bottomMaxPage);
        addPager(bottomSectionTopY(), bottomPage, bottomMaxPage, p -> { bottomPage = p; init(); });
    }

    private void initItems() {
        List<String> libLabels = menu.getItemLibraryLabels();
        int topY = sectionTopY();
        int topMaxPage = maxPage(libLabels.size());
        topPage = Math.min(topPage, topMaxPage);

        int start = topPage * ROWS_PER_PAGE;
        int end = Math.min(libLabels.size(), start + ROWS_PER_PAGE);
        for (int i = start; i < end; i++) {
            int row = i - start;
            int y = topY + row * ROW_HEIGHT;
            int libIndex = i;
            this.addRenderableWidget(Button.builder(Component.literal("Del"), b -> {
                        ModPacketHandler.sendToServer(new StaffItemActionPacket(
                                menu.isMainHand(), StaffItemActionPacket.ACTION_FORGET_FROM_LIBRARY, libIndex, new UUID(0, 0)));
                        this.onClose();
                    })
                    .bounds(leftPos + imageWidth - 10 - 28, y + 1, 28, ROW_HEIGHT - 4)
                    .build());
        }
        addPager(topY, topPage, topMaxPage, p -> { topPage = p; init(); });

        int bottomMaxPage = maxPage(menu.getItemActiveIds().size());
        bottomPage = Math.min(bottomPage, bottomMaxPage);
        addPager(bottomSectionTopY(), bottomPage, bottomMaxPage, p -> { bottomPage = p; init(); });
    }

    private int maxPage(int itemCount) {
        return Math.max(0, (itemCount - 1) / ROWS_PER_PAGE);
    }

    private void addPager(int sectionTop, int page, int maxPage, java.util.function.IntConsumer onPageChange) {
        if (maxPage <= 0) return;
        int y = pagerY(sectionTop);

        Button prev = Button.builder(Component.literal("< Prev"), b -> onPageChange.accept(Math.max(0, page - 1)))
                .bounds(leftPos + 10, y, 60, 16)
                .build();
        prev.active = page > 0;
        this.addRenderableWidget(prev);

        Button next = Button.builder(Component.literal("Next >"), b -> onPageChange.accept(Math.min(maxPage, page + 1)))
                .bounds(leftPos + imageWidth - 10 - 60, y, 60, 16)
                .build();
        next.active = page < maxPage;
        this.addRenderableWidget(next);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        if (button != 0) return false;

        boolean shift = hasShiftDown();

        switch (mode) {
            case ENTITIES -> {
                List<String> activeIds = menu.getEntityActiveIds();
                int aRow = rowAt(mouseX, mouseY, sectionTopY(), pageCount(activeIds.size(), topPage));
                if (aRow >= 0) {
                    String id = activeIds.get(topPage * ROWS_PER_PAGE + aRow);
                    ModPacketHandler.sendToServer(new StaffEntityActionPacket(
                            menu.isMainHand(), StaffEntityActionPacket.ACTION_WITHDRAW, UUID.fromString(id)));
                    this.onClose();
                    return true;
                }

                List<String> watchIds = menu.getEntityWatchIds();
                List<Boolean> eligible = menu.getEntityEligible();
                int wRow = rowAt(mouseX, mouseY, bottomSectionTopY(), pageCount(watchIds.size(), bottomPage));
                if (wRow >= 0) {
                    int index = bottomPage * ROWS_PER_PAGE + wRow;
                    String uuid = watchIds.get(index);
                    if (shift) {
                        ModPacketHandler.sendToServer(new StaffEntityActionPacket(
                                menu.isMainHand(), StaffEntityActionPacket.ACTION_FORGET, UUID.fromString(uuid)));
                        this.onClose();
                    } else if (eligible.get(index)) {
                        if (uuid.equals(pendingConfirmUUID)) {
                            ModPacketHandler.sendToServer(new StaffEntityActionPacket(
                                    menu.isMainHand(), StaffEntityActionPacket.ACTION_SUMMON, UUID.fromString(uuid)));
                            this.onClose();
                        } else {
                            pendingConfirmUUID = uuid;
                        }
                    }
                    return true;
                }
            }
            case ITEMS -> {
                List<String> libLabels = menu.getItemLibraryLabels();
                int libRow = rowAt(mouseX, mouseY, sectionTopY(), pageCount(libLabels.size(), topPage));
                if (libRow >= 0) {
                    int index = topPage * ROWS_PER_PAGE + libRow;
                    ModPacketHandler.sendToServer(new StaffItemActionPacket(
                            menu.isMainHand(), StaffItemActionPacket.ACTION_SUMMON_FROM_LIBRARY, index, new UUID(0, 0)));
                    this.onClose();
                    return true;
                }

                List<String> itemIds = menu.getItemActiveIds();
                int iRow = rowAt(mouseX, mouseY, bottomSectionTopY(), pageCount(itemIds.size(), bottomPage));
                if (iRow >= 0) {
                    int index = bottomPage * ROWS_PER_PAGE + iRow;
                    ModPacketHandler.sendToServer(new StaffItemActionPacket(
                            menu.isMainHand(), StaffItemActionPacket.ACTION_WITHDRAW, 0, UUID.fromString(itemIds.get(index))));
                    this.onClose();
                    return true;
                }
            }
            default -> {

            }
        }
        return false;
    }

    private int pageCount(int totalItems, int page) {
        int start = page * ROWS_PER_PAGE;
        return Math.max(0, Math.min(ROWS_PER_PAGE, totalItems - start));
    }

    private int rowAt(double mouseX, double mouseY, int listY, int rowCount) {
        for (int row = 0; row < rowCount; row++) {
            int y = listY + row * ROW_HEIGHT;
            if (mouseX >= leftPos + 10 && mouseX <= leftPos + imageWidth - 10 && mouseY >= y && mouseY <= y + ROW_HEIGHT - 2) {
                return row;
            }
        }
        return -1;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBg(guiGraphics, partialTick, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.fill(0, 0, this.width, this.height, 0x90000000);
        guiGraphics.blit(PANEL_BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
        guiGraphics.drawCenteredString(this.font, this.title, leftPos + imageWidth / 2, topPos + 6, TITLE_COLOR);

        int y = sectionTopY();

        switch (mode) {
            case ABILITIES -> {
                List<String> wheelLabels = menu.getWheelLabels();
                guiGraphics.drawString(this.font, "Wheel - " + wheelLabels.size() + " abilities (^v reorder, Off removes):",
                        leftPos + 10, y - 10, LABEL_COLOR, false);
                drawRowsPage(guiGraphics, wheelLabels, y, topPage, mouseX, mouseY, null, 74);
                drawPageLabel(guiGraphics, y, topPage, maxPage(wheelLabels.size()));

                List<String> libraryLabels = menu.getLibraryLabels();
                int bottomY = bottomSectionTopY();
                guiGraphics.drawString(this.font, "Recorded abilities - " + libraryLabels.size() + " known:",
                        leftPos + 10, bottomY - 10, LABEL_COLOR, false);
                drawRowsPage(guiGraphics, libraryLabels, bottomY, bottomPage, mouseX, mouseY, null, 90);
                drawPageLabel(guiGraphics, bottomY, bottomPage, maxPage(libraryLabels.size()));
            }
            case ENTITIES -> {
                List<String> activeLabels = menu.getEntityActiveLabels();
                guiGraphics.drawString(this.font, "Currently summoned (click to withdraw):", leftPos + 10, y - 10, LABEL_COLOR, false);
                drawRowsPage(guiGraphics, activeLabels, y, topPage, mouseX, mouseY, null, 0);
                drawPageLabel(guiGraphics, y, topPage, maxPage(activeLabels.size()));

                List<String> watchLabels = menu.getEntityWatchLabels();
                List<String> watchIds = menu.getEntityWatchIds();
                int bottomY = bottomSectionTopY();
                guiGraphics.drawString(this.font, "Watched (click=confirm summon, shift=forget):", leftPos + 10, bottomY - 10, LABEL_COLOR, false);
                Integer confirmRow = null;
                if (pendingConfirmUUID != null) {
                    int idx = watchIds.indexOf(pendingConfirmUUID);
                    if (idx >= bottomPage * ROWS_PER_PAGE && idx < bottomPage * ROWS_PER_PAGE + ROWS_PER_PAGE) {
                        confirmRow = idx - bottomPage * ROWS_PER_PAGE;
                    }
                }
                drawRowsPage(guiGraphics, watchLabels, bottomY, bottomPage, mouseX, mouseY, confirmRow, 0);
                drawPageLabel(guiGraphics, bottomY, bottomPage, maxPage(watchLabels.size()));
            }
            case ITEMS -> {
                List<String> libLabels = menu.getItemLibraryLabels();
                guiGraphics.drawString(this.font, "Recorded (click=summon, Del=forget):", leftPos + 10, y - 10, LABEL_COLOR, false);
                drawRowsPage(guiGraphics, libLabels, y, topPage, mouseX, mouseY, null, 32);
                drawPageLabel(guiGraphics, y, topPage, maxPage(libLabels.size()));

                List<String> activeLabels = menu.getItemActiveLabels();
                int bottomY = bottomSectionTopY();
                guiGraphics.drawString(this.font, "Currently conjured (click to withdraw):", leftPos + 10, bottomY - 10, LABEL_COLOR, false);
                drawRowsPage(guiGraphics, activeLabels, bottomY, bottomPage, mouseX, mouseY, null, 0);
                drawPageLabel(guiGraphics, bottomY, bottomPage, maxPage(activeLabels.size()));
            }
        }
    }

    private void drawRowsPage(GuiGraphics guiGraphics, List<String> labels, int listY, int page,
                               int mouseX, int mouseY, Integer confirmRow, int rightReserved) {
        int start = page * ROWS_PER_PAGE;
        int end = Math.min(labels.size(), start + ROWS_PER_PAGE);

        if (labels.isEmpty()) {
            guiGraphics.drawString(this.font, "  (none)", leftPos + 14, listY + 2, 0xFF7A6A9A, false);
            return;
        }

        for (int i = start; i < end; i++) {
            int row = i - start;
            int y = listY + row * ROW_HEIGHT;
            boolean hovered = mouseX >= leftPos + 10 && mouseX <= leftPos + imageWidth - 10 && mouseY >= y && mouseY <= y + ROW_HEIGHT - 2;
            boolean confirming = confirmRow != null && confirmRow == row;
            int bg = confirming ? ROW_BG_CONFIRM : (hovered ? ROW_BG_HOVER : ROW_BG);
            guiGraphics.fill(leftPos + 8, y, leftPos + imageWidth - 8, y + ROW_HEIGHT - 2, bg);

            String text = confirming ? "Confirm summon: " + labels.get(i) + "?" : labels.get(i);
            int maxWidth = imageWidth - 24 - rightReserved;
            if (this.font.width(text) > maxWidth) {
                while (!text.isEmpty() && this.font.width(text + "...") > maxWidth) {
                    text = text.substring(0, text.length() - 1);
                }
                text = text + "...";
            }
            guiGraphics.drawString(this.font, text, leftPos + 12, y + 4, TEXT_COLOR, false);
        }
    }

    private void drawPageLabel(GuiGraphics guiGraphics, int sectionTop, int page, int maxPage) {
        if (maxPage <= 0) return;
        int y = pagerY(sectionTop);
        String label = "Page " + (page + 1) + "/" + (maxPage + 1);
        guiGraphics.drawCenteredString(this.font, label, leftPos + imageWidth / 2, y + 4, 0xFF8A7A50);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

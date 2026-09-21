package com.Maul.lotmmi.gui;

import com.Maul.lotmmi.LotmMysticalItems;
import com.Maul.lotmmi.network.ModPacketHandler;
import com.Maul.lotmmi.network.packets.toServer.StaffAbilityActionPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class StaffIntrospectScreen extends AbstractContainerScreen<StaffIntrospectMenu> {

    private static final int ROW_HEIGHT = 18;
    private static final int ROWS_PER_PAGE = 6;
    private static final ResourceLocation PANEL_BACKGROUND = ResourceLocation.fromNamespaceAndPath(
            LotmMysticalItems.MOD_ID, "textures/gui/staff_introspect/panel_background.png");
    private static final int TITLE_COLOR = 0xFFE8C866;
    private static final int LABEL_COLOR = 0xFFC8A028;

    private int topPage = 0;
    private int bottomPage = 0;

    public StaffIntrospectScreen(StaffIntrospectMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 240;
        this.imageHeight = 300;
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
        this.clearWidgets();

        List<String> wheelIds = menu.getWheelIds();
        int topY = topPos + 32;
        int topMaxPage = maxPage(wheelIds.size());
        topPage = Math.min(topPage, topMaxPage);

        int start = topPage * ROWS_PER_PAGE;
        int end = Math.min(wheelIds.size(), start + ROWS_PER_PAGE);
        for (int i = start; i < end; i++) {
            int row = i - start;
            int y = topY + row * ROW_HEIGHT;
            int rowIndex = i;
            String abilityId = wheelIds.get(i);

            Button up = Button.builder(Component.literal("^"), b -> sendAbilityAction(
                            StaffAbilityActionPacket.ACTION_MOVE_UP, abilityId, rowIndex))
                    .bounds(leftPos + imageWidth - 10 - 68, y + 1, 14, ROW_HEIGHT - 4)
                    .build();
            up.active = rowIndex > 0;
            this.addRenderableWidget(up);

            Button down = Button.builder(Component.literal("v"), b -> sendAbilityAction(
                            StaffAbilityActionPacket.ACTION_MOVE_DOWN, abilityId, rowIndex))
                    .bounds(leftPos + imageWidth - 10 - 52, y + 1, 14, ROW_HEIGHT - 4)
                    .build();
            down.active = rowIndex < wheelIds.size() - 1;
            this.addRenderableWidget(down);

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

    private int bottomSectionTopY() {
        return topPos + 32 + 10 + ROWS_PER_PAGE * ROW_HEIGHT + 30;
    }

    private void sendAbilityAction(int action, String abilityId, int wheelIndex) {
        ModPacketHandler.sendToServer(new StaffAbilityActionPacket(menu.isMainHand(), action, abilityId, wheelIndex));
        this.onClose();
    }

    private int maxPage(int itemCount) {
        return Math.max(0, (itemCount - 1) / ROWS_PER_PAGE);
    }

    private void addPager(int sectionTop, int page, int maxPage, java.util.function.IntConsumer onPageChange) {
        if (maxPage <= 0) return;
        int y = sectionTop + ROWS_PER_PAGE * ROW_HEIGHT + 2;

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

        List<String> wheelLabels = menu.getWheelLabels();
        int topY = topPos + 32;
        guiGraphics.drawString(this.font, "Wheel - " + wheelLabels.size() + " abilities (^v reorder, Off removes):",
                leftPos + 10, topY - 10, LABEL_COLOR, false);
        drawRowsPage(guiGraphics, wheelLabels, topY, topPage, 74);
        drawPageLabel(guiGraphics, topY, topPage, maxPage(wheelLabels.size()));

        List<String> libraryLabels = menu.getLibraryLabels();
        int bottomY = bottomSectionTopY();
        guiGraphics.drawString(this.font, "Recorded abilities - " + libraryLabels.size() + " known:",
                leftPos + 10, bottomY - 10, LABEL_COLOR, false);
        drawRowsPage(guiGraphics, libraryLabels, bottomY, bottomPage, 90);
        drawPageLabel(guiGraphics, bottomY, bottomPage, maxPage(libraryLabels.size()));
    }

    private void drawRowsPage(GuiGraphics guiGraphics, List<String> labels, int listY, int page, int rightReserved) {
        int start = page * ROWS_PER_PAGE;
        int end = Math.min(labels.size(), start + ROWS_PER_PAGE);

        if (labels.isEmpty()) {
            guiGraphics.drawString(this.font, "  (none)", leftPos + 14, listY + 2, 0xFF7A6A9A, false);
            return;
        }

        for (int i = start; i < end; i++) {
            int row = i - start;
            int y = listY + row * ROW_HEIGHT;
            guiGraphics.fill(leftPos + 8, y, leftPos + imageWidth - 8, y + ROW_HEIGHT - 2, 0xFF221836);

            String text = labels.get(i);
            int maxWidth = imageWidth - 24 - rightReserved;
            if (this.font.width(text) > maxWidth) {
                while (!text.isEmpty() && this.font.width(text + "...") > maxWidth) {
                    text = text.substring(0, text.length() - 1);
                }
                text = text + "...";
            }
            guiGraphics.drawString(this.font, text, leftPos + 12, y + 4, 0xFFE8DFF5, false);
        }
    }

    private void drawPageLabel(GuiGraphics guiGraphics, int sectionTop, int page, int maxPage) {
        if (maxPage <= 0) return;
        int y = sectionTop + ROWS_PER_PAGE * ROW_HEIGHT + 2;
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

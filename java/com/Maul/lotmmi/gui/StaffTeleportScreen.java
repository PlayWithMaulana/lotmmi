package com.Maul.lotmmi.gui;

import com.Maul.lotmmi.item.custom.StaffTeleportUtil;
import com.Maul.lotmmi.network.ModPacketHandler;
import com.Maul.lotmmi.network.packets.toServer.StaffSlotActionPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

public class StaffTeleportScreen extends AbstractContainerScreen<StaffTeleportMenu> {

    private static final int ROW_HEIGHT = 26;
    private static final int TEXT_BOX_WIDTH = 138;

    private List<String> slotDetails;
    private int lastSeenSyncVersion;
    private int lastChancePercent = -1;
    private final List<Button> rowButtons = new ArrayList<>();

    public StaffTeleportScreen(StaffTeleportMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.slotDetails = menu.getSlotDetails();
        this.lastSeenSyncVersion = menu.getSyncVersion();

        this.imageWidth = 250;
        this.imageHeight = 34 + slotDetails.size() * ROW_HEIGHT + 26;
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
        refreshButtons();
    }

    private boolean isOccupied(int index) {
        return index < slotDetails.size() && !slotDetails.get(index).isEmpty();
    }

    private int misrecallPercent() {
        if (this.minecraft == null || this.minecraft.player == null) return 1;
        return (int) Math.round(StaffTeleportUtil.misrecallChance(this.minecraft.player) * 100.0D);
    }

    private void refreshButtons() {
        for (Button button : rowButtons) this.removeWidget(button);
        rowButtons.clear();
        lastChancePercent = misrecallPercent();

        for (int i = 0; i < slotDetails.size(); i++) {
            int index = i;
            int y = topPos + 34 + i * ROW_HEIGHT;
            int x = leftPos + 8 + TEXT_BOX_WIDTH + 4;
            boolean occupied = isOccupied(i);

            Button go = Button.builder(Component.literal("Go"), b -> {
                        ModPacketHandler.sendToServer(new StaffSlotActionPacket(menu.isMainHand(), index, StaffSlotActionPacket.ACTION_TELEPORT));
                        this.onClose();
                    })
                    .bounds(x, y, 28, ROW_HEIGHT - 4).build();
            go.active = occupied;
            go.setTooltip(Tooltip.create(Component.literal("Teleport to this location.\nChance to misremember: " + lastChancePercent + "%")));
            addButton(go);

            Button save = Button.builder(Component.literal("Save"),
                            b -> ModPacketHandler.sendToServer(new StaffSlotActionPacket(menu.isMainHand(), index, StaffSlotActionPacket.ACTION_SAVE)))
                    .bounds(x + 30, y, 34, ROW_HEIGHT - 4).build();
            save.setTooltip(Tooltip.create(Component.literal(occupied
                    ? "Replace this location with your current position."
                    : "Save your current position and dimension.")));
            addButton(save);

            Button clear = Button.builder(Component.literal("Del"),
                            b -> ModPacketHandler.sendToServer(new StaffSlotActionPacket(menu.isMainHand(), index, StaffSlotActionPacket.ACTION_CLEAR)))
                    .bounds(x + 66, y, 26, ROW_HEIGHT - 4).build();
            clear.active = occupied;
            addButton(clear);
        }
    }

    private void addButton(Button button) {
        rowButtons.add(button);
        this.addRenderableWidget(button);
    }

    @Override
    protected void containerTick() {
        super.containerTick();

        if (menu.getSyncVersion() != lastSeenSyncVersion) {
            lastSeenSyncVersion = menu.getSyncVersion();
            slotDetails = menu.getSlotDetails();
            refreshButtons();
        } else if (misrecallPercent() != lastChancePercent) {
            refreshButtons();
        }
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
        guiGraphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xEE12100A);
        StaffGuiTheme.drawStarfield(guiGraphics, leftPos, topPos, imageWidth, imageHeight);
        guiGraphics.renderOutline(leftPos, topPos, imageWidth, imageHeight, 0xFFC8A028);

        guiGraphics.drawCenteredString(this.font, this.title, leftPos + imageWidth / 2, topPos + 8, 0xFFE8C866);

        for (int i = 0; i < slotDetails.size(); i++) {
            int y = topPos + 34 + i * ROW_HEIGHT;
            guiGraphics.fill(leftPos + 8, y, leftPos + 8 + TEXT_BOX_WIDTH, y + ROW_HEIGHT - 4, 0xFF1E1A10);

            guiGraphics.drawString(this.font, "Location " + (i + 1), leftPos + 12, y + 3, 0xFFC8A028, false);
            String detail = slotDetails.get(i);
            if (detail.isEmpty()) {
                guiGraphics.drawString(this.font, "Empty", leftPos + 12, y + 12, 0xFF6A6048, false);
            } else {
                guiGraphics.drawString(this.font, this.font.plainSubstrByWidth(detail, TEXT_BOX_WIDTH - 8),
                        leftPos + 12, y + 12, 0xFFEAD9A0, false);
            }
        }

        guiGraphics.drawCenteredString(this.font, Component.literal("Misremember chance: " + misrecallPercent() + "% (Panic raises it)"),
                leftPos + imageWidth / 2, topPos + imageHeight - 14, 0xFF8A7A50);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

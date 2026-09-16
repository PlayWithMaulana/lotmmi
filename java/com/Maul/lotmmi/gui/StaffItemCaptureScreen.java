package com.Maul.lotmmi.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class StaffItemCaptureScreen extends AbstractContainerScreen<StaffItemCaptureMenu> {

    public StaffItemCaptureScreen(StaffItemCaptureMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 18 + StaffItemCaptureMenu.CAPTURE_ROWS * 18 + 14 + 76 + 10;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.fill(0, 0, this.width, this.height, 0x90000000);
        guiGraphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xEE12100A);
        StaffGuiTheme.drawStarfield(guiGraphics, leftPos, topPos, imageWidth, imageHeight);
        guiGraphics.renderOutline(leftPos, topPos, imageWidth, imageHeight, 0xFFC8A028);

        for (var slot : menu.slots) {
            int x = leftPos + slot.x - 1;
            int y = topPos + slot.y - 1;
            guiGraphics.fill(x, y, x + 18, y + 18, 0xFF241E10);
            guiGraphics.renderOutline(x, y, 18, 18, 0xFF4A3D18);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, 8, 6, 0xFFE8C866, false);
        guiGraphics.drawString(this.font, "Place items to record, then close.", 8, this.inventoryLabelY - 10, 0xFF8A7A50, false);
    }
}

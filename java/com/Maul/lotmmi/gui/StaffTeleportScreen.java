package com.Maul.lotmmi.gui;

import com.Maul.lotmmi.network.ModPacketHandler;
import com.Maul.lotmmi.network.packets.toServer.StaffRenameSlotPacket;
import com.Maul.lotmmi.network.packets.toServer.StaffSlotActionPacket;
import com.Maul.lotmmi.network.packets.toServer.StaffTypedTeleportPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public class StaffTeleportScreen extends AbstractContainerScreen<StaffTeleportMenu> {

    private static final int ROW_HEIGHT = 20;
    private static final int RENAME_BUTTON_WIDTH = 46;

    private final List<String> slotLabels;
    private final List<String> slotNames;

    private EditBox xBox;
    private EditBox yBox;
    private EditBox zBox;
    private String errorMessage = null;

    private final List<ResourceKey<Level>> availableDimensions = new ArrayList<>();
    private int selectedDimensionIndex = 0;
    private Button dimensionButton;

    private final List<Button> renameButtons = new ArrayList<>();

    private int renamingIndex = -1;
    private EditBox renameBox;
    private Button renameSaveButton;

    public StaffTeleportScreen(StaffTeleportMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.slotLabels = menu.getSlotLabels();
        this.slotNames = menu.getSlotNames();

        this.imageWidth = 220;
        this.imageHeight = 30 + slotLabels.size() * ROW_HEIGHT + 92;
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;

        if (this.minecraft != null && this.minecraft.getConnection() != null) {
            availableDimensions.clear();
            availableDimensions.addAll(this.minecraft.getConnection().levels());
            availableDimensions.sort((a, b) -> a.location().toString().compareTo(b.location().toString()));
            if (this.minecraft.player != null) {
                int currentIndex = availableDimensions.indexOf(this.minecraft.player.level().dimension());
                if (currentIndex >= 0) selectedDimensionIndex = currentIndex;
            }
        }

        int dimensionY = topPos + 34 + slotLabels.size() * ROW_HEIGHT + 6;
        int typedY = dimensionY + 22;

        dimensionButton = Button.builder(Component.literal(currentDimensionLabel()), b -> cycleDimension())
                .bounds(leftPos + 10, dimensionY, imageWidth - 20, 18)
                .build();
        dimensionButton.active = !availableDimensions.isEmpty();
        this.addRenderableWidget(dimensionButton);

        xBox = new EditBox(this.font, leftPos + 10, typedY, 60, 16, Component.literal("X"));
        yBox = new EditBox(this.font, leftPos + 78, typedY, 60, 16, Component.literal("Y"));
        zBox = new EditBox(this.font, leftPos + 146, typedY, 60, 16, Component.literal("Z"));
        xBox.setHint(Component.literal("X").withStyle(ChatFormatting.DARK_GRAY));
        yBox.setHint(Component.literal("Y").withStyle(ChatFormatting.DARK_GRAY));
        zBox.setHint(Component.literal("Z").withStyle(ChatFormatting.DARK_GRAY));

        this.addRenderableWidget(xBox);
        this.addRenderableWidget(yBox);
        this.addRenderableWidget(zBox);

        this.addRenderableWidget(Button.builder(Component.literal("Attempt Teleport"), b -> attemptTypedTeleport())
                .bounds(leftPos + 10, typedY + 22, imageWidth - 20, 20)
                .build());

        renameButtons.clear();
        for (int i = 0; i < slotLabels.size(); i++) {
            boolean occupied = slotNames != null && i < slotNames.size() && !slotLabels.get(i).startsWith("Empty");
            if (!occupied) {
                renameButtons.add(null);
                continue;
            }

            int rowIndex = i;
            int y = topPos + 34 + i * ROW_HEIGHT;
            Button renameButton = Button.builder(Component.literal("Rename"), b -> toggleRename(rowIndex))
                    .bounds(leftPos + imageWidth - 10 - RENAME_BUTTON_WIDTH, y + 1, RENAME_BUTTON_WIDTH, ROW_HEIGHT - 4)
                    .build();
            renameButtons.add(renameButton);
            this.addRenderableWidget(renameButton);
        }
    }

    private String currentDimensionLabel() {
        if (availableDimensions.isEmpty()) return "Dimension: (unknown)";
        return "Dimension: " + availableDimensions.get(selectedDimensionIndex).location();
    }

    private void cycleDimension() {
        if (availableDimensions.isEmpty()) return;
        selectedDimensionIndex = (selectedDimensionIndex + 1) % availableDimensions.size();
        dimensionButton.setMessage(Component.literal(currentDimensionLabel()));
    }

    private void toggleRename(int index) {
        if (renamingIndex == index) {
            cancelRename();
            return;
        }
        cancelRename();

        renamingIndex = index;
        int y = topPos + 34 + index * ROW_HEIGHT;

        if (renameButtons.get(index) != null) {
            this.removeWidget(renameButtons.get(index));
        }

        int boxWidth = imageWidth - 20 - 44;
        renameBox = new EditBox(this.font, leftPos + 10, y + 1, boxWidth, ROW_HEIGHT - 4, Component.literal("Name"));
        renameBox.setMaxLength(StaffRenameSlotPacket.MAX_NAME_LENGTH);
        renameBox.setValue(index < slotNames.size() ? slotNames.get(index) : "");
        renameBox.setHint(Component.literal("Name this location").withStyle(ChatFormatting.DARK_GRAY));
        renameBox.setFocused(true);
        this.addRenderableWidget(renameBox);

        renameSaveButton = Button.builder(Component.literal("Save"), b -> confirmRename())
                .bounds(leftPos + 10 + boxWidth + 4, y + 1, 40, ROW_HEIGHT - 4)
                .build();
        this.addRenderableWidget(renameSaveButton);
    }

    private void confirmRename() {
        if (renamingIndex < 0 || renameBox == null) return;
        ModPacketHandler.sendToServer(new StaffRenameSlotPacket(menu.isMainHand(), renamingIndex, renameBox.getValue().trim()));
        cancelRename();

        this.onClose();
    }

    private void cancelRename() {
        if (renameBox != null) {
            this.removeWidget(renameBox);
            renameBox = null;
        }
        if (renameSaveButton != null) {
            this.removeWidget(renameSaveButton);
            renameSaveButton = null;
        }
        if (renamingIndex >= 0 && renamingIndex < renameButtons.size() && renameButtons.get(renamingIndex) != null) {
            this.addRenderableWidget(renameButtons.get(renamingIndex));
        }
        renamingIndex = -1;
    }

    private void attemptTypedTeleport() {
        Double x = parseCoordinate(xBox.getValue());
        Double y = parseCoordinate(yBox.getValue());
        Double z = parseCoordinate(zBox.getValue());
        if (x == null || y == null || z == null) {
            errorMessage = "Enter valid numbers for X, Y, and Z.";
            return;
        }
        if (availableDimensions.isEmpty()) {
            errorMessage = "No dimension available to teleport to.";
            return;
        }
        errorMessage = null;
        String dimension = availableDimensions.get(selectedDimensionIndex).location().toString();
        ModPacketHandler.sendToServer(new StaffTypedTeleportPacket(menu.isMainHand(), dimension, x, y, z));
    }

    private Double parseCoordinate(String raw) {
        try {
            return Double.parseDouble(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {

        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        if (button == 0) {
            int index = slotRowAt(mouseX, mouseY);
            if (index >= 0 && index != renamingIndex) {
                boolean forceRecord = hasShiftDown();
                ModPacketHandler.sendToServer(new StaffSlotActionPacket(menu.isMainHand(), index, forceRecord));
                this.onClose();
                return true;
            }
        }
        return false;
    }

    private int slotRowAt(double mouseX, double mouseY) {
        int listY = topPos + 34;
        for (int i = 0; i < slotLabels.size(); i++) {
            int y = listY + i * ROW_HEIGHT;
            if (mouseX >= leftPos + 10 && mouseX <= leftPos + imageWidth - 10 && mouseY >= y && mouseY <= y + ROW_HEIGHT - 2) {
                return i;
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
        guiGraphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xEE12100A);
        StaffGuiTheme.drawStarfield(guiGraphics, leftPos, topPos, imageWidth, imageHeight);
        guiGraphics.renderOutline(leftPos, topPos, imageWidth, imageHeight, 0xFFC8A028);

        guiGraphics.drawCenteredString(this.font, this.title, leftPos + imageWidth / 2, topPos + 8, 0xFFE8C866);

        int hoveredRow = slotRowAt(mouseX, mouseY);
        int listY = topPos + 34;
        for (int i = 0; i < slotLabels.size(); i++) {
            int y = listY + i * ROW_HEIGHT;
            boolean hovered = i == hoveredRow;
            guiGraphics.fill(leftPos + 8, y, leftPos + imageWidth - 8, y + ROW_HEIGHT - 2, hovered ? 0xFF3A2F10 : 0xFF1E1A10);

            if (i == renamingIndex) {

                guiGraphics.drawString(this.font, "Slot " + (i + 1) + ":", leftPos + 14, y - 9, 0xFFC8A028, false);
            } else {
                guiGraphics.drawString(this.font, "Slot " + (i + 1) + ": " + slotLabels.get(i), leftPos + 14, y + 5, 0xFFEAD9A0, false);
            }
        }

        int typedLabelY = listY + slotLabels.size() * ROW_HEIGHT + 6 - 12;
        guiGraphics.drawString(this.font, "Dimension, then X/Y/Z (30% random, 5% Spirit World):", leftPos + 10, typedLabelY, 0xFF8A7A50, false);

        if (errorMessage != null) {
            guiGraphics.drawCenteredString(this.font, errorMessage, leftPos + imageWidth / 2, topPos + imageHeight - 12, 0xFFFF6060);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

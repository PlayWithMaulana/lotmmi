package com.Maul.lotmmi.gui;

import com.Maul.lotmmi.network.ModPacketHandler;
import com.Maul.lotmmi.network.packets.toServer.ChooseGrazeAbilityPacket;
import de.jakob.lotm.LOTMCraft;
import de.jakob.lotm.beyonders.abilities.core.Ability;
import de.jakob.lotm.beyonders.abilities.core.SelectableAbility;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GrazeChoiceScreen extends AbstractContainerScreen<GrazeChoiceMenu> {

    private static final Random RANDOM = new Random();

    private static final int ICON_SIZE = 16;
    private static final int CELL = 22;
    private static final int COLS = 8;
    private static final int ROWS_PER_PAGE = 5;

    private final List<Ability> abilities = new ArrayList<>();
    private boolean choiceMade = false;
    private int page = 0;

    private int gridX;
    private int gridY;

    public GrazeChoiceScreen(GrazeChoiceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        for (String id : menu.getAbilityIds()) {
            Ability ability = LOTMCraft.abilityHandler.getById(id);
            if (ability != null) abilities.add(ability);
        }
        this.imageWidth = COLS * CELL + 24;
        this.imageHeight = 62 + ROWS_PER_PAGE * CELL + 26;
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
        gridX = leftPos + 12;
        gridY = topPos + 58;

        layoutButtons();
    }

    private void layoutButtons() {
        this.clearWidgets();

        int maxPage = Math.max(0, (abilities.size() - 1) / (COLS * ROWS_PER_PAGE));

        int pagerY = topPos + imageHeight - 22;
        Button prev = Button.builder(Component.literal("< Prev"), b -> {
            page = Math.max(0, page - 1);
            layoutButtons();
        }).bounds(leftPos + 12, pagerY, 60, 16).build();
        prev.active = page > 0;
        this.addRenderableWidget(prev);

        Button next = Button.builder(Component.literal("Next >"), b -> {
            page = Math.min(maxPage, page + 1);
            layoutButtons();
        }).bounds(leftPos + imageWidth - 12 - 60, pagerY, 60, 16).build();
        next.active = page < maxPage;
        this.addRenderableWidget(next);
    }

    private void choose(Ability ability) {
        choiceMade = true;
        ModPacketHandler.sendToServer(new ChooseGrazeAbilityPacket(ability.getId()));
        this.onClose();
    }

    @Override
    public void onClose() {
        if (!choiceMade && !abilities.isEmpty()) {
            choiceMade = true;
            Ability fallback = abilities.get(RANDOM.nextInt(abilities.size()));
            ModPacketHandler.sendToServer(new ChooseGrazeAbilityPacket(fallback.getId()));
        }
        super.onClose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            Ability hovered = abilityAt(mouseX, mouseY);
            if (hovered != null) {
                choose(hovered);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private Ability abilityAt(double mouseX, double mouseY) {
        int perPage = COLS * ROWS_PER_PAGE;
        int start = page * perPage;
        int end = Math.min(abilities.size(), start + perPage);

        for (int i = start; i < end; i++) {
            int slot = i - start;
            int col = slot % COLS;
            int row = slot / COLS;
            int x = gridX + col * CELL;
            int y = gridY + row * CELL;
            if (mouseX >= x && mouseX < x + ICON_SIZE && mouseY >= y && mouseY < y + ICON_SIZE) {
                return abilities.get(i);
            }
        }
        return null;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        Ability hovered = abilityAt(mouseX, mouseY);
        if (hovered != null) {
            renderAbilityTooltip(guiGraphics, hovered, mouseX, mouseY);
        }
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBg(guiGraphics, partialTick, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.fill(0, 0, this.width, this.height, 0x90000000);

        guiGraphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xEE120303);
        guiGraphics.renderOutline(leftPos, topPos, imageWidth, imageHeight, 0xFFB4222A);
        guiGraphics.renderOutline(leftPos + 2, topPos + 2, imageWidth - 4, imageHeight - 4, 0xFF4A0A0C);

        guiGraphics.drawCenteredString(this.font, this.title, leftPos + imageWidth / 2, topPos + 10, 0xFFE0454A);

        String subtitle = "Creeping Hunger has awoken over " + menu.getTargetName() + "'s soul";
        guiGraphics.drawCenteredString(this.font, subtitle, leftPos + imageWidth / 2, topPos + 24, 0xFFB0B0B0);

        String pathwayLine = formatPathway(menu.getPathway()) + " - Sequence " + menu.getSequence();
        guiGraphics.drawCenteredString(this.font, pathwayLine, leftPos + imageWidth / 2, topPos + 35, 0xFF8A8A8A);

        if (abilities.isEmpty()) {
            guiGraphics.drawCenteredString(this.font, "No abilities available.", leftPos + imageWidth / 2, gridY + 10, 0xFF8A8A8A);
        } else {
            renderGrid(guiGraphics, mouseX, mouseY);
        }

        int maxPage = Math.max(0, (abilities.size() - 1) / (COLS * ROWS_PER_PAGE));
        if (maxPage > 0) {
            String pageLabel = "Page " + (page + 1) + "/" + (maxPage + 1);
            guiGraphics.drawCenteredString(this.font, pageLabel, leftPos + imageWidth / 2, topPos + imageHeight - 20, 0xFF8A8A8A);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {

    }

    private void renderGrid(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int perPage = COLS * ROWS_PER_PAGE;
        int start = page * perPage;
        int end = Math.min(abilities.size(), start + perPage);

        for (int i = start; i < end; i++) {
            int slot = i - start;
            int col = slot % COLS;
            int row = slot / COLS;
            int x = gridX + col * CELL;
            int y = gridY + row * CELL;

            boolean hovered = mouseX >= x && mouseX < x + ICON_SIZE && mouseY >= y && mouseY < y + ICON_SIZE;

            guiGraphics.fill(x - 2, y - 2, x + ICON_SIZE + 2, y + ICON_SIZE + 2,
                    hovered ? 0xFF5A1216 : 0xFF250608);
            guiGraphics.renderOutline(x - 2, y - 2, ICON_SIZE + 4, ICON_SIZE + 4,
                    hovered ? 0xFFE0454A : 0xFF6A1418);

            Ability ability = abilities.get(i);
            if (ability.getTextureLocation() != null) {
                guiGraphics.blit(ability.getTextureLocation(), x, y, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
            } else {
                guiGraphics.fill(x, y, x + ICON_SIZE, y + ICON_SIZE, 0xFFFFFFFF);
                guiGraphics.renderOutline(x, y, ICON_SIZE, ICON_SIZE, 0xFF000000);
            }
        }
    }

    private void renderAbilityTooltip(GuiGraphics guiGraphics, Ability ability, int mouseX, int mouseY) {
        List<Component> lines = new ArrayList<>();
        lines.add(ability.getName().withStyle(ChatFormatting.BOLD).withColor(0xE0454A));

        Component description = ability.getDescription();
        if (description != null) {
            for (String line : wrapText(description.getString(), 150)) {
                lines.add(Component.literal(line).withStyle(ChatFormatting.DARK_GRAY));
            }
        }

        int cooldown = ability.getCooldown();
        if (cooldown > 0) {
            lines.add(Component.literal("Cooldown: ").withStyle(ChatFormatting.DARK_GRAY)
                    .append(Component.literal((cooldown / 20) + "s").withStyle(ChatFormatting.BLUE)));
        }

        float spiritualityCost = ability.spiritualityCost();
        if (spiritualityCost > 0) {
            lines.add(Component.literal("Spirituality Cost: ").withStyle(ChatFormatting.DARK_GRAY)
                    .append(Component.literal(spiritualityCost + "").withStyle(ChatFormatting.DARK_PURPLE)));
        }

        if (ability instanceof SelectableAbility selectable) {
            lines.add(Component.literal("Sub-abilities:").withStyle(ChatFormatting.GOLD));
            for (String subKey : selectable.getAbilityNamesCopy()) {
                lines.add(Component.literal(" - ").withStyle(ChatFormatting.DARK_GRAY)
                        .append(Component.translatable(subKey).withStyle(ChatFormatting.GRAY)));
            }
        }

        guiGraphics.renderTooltip(this.font,
                lines.stream().map(Component::getVisualOrderText).toList(), mouseX, mouseY);
    }

    private List<String> wrapText(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : text.split(" ")) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (this.font.width(candidate) > maxWidth && !current.isEmpty()) {
                lines.add(current.toString());
                current = new StringBuilder(word);
            } else {
                current = new StringBuilder(candidate);
            }
        }
        if (!current.isEmpty()) lines.add(current.toString());
        return lines;
    }

    private static String formatPathway(String pathway) {
        if (pathway == null || pathway.isEmpty()) return "Unknown";
        String[] parts = pathway.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

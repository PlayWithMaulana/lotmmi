package com.Maul.lotmmi.gui;

import com.Maul.lotmmi.LotmMysticalItems;
import com.Maul.lotmmi.item.custom.CreepingHungerItem;
import com.Maul.lotmmi.network.ModPacketHandler;
import com.Maul.lotmmi.network.packets.toServer.FeedReserveSoulPacket;
import com.Maul.lotmmi.network.packets.toServer.ReorderAbilityPacket;
import com.Maul.lotmmi.network.packets.toServer.SpewSoulPacket;
import com.Maul.lotmmi.network.packets.toServer.ToggleAbilityPacket;
import com.mojang.math.Axis;
import de.jakob.lotm.LOTMCraft;
import de.jakob.lotm.beyonders.abilities.core.Ability;
import de.jakob.lotm.beyonders.abilities.core.SelectableAbility;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ItemIntrospectScreen extends AbstractContainerScreen<ItemIntrospectMenu> {

    private static final ResourceLocation BACKGROUND =
            ResourceLocation.fromNamespaceAndPath(LotmMysticalItems.MOD_ID, "textures/gui/item_introspect/panel_background.png");
    private static final ResourceLocation SOUL_SLOT_ACTIVE =
            ResourceLocation.fromNamespaceAndPath(LotmMysticalItems.MOD_ID, "textures/gui/item_introspect/soul_slot_active.png");
    private static final ResourceLocation SOUL_SLOT_EMPTY =
            ResourceLocation.fromNamespaceAndPath(LotmMysticalItems.MOD_ID, "textures/gui/item_introspect/soul_slot_empty.png");
    private static final ResourceLocation RESERVE_SLOT_FED =
            ResourceLocation.fromNamespaceAndPath(LotmMysticalItems.MOD_ID, "textures/gui/item_introspect/reserve_slot_fed.png");
    private static final ResourceLocation RESERVE_SLOT_EMPTY =
            ResourceLocation.fromNamespaceAndPath(LotmMysticalItems.MOD_ID, "textures/gui/item_introspect/reserve_slot_empty.png");
    private static final ResourceLocation CLOCK_FACE =
            ResourceLocation.fromNamespaceAndPath(LotmMysticalItems.MOD_ID, "textures/gui/item_introspect/clock_face.png");
    private static final ResourceLocation CLOCK_HAND =
            ResourceLocation.fromNamespaceAndPath(LotmMysticalItems.MOD_ID, "textures/gui/item_introspect/clock_hand.png");

    private static final String PATHWAY_ICON_NAMESPACE = "lotmcraft";
    private static final Set<String> KNOWN_PATHWAY_ICONS = Set.of(
            "twilight_giant", "darkness", "chained", "abyss", "sun", "visionary",
            "wheel_of_fortune", "demoness", "mother", "justiciar", "moon", "black_emperor",
            "door", "hermit", "white_tower", "death", "hanged_man", "red_priest", "fool",
            "paragon", "tyrant", "error"
    );

    private static ResourceLocation pathwayIcon(String pathway) {
        String key = pathway == null ? "error" : pathway.toLowerCase();
        if (!KNOWN_PATHWAY_ICONS.contains(key)) key = "error";
        return ResourceLocation.fromNamespaceAndPath(PATHWAY_ICON_NAMESPACE, "textures/gui/icons/" + key + "_icon.png");
    }

    private static final int PANEL_WIDTH = 248;
    private static final int PANEL_HEIGHT = 264;
    private static final int SLOT_SIZE = 26;
    private static final int ICON_SIZE = 16;
    private static final int RESERVE_SLOT_SIZE = 30;
    private static final int RESERVE_ICON_SIZE = 18;
    private static final int ABILITIES_PER_PAGE = 5;
    private static final int ABILITY_ROW_HEIGHT = 14;

    private final InteractionHand hand;

    private List<CreepingHungerItem.SoulSlot> souls = new ArrayList<>();
    private CreepingHungerItem.SoulSlot foodSoul = null;
    private List<CreepingHungerItem.AbilityEntry> abilityOrder = new ArrayList<>();
    private int maxSlots = CreepingHungerItem.MAX_SLOTS;
    private int abilityPage = 0;

    private int lastSeenSyncVersion = -1;

    private int[] soulSlotX = new int[0];
    private int soulSlotY;
    private int reserveSlotX;
    private int reserveSlotY;
    private int clockCenterX;
    private int clockCenterY;
    private int abilityListY;

    public ItemIntrospectScreen(ItemIntrospectMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.hand = menu.getHand();
        this.imageWidth = PANEL_WIDTH;
        this.imageHeight = PANEL_HEIGHT;
        refreshFromMenu();
    }

    private void refreshFromMenu() {
        this.souls = this.menu.getSouls();
        this.foodSoul = this.menu.getFoodSoul();
        this.abilityOrder = this.menu.getAbilityOrder();
        this.maxSlots = this.menu.getMaxSlots();
        this.lastSeenSyncVersion = this.menu.getSyncVersion();

        int maxPage = Math.max(0, (abilityOrder.size() - 1) / ABILITIES_PER_PAGE);
        if (abilityPage > maxPage) abilityPage = maxPage;
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - PANEL_WIDTH) / 2;
        this.topPos = (this.height - PANEL_HEIGHT) / 2;

        refreshLayout();
    }

    private void refreshLayout() {
        this.clearWidgets();

        int startX = leftPos + 15;
        int available = PANEL_WIDTH - 30 - SLOT_SIZE;
        int gap = maxSlots > 1 ? Math.max(SLOT_SIZE + 4, available / (maxSlots - 1)) : 0;
        soulSlotY = topPos + 32;
        soulSlotX = new int[maxSlots];

        int spewWidth = Math.min(SLOT_SIZE + 8, Math.max(20, gap - 2));

        for (int i = 0; i < maxSlots; i++) {
            int x = startX + i * gap;
            soulSlotX[i] = x;

            if (i < souls.size()) {
                int slotIndex = i;
                this.addRenderableWidget(Button.builder(Component.literal("Spew"), b -> onSpew(slotIndex))
                        .bounds(x + (SLOT_SIZE - spewWidth) / 2, soulSlotY + SLOT_SIZE + 3, spewWidth, 14)
                        .build());
            }
        }

        reserveSlotX = leftPos + 63 - RESERVE_SLOT_SIZE / 2;
        reserveSlotY = topPos + 98;

        Button feedButton = Button.builder(Component.literal("Feed Now"), b -> onFeedReserve())
                .bounds(leftPos + 28, reserveSlotY + RESERVE_SLOT_SIZE + 10, 70, 16)
                .build();
        feedButton.active = foodSoul != null;
        this.addRenderableWidget(feedButton);

        clockCenterX = leftPos + 182;
        clockCenterY = topPos + 118;

        abilityListY = topPos + 170;
        int pageStart = abilityPage * ABILITIES_PER_PAGE;
        int pageEnd = Math.min(abilityOrder.size(), pageStart + ABILITIES_PER_PAGE);

        int rowRight = leftPos + PANEL_WIDTH - 8;
        int toggleWidth = 36;
        int arrowWidth = 14;
        int arrowGap = 2;

        for (int row = 0; pageStart + row < pageEnd; row++) {
            int i = pageStart + row;
            CreepingHungerItem.AbilityEntry entry = abilityOrder.get(i);
            int y = abilityListY + row * ABILITY_ROW_HEIGHT;

            int toggleX = rowRight - toggleWidth;
            int downX = toggleX - arrowGap - arrowWidth;
            int upX = downX - arrowGap - arrowWidth;

            Button up = Button.builder(Component.literal("^"), b -> onReorder(entry.id(), -1))
                    .bounds(upX, y - 1, arrowWidth, 12)
                    .build();
            up.active = i > 0;
            this.addRenderableWidget(up);

            Button down = Button.builder(Component.literal("v"), b -> onReorder(entry.id(), 1))
                    .bounds(downX, y - 1, arrowWidth, 12)
                    .build();
            down.active = i < abilityOrder.size() - 1;
            this.addRenderableWidget(down);

            this.addRenderableWidget(Button.builder(
                            Component.literal(entry.enabled() ? "ON" : "OFF"), b -> onToggle(entry.id()))
                    .bounds(toggleX, y - 1, toggleWidth, 12)
                    .build());
        }

        int maxPage = Math.max(0, (abilityOrder.size() - 1) / ABILITIES_PER_PAGE);
        int pagerY = topPos + 170 + ABILITIES_PER_PAGE * ABILITY_ROW_HEIGHT + 2;

        Button prevPage = Button.builder(Component.literal("< Prev"), b -> {
            abilityPage = Math.max(0, abilityPage - 1);
            refreshLayout();
        }).bounds(leftPos + 14, pagerY, 60, 14).build();
        prevPage.active = abilityPage > 0;
        this.addRenderableWidget(prevPage);

        Button nextPage = Button.builder(Component.literal("Next >"), b -> {
            abilityPage = Math.min(maxPage, abilityPage + 1);
            refreshLayout();
        }).bounds(leftPos + PANEL_WIDTH - 14 - 60, pagerY, 60, 14).build();
        nextPage.active = abilityPage < maxPage;
        this.addRenderableWidget(nextPage);
    }

    private void onSpew(int slotIndex) {
        ModPacketHandler.sendToServer(new SpewSoulPacket(hand == InteractionHand.MAIN_HAND, slotIndex));

    }

    private void onFeedReserve() {
        if (foodSoul == null) return;
        ModPacketHandler.sendToServer(new FeedReserveSoulPacket(hand == InteractionHand.MAIN_HAND));
    }

    private void onReorder(String abilityId, int direction) {
        ModPacketHandler.sendToServer(new ReorderAbilityPacket(hand == InteractionHand.MAIN_HAND, abilityId, direction));
    }

    private void onToggle(String abilityId) {
        ModPacketHandler.sendToServer(new ToggleAbilityPacket(hand == InteractionHand.MAIN_HAND, abilityId));
    }

    @Override
    protected void containerTick() {
        super.containerTick();

        if (menu.getSyncVersion() != lastSeenSyncVersion) {
            refreshFromMenu();
            refreshLayout();
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltips(guiGraphics, mouseX, mouseY);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBg(guiGraphics, partialTick, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {

        guiGraphics.fill(0, 0, this.width, this.height, 0x60000000);

        guiGraphics.blit(BACKGROUND, leftPos, topPos, 0, 0, PANEL_WIDTH, PANEL_HEIGHT, PANEL_WIDTH, PANEL_HEIGHT);

        guiGraphics.drawCenteredString(this.font, this.title, leftPos + PANEL_WIDTH / 2, topPos + 8, 0xFFC24A4A);

        renderSoulSlots(guiGraphics);
        renderReserveSlot(guiGraphics);
        renderClock(guiGraphics);
        renderAbilityList(guiGraphics);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {

    }

    private void renderSoulSlots(GuiGraphics guiGraphics) {
        guiGraphics.drawString(this.font, Component.literal("Souls Held: " + souls.size() + "/" + maxSlots),
                leftPos + 12, topPos + 21, 0xFFB0B0B0, false);

        for (int i = 0; i < maxSlots; i++) {
            int x = soulSlotX[i];
            boolean active = i < souls.size();

            guiGraphics.blit(active ? SOUL_SLOT_ACTIVE : SOUL_SLOT_EMPTY, x, soulSlotY, 0, 0, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE);

            if (active) {
                CreepingHungerItem.SoulSlot soul = souls.get(i);
                ResourceLocation icon = pathwayIcon(soul.pathway());
                int iconX = x + (SLOT_SIZE - ICON_SIZE) / 2;
                int iconY = soulSlotY + (SLOT_SIZE - ICON_SIZE) / 2;
                guiGraphics.blit(icon, iconX, iconY, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
            }
        }
    }

    private void renderReserveSlot(GuiGraphics guiGraphics) {
        guiGraphics.drawString(this.font, Component.literal("Reserve / Extra Soul"), leftPos + 13, topPos + 84, 0xFFB0B0B0, false);

        boolean hasFood = foodSoul != null;
        guiGraphics.blit(hasFood ? RESERVE_SLOT_FED : RESERVE_SLOT_EMPTY, reserveSlotX, reserveSlotY, 0, 0,
                RESERVE_SLOT_SIZE, RESERVE_SLOT_SIZE, RESERVE_SLOT_SIZE, RESERVE_SLOT_SIZE);

        if (hasFood) {
            ResourceLocation icon = pathwayIcon(foodSoul.pathway());
            int iconX = reserveSlotX + (RESERVE_SLOT_SIZE - RESERVE_ICON_SIZE) / 2;
            int iconY = reserveSlotY + (RESERVE_SLOT_SIZE - RESERVE_ICON_SIZE) / 2;
            guiGraphics.blit(icon, iconX, iconY, 0, 0, RESERVE_ICON_SIZE, RESERVE_ICON_SIZE, RESERVE_ICON_SIZE, RESERVE_ICON_SIZE);
        } else {
            String label = "empty";
            guiGraphics.drawCenteredString(this.font, label, reserveSlotX + RESERVE_SLOT_SIZE / 2, reserveSlotY + RESERVE_SLOT_SIZE / 2 - 4, 0xFF804040);
        }
    }

    private void renderClock(GuiGraphics guiGraphics) {
        guiGraphics.drawString(this.font, Component.literal("Next Feeding"), leftPos + 149, topPos + 84, 0xFFB0B0B0, false);

        long gameTime = this.minecraft != null && this.minecraft.level != null ? this.minecraft.level.getGameTime() : 0L;
        long remaining = CreepingHungerItem.getTicksUntilNextFeed(this.menu.getLastFedTick(), gameTime);

        float progress = 1f - (float) remaining / (float) CreepingHungerItem.FEED_INTERVAL_TICKS;
        float angleDeg = progress * 360f;

        guiGraphics.blit(CLOCK_FACE, clockCenterX - 24, clockCenterY - 24, 0, 0, 48, 48, 48, 48);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(clockCenterX, clockCenterY, 0);
        guiGraphics.pose().mulPose(Axis.ZP.rotationDegrees(angleDeg));
        guiGraphics.blit(CLOCK_HAND, -3, -20, 0, 0, 6, 20, 6, 20);
        guiGraphics.pose().popPose();

        long totalSeconds = remaining / 20;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        String countdown = String.format("%d:%02d", minutes, seconds);
        int color = remaining < 20L * 60 ? 0xFFE04030 : 0xFFD8D8D8;
        guiGraphics.drawCenteredString(this.font, countdown, clockCenterX, clockCenterY + 28, color);
    }

    private void renderAbilityList(GuiGraphics guiGraphics) {
        int maxPage = Math.max(0, (abilityOrder.size() - 1) / ABILITIES_PER_PAGE);
        String header = "Abilities (Wheel Order)"
                + (abilityOrder.isEmpty() ? "" : " - page " + (abilityPage + 1) + "/" + (maxPage + 1));
        guiGraphics.drawString(this.font, header, leftPos + 12, topPos + 158, 0xFFB0B0B0, false);

        if (abilityOrder.isEmpty()) {
            guiGraphics.drawString(this.font, "Graze a soul to add abilities here.",
                    leftPos + 12, abilityListY + 2, 0xFF8A8A8A, false);
            return;
        }

        int pageStart = abilityPage * ABILITIES_PER_PAGE;
        int pageEnd = Math.min(abilityOrder.size(), pageStart + ABILITIES_PER_PAGE);

        int nameMaxWidth = 130;
        for (int row = 0; pageStart + row < pageEnd; row++) {
            CreepingHungerItem.AbilityEntry entry = abilityOrder.get(pageStart + row);
            int y = abilityListY + row * ABILITY_ROW_HEIGHT + 1;

            String name = abilityDisplayName(entry.id());
            if (this.font.width(name) > nameMaxWidth) {
                while (!name.isEmpty() && this.font.width(name + "...") > nameMaxWidth) {
                    name = name.substring(0, name.length() - 1);
                }
                name = name + "...";
            }
            int color = entry.enabled() ? 0xFFD8D8D8 : 0xFF707070;
            guiGraphics.drawString(this.font, name, leftPos + 12, y, color, false);
        }
    }

    private static String abilityDisplayName(String abilityId) {
        try {
            Ability ability = LOTMCraft.abilityHandler.getById(abilityId);
            if (ability != null) return ability.getName().getString();
        } catch (Exception ignored) {

        }
        return abilityId;
    }

    private void renderTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        for (int i = 0; i < souls.size(); i++) {
            int x = soulSlotX[i];
            if (mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= soulSlotY && mouseY < soulSlotY + SLOT_SIZE) {
                CreepingHungerItem.SoulSlot soul = souls.get(i);
                guiGraphics.renderTooltip(this.font, tooltipOf(
                        Component.literal(soul.ownerName()).withStyle(ChatFormatting.LIGHT_PURPLE),
                        Component.literal(formatPathway(soul.pathway()) + " - Sequence " + soul.sequence())
                                .withStyle(ChatFormatting.GRAY),
                        Component.literal(soul.abilityIds().size() + " abilities granted").withStyle(ChatFormatting.DARK_GRAY)
                ), mouseX, mouseY);
                return;
            }
        }

        if (mouseX >= reserveSlotX && mouseX < reserveSlotX + RESERVE_SLOT_SIZE
                && mouseY >= reserveSlotY && mouseY < reserveSlotY + RESERVE_SLOT_SIZE) {
            if (foodSoul != null) {
                guiGraphics.renderTooltip(this.font, tooltipOf(
                        Component.literal(foodSoul.ownerName()).withStyle(ChatFormatting.DARK_GREEN),
                        Component.literal(formatPathway(foodSoul.pathway()) + " - Sequence " + foodSoul.sequence())
                                .withStyle(ChatFormatting.GRAY),
                        Component.literal("Held in reserve to feed Creeping Hunger").withStyle(ChatFormatting.DARK_GRAY)
                ), mouseX, mouseY);
            } else {
                guiGraphics.renderTooltip(this.font, tooltipOf(
                        Component.literal("Reserve empty").withStyle(ChatFormatting.DARK_RED),
                        Component.literal("Grazing with all slots full, or /creepinghunger reserve <slot>,")
                                .withStyle(ChatFormatting.DARK_GRAY),
                        Component.literal("stashes a soul here instead.").withStyle(ChatFormatting.DARK_GRAY)
                ), mouseX, mouseY);
            }
        }

        renderAbilityRowTooltip(guiGraphics, mouseX, mouseY);
    }

    private void renderAbilityRowTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (abilityOrder.isEmpty()) return;

        int pageStart = abilityPage * ABILITIES_PER_PAGE;
        int pageEnd = Math.min(abilityOrder.size(), pageStart + ABILITIES_PER_PAGE);
        int rowRight = leftPos + PANEL_WIDTH - 8;

        for (int row = 0; pageStart + row < pageEnd; row++) {
            int y = abilityListY + row * ABILITY_ROW_HEIGHT;
            if (mouseX < leftPos + 12 || mouseX >= rowRight || mouseY < y - 1 || mouseY >= y - 1 + 12) continue;

            CreepingHungerItem.AbilityEntry entry = abilityOrder.get(pageStart + row);
            Ability ability;
            try {
                ability = LOTMCraft.abilityHandler.getById(entry.id());
            } catch (Exception e) {
                ability = null;
            }
            if (ability == null) return;

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

            if (!entry.enabled()) {
                lines.add(Component.literal("Hidden from the Artifact Wheel").withStyle(ChatFormatting.DARK_RED));
            }

            if (ability instanceof SelectableAbility selectable) {
                lines.add(Component.literal("Sub-abilities:").withStyle(ChatFormatting.GOLD));
                for (String subKey : selectable.getAbilityNamesCopy()) {
                    lines.add(Component.literal(" - ").withStyle(ChatFormatting.DARK_GRAY)
                            .append(Component.translatable(subKey).withStyle(ChatFormatting.GRAY)));
                }
            }

            guiGraphics.renderTooltip(this.font, tooltipOf(lines.toArray(new Component[0])), mouseX, mouseY);
            return;
        }
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

    private static List<net.minecraft.util.FormattedCharSequence> tooltipOf(Component... components) {
        List<net.minecraft.util.FormattedCharSequence> lines = new ArrayList<>();
        for (Component c : components) lines.add(c.getVisualOrderText());
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

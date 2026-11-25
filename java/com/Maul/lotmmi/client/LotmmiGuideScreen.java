package com.Maul.lotmmi.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * The lotmmi chapter of LOTMCraft's ingredient encyclopedia. It is drawn with the encyclopedia's own book textures and
 * the same page layout, and it returns to the encyclopedia it was opened from.
 */
public class LotmmiGuideScreen extends Screen {

    static final int BOOK_W = 344;
    static final int BOOK_H = 212;
    private static final int PAGE_W = 162;
    private static final int PAGE_H = 196;
    private static final int INK = 0xFF3B2A14;
    private static final int INK_SOFT = 0xFF74603E;
    private static final int INK_TEXT = 0xFF4A3A22;
    private static final int TAB_BORDER = 0xFF1E1208;
    private static final int ARROW_LIT = 0xFFB5841A;
    private static final int ARROW_DIM = 0xFF74603E;
    private static final float TEXT_SCALE = 0.75F;
    private static final int TEXT_WRAP = (int) (138 / TEXT_SCALE);
    private static final int AREA_H = 158;
    private static final int LINE_H = 7;

    private final Screen parent;
    private final List<List<Block>> pages = new ArrayList<>();
    private int spread;

    private record Block(boolean heading, String title, String tag, int color, ItemStack icon, List<FormattedCharSequence> lines) {
        int height() {
            return heading ? 22 : 22 + lines.size() * LINE_H + 7;
        }
    }

    private record Hover(int x, int y, ItemStack stack) {
    }

    private final List<Hover> hovers = new ArrayList<>();

    public LotmmiGuideScreen(Screen parent) {
        super(Component.literal(LotmmiGuideContent.TITLE));
        this.parent = parent;
    }

    private static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath("lotmcraft", "textures/gui/encyclopedia/" + name + ".png");
    }

    private static ItemStack iconOf(String id) {
        ResourceLocation key = ResourceLocation.tryParse(id);
        if (key == null) return ItemStack.EMPTY;
        return new ItemStack(BuiltInRegistries.ITEM.get(key));
    }

    @Override
    protected void init() {
        super.init();
        if (!pages.isEmpty()) return;
        List<Block> blocks = new ArrayList<>();
        for (LotmmiGuideContent.Section section : LotmmiGuideContent.SECTIONS) {
            blocks.add(new Block(true, section.title(), "", section.color(), ItemStack.EMPTY, List.of()));
            for (LotmmiGuideContent.Item item : section.items()) {
                blocks.add(new Block(false, item.title(), item.tag(), section.color(), iconOf(item.icon()),
                        font.split(Component.literal(item.text()), TEXT_WRAP)));
            }
        }
        List<Block> current = new ArrayList<>();
        int used = 0;
        for (int i = 0; i < blocks.size(); i++) {
            Block block = blocks.get(i);
            int need = block.height();
            // A heading is never left alone at the foot of a page.
            if (block.heading() && i + 1 < blocks.size()) need += blocks.get(i + 1).height();
            if (!current.isEmpty() && used + need > AREA_H) {
                pages.add(current);
                current = new ArrayList<>();
                used = 0;
            }
            current.add(block);
            used += block.height();
        }
        if (!current.isEmpty()) pages.add(current);
    }

    private int pageCount() {
        return pages.size() + 1;
    }

    private int maxSpread() {
        return (pageCount() - 1) / 2;
    }

    private int bookX() {
        return (width - BOOK_W) / 2;
    }

    private int bookY() {
        return (height - BOOK_H) / 2;
    }

    private static void blit(GuiGraphics g, ResourceLocation texture, int x, int y, int w, int h) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.blit(texture, x, y, 0.0F, 0.0F, w, h, w, h);
    }

    private void drawScaled(GuiGraphics g, FormattedCharSequence text, float x, float y, float scale, int color) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0.0F);
        g.pose().scale(scale, scale, 1.0F);
        g.drawString(font, text, 0, 0, color, false);
        g.pose().popPose();
    }

    private void drawCentered(GuiGraphics g, String text, float centerX, float y, float scale, int color) {
        FormattedCharSequence sequence = Component.literal(text).getVisualOrderText();
        float x = centerX - font.width(sequence) * scale / 2.0F;
        drawScaled(g, sequence, x, y, scale, color);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        hovers.clear();
        int bx = bookX();
        int by = bookY();
        int leftX = bx + 8;
        int rightX = leftX + PAGE_W + 4;
        int pageY = by + 8;

        g.fill(bx + 3, by + 4, bx + BOOK_W + 3, by + BOOK_H + 4, 0x66000000);
        blit(g, tex("cover"), bx, by, BOOK_W, BOOK_H);
        blit(g, tex("ribbon"), leftX + PAGE_W - 28, by - 3, 9, 51);

        drawPage(g, 2 * spread, leftX, pageY, true);
        drawPage(g, 2 * spread + 1, rightX, pageY, false);
        drawArrows(g, leftX, rightX, pageY, mouseX, mouseY);
        drawBackTab(g, bx, by, mouseX, mouseY);

        for (Hover hover : hovers) {
            if (mouseX >= hover.x() && mouseX < hover.x() + 18 && mouseY >= hover.y() && mouseY < hover.y() + 18) {
                g.renderTooltip(font, hover.stack(), mouseX, mouseY);
            }
        }
    }

    private void drawPage(GuiGraphics g, int index, int x, int y, boolean left) {
        blit(g, tex(left ? "page_left" : "page_right"), x, y, PAGE_W, PAGE_H);
        if (index >= pageCount()) return;
        int cx = x + 12;
        int cy = y + 20;
        if (index == 0) {
            drawTitlePage(g, x, y);
            return;
        }
        drawCentered(g, LotmmiGuideContent.TITLE, x + 81, y + 7, 0.7F, INK_SOFT);
        g.hLine(x + 12, x + PAGE_W - 12 - 1, y + 16, 0x55805030);
        int at = cy;
        for (Block block : pages.get(index - 1)) {
            if (block.heading()) drawHeading(g, block, cx, at);
            else drawEntry(g, block, cx, at);
            at += block.height();
        }
        drawCentered(g, String.valueOf(index + 1), x + 81, y + PAGE_H - 12, 0.8F, INK_SOFT);
    }

    private void drawTitlePage(GuiGraphics g, int x, int y) {
        float middle = x + 81;
        drawCentered(g, "lotmmi", middle, y + 40, 3.0F, INK);
        g.fill(x + 45, y + 76, x + PAGE_W - 45, y + 77, 0x55805030);
        drawCentered(g, LotmmiGuideContent.TITLE, middle, y + 86, 1.0F, INK);
        List<FormattedCharSequence> lines = font.split(Component.literal(LotmmiGuideContent.BLURB), (int) (120 / TEXT_SCALE));
        int at = y + 106;
        for (FormattedCharSequence line : lines) {
            float w = font.width(line) * TEXT_SCALE;
            drawScaled(g, line, middle - w / 2.0F, at, TEXT_SCALE, INK_TEXT);
            at += LINE_H + 1;
        }
    }

    private void drawHeading(GuiGraphics g, Block block, int x, int y) {
        drawScaled(g, Component.literal(block.title()).withStyle(net.minecraft.ChatFormatting.BOLD).getVisualOrderText(),
                x, y + 2, 1.2F, block.color() | 0xFF000000);
        g.fill(x, y + 15, x + 138, y + 16, 0x55805030);
    }

    private void drawEntry(GuiGraphics g, Block block, int x, int y) {
        blit(g, tex("slot"), x, y, 18, 18);
        g.fill(x + 1, y + 15, x + 17, y + 17, block.color());
        if (!block.icon().isEmpty()) {
            g.renderItem(block.icon(), x + 1, y + 1);
            hovers.add(new Hover(x, y, block.icon()));
        }
        g.drawString(font, block.title(), x + 24, y + 1, INK, false);
        drawScaled(g, Component.literal(block.tag()).getVisualOrderText(), x + 24, y + 11, 0.7F, block.color());
        int at = y + 22;
        for (FormattedCharSequence line : block.lines()) {
            drawScaled(g, line, x, at, TEXT_SCALE, INK_TEXT);
            at += LINE_H;
        }
    }

    private void drawArrows(GuiGraphics g, int leftX, int rightX, int pageY, int mouseX, int mouseY) {
        int y = pageY + PAGE_H - 16;
        if (spread > 0) {
            int x = leftX + 10;
            drawArrow(g, x, y, true, inside(x - 3, y - 3, 16, 15, mouseX, mouseY));
        }
        if (spread < maxSpread()) {
            int x = rightX + PAGE_W - 20;
            drawArrow(g, x, y, false, inside(x - 3, y - 3, 16, 15, mouseX, mouseY));
        }
    }

    private static void drawArrow(GuiGraphics g, int x, int y, boolean left, boolean lit) {
        int color = lit ? ARROW_LIT : ARROW_DIM;
        for (int i = 0; i < 5; i++) {
            int column = left ? x + 2 * i : x + 8 - 2 * i;
            g.fill(column, y + i, column + 2, y + 9 - i, color);
        }
    }

    /** The way back to the encyclopedia: a tab on the left edge, level with the one that opened this chapter. */
    private void drawBackTab(GuiGraphics g, int bx, int by, int mouseX, int mouseY) {
        int w = 15;
        int y = by + 14;
        int left = bx + 1 - 20;
        g.fill(left, y, left + 20, y + 16, TAB_BORDER);
        g.fill(left + 1, y + 1, left + 20, y + 15, 0xFF5B3F8C);
        blit(g, tex("tab_gloss"), left, y, 19, 16);
        String label = "<";
        g.drawString(font, label, left + (20 - font.width(label)) / 2, y + 4, -1, true);
        if (inside(left - 4, y, w + 9, 16, mouseX, mouseY)) {
            g.renderTooltip(font, Component.literal("Back to the encyclopedia"), mouseX, mouseY);
        }
    }

    private static boolean inside(int x, int y, int w, int h, double mx, double my) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private void flip(int by) {
        int next = Math.max(0, Math.min(maxSpread(), spread + by));
        if (next != spread) {
            spread = next;
            if (minecraft != null) {
                minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                        net.minecraft.sounds.SoundEvents.BOOK_PAGE_TURN, 1.0F));
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int bx = bookX();
            int by = bookY();
            int leftX = bx + 8;
            int rightX = leftX + PAGE_W + 4;
            int y = by + 8 + PAGE_H - 16;
            if (spread > 0 && inside(leftX + 10 - 3, y - 3, 16, 15, mouseX, mouseY)) {
                flip(-1);
                return true;
            }
            if (spread < maxSpread() && inside(rightX + PAGE_W - 20 - 3, y - 3, 16, 15, mouseX, mouseY)) {
                flip(1);
                return true;
            }
            if (inside(bx + 1 - 20 - 4, by + 14, 24, 16, mouseX, mouseY)) {
                onClose();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0.0D) {
            flip(scrollY > 0.0D ? -1 : 1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (key == 263 || key == 265) {
            flip(-1);
            return true;
        }
        if (key == 262 || key == 264) {
            flip(1);
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

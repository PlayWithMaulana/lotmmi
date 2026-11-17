package com.Maul.lotmmi.gui;

import com.Maul.lotmmi.network.ModPacketHandler;
import com.Maul.lotmmi.network.packets.toClient.ArrodesReplyPacket;
import com.Maul.lotmmi.network.packets.toServer.AskArrodesPacket;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class ArrodesScreen extends AbstractContainerScreen<ArrodesMenu> {

    private static final int FRAME_W = 300;
    private static final int FRAME_H = 178;
    private static final int GLASS_X = 12;
    private static final int GLASS_Y = 22;
    private static final int GLASS_W = 276;
    private static final int GLASS_H = 118;
    private static final int LINE_H = 10;
    private static final int VISIBLE = 10;
    private static final int FADE_CHARS = 7;
    private static final int CHARS_PER_TICK = 2;

    private static final int C_GREETING = 0xBDBDBD;
    private static final int C_ANSWER = 0xDCDCDC;
    private static final int C_NOTE = 0x8E8E8E;
    private static final int C_QUESTION = 0xF0F0F0;
    private static final int C_DONE = 0xA8A8A8;

    private record Line(String text, int rgb, int start) {
    }

    private final List<Line> lines = new ArrayList<>();
    private int totalChars;
    private int revealed;
    private int scroll;
    private boolean manualScroll;
    private boolean answering;
    private boolean finished;
    private boolean started;
    private float fog = 1.0F;
    private EditBox input;

    public ArrodesScreen(ArrodesMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = FRAME_W;
        this.imageHeight = FRAME_H;
        this.answering = menu.isAnswering();
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;

        if (!started) {
            started = true;
            addSegment(List.of(menu.getOpening()), answering ? C_QUESTION : C_GREETING, true);
        }

        String kept = input == null ? "" : input.getValue();
        input = new EditBox(this.font, leftPos + GLASS_X, topPos + 146, GLASS_W, 18, Component.literal("Question"));
        input.setMaxLength(AskArrodesPacket.MAX_LENGTH);
        input.setValue(kept);
        this.addRenderableWidget(input);
        this.setInitialFocus(input);

        applyMode();
    }

    private void applyMode() {
        input.setEditable(!finished);
        input.setHint(Component.literal(answering ? "Answer Arrodes" : finished ? "The mirror is silent." : "Ask the mirror..."));
    }

    private void submit() {
        String text = input.getValue().trim();
        if (text.isEmpty() || finished) return;
        ModPacketHandler.sendToServer(new AskArrodesPacket(text));
        input.setValue("");
    }

    private List<String> wrap(String text) {
        List<String> out = new ArrayList<>();
        for (FormattedCharSequence sequence : this.font.split(Component.literal(text), GLASS_W - 24)) {
            StringBuilder builder = new StringBuilder();
            sequence.accept((index, style, codePoint) -> {
                builder.appendCodePoint(codePoint);
                return true;
            });
            out.add(builder.toString());
        }
        if (out.isEmpty()) out.add("");
        return out;
    }

    private void addSegment(List<String> raw, int rgb, boolean replace) {
        if (replace) {
            lines.clear();
            totalChars = 0;
            revealed = 0;
            scroll = 0;
            manualScroll = false;
        } else if (!lines.isEmpty()) {
            lines.add(new Line("", rgb, totalChars));
        }
        for (String text : raw) {
            for (String piece : wrap(text)) {
                lines.add(new Line(piece, rgb, totalChars));
                totalChars += piece.length();
            }
        }
        manualScroll = false;
    }

    private void handle(ArrodesMenu.Reply reply) {
        switch (reply.kind()) {
            case ArrodesReplyPacket.KIND_ANSWER -> addSegment(reply.lines(), C_ANSWER, true);
            case ArrodesReplyPacket.KIND_NOTE -> addSegment(reply.lines(), C_NOTE, !answering);
            case ArrodesReplyPacket.KIND_QUESTION -> {
                answering = true;
                addSegment(reply.lines(), C_QUESTION, false);
            }
            case ArrodesReplyPacket.KIND_DONE -> {
                answering = false;
                addSegment(reply.lines(), C_DONE, false);
            }
            case ArrodesReplyPacket.KIND_LOCKED -> {
                answering = false;
                finished = true;
                addSegment(reply.lines(), C_DONE, false);
            }
            default -> addSegment(reply.lines(), C_NOTE, true);
        }
        applyMode();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        ArrodesMenu.Reply reply;
        while ((reply = menu.poll()) != null) handle(reply);
        if (revealed < totalChars) revealed = Math.min(totalChars, revealed + CHARS_PER_TICK);
    }

    private int followLine() {
        int count = 0;
        for (Line line : lines) {
            if (line.start() < revealed || revealed >= totalChars) count++;
            else break;
        }
        return Math.max(0, count - VISIBLE);
    }

    private boolean overGlass(double mouseX, double mouseY) {
        return mouseX >= leftPos + GLASS_X && mouseX < leftPos + GLASS_X + GLASS_W
                && mouseY >= topPos + GLASS_Y && mouseY < topPos + GLASS_Y + GLASS_H;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int max = Math.max(0, lines.size() - VISIBLE);
        if (max > 0 && overGlass(mouseX, mouseY)) {
            int current = manualScroll ? scroll : followLine();
            scroll = Mth.clamp(current - (int) Math.signum(scrollY), 0, max);
            manualScroll = true;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (overGlass(mouseX, mouseY)) {
            revealed = totalChars;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            submit();
            return true;
        }
        if (input.keyPressed(keyCode, scanCode, modifiers) || input.canConsumeInput()) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
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
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.fill(0, 0, this.width, this.height, 0xB0050505);
        drawFrame(g);

        int gx = leftPos + GLASS_X;
        int gy = topPos + GLASS_Y;

        float target = revealed >= totalChars ? 0.45F : 1.0F;
        fog += (target - fog) * 0.03F;

        g.fillGradient(gx, gy, gx + GLASS_W, gy + GLASS_H, 0xFF2E2E2E, 0xFF121212);
        g.enableScissor(gx, gy, gx + GLASS_W, gy + GLASS_H);
        drawFog(g, gx, gy, 1.0F, 1);
        drawLines(g, gx, gy);
        drawFog(g, gx, gy, 0.3F, 3);
        for (int i = 0; i < 6; i++) {
            g.renderOutline(gx + i, gy + i, GLASS_W - 2 * i, GLASS_H - 2 * i, (0x60 - i * 0x10) << 24);
        }
        for (int k = 0; k < 38; k++) {
            g.fill(gx + 20 + k, gy + GLASS_H - 10 - k, gx + 22 + k, gy + GLASS_H - 9 - k, 0x14FFFFFF);
        }
        g.disableScissor();

        g.drawCenteredString(this.font, "A R R O D E S", leftPos + FRAME_W / 2, topPos + 8, 0xFFBEBEBE);
    }

    private void drawFrame(GuiGraphics g) {
        int x = leftPos;
        int y = topPos;
        g.fill(x, y, x + FRAME_W, y + FRAME_H, 0xFF141414);
        g.fill(x + 2, y + 2, x + FRAME_W - 2, y + FRAME_H - 2, 0xFF4A4A4A);
        g.fill(x + 4, y + 4, x + FRAME_W - 4, y + FRAME_H - 4, 0xFF262626);
        g.renderOutline(x + 3, y + 3, FRAME_W - 6, FRAME_H - 6, 0xFF9A9A9A);
        g.renderOutline(x + GLASS_X - 2, y + GLASS_Y - 2, GLASS_W + 4, GLASS_H + 4, 0xFF9A9A9A);
        g.renderOutline(x + GLASS_X - 1, y + GLASS_Y - 1, GLASS_W + 2, GLASS_H + 2, 0xFF141414);
        int[][] corners = {{x + 1, y + 1}, {x + FRAME_W - 9, y + 1}, {x + 1, y + FRAME_H - 9}, {x + FRAME_W - 9, y + FRAME_H - 9}};
        for (int[] corner : corners) {
            g.fill(corner[0], corner[1], corner[0] + 8, corner[1] + 8, 0xFFBEBEBE);
            g.fill(corner[0] + 2, corner[1] + 2, corner[0] + 6, corner[1] + 6, 0xFF4A4A4A);
        }
    }

    private void drawFog(GuiGraphics g, int gx, int gy, float scale, int step) {
        double time = Util.getMillis() / 1000.0D;
        for (int i = 0; i < 7; i += step) {
            int by = gy + (int) (GLASS_H * (i / 7.0D) + Math.sin(time * 0.6D + i * 1.7D) * 9.0D);
            int bx = (int) (Math.sin(time * 0.3D + i) * 14.0D);
            int alpha = Mth.clamp((int) ((26.0D + 10.0D * Math.sin(time * 0.9D + i * 2.3D)) * fog * scale), 0, 255);
            int solid = (alpha << 24) | 0xD0D0D0;
            g.fillGradient(gx + bx - 20, by, gx + GLASS_W + 20 + bx, by + 8, 0x00D0D0D0, solid);
            g.fillGradient(gx + bx - 20, by + 8, gx + GLASS_W + 20 + bx, by + 16, solid, 0x00D0D0D0);
        }
        if (step != 1) return;
        for (int i = 0; i < 14; i++) {
            double speed = 4 + (i % 4) * 3;
            int wx = gx + (int) (((i * 53) + time * speed) % (GLASS_W + 40)) - 20;
            int wy = gy + 6 + (i * 29) % (GLASS_H - 12) + (int) (Math.sin(time + i) * 3.0D);
            int alpha = Mth.clamp((int) (34.0F * fog), 0, 255);
            g.fill(wx, wy, wx + 14 + (i % 3) * 6, wy + 2, (alpha << 24) | 0xE4E4E4);
        }
    }

    private void drawLines(GuiGraphics g, int gx, int gy) {
        int first = manualScroll ? scroll : followLine();
        int tx = gx + 12;
        int ty = gy + 9;
        int end = Math.min(lines.size(), first + VISIBLE);
        for (int i = first; i < end; i++) {
            Line line = lines.get(i);
            int length = line.text().length();
            int shown = Mth.clamp(revealed - line.start(), 0, length);
            if (shown <= 0 && length > 0) break;
            if (length == 0) continue;
            drawTyped(g, line.text(), shown, tx, ty + (i - first) * LINE_H, line.rgb());
        }
        if (lines.size() > VISIBLE) {
            int track = GLASS_H - 8;
            int bar = Math.max(8, track * VISIBLE / lines.size());
            int barY = gy + 4 + (track - bar) * first / (lines.size() - VISIBLE);
            g.fill(gx + GLASS_W - 6, barY, gx + GLASS_W - 4, barY + bar, 0x80A0A0A0);
        }
    }

    private void drawTyped(GuiGraphics g, String text, int shown, int x, int y, int rgb) {
        boolean typing = shown < text.length();
        int full = typing ? Math.max(0, shown - FADE_CHARS) : shown;
        String head = text.substring(0, full);
        g.drawString(this.font, head, x, y, 0xFF000000 | rgb, false);
        int cx = x + this.font.width(head);
        for (int i = full; i < shown; i++) {
            String ch = String.valueOf(text.charAt(i));
            float t = (i - full + 1) / (float) (FADE_CHARS + 1);
            int alpha = Math.max(8, (int) (255.0F * t * t));
            g.drawString(this.font, ch, cx, y, (alpha << 24) | rgb, false);
            cx += this.font.width(ch);
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

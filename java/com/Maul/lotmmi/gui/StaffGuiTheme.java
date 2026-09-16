package com.Maul.lotmmi.gui;

import net.minecraft.client.gui.GuiGraphics;

import java.util.Random;

public class StaffGuiTheme {

    private static final int STAR_COUNT = 50;

    public static void drawStarfield(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        Random random = new Random(1337);
        for (int i = 0; i < STAR_COUNT; i++) {
            int sx = x + 3 + random.nextInt(Math.max(1, width - 6));
            int sy = y + 3 + random.nextInt(Math.max(1, height - 6));
            int alpha = 30 + random.nextInt(90);
            boolean gold = random.nextFloat() < 0.7f;
            int color = (gold ? 0x00E8C866 : 0x00C8BEDC) | (alpha << 24);
            guiGraphics.fill(sx, sy, sx + 1, sy + 1, color);
        }
    }
}

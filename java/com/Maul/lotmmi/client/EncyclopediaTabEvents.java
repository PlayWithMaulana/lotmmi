package com.Maul.lotmmi.client;

import com.Maul.lotmmi.LotmMysticalItems;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.slf4j.Logger;

/**
 * Adds a "lotmmi" tab to the edge of LOTMCraft's ingredient encyclopedia. The tab opens {@link LotmmiGuideScreen}.
 * Nothing of the base screen is changed: the tab is drawn over it and its click is caught before the book sees it.
 * The Sequence tabs fill the right edge of the book, so this one sits on the left edge.
 */
@EventBusSubscriber(modid = LotmMysticalItems.MOD_ID, value = Dist.CLIENT)
public final class EncyclopediaTabEvents {

    private static final String ENCYCLOPEDIA = "de.jakob.lotm.gui.custom.encyclopedia.IngredientEncyclopediaScreen";
    private static final int TAB_W = 15;
    private static final int TAB_H = 16;
    private static final int TAB_BORDER = 0xFF1E1208;
    private static final int TAB_COLOR = 0xFF5B3F8C;

    private static final Logger LOGGER = LogUtils.getLogger();

    private static boolean failed;

    private EncyclopediaTabEvents() {
    }

    private static boolean isEncyclopedia(Screen screen) {
        return !failed && screen != null && ENCYCLOPEDIA.equals(screen.getClass().getName());
    }

    private static int bookX(Screen screen) {
        return (screen.width - LotmmiGuideScreen.BOOK_W) / 2;
    }

    private static int tabY(Screen screen) {
        return (screen.height - LotmmiGuideScreen.BOOK_H) / 2 + 14;
    }

    private static boolean overTab(Screen screen, double mouseX, double mouseY) {
        int x = bookX(screen) + 1 - TAB_W - 4;
        int y = tabY(screen);
        return mouseX >= x && mouseX < x + TAB_W + 4 && mouseY >= y && mouseY < y + TAB_H;
    }

    @SubscribeEvent
    public static void onRender(ScreenEvent.Render.Post event) {
        Screen screen = event.getScreen();
        if (!isEncyclopedia(screen)) return;
        try {
            GuiGraphics g = event.getGuiGraphics();
            boolean hovered = overTab(screen, event.getMouseX(), event.getMouseY());
            int w = hovered ? 18 : TAB_W;
            int x = bookX(screen) + 1 - w;
            int y = tabY(screen);
            g.fill(x, y, x + w, y + TAB_H, TAB_BORDER);
            g.fill(x, y + 1, x + w - 1, y + TAB_H - 1, TAB_COLOR);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            g.blit(ResourceLocation.fromNamespaceAndPath("lotmcraft", "textures/gui/encyclopedia/tab_gloss.png"),
                    x, y, 0.0F, 0.0F, w - 1, TAB_H, 20, 16);
            var font = Minecraft.getInstance().font;
            String label = "lm";
            g.drawString(font, label, x + (w - font.width(label)) / 2, y + 4, -1, true);
            if (hovered) g.renderTooltip(font, Component.literal("lotmmi"), event.getMouseX(), event.getMouseY());
        } catch (RuntimeException e) {
            failed = true;
            LOGGER.warn("The lotmmi encyclopedia tab was switched off: {}", e.toString());
        }
    }

    @SubscribeEvent
    public static void onClick(ScreenEvent.MouseButtonPressed.Pre event) {
        Screen screen = event.getScreen();
        if (!isEncyclopedia(screen) || event.getButton() != 0) return;
        if (!overTab(screen, event.getMouseX(), event.getMouseY())) return;
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
        minecraft.setScreen(new LotmmiGuideScreen(screen));
        event.setCanceled(true);
    }
}

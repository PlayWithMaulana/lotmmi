package com.Maul.lotmmi.client;

import com.Maul.lotmmi.LotmMysticalItems;
import com.Maul.lotmmi.gui.GrazeChoiceScreen;
import com.Maul.lotmmi.gui.ItemIntrospectScreen;
import com.Maul.lotmmi.gui.ModMenuTypes;
import com.Maul.lotmmi.gui.StaffIntrospectScreen;
import com.Maul.lotmmi.gui.StaffTeleportScreen;
import com.Maul.lotmmi.gui.StaffItemCaptureScreen;
import com.Maul.lotmmi.item.custom.CreepingHungerItem;
import com.Maul.lotmmi.item.custom.StaffOfStarsItem;
import com.Maul.lotmmi.network.ModPacketHandler;
import com.Maul.lotmmi.network.packets.toServer.OpenItemIntrospectPacket;
import com.Maul.lotmmi.network.packets.toServer.OpenStaffIntrospectPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = LotmMysticalItems.MOD_ID, value = Dist.CLIENT)
public class ClientEvents {

    public static final String KEY_CATEGORY = "key.categories.lotmmi";

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        LotmMysticalItems.itemIntrospectKey = new KeyMapping(
                "key.lotmmi.item_introspect", GLFW.GLFW_KEY_G, KEY_CATEGORY);

        event.register(LotmMysticalItems.itemIntrospectKey);
    }

    @SubscribeEvent
    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.ITEM_INTROSPECT_MENU.get(), ItemIntrospectScreen::new);
        event.register(ModMenuTypes.GRAZE_CHOICE_MENU.get(), GrazeChoiceScreen::new);
        event.register(ModMenuTypes.STAFF_INTROSPECT_MENU.get(), StaffIntrospectScreen::new);
        event.register(ModMenuTypes.STAFF_TELEPORT_MENU.get(), StaffTeleportScreen::new);
        event.register(ModMenuTypes.STAFF_ITEM_CAPTURE_MENU.get(), StaffItemCaptureScreen::new);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        KeyMapping key = LotmMysticalItems.itemIntrospectKey;
        if (key == null || !key.consumeClick()) return;

        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        InteractionHand staffHand = findHeldItem(player, StaffOfStarsItem.class);
        if (staffHand != null) {
            ModPacketHandler.sendToServer(new OpenStaffIntrospectPacket(staffHand == InteractionHand.MAIN_HAND));
            return;
        }

        InteractionHand creepingHungerHand = findHeldItem(player, CreepingHungerItem.class);
        if (creepingHungerHand != null) {
            ModPacketHandler.sendToServer(new OpenItemIntrospectPacket(creepingHungerHand == InteractionHand.MAIN_HAND));
            return;
        }

        player.displayClientMessage(
                Component.literal("Hold Creeping Hunger or the Staff of the Stars to Introspect it.")
                        .withStyle(ChatFormatting.DARK_GRAY),
                true);
    }

    private static InteractionHand findHeldItem(Player player, Class<?> itemClass) {
        ItemStack main = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (itemClass.isInstance(main.getItem())) return InteractionHand.MAIN_HAND;

        ItemStack off = player.getItemInHand(InteractionHand.OFF_HAND);
        if (itemClass.isInstance(off.getItem())) return InteractionHand.OFF_HAND;

        return null;
    }
}

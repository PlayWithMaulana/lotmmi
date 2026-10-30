package com.Maul.lotmmi.client;

import com.Maul.lotmmi.LotmMysticalItems;
import com.Maul.lotmmi.gui.GrazeChoiceScreen;
import com.Maul.lotmmi.gui.ItemIntrospectScreen;
import com.Maul.lotmmi.gui.ModMenuTypes;
import com.Maul.lotmmi.gui.StaffIntrospectScreen;
import com.Maul.lotmmi.gui.ScepterIntrospectScreen;
import com.Maul.lotmmi.gui.StaffTeleportScreen;
import com.Maul.lotmmi.item.custom.AwakenedCreepingHungerItem;
import com.Maul.lotmmi.item.custom.CreepingHungerItem;
import com.Maul.lotmmi.item.custom.StaffOfStarsItem;
import com.Maul.lotmmi.network.ModPacketHandler;
import com.Maul.lotmmi.network.packets.toServer.CycleSoulPacket;
import com.Maul.lotmmi.item.custom.SeaGodScepterItem;
import com.Maul.lotmmi.network.packets.toServer.OpenItemIntrospectPacket;
import com.Maul.lotmmi.network.packets.toServer.OpenScepterIntrospectPacket;
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
        LotmMysticalItems.cycleSoulLeftKey = new KeyMapping(
                "key.lotmmi.cycle_soul_left", GLFW.GLFW_KEY_Z, KEY_CATEGORY);
        LotmMysticalItems.cycleSoulRightKey = new KeyMapping(
                "key.lotmmi.cycle_soul_right", GLFW.GLFW_KEY_X, KEY_CATEGORY);

        event.register(LotmMysticalItems.itemIntrospectKey);
        event.register(LotmMysticalItems.cycleSoulLeftKey);
        event.register(LotmMysticalItems.cycleSoulRightKey);
    }

    @SubscribeEvent
    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.ITEM_INTROSPECT_MENU.get(), ItemIntrospectScreen::new);
        event.register(ModMenuTypes.GRAZE_CHOICE_MENU.get(), GrazeChoiceScreen::new);
        event.register(ModMenuTypes.STAFF_INTROSPECT_MENU.get(), StaffIntrospectScreen::new);
        event.register(ModMenuTypes.STAFF_TELEPORT_MENU.get(), StaffTeleportScreen::new);
        event.register(ModMenuTypes.SCEPTER_INTROSPECT_MENU.get(), ScepterIntrospectScreen::new);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        handleCycleKey(LotmMysticalItems.cycleSoulLeftKey, -1);
        handleCycleKey(LotmMysticalItems.cycleSoulRightKey, 1);
        handleIntrospectKey();
    }

    private static void handleCycleKey(KeyMapping key, int direction) {
        if (key == null) return;

        while (key.consumeClick()) {
            Player player = Minecraft.getInstance().player;
            if (player == null || findHeldItem(player, AwakenedCreepingHungerItem.class) == null) continue;
            ModPacketHandler.sendToServer(new CycleSoulPacket(direction));
        }
    }

    private static void handleIntrospectKey() {
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

        InteractionHand scepterHand = findHeldItem(player, SeaGodScepterItem.class);
        if (scepterHand != null) {
            ModPacketHandler.sendToServer(new OpenScepterIntrospectPacket(scepterHand == InteractionHand.MAIN_HAND));
            return;
        }

        player.displayClientMessage(
                Component.literal("Hold Creeping Hunger, the Staff of the Stars, or the Sea God Scepter to Introspect it.")
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

package com.Maul.lotmmi.gui;

import com.Maul.lotmmi.LotmMysticalItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenuTypes {

    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, LotmMysticalItems.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<ItemIntrospectMenu>> ITEM_INTROSPECT_MENU =
            MENU_TYPES.register("item_introspect_menu", () ->
                    IMenuTypeExtension.create((windowId, inv, data) -> new ItemIntrospectMenu(windowId, inv, data)));

    public static final DeferredHolder<MenuType<?>, MenuType<GrazeChoiceMenu>> GRAZE_CHOICE_MENU =
            MENU_TYPES.register("graze_choice_menu", () ->
                    IMenuTypeExtension.create((windowId, inv, data) -> new GrazeChoiceMenu(windowId, inv, data)));

    public static final DeferredHolder<MenuType<?>, MenuType<StaffIntrospectMenu>> STAFF_INTROSPECT_MENU =
            MENU_TYPES.register("staff_introspect_menu", () ->
                    IMenuTypeExtension.create((windowId, inv, data) -> new StaffIntrospectMenu(windowId, inv, data)));

    public static final DeferredHolder<MenuType<?>, MenuType<StaffTeleportMenu>> STAFF_TELEPORT_MENU =
            MENU_TYPES.register("staff_teleport_menu", () ->
                    IMenuTypeExtension.create((windowId, inv, data) -> new StaffTeleportMenu(windowId, inv, data)));

    public static final DeferredHolder<MenuType<?>, MenuType<StaffItemCaptureMenu>> STAFF_ITEM_CAPTURE_MENU =
            MENU_TYPES.register("staff_item_capture_menu", () ->
                    IMenuTypeExtension.create((windowId, inv, data) -> new StaffItemCaptureMenu(windowId, inv, data)));

    public static void register(IEventBus eventBus) {
        MENU_TYPES.register(eventBus);
    }
}

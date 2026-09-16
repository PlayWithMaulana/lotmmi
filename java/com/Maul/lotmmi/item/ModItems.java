package com.Maul.lotmmi.item;

import com.Maul.lotmmi.LotmMysticalItems;
import com.Maul.lotmmi.item.custom.AwakenedCreepingHungerItem;
import com.Maul.lotmmi.item.custom.CreepingHungerItem;
import com.Maul.lotmmi.item.custom.StaffOfStarsItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(LotmMysticalItems.MOD_ID);

    public static final DeferredItem<CreepingHungerItem> CREEPING_HUNGER =
            ITEMS.register("creeping_hunger", () -> new CreepingHungerItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<AwakenedCreepingHungerItem> AWAKENED_CREEPING_HUNGER =
            ITEMS.register("awakened_creeping_hunger", () -> new AwakenedCreepingHungerItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<StaffOfStarsItem> STAFF_OF_THE_STARS =
            ITEMS.register("staff_of_the_stars", () -> new StaffOfStarsItem(new Item.Properties().stacksTo(1).fireResistant()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}

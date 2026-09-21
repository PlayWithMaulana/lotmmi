package com.Maul.lotmmi.item;

import com.Maul.lotmmi.LotmMysticalItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, LotmMysticalItems.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> LOTMMI_TAB =
            CREATIVE_MODE_TABS.register("lotmmi", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.lotmmi"))
                    .icon(() -> new ItemStack(ModItems.CREEPING_HUNGER.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.CREEPING_HUNGER.get());
                        output.accept(ModItems.AWAKENED_CREEPING_HUNGER.get());
                        output.accept(ModItems.STAFF_OF_THE_STARS.get());
                        output.accept(ModItems.QILANGOS_SPAWN_EGG.get());
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}

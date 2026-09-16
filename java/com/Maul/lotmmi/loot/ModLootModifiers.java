package com.Maul.lotmmi.loot;

import com.Maul.lotmmi.LotmMysticalItems;
import com.mojang.serialization.MapCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class ModLootModifiers {

    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, LotmMysticalItems.MOD_ID);

    public static final Supplier<MapCodec<CreepingHungerChestLootModifier>> CREEPING_HUNGER_CHEST_LOOT =
            LOOT_MODIFIERS.register("creeping_hunger_chest_loot", CreepingHungerChestLootModifier.CODEC);

    public static final Supplier<MapCodec<StaffOfStarsChestLootModifier>> STAFF_OF_STARS_CHEST_LOOT =
            LOOT_MODIFIERS.register("staff_of_stars_chest_loot", StaffOfStarsChestLootModifier.CODEC);

    public static void register(IEventBus modEventBus) {
        LOOT_MODIFIERS.register(modEventBus);
    }
}

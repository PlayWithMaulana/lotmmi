package com.Maul.lotmmi.data;

import com.mojang.serialization.Codec;
import com.Maul.lotmmi.LotmMysticalItems;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModDataComponents {

    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, LotmMysticalItems.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> SOULS =
            DATA_COMPONENT_TYPES.register("souls", () -> DataComponentType.<String>builder()
                    .persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> FOOD_SOUL =
            DATA_COMPONENT_TYPES.register("food_soul", () -> DataComponentType.<String>builder()
                    .persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> LAST_FED_TICK =
            DATA_COMPONENT_TYPES.register("last_fed_tick", () -> DataComponentType.<Long>builder()
                    .persistent(Codec.LONG)
                    .networkSynchronized(ByteBufCodecs.VAR_LONG)
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> ABILITY_ORDER =
            DATA_COMPONENT_TYPES.register("ability_order", () -> DataComponentType.<String>builder()
                    .persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CompoundTag>> STAFF_MEMORY =
            DATA_COMPONENT_TYPES.register("staff_memory", () -> DataComponentType.<CompoundTag>builder()
                    .persistent(CompoundTag.CODEC)
                    .networkSynchronized(ByteBufCodecs.COMPOUND_TAG)
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> STAFF_TELEPORT_SLOTS =
            DATA_COMPONENT_TYPES.register("staff_teleport_slots", () -> DataComponentType.<String>builder()
                    .persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> STAFF_PANIC_LEVEL =
            DATA_COMPONENT_TYPES.register("staff_panic_level", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    public static void register(IEventBus eventBus) {
        DATA_COMPONENT_TYPES.register(eventBus);
    }
}

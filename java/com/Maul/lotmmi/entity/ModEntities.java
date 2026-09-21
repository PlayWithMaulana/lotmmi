package com.Maul.lotmmi.entity;

import com.Maul.lotmmi.LotmMysticalItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, LotmMysticalItems.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<QilangosEntity>> QILANGOS =
            ENTITY_TYPES.register("qilangos", () -> EntityType.Builder
                    .of(QilangosEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(32)
                    .updateInterval(2)
                    .build("qilangos"));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}

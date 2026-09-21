package com.Maul.lotmmi.events;

import com.Maul.lotmmi.LotmMysticalItems;
import com.Maul.lotmmi.entity.ModEntities;
import com.Maul.lotmmi.entity.QilangosEntity;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

@EventBusSubscriber(modid = LotmMysticalItems.MOD_ID)
public class QilangosEvents {

    @SubscribeEvent
    public static void onRegisterAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.QILANGOS.get(), QilangosEntity.createAttributes().build());
    }

    @SubscribeEvent
    public static void onRegisterSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(ModEntities.QILANGOS.get(), SpawnPlacementTypes.NO_RESTRICTIONS,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, QilangosEntity::checkQilangosSpawn,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
}

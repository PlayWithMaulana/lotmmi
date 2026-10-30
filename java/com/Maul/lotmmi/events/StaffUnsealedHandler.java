package com.Maul.lotmmi.events;

import com.Maul.lotmmi.LotmMysticalItems;
import com.Maul.lotmmi.config.LotmmiConfig;
import com.Maul.lotmmi.item.custom.ArtifactEffects;
import com.Maul.lotmmi.item.custom.SeaGodScepterItem;
import com.Maul.lotmmi.item.custom.StaffOfStarsItem;
import com.Maul.lotmmi.item.custom.StaffSealUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.Random;

@EventBusSubscriber(modid = LotmMysticalItems.MOD_ID)
public class StaffUnsealedHandler {
    private static final Random RANDOM = new Random();

    private static final int AMBIENT_INTERVAL_TICKS = 100;
    private static final double AMBIENT_CHANCE = 0.35D;
    private static final int SLIP_INTERVAL_TICKS = 1200;

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof ItemEntity item)) return;

        if (item.getItem().getItem() instanceof StaffOfStarsItem || item.getItem().getItem() instanceof SeaGodScepterItem) {
            item.setUnlimitedLifetime();
            item.setInvulnerable(true);
        }
    }

    @SubscribeEvent
    public static void onItemTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof ItemEntity item)) return;
        if (!(item.level() instanceof ServerLevel level)) return;

        boolean staff = item.getItem().getItem() instanceof StaffOfStarsItem;
        boolean scepter = item.getItem().getItem() instanceof SeaGodScepterItem;
        if (!staff && !scepter) return;

        int age = item.tickCount;
        if (age <= 0) return;

        if (age % 6 == 0) ArtifactEffects.droppedGlow(level, item, staff);
        if (!staff || StaffSealUtil.isSealed(level)) return;

        if (age % AMBIENT_INTERVAL_TICKS == 0 && LotmmiConfig.STAFF_AMBIENT_ANOMALIES.get() && RANDOM.nextDouble() < AMBIENT_CHANCE) {
            StaffSealUtil.ambient(level, item);
        }

        if (age % SLIP_INTERVAL_TICKS == 0 && RANDOM.nextDouble() < LotmmiConfig.STAFF_SLIP_CHANCE_DROPPED.get()) {
            StaffSealUtil.slipAway(level, item);
        }
    }
}

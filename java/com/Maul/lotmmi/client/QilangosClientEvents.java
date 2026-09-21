package com.Maul.lotmmi.client;

import com.Maul.lotmmi.LotmMysticalItems;
import com.Maul.lotmmi.entity.ModEntities;
import de.jakob.lotm.entity.custom.BeyonderNPCEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = LotmMysticalItems.MOD_ID, value = Dist.CLIENT)
public class QilangosClientEvents {

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.<BeyonderNPCEntity>registerEntityRenderer(ModEntities.QILANGOS.get(), QilangosRenderer::new);
    }
}

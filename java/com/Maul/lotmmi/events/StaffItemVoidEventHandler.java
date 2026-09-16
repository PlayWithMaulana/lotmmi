package com.Maul.lotmmi.events;

import com.Maul.lotmmi.LotmMysticalItems;
import com.Maul.lotmmi.item.custom.StaffVoidBlockTracker;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid = LotmMysticalItems.MOD_ID)
public class StaffItemVoidEventHandler {

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!(event.getEntity() instanceof net.minecraft.world.entity.player.Player player)) return;

        ItemStack main = player.getMainHandItem();
        CustomData data = main.get(DataComponents.CUSTOM_DATA);
        if (data == null) return;

        CompoundTag tag = data.copyTag();
        if (!tag.contains("VoidSummonTime") || !tag.hasUUID("StaffTrackingId")) return;

        StaffVoidBlockTracker.track(event.getPos(), tag.getUUID("StaffTrackingId"));
    }
}

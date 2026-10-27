package com.Maul.lotmmi.events;

import com.Maul.lotmmi.LotmMysticalItems;
import com.Maul.lotmmi.command.CreepingHungerCommand;
import com.Maul.lotmmi.item.custom.AwakenedCreepingHungerItem;
import com.Maul.lotmmi.item.custom.CreepingHungerItem;
import de.jakob.lotm.beyonders.potions.BeyonderCharacteristicItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

@EventBusSubscriber(modid = LotmMysticalItems.MOD_ID)
public class ModEvents {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CreepingHungerCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide) return;

        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;

        ItemStack main = player.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack off = player.getItemInHand(InteractionHand.OFF_HAND);

        ItemStack glove;
        if (main.getItem() instanceof CreepingHungerItem) {
            glove = main;
        } else if (off.getItem() instanceof CreepingHungerItem) {
            glove = off;
        } else {
            return;
        }

        boolean devoured = glove.getItem() instanceof AwakenedCreepingHungerItem
                ? AwakenedCreepingHungerItem.grazeSoul(player, target, glove)
                : CreepingHungerItem.grazeSoul(player, target, glove);

        if (devoured) {
            target.getPersistentData().putLong(CreepingHungerItem.DEVOURED_TAG, target.level().getGameTime());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onLivingDrops(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();
        CompoundTag data = entity.getPersistentData();
        if (!data.contains(CreepingHungerItem.DEVOURED_TAG)) return;

        long stamp = data.getLong(CreepingHungerItem.DEVOURED_TAG);
        data.remove(CreepingHungerItem.DEVOURED_TAG);
        if (Math.abs(entity.level().getGameTime() - stamp) > 1L) return;

        event.getDrops().removeIf(drop -> drop.getItem().getItem() instanceof BeyonderCharacteristicItem);
    }
}

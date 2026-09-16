package com.Maul.lotmmi.events;

import com.Maul.lotmmi.LotmMysticalItems;
import com.Maul.lotmmi.item.ModItems;
import com.Maul.lotmmi.item.custom.StaffMemoryUtil;
import de.jakob.lotm.beyonders.abilities.core.Ability;
import de.jakob.lotm.beyonders.abilities.core.AbilityUsedEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.List;

@EventBusSubscriber(modid = LotmMysticalItems.MOD_ID)
public class StaffOfStarsEventHandler {

    private static final double WITNESS_RADIUS = 24.0D;

    @SubscribeEvent
    public static void onAbilityUsed(AbilityUsedEvent event) {
        Ability ability = event.getAbility();
        if (ability == null) return;

        List<ServerPlayer> nearby = event.getLevel().getPlayers(player ->
                player.distanceToSqr(event.getPosition()) <= WITNESS_RADIUS * WITNESS_RADIUS);

        for (ServerPlayer player : nearby) {
            ItemStack main = player.getItemInHand(InteractionHand.MAIN_HAND);
            ItemStack off = player.getItemInHand(InteractionHand.OFF_HAND);

            if (main.is(ModItems.STAFF_OF_THE_STARS.get())) {
                StaffMemoryUtil.witnessAbility(main, ability.getId());
            } else if (off.is(ModItems.STAFF_OF_THE_STARS.get())) {
                StaffMemoryUtil.witnessAbility(off, ability.getId());
            }
        }
    }
}

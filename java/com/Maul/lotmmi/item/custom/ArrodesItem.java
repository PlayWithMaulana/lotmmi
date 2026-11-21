package com.Maul.lotmmi.item.custom;

import com.Maul.lotmmi.arrodes.ArrodesHandler;
import com.Maul.lotmmi.events.StaffCurseHandler;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ArrodesItem extends Item {

    private static final double ORBIT_RADIUS = 0.85D;

    public ArrodesItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResultHolder.pass(stack);
        if (StaffCurseHandler.isCursed(serverPlayer)) return InteractionResultHolder.fail(stack);

        ArrodesHandler.open(serverPlayer);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel)) return;
        if (!selected) return;

        long time = level.getGameTime();
        double x = entity.getX();
        double y = entity.getY();
        double z = entity.getZ();

        if (time % 4L == 0L) {
            double angle = time * 0.12D;
            double lift = 1.0D + Math.sin(time * 0.05D) * 0.25D;
            for (int i = 0; i < 2; i++) {
                double a = angle + i * Math.PI;
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        x + Math.cos(a) * ORBIT_RADIUS, y + lift, z + Math.sin(a) * ORBIT_RADIUS,
                        1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }
        if (time % 10L == 0L) {
            serverLevel.sendParticles(ParticleTypes.CLOUD, x, y + 0.1D, z, 2, 0.45D, 0.03D, 0.45D, 0.004D);
            serverLevel.sendParticles(ParticleTypes.ASH, x, y + 1.5D, z, 4, 0.55D, 0.5D, 0.55D, 0.0D);
        }
    }

    /**
     * Keeps the held item still when only its data changes. Without this the hand dips and comes back up whenever the
     * stack's data differs from the copy Minecraft last drew. Switching slots, or swapping it for a different item in the
     * same hand, still plays the normal equip animation.
     * (NeoForge 1.21.1: IItemExtension#shouldCauseReequipAnimation(ItemStack, ItemStack, boolean).)
     */
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || !ItemStack.isSameItem(oldStack, newStack);
    }
}

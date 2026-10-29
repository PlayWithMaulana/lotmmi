package com.Maul.lotmmi.item.custom;

import com.Maul.lotmmi.config.LotmmiConfig;
import com.Maul.lotmmi.data.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class SeaGodCharmItem extends Item {
    private static final float MIN_POTENCY_SCALE = 0.35F;
    private static final int USE_COOLDOWN_TICKS = 20;

    public SeaGodCharmItem(Properties properties) {
        super(properties);
    }

    public static long lifetimeTicks() {
        return LotmmiConfig.CHARM_LIFETIME_DAYS.get() * 24000L;
    }

    public static CharmType typeOf(ItemStack stack) {
        return CharmType.byId(stack.get(ModDataComponents.CHARM_ABILITY_ID.get()));
    }

    public static float potency(ItemStack stack, long now) {
        Long created = stack.get(ModDataComponents.CHARM_CREATED_TICK.get());
        if (created == null) return 1.0F;

        long lifetime = lifetimeTicks();
        long remaining = lifetime - Math.max(0L, now - created);
        return Math.max(0.0F, Math.min(1.0F, (float) remaining / (float) lifetime));
    }

    public static boolean isFaded(ItemStack stack, long now) {
        return typeOf(stack) == null || potency(stack, now) <= 0.0F;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.success(stack);
        }

        long now = level.getGameTime();
        CharmType type = typeOf(stack);
        if (type == null || potency(stack, now) <= 0.0F) {
            serverPlayer.displayClientMessage(Component.literal("This charm has no spirituality left.")
                    .withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.fail(stack);
        }

        float scale = MIN_POTENCY_SCALE + (1.0F - MIN_POTENCY_SCALE) * potency(stack, now);
        if (!type.apply(serverLevel, serverPlayer, scale)) {
            return InteractionResultHolder.fail(stack);
        }

        player.getCooldowns().addCooldown(this, USE_COOLDOWN_TICKS);
        stack.shrink(1);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (level.isClientSide) return;
        if (stack.get(ModDataComponents.CHARM_CREATED_TICK.get()) == null) {
            stack.set(ModDataComponents.CHARM_CREATED_TICK.get(), level.getGameTime());
        }
    }

    @Override
    public Component getName(ItemStack stack) {
        CharmType type = typeOf(stack);
        if (type == null) return Component.literal("Faded Sea God Charm");
        return Component.literal(type.displayName() + " Charm");
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        CharmType type = typeOf(stack);
        if (type != null) {
            tooltip.add(Component.literal(type.description()).withStyle(ChatFormatting.AQUA));
        }

        Level level = context.level();
        if (level != null) {
            long now = level.getGameTime();
            if (isFaded(stack, now)) {
                tooltip.add(Component.literal("Faded - no spirituality left").withStyle(ChatFormatting.DARK_GRAY));
            } else {
                int percent = Math.round(potency(stack, now) * 100.0F);
                tooltip.add(Component.literal("Spirituality: " + percent + "%").withStyle(ChatFormatting.GRAY));
            }
        }
        tooltip.add(Component.literal("One-time use").withStyle(ChatFormatting.GRAY));
    }
}

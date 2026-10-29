package com.Maul.lotmmi.events;

import com.Maul.lotmmi.LotmMysticalItems;
import com.Maul.lotmmi.effect.ModEffects;
import de.jakob.lotm.beyonders.abilities.core.AbilityUseEvent;
import de.jakob.lotm.util.shapeShifting.ShapeShiftingUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.List;
import java.util.Random;

@EventBusSubscriber(modid = LotmMysticalItems.MOD_ID)
public class StaffCurseHandler {
    private static final String TAG = "lotmmi_cursed";
    private static final Random RANDOM = new Random();

    private static final ResourceLocation HEALTH_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(LotmMysticalItems.MOD_ID, "cursed_health");

    private record Form(String entityId, String name, float health, boolean hops) {}

    private static final List<Form> FORMS = List.of(
            new Form("minecraft:frog", "frog", 10.0F, true),
            new Form("minecraft:chicken", "chicken", 4.0F, false),
            new Form("minecraft:rabbit", "rabbit", 3.0F, true),
            new Form("minecraft:sheep", "sheep", 8.0F, false),
            new Form("minecraft:pig", "pig", 10.0F, false),
            new Form("minecraft:cat", "cat", 10.0F, false)
    );

    public static boolean isCursed(Player player) {
        return player.hasEffect(ModEffects.CURSED);
    }

    public static void curse(ServerPlayer player, int seconds) {
        if (isCursed(player)) return;
        player.addEffect(new MobEffectInstance(ModEffects.CURSED, seconds * 20, 0, false, true, true));
    }

    private static boolean isEdible(ItemStack stack) {
        UseAnim animation = stack.getUseAnimation();
        return animation == UseAnim.EAT || animation == UseAnim.DRINK;
    }

    private static void transform(ServerPlayer player) {
        Form form = FORMS.get(RANDOM.nextInt(FORMS.size()));

        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        float maxBefore = maxHealth == null ? 20.0F : (float) maxHealth.getValue();
        float healthBefore = player.getHealth();

        CompoundTag tag = new CompoundTag();
        tag.putString("Form", form.entityId());
        tag.putBoolean("Hops", form.hops());
        player.getPersistentData().put(TAG, tag);

        if (maxHealth != null && maxBefore > 0.0F) {
            maxHealth.removeModifier(HEALTH_MODIFIER_ID);
            maxHealth.addTransientModifier(new AttributeModifier(HEALTH_MODIFIER_ID,
                    form.health() / maxBefore - 1.0D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            player.setHealth(Math.max(1.0F, Math.min(form.health(), healthBefore * form.health() / maxBefore)));
        }

        ShapeShiftingUtil.shapeShift(player, form.entityId(), false);

        MobEffectInstance curse = player.getEffect(ModEffects.CURSED);
        if (form.hops() && curse != null) {
            player.addEffect(new MobEffectInstance(MobEffects.JUMP, curse.getDuration(), 2, false, false, true));
        }

        player.level().playSound(null, player.blockPosition(), SoundEvents.ENDER_EYE_DEATH, SoundSource.PLAYERS, 1.0F, 0.8F);
        player.displayClientMessage(Component.literal("The stars turn you into a " + form.name() + "! Your powers are sealed.")
                .withStyle(ChatFormatting.DARK_RED), true);
    }

    private static void revert(ServerPlayer player, boolean restoreHealth, boolean announce) {
        CompoundTag tag = player.getPersistentData().getCompound(TAG);
        boolean hopped = tag.getBoolean("Hops");
        player.getPersistentData().remove(TAG);

        float ratio = player.getMaxHealth() > 0.0F ? player.getHealth() / player.getMaxHealth() : 1.0F;

        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) maxHealth.removeModifier(HEALTH_MODIFIER_ID);
        if (restoreHealth && player.isAlive()) {
            player.setHealth(Math.max(1.0F, ratio * player.getMaxHealth()));
        }

        ShapeShiftingUtil.resetShape(player);
        if (hopped) player.removeEffect(MobEffects.JUMP);
        player.removeEffect(ModEffects.CURSED);

        if (announce) {
            player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 1.0F);
            player.displayClientMessage(Component.literal("You are back to normal.").withStyle(ChatFormatting.GREEN), true);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        boolean cursed = isCursed(player);
        boolean transformed = player.getPersistentData().contains(TAG);

        if (cursed && !transformed) {
            transform(player);
        } else if (!cursed && transformed) {
            revert(player, true, true);
        } else if (cursed && player.level().getGameTime() % 120L == 0L
                && "minecraft:frog".equals(player.getPersistentData().getCompound(TAG).getString("Form"))) {
            player.level().playSound(null, player.blockPosition(), SoundEvents.FROG_AMBIENT, SoundSource.PLAYERS, 0.8F, 1.0F);
        }
    }

    @SubscribeEvent
    public static void onAbilityUse(AbilityUseEvent event) {
        if (!(event.getEntity() instanceof Player player) || !isCursed(player)) return;

        event.setCanceled(true);
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.displayClientMessage(Component.literal("You are cursed. Your powers are sealed.")
                    .withStyle(ChatFormatting.DARK_RED), true);
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (isCursed(event.getEntity()) && !isEdible(event.getItemStack())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (isCursed(event.getEntity()) && !isEdible(event.getItemStack())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (isCursed(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (isCursed(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (isCursed(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (isCursed(event.getPlayer())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onUseItemFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !isCursed(player)) return;

        ItemStack item = event.getItem();
        if (item.is(Items.GOLDEN_APPLE) || item.is(Items.ENCHANTED_GOLDEN_APPLE)) {
            player.removeEffect(ModEffects.CURSED);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.getPersistentData().contains(TAG)) revert(player, false, false);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.getPersistentData().contains(TAG) || isCursed(player)) revert(player, true, false);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.getPersistentData().contains(TAG)) revert(player, true, false);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        if (player.getPersistentData().contains(TAG)) {
            revert(player, false, false);
        } else {
            player.removeEffect(ModEffects.CURSED);
            AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
            if (maxHealth != null) maxHealth.removeModifier(HEALTH_MODIFIER_ID);
        }
    }
}

package com.Maul.lotmmi.item.custom;

import com.Maul.lotmmi.data.ModDataComponents;
import de.jakob.lotm.LOTMCraft;
import de.jakob.lotm.attachments.DoorAuthorityData;
import de.jakob.lotm.beyonders.abilities.core.Ability;
import de.jakob.lotm.beyonders.artifacts.SealedArtifactData;
import de.jakob.lotm.beyonders.artifacts.NegativeEffect;
import de.jakob.lotm.beyonders.potions.BeyonderCharacteristicItem;
import de.jakob.lotm.beyonders.potions.BeyonderCharacteristicItemHandler;
import de.jakob.lotm.gamerule.ModGameRules;
import de.jakob.lotm.util.BeyonderData;
import de.jakob.lotm.util.helper.AbilityUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CreepingHungerItem extends Item {

    public static final int MAX_SLOTS = 5;
    public static final int ABILITIES_PER_SOUL = 3;

    public static final long FEED_INTERVAL_TICKS = 24000L;
    private static final Random RANDOM = new Random();

    public CreepingHungerItem(Properties properties) {
        super(properties);
    }

    public int getMaxSlots() {
        return MAX_SLOTS;
    }

    public static int maxSlotsFor(ItemStack stack) {
        return stack.getItem() instanceof CreepingHungerItem item ? item.getMaxSlots() : MAX_SLOTS;
    }

    private static List<NegativeEffect> tokenNegatives() {
        List<NegativeEffect> list = new ArrayList<>();

        list.add(new NegativeEffect(NegativeEffect.NegativeEffectType.CURSED, 0, null, 0));
        return list;
    }

    public record SoulSlot(String pathway, int sequence, String ownerName, String ownerUUID, List<String> abilityIds) {

        public String serialize() {
            return pathway + "|" + sequence + "|" + ownerName + "|" + ownerUUID + "|" + String.join(",", abilityIds);
        }

        public static SoulSlot deserialize(String raw) {
            String[] parts = raw.split("\\|", -1);
            if (parts.length != 5) return null;
            try {
                List<String> ids = parts[4].isEmpty() ? new ArrayList<>() : new ArrayList<>(List.of(parts[4].split(",")));
                return new SoulSlot(parts[0], Integer.parseInt(parts[1]), parts[2], parts[3], ids);
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }

    public static List<SoulSlot> getSouls(ItemStack stack) {
        return parseSouls(stack.getOrDefault(ModDataComponents.SOULS.get(), ""));
    }

    public static List<SoulSlot> parseSouls(String raw) {
        List<SoulSlot> souls = new ArrayList<>();
        if (raw == null || raw.isEmpty()) return souls;
        for (String entry : raw.split(";")) {
            SoulSlot slot = SoulSlot.deserialize(entry);
            if (slot != null) souls.add(slot);
        }
        return souls;
    }

    public static void setSouls(ItemStack stack, List<SoulSlot> souls) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < souls.size(); i++) {
            if (i > 0) sb.append(";");
            sb.append(souls.get(i).serialize());
        }
        stack.set(ModDataComponents.SOULS.get(), sb.toString());
    }

    public record AbilityEntry(String id, boolean enabled) {}

    public static List<AbilityEntry> getAbilityOrder(ItemStack stack) {
        return parseAbilityOrder(stack.getOrDefault(ModDataComponents.ABILITY_ORDER.get(), ""));
    }

    public static List<AbilityEntry> parseAbilityOrder(String raw) {
        List<AbilityEntry> list = new ArrayList<>();
        if (raw == null || raw.isEmpty()) return list;
        for (String part : raw.split(",")) {
            if (part.isEmpty()) continue;
            String[] kv = part.split(":", -1);
            if (kv.length != 2) continue;
            list.add(new AbilityEntry(kv[0], "1".equals(kv[1])));
        }
        return list;
    }

    public static void setAbilityOrder(ItemStack stack, List<AbilityEntry> order) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < order.size(); i++) {
            if (i > 0) sb.append(",");
            AbilityEntry e = order.get(i);
            sb.append(e.id()).append(":").append(e.enabled() ? "1" : "0");
        }
        stack.set(ModDataComponents.ABILITY_ORDER.get(), sb.toString());
    }

    public static void recomputeArtifactData(ItemStack glove) {
        List<SoulSlot> souls = getSouls(glove);

        List<String> ownedIds = new ArrayList<>();
        for (SoulSlot soul : souls) {
            for (String id : soul.abilityIds()) {
                if (!ownedIds.contains(id)) ownedIds.add(id);
            }
        }

        List<AbilityEntry> order = new ArrayList<>();
        for (AbilityEntry entry : getAbilityOrder(glove)) {
            if (ownedIds.contains(entry.id())) order.add(entry);
        }
        List<String> known = order.stream().map(AbilityEntry::id).toList();
        for (String id : ownedIds) {
            if (!known.contains(id)) order.add(new AbilityEntry(id, true));
        }
        setAbilityOrder(glove, order);

        List<Ability> abilities = new ArrayList<>();
        for (AbilityEntry entry : order) {
            if (!entry.enabled()) continue;
            Ability ability;
            try {
                ability = LOTMCraft.abilityHandler.getById(entry.id());
            } catch (Exception e) {
                ability = null;
            }
            if (ability != null) abilities.add(ability);
        }

        if (abilities.isEmpty() || souls.isEmpty()) {
            glove.remove(de.jakob.lotm.data.ModDataComponents.SEALED_ARTIFACT_DATA.get());
            glove.remove(de.jakob.lotm.data.ModDataComponents.SEALED_ARTIFACT_SELECTED.get());
            return;
        }

        SoulSlot fallback = souls.get(souls.size() - 1);
        SealedArtifactData newData = new SealedArtifactData(fallback.pathway(), fallback.sequence(), abilities, tokenNegatives());
        glove.set(de.jakob.lotm.data.ModDataComponents.SEALED_ARTIFACT_DATA.get(), newData);

        int selected = glove.getOrDefault(de.jakob.lotm.data.ModDataComponents.SEALED_ARTIFACT_SELECTED.get(), 0);
        if (selected >= abilities.size()) {
            glove.set(de.jakob.lotm.data.ModDataComponents.SEALED_ARTIFACT_SELECTED.get(), 0);
        }
    }

    public static void reorderAbility(ItemStack glove, String abilityId, int direction) {
        List<AbilityEntry> order = getAbilityOrder(glove);
        int index = -1;
        for (int i = 0; i < order.size(); i++) {
            if (order.get(i).id().equals(abilityId)) { index = i; break; }
        }
        if (index < 0) return;
        int target = index + (direction < 0 ? -1 : 1);
        if (target < 0 || target >= order.size()) return;

        AbilityEntry a = order.get(index);
        AbilityEntry b = order.get(target);
        order.set(index, b);
        order.set(target, a);
        setAbilityOrder(glove, order);
        recomputeArtifactData(glove);
    }

    public static void toggleAbility(ItemStack glove, String abilityId) {
        List<AbilityEntry> order = getAbilityOrder(glove);
        for (int i = 0; i < order.size(); i++) {
            AbilityEntry e = order.get(i);
            if (e.id().equals(abilityId)) {
                order.set(i, new AbilityEntry(e.id(), !e.enabled()));
                break;
            }
        }
        setAbilityOrder(glove, order);
        recomputeArtifactData(glove);
    }

    public static SoulSlot getFoodSoul(ItemStack stack) {
        String raw = stack.getOrDefault(ModDataComponents.FOOD_SOUL.get(), "");
        if (raw.isEmpty()) return null;
        return SoulSlot.deserialize(raw);
    }

    public static void setFoodSoul(ItemStack stack, SoulSlot soul) {
        stack.set(ModDataComponents.FOOD_SOUL.get(), soul == null ? "" : soul.serialize());
    }

    public static boolean hasFoodSoul(ItemStack stack) {
        return getFoodSoul(stack) != null;
    }

    public static boolean feedSoul(ServerPlayer player, ItemStack glove, LivingEntity target) {
        if (!BeyonderData.isBeyonder(target)) {
            player.sendSystemMessage(Component.literal("This target has no soul worth feeding on.")
                    .withStyle(ChatFormatting.RED));
            return false;
        }
        if (hasFoodSoul(glove)) {
            player.sendSystemMessage(Component.literal("Creeping Hunger's reserve is already full.")
                    .withStyle(ChatFormatting.RED));
            return false;
        }

        String pathway = BeyonderData.getPathway(target);
        int sequence = BeyonderData.getSequence(target);
        SoulSlot food = new SoulSlot(pathway, sequence, target.getName().getString(),
                target.getUUID().toString(), new ArrayList<>());
        setFoodSoul(glove, food);

        player.sendSystemMessage(Component.literal(
                "Fed the soul of " + target.getName().getString() + " into the reserve. It will be devoured before you are."
        ).withStyle(ChatFormatting.DARK_PURPLE));
        return true;
    }

    public static void moveToReserve(ServerPlayer player, ItemStack glove, int slotIndex) {
        List<SoulSlot> souls = getSouls(glove);
        if (slotIndex < 0 || slotIndex >= souls.size()) return;
        if (hasFoodSoul(glove)) {
            player.sendSystemMessage(Component.literal("The reserve is already full.")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        SoulSlot moved = souls.remove(slotIndex);
        setSouls(glove, souls);
        recomputeArtifactData(glove);

        setFoodSoul(glove, new SoulSlot(moved.pathway(), moved.sequence(),
                moved.ownerName(), moved.ownerUUID(), new ArrayList<>()));

        player.sendSystemMessage(Component.literal(
                "Moved the soul of " + moved.ownerName() + " into the reserve. It will be devoured before you are."
        ).withStyle(ChatFormatting.DARK_PURPLE));
    }

    private static SoulSlot findSoulForAbility(List<SoulSlot> souls, String abilityId) {
        for (SoulSlot soul : souls) {
            if (soul.abilityIds().contains(abilityId)) return soul;
        }
        return null;
    }

    protected static List<SoulSlot> prepareGrazeSlots(ServerPlayer player, ItemStack glove, LivingEntity target, int maxSlots) {
        List<SoulSlot> souls = getSouls(glove);
        if (souls.size() >= maxSlots) {

            if (!hasFoodSoul(glove)) {
                feedSoul(player, glove, target);
            } else {
                player.sendSystemMessage(Component.literal("Creeping Hunger holds " + maxSlots
                        + " souls and its reserve is full. Release one first.")
                        .withStyle(ChatFormatting.RED));
            }
            return null;
        }
        return souls;
    }

    protected static List<Ability> rollAbilities(String pathway, int sequence, int wantCount, List<Ability> excluded) {
        List<Ability> granted = new ArrayList<>();
        boolean exact = true;
        while (granted.size() < wantCount) {
            Ability ability = LOTMCraft.abilityHandler.getRandomAbility(pathway, sequence, RANDOM, exact, excluded);
            exact = false;

            if (ability == null) break;

            excluded.add(ability);
            if (ability.canBeCopied) {
                granted.add(ability);
            }
        }
        return granted;
    }

    protected static void finalizeGraze(ServerPlayer player, ItemStack glove, String targetName, String targetUUID,
                                         String pathway, int sequence, List<SoulSlot> souls, List<Ability> granted) {
        List<String> grantedIds = granted.stream().map(Ability::getId).toList();
        SoulSlot soul = new SoulSlot(pathway, sequence, targetName, targetUUID, grantedIds);
        souls.add(soul);
        setSouls(glove, souls);
        recomputeArtifactData(glove);

        player.sendSystemMessage(Component.literal(
                "Grazed the soul of " + targetName + " (" + pathway + " Sequence " + sequence
                        + ") - " + granted.size() + " abilities added to your Artifact Wheel."
        ).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    public static void grazeSoul(ServerPlayer player, LivingEntity target, ItemStack glove) {
        if (!BeyonderData.isBeyonder(target)) {
            player.sendSystemMessage(Component.literal("This target has no soul worth grazing.")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        List<SoulSlot> souls = prepareGrazeSlots(player, glove, target, maxSlotsFor(glove));
        if (souls == null) return;

        String targetPathway = BeyonderData.getPathway(target);
        int targetSequence = BeyonderData.getSequence(target);

        List<Ability> excluded = new ArrayList<>();
        List<Ability> granted = rollAbilities(targetPathway, targetSequence, ABILITIES_PER_SOUL, excluded);

        if (granted.isEmpty()) {
            player.sendSystemMessage(Component.literal("This soul's power resists being grazed.")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        finalizeGraze(player, glove, target.getName().getString(), target.getUUID().toString(),
                targetPathway, targetSequence, souls, granted);
    }

    public static void releaseSoul(ServerPlayer player, ItemStack glove, int slotIndex) {
        List<SoulSlot> souls = getSouls(glove);
        if (slotIndex < 0 || slotIndex >= souls.size()) return;

        SoulSlot released = souls.remove(slotIndex);
        setSouls(glove, souls);
        recomputeArtifactData(glove);

        spitCharacteristic(player, released.pathway(), released.sequence());

        glove.set(ModDataComponents.LAST_FED_TICK.get(), player.level().getGameTime());
        player.heal(4.0f);

        player.sendSystemMessage(Component.literal(
                "Released the soul of " + released.ownerName() + ". Its Characteristic has been spat back out."
        ).withStyle(ChatFormatting.DARK_PURPLE));
    }

    private static void spitCharacteristic(ServerPlayer player, String pathway, int sequence) {
        BeyonderCharacteristicItem characteristic = findCharacteristicItem(pathway, sequence);
        if (characteristic == null) return;
        ItemStack spat = new ItemStack(characteristic);
        if (!player.getInventory().add(spat)) {
            player.drop(spat, false);
        }
    }

    public static long getTicksUntilNextFeed(ItemStack stack, long currentGameTime) {
        long lastFed = stack.getOrDefault(ModDataComponents.LAST_FED_TICK.get(), currentGameTime);
        return getTicksUntilNextFeed(lastFed, currentGameTime);
    }

    public static long getTicksUntilNextFeed(long lastFedTick, long currentGameTime) {
        long elapsed = currentGameTime - lastFedTick;
        if (elapsed < 0) elapsed = 0;
        long remaining = FEED_INTERVAL_TICKS - elapsed;
        if (remaining < 0) remaining = 0;
        if (remaining > FEED_INTERVAL_TICKS) remaining = FEED_INTERVAL_TICKS;
        return remaining;
    }

    public static boolean feedReserveNow(ServerPlayer player, ItemStack glove) {
        SoulSlot food = getFoodSoul(glove);
        if (food == null) {
            player.sendSystemMessage(Component.literal(
                    "Creeping Hunger's reserve is empty - there is nothing to feed it.")
                    .withStyle(ChatFormatting.RED));
            return false;
        }

        setFoodSoul(glove, null);
        glove.set(ModDataComponents.LAST_FED_TICK.get(), player.level().getGameTime());
        spitCharacteristic(player, food.pathway(), food.sequence());

        player.sendSystemMessage(Component.literal(
                "You feed Creeping Hunger the reserve soul of " + food.ownerName()
                        + ". It spits out their Characteristic and settles for another day."
        ).withStyle(ChatFormatting.DARK_PURPLE));
        return true;
    }

    private static BeyonderCharacteristicItem findCharacteristicItem(String pathway, int sequence) {
        return BeyonderCharacteristicItemHandler.ITEMS.getEntries().stream()
                .map(DeferredHolder::get)
                .filter(i -> i instanceof BeyonderCharacteristicItem)
                .map(i -> (BeyonderCharacteristicItem) i)
                .filter(c -> c.getPathway().equals(pathway) && c.getSequence() == sequence)
                .findFirst()
                .orElse(null);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        if (!level.getGameRules().getBoolean(ModGameRules.ALLOW_ARTIFACTS)) {
            return InteractionResultHolder.fail(stack);
        }

        DoorAuthorityData doorData = DoorAuthorityData.get((ServerLevel) level);
        if (doorData.isActive() && doorData.getEffectId().equalsIgnoreCase("strengthen")) {
            de.jakob.lotm.util.helper.ParticleUtil.spawnParticles((ServerLevel) level, ParticleTypes.END_ROD, player.getEyePosition(), 40, .5, .05);
            return InteractionResultHolder.fail(stack);
        }

        SealedArtifactData data = stack.get(de.jakob.lotm.data.ModDataComponents.SEALED_ARTIFACT_DATA.get());
        if (data == null || data.abilities().isEmpty()) {
            player.sendSystemMessage(Component.literal("Creeping Hunger holds no souls to draw power from.")
                    .withStyle(ChatFormatting.RED));
            return InteractionResultHolder.fail(stack);
        }

        int selectedIndex = stack.getOrDefault(de.jakob.lotm.data.ModDataComponents.SEALED_ARTIFACT_SELECTED.get(), 0);
        if (selectedIndex >= data.abilities().size()) selectedIndex = 0;
        Ability ability = data.abilities().get(selectedIndex);

        List<SoulSlot> souls = getSouls(stack);
        SoulSlot owningSoul = findSoulForAbility(souls, ability.getId());
        String scalePathway = owningSoul != null ? owningSoul.pathway() : data.pathway();
        int scaleSequence = owningSoul != null ? owningSoul.sequence() : data.sequence();
        AbilityUtil.setArtifactScaling(player, scalePathway, scaleSequence);

        ability.useAbility((ServerLevel) level, player, true, false, true, false);

        return InteractionResultHolder.success(stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity, int slotId, boolean isSelected) {
        if (level.isClientSide || !(entity instanceof ServerPlayer player)) return;

        long lastFed = stack.getOrDefault(ModDataComponents.LAST_FED_TICK.get(), level.getGameTime());
        long ticksSinceFed = level.getGameTime() - lastFed;

        if (ticksSinceFed < FEED_INTERVAL_TICKS) return;

        SoulSlot food = getFoodSoul(stack);
        if (food != null) {

            setFoodSoul(stack, null);
            stack.set(ModDataComponents.LAST_FED_TICK.get(), level.getGameTime());
            spitCharacteristic(player, food.pathway(), food.sequence());
            player.sendSystemMessage(Component.literal(
                    "Creeping Hunger devours its reserve soul (" + food.ownerName()
                            + ") and spits out their Characteristic."
            ).withStyle(ChatFormatting.DARK_PURPLE));
        } else {

            player.sendSystemMessage(Component.literal(
                    "Creeping Hunger, its reserve empty, finds nothing to eat but you."
            ).withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
            stack.set(ModDataComponents.LAST_FED_TICK.get(), level.getGameTime());
            player.kill();
        }
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(this.getDescriptionId(stack));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {

        tooltip.add(Component.literal("Sealed Artifact - The Devouring Glove")
                .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
        tooltip.add(Component.literal("\"It is always hungry. Slay a Beyonder while wearing it and")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.literal(" the glove gnaws their soul from their corpse, keeping a")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.literal(" scrap of their power for itself... and for you.\"")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.empty());

        List<SoulSlot> souls = getSouls(stack);
        tooltip.add(Component.literal("Souls held: " + souls.size() + "/" + maxSlotsFor(stack))
                .withStyle(ChatFormatting.LIGHT_PURPLE));

        for (int i = 0; i < souls.size(); i++) {
            SoulSlot s = souls.get(i);
            tooltip.add(Component.literal("  [" + i + "] " + s.ownerName() + " - " + s.pathway() + " Seq " + s.sequence())
                    .withStyle(ChatFormatting.GRAY));
        }

        SoulSlot food = getFoodSoul(stack);
        if (food != null) {
            tooltip.add(Component.literal("Reserve (food): " + food.ownerName() + " - "
                    + food.pathway() + " Seq " + food.sequence()).withStyle(ChatFormatting.DARK_GREEN));
        } else {
            tooltip.add(Component.literal("Reserve (food): empty - feed it, or it feeds on you")
                    .withStyle(ChatFormatting.DARK_RED));
        }

        SealedArtifactData data = stack.get(de.jakob.lotm.data.ModDataComponents.SEALED_ARTIFACT_DATA.get());
        if (data != null && !data.abilities().isEmpty()) {
            int selected = stack.getOrDefault(de.jakob.lotm.data.ModDataComponents.SEALED_ARTIFACT_SELECTED.get(), 0);
            if (selected < data.abilities().size()) {
                tooltip.add(Component.literal("Selected ability: " + data.abilities().get(selected).getId())
                        .withStyle(ChatFormatting.GOLD));
            }
        }

        tooltip.add(Component.empty());
        tooltip.add(Component.literal("Kill a Beyonder (either hand) to graze 3 of their abilities")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("Open the Artifact Wheel to select, right-click to cast")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("Grazing with 5 souls full stashes the soul as reserve food")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("It devours a soul once every Minecraft day - an empty")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("reserve when the clock runs out means it kills you instead")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("Hold while pressing [Item Introspect] (default G) to inspect it")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("/creepinghunger list | release <slot> | reserve <slot>")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}

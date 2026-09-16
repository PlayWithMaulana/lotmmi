package com.Maul.lotmmi.item.custom;

import com.Maul.lotmmi.data.ModDataComponents;
import de.jakob.lotm.util.BeyonderData;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class StaffMemoryUtil {

    public static final int MAX_WITNESS_COUNT = 10;
    public static final int MAX_ACTIVE_ENTITY_SUMMONS = 2;
    public static final int MAX_RECORDED_ITEMS = 20;
    public static final int MAX_ACTIVE_ITEM_SUMMONS = 20;

    public static final long STARE_UNLOCK_TICKS = 20 * 20;
    public static final long STARE_MAX_TICKS = 20 * 60 * 10;

    public record EntityWatch(UUID sourceUUID, String entityType, String displayName, String pathway, int sequence,
                               long watchedTicks, CompoundTag entityNbt) {}

    public record ActiveEntitySummon(UUID summonedUUID, String displayName, String pathway, int sequence, long expiryTick) {}

    public record ActiveItemSummon(UUID trackingId, String displayName, long expiryTick) {}

    private static CompoundTag get(ItemStack stack) {
        CompoundTag tag = stack.get(ModDataComponents.STAFF_MEMORY.get());
        return tag == null ? new CompoundTag() : tag.copy();
    }

    private static void save(ItemStack stack, CompoundTag tag) {
        stack.set(ModDataComponents.STAFF_MEMORY.get(), tag);
    }

    public static int capPercentForSequence(int sequence) {
        if (sequence >= 5) return 90;
        return switch (sequence) {
            case 4 -> 70;
            case 3 -> 60;
            case 2 -> 40;
            case 1 -> 30;
            default -> 20;
        };
    }

    public static void witnessAbility(ItemStack stack, String abilityId) {
        CompoundTag root = get(stack);
        CompoundTag abilities = root.contains("Abilities") ? root.getCompound("Abilities") : new CompoundTag();
        int current = abilities.contains(abilityId) ? abilities.getInt(abilityId) : 0;
        abilities.putInt(abilityId, Math.min(MAX_WITNESS_COUNT, current + 1));
        root.put("Abilities", abilities);
        save(stack, root);
    }

    public static int getAbilityWitnessCount(ItemStack stack, String abilityId) {
        CompoundTag root = get(stack);
        if (!root.contains("Abilities")) return 0;
        CompoundTag abilities = root.getCompound("Abilities");
        return abilities.contains(abilityId) ? abilities.getInt(abilityId) : 0;
    }

    public static List<String> getWitnessedAbilityIds(ItemStack stack) {
        CompoundTag root = get(stack);
        List<String> ids = new ArrayList<>();
        if (!root.contains("Abilities")) return ids;
        ids.addAll(root.getCompound("Abilities").getAllKeys());
        return ids;
    }

    public static double getAccuracy(ItemStack stack, String abilityId, int requiredSequence) {
        int count = getAbilityWitnessCount(stack, abilityId);
        int cap = capPercentForSequence(requiredSequence);
        return (Math.min(count, MAX_WITNESS_COUNT) / (double) MAX_WITNESS_COUNT) * (cap / 100.0);
    }

    public static List<String> getWheel(ItemStack stack) {
        CompoundTag root = get(stack);
        List<String> wheel = new ArrayList<>();
        if (!root.contains("Wheel")) return wheel;
        ListTag list = root.getList("Wheel", Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) wheel.add(list.getString(i));
        return wheel;
    }

    private static void saveWheel(ItemStack stack, List<String> wheel) {
        CompoundTag root = get(stack);
        ListTag list = new ListTag();
        for (String id : wheel) list.add(net.minecraft.nbt.StringTag.valueOf(id));
        root.put("Wheel", list);
        save(stack, root);
    }

    public static void addToWheel(ItemStack stack, String abilityId) {
        List<String> wheel = new ArrayList<>(getWheel(stack));
        if (!wheel.contains(abilityId)) {
            wheel.add(abilityId);
            saveWheel(stack, wheel);
        }
    }

    public static void removeFromWheel(ItemStack stack, String abilityId) {
        List<String> wheel = new ArrayList<>(getWheel(stack));
        if (wheel.remove(abilityId)) {
            saveWheel(stack, wheel);
        }
    }

    public static void moveInWheel(ItemStack stack, int index, int direction) {
        List<String> wheel = new ArrayList<>(getWheel(stack));
        int target = index + direction;
        if (index < 0 || index >= wheel.size() || target < 0 || target >= wheel.size()) return;
        String tmp = wheel.get(index);
        wheel.set(index, wheel.get(target));
        wheel.set(target, tmp);
        saveWheel(stack, wheel);
    }

    public static void forgetAbility(ItemStack stack, String abilityId) {
        CompoundTag root = get(stack);
        if (root.contains("Abilities")) {
            CompoundTag abilities = root.getCompound("Abilities");
            abilities.remove(abilityId);
            root.put("Abilities", abilities);
        }
        save(stack, root);
        removeFromWheel(stack, abilityId);
    }

    public static List<EntityWatch> getEntityWatches(ItemStack stack) {
        CompoundTag root = get(stack);
        List<EntityWatch> result = new ArrayList<>();
        if (!root.contains("EntityWatch")) return result;
        ListTag list = root.getList("EntityWatch", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag e = list.getCompound(i);
            result.add(new EntityWatch(e.getUUID("SourceUUID"), e.getString("EntityType"), e.getString("DisplayName"),
                    e.getString("Pathway"), e.getInt("Sequence"), e.getLong("WatchedTicks"), e.getCompound("EntityNBT")));
        }
        return result;
    }

    public static void addStareTicks(ItemStack stack, LivingEntity target, long ticksToAdd) {
        CompoundTag root = get(stack);
        ListTag list = root.contains("EntityWatch") ? root.getList("EntityWatch", Tag.TAG_COMPOUND) : new ListTag();

        for (int i = 0; i < list.size(); i++) {
            CompoundTag e = list.getCompound(i);
            if (e.hasUUID("SourceUUID") && e.getUUID("SourceUUID").equals(target.getUUID())) {
                long watched = Math.min(STARE_MAX_TICKS, e.getLong("WatchedTicks") + ticksToAdd);
                e.putLong("WatchedTicks", watched);
                list.set(i, e);
                root.put("EntityWatch", list);
                save(stack, root);
                return;
            }
        }

        CompoundTag entry = new CompoundTag();
        entry.putUUID("SourceUUID", target.getUUID());
        entry.putString("EntityType", EntityType.getKey(target.getType()).toString());
        entry.putString("DisplayName", target.getDisplayName().getString());
        entry.putString("Pathway", BeyonderData.getPathway(target));
        entry.putInt("Sequence", BeyonderData.getSequence(target));
        entry.putLong("WatchedTicks", ticksToAdd);
        CompoundTag nbt = new CompoundTag();
        target.saveWithoutId(nbt);
        entry.put("EntityNBT", nbt);
        list.add(entry);
        root.put("EntityWatch", list);
        save(stack, root);
    }

    public static void removeWatch(ItemStack stack, UUID sourceUUID) {
        CompoundTag root = get(stack);
        if (!root.contains("EntityWatch")) return;
        ListTag list = root.getList("EntityWatch", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            if (list.getCompound(i).getUUID("SourceUUID").equals(sourceUUID)) {
                list.remove(i);
                break;
            }
        }
        root.put("EntityWatch", list);
        save(stack, root);
    }

    public static List<CompoundTag> getRecordedItems(ItemStack stack) {
        CompoundTag root = get(stack);
        List<CompoundTag> result = new ArrayList<>();
        if (!root.contains("RecordedItems")) return result;
        ListTag list = root.getList("RecordedItems", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) result.add(list.getCompound(i));
        return result;
    }

    public static boolean addRecordedItem(ItemStack stack, CompoundTag itemEntry) {
        CompoundTag root = get(stack);
        ListTag list = root.contains("RecordedItems") ? root.getList("RecordedItems", Tag.TAG_COMPOUND) : new ListTag();
        if (list.size() >= MAX_RECORDED_ITEMS) return false;
        list.add(itemEntry);
        root.put("RecordedItems", list);
        save(stack, root);
        return true;
    }

    public static void removeRecordedItem(ItemStack stack, int index) {
        CompoundTag root = get(stack);
        if (!root.contains("RecordedItems")) return;
        ListTag list = root.getList("RecordedItems", Tag.TAG_COMPOUND);
        if (index < 0 || index >= list.size()) return;
        list.remove(index);
        root.put("RecordedItems", list);
        save(stack, root);
    }

    public static List<ActiveEntitySummon> getActiveEntitySummons(ItemStack stack) {
        CompoundTag root = get(stack);
        List<ActiveEntitySummon> result = new ArrayList<>();
        if (!root.contains("ActiveEntitySummons")) return result;
        ListTag list = root.getList("ActiveEntitySummons", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag e = list.getCompound(i);
            result.add(new ActiveEntitySummon(e.getUUID("SummonedUUID"), e.getString("DisplayName"),
                    e.getString("Pathway"), e.getInt("Sequence"), e.getLong("ExpiryTick")));
        }
        return result;
    }

    public static void addActiveEntitySummon(ItemStack stack, ActiveEntitySummon summon) {
        CompoundTag root = get(stack);
        ListTag list = root.contains("ActiveEntitySummons") ? root.getList("ActiveEntitySummons", Tag.TAG_COMPOUND) : new ListTag();
        CompoundTag entry = new CompoundTag();
        entry.putUUID("SummonedUUID", summon.summonedUUID());
        entry.putString("DisplayName", summon.displayName());
        entry.putString("Pathway", summon.pathway());
        entry.putInt("Sequence", summon.sequence());
        entry.putLong("ExpiryTick", summon.expiryTick());
        list.add(entry);
        root.put("ActiveEntitySummons", list);
        save(stack, root);
    }

    public static void removeActiveEntitySummon(ItemStack stack, UUID summonedUUID) {
        CompoundTag root = get(stack);
        if (!root.contains("ActiveEntitySummons")) return;
        ListTag list = root.getList("ActiveEntitySummons", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            if (list.getCompound(i).getUUID("SummonedUUID").equals(summonedUUID)) {
                list.remove(i);
                break;
            }
        }
        root.put("ActiveEntitySummons", list);
        save(stack, root);
    }

    public static List<ActiveItemSummon> getActiveItemSummons(ItemStack stack) {
        CompoundTag root = get(stack);
        List<ActiveItemSummon> result = new ArrayList<>();
        if (!root.contains("ActiveItemSummons")) return result;
        ListTag list = root.getList("ActiveItemSummons", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag e = list.getCompound(i);
            result.add(new ActiveItemSummon(e.getUUID("TrackingId"), e.getString("DisplayName"), e.getLong("ExpiryTick")));
        }
        return result;
    }

    public static void addActiveItemSummon(ItemStack stack, ActiveItemSummon summon) {
        CompoundTag root = get(stack);
        ListTag list = root.contains("ActiveItemSummons") ? root.getList("ActiveItemSummons", Tag.TAG_COMPOUND) : new ListTag();
        CompoundTag entry = new CompoundTag();
        entry.putUUID("TrackingId", summon.trackingId());
        entry.putString("DisplayName", summon.displayName());
        entry.putLong("ExpiryTick", summon.expiryTick());
        list.add(entry);
        root.put("ActiveItemSummons", list);
        save(stack, root);
    }

    public static void removeActiveItemSummon(ItemStack stack, UUID trackingId) {
        CompoundTag root = get(stack);
        if (!root.contains("ActiveItemSummons")) return;
        ListTag list = root.getList("ActiveItemSummons", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            if (list.getCompound(i).getUUID("TrackingId").equals(trackingId)) {
                list.remove(i);
                break;
            }
        }
        root.put("ActiveItemSummons", list);
        save(stack, root);
    }

    public static ItemStack reconstructItem(CompoundTag itemData, HolderLookup.Provider registries) {
        if (itemData.contains("ItemNBT")) {
            var result = ItemStack.CODEC.parse(registries.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), itemData.get("ItemNBT"));
            if (result.result().isPresent()) {
                return result.result().get();
            }
        }

        String itemId = itemData.getString("ItemId");
        var item = BuiltInRegistries.ITEM.getOptional(net.minecraft.resources.ResourceLocation.parse(itemId));
        return item.map(value -> new ItemStack(value, Math.max(1, itemData.getInt("Count")))).orElse(ItemStack.EMPTY);
    }
}

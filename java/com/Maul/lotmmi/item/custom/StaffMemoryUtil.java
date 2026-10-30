package com.Maul.lotmmi.item.custom;

import com.Maul.lotmmi.data.ModDataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class StaffMemoryUtil {
    public static final int MAX_WITNESS_COUNT = 10;

    public static final long STARE_UNLOCK_TICKS = 10L * 20L;
    public static final long STARE_MAX_TICKS = 60L * 20L;
    public static final int MAX_WATCHES = 30;

    private static final String KEY_ENTITY_WATCHES = "EntityWatches";

    public record EntityWatch(UUID sourceUUID, String entityType, CompoundTag entityNbt,
                              String displayName, String pathway, int sequence, long watchedTicks) {
        CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("SourceUUID", sourceUUID);
            tag.putString("EntityType", entityType);
            tag.put("EntityNbt", entityNbt.copy());
            tag.putString("DisplayName", displayName);
            tag.putString("Pathway", pathway);
            tag.putInt("Sequence", sequence);
            tag.putLong("WatchedTicks", watchedTicks);
            return tag;
        }

        static EntityWatch fromTag(CompoundTag tag) {
            return new EntityWatch(
                    tag.getUUID("SourceUUID"),
                    tag.getString("EntityType"),
                    tag.getCompound("EntityNbt"),
                    tag.getString("DisplayName"),
                    tag.getString("Pathway"),
                    tag.getInt("Sequence"),
                    tag.getLong("WatchedTicks"));
        }
    }

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

    private static List<CompoundTag> readCompoundList(ItemStack stack, String key) {
        CompoundTag root = get(stack);
        List<CompoundTag> out = new ArrayList<>();
        if (!root.contains(key)) return out;
        ListTag list = root.getList(key, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) out.add(list.getCompound(i));
        return out;
    }

    private static void writeCompoundList(ItemStack stack, String key, List<CompoundTag> entries) {
        CompoundTag root = get(stack);
        ListTag list = new ListTag();
        for (CompoundTag entry : entries) list.add(entry);
        root.put(key, list);
        save(stack, root);
    }

    public static List<EntityWatch> getEntityWatches(ItemStack stack) {
        List<EntityWatch> out = new ArrayList<>();
        for (CompoundTag tag : readCompoundList(stack, KEY_ENTITY_WATCHES)) {
            if (tag.hasUUID("SourceUUID")) out.add(EntityWatch.fromTag(tag));
        }
        return out;
    }

    public static void putWatch(ItemStack stack, EntityWatch watch) {
        List<CompoundTag> entries = readCompoundList(stack, KEY_ENTITY_WATCHES);
        entries.removeIf(tag -> tag.hasUUID("SourceUUID") && tag.getUUID("SourceUUID").equals(watch.sourceUUID()));
        entries.add(watch.toTag());
        writeCompoundList(stack, KEY_ENTITY_WATCHES, entries);
    }

}

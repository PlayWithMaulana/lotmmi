package com.Maul.lotmmi.item.custom;

import com.Maul.lotmmi.data.ModDataComponents;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class StaffMemoryUtil {

    public static final int MAX_WITNESS_COUNT = 10;

    // Entity summoning: how long the wielder must stare at an entity before it can be summoned,
    // and the stare time at which summons reach full duration (see StaffEntitySummonUtil).
    // NOTE: placeholder values - tune to taste.
    public static final long STARE_UNLOCK_TICKS = 10L * 20L;
    public static final long STARE_MAX_TICKS = 60L * 20L;
    public static final int MAX_ACTIVE_ENTITY_SUMMONS = 5;

    // Item summoning: recorded library size and simultaneous conjured items.
    public static final int MAX_RECORDED_ITEMS = 20;
    public static final int MAX_ACTIVE_ITEM_SUMMONS = 20;

    private static final String KEY_ENTITY_WATCHES = "EntityWatches";
    private static final String KEY_ACTIVE_ENTITIES = "ActiveEntitySummons";
    private static final String KEY_RECORDED_ITEMS = "RecordedItems";
    private static final String KEY_ACTIVE_ITEMS = "ActiveItemSummons";

    // ---------------------------------------------------------------------------------------------
    // Records
    // ---------------------------------------------------------------------------------------------

    /** An entity the wielder has stared at. Holds enough saved data to re-create it. */
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

    /** An entity currently summoned by the staff. */
    public record ActiveEntitySummon(UUID summonedUUID, String displayName, String pathway,
                                     int sequence, long expiryTick) {

        CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("SummonedUUID", summonedUUID);
            tag.putString("DisplayName", displayName);
            tag.putString("Pathway", pathway);
            tag.putInt("Sequence", sequence);
            tag.putLong("ExpiryTick", expiryTick);
            return tag;
        }

        static ActiveEntitySummon fromTag(CompoundTag tag) {
            return new ActiveEntitySummon(
                    tag.getUUID("SummonedUUID"),
                    tag.getString("DisplayName"),
                    tag.getString("Pathway"),
                    tag.getInt("Sequence"),
                    tag.getLong("ExpiryTick"));
        }
    }

    /** An item currently conjured by the staff. */
    public record ActiveItemSummon(UUID trackingId, String displayName, long expiryTick) {

        CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("TrackingId", trackingId);
            tag.putString("DisplayName", displayName);
            tag.putLong("ExpiryTick", expiryTick);
            return tag;
        }

        static ActiveItemSummon fromTag(CompoundTag tag) {
            return new ActiveItemSummon(
                    tag.getUUID("TrackingId"),
                    tag.getString("DisplayName"),
                    tag.getLong("ExpiryTick"));
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

    // ---------------------------------------------------------------------------------------------
    // Generic list helpers
    // ---------------------------------------------------------------------------------------------

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

    // ---------------------------------------------------------------------------------------------
    // Entity watches (stared-at entities)
    // ---------------------------------------------------------------------------------------------

    public static List<EntityWatch> getEntityWatches(ItemStack stack) {
        List<EntityWatch> out = new ArrayList<>();
        for (CompoundTag tag : readCompoundList(stack, KEY_ENTITY_WATCHES)) {
            if (tag.hasUUID("SourceUUID")) out.add(EntityWatch.fromTag(tag));
        }
        return out;
    }

    /** Adds the watch, or replaces the existing one with the same source UUID. */
    public static void putWatch(ItemStack stack, EntityWatch watch) {
        List<CompoundTag> entries = readCompoundList(stack, KEY_ENTITY_WATCHES);
        entries.removeIf(tag -> tag.hasUUID("SourceUUID") && tag.getUUID("SourceUUID").equals(watch.sourceUUID()));
        entries.add(watch.toTag());
        writeCompoundList(stack, KEY_ENTITY_WATCHES, entries);
    }

    public static void removeWatch(ItemStack stack, UUID sourceUUID) {
        List<CompoundTag> entries = readCompoundList(stack, KEY_ENTITY_WATCHES);
        if (entries.removeIf(tag -> tag.hasUUID("SourceUUID") && tag.getUUID("SourceUUID").equals(sourceUUID))) {
            writeCompoundList(stack, KEY_ENTITY_WATCHES, entries);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Active entity summons
    // ---------------------------------------------------------------------------------------------

    public static List<ActiveEntitySummon> getActiveEntitySummons(ItemStack stack) {
        List<ActiveEntitySummon> out = new ArrayList<>();
        for (CompoundTag tag : readCompoundList(stack, KEY_ACTIVE_ENTITIES)) {
            if (tag.hasUUID("SummonedUUID")) out.add(ActiveEntitySummon.fromTag(tag));
        }
        return out;
    }

    public static void addActiveEntitySummon(ItemStack stack, ActiveEntitySummon summon) {
        List<CompoundTag> entries = readCompoundList(stack, KEY_ACTIVE_ENTITIES);
        entries.add(summon.toTag());
        writeCompoundList(stack, KEY_ACTIVE_ENTITIES, entries);
    }

    public static void removeActiveEntitySummon(ItemStack stack, UUID summonedUUID) {
        List<CompoundTag> entries = readCompoundList(stack, KEY_ACTIVE_ENTITIES);
        if (entries.removeIf(tag -> tag.hasUUID("SummonedUUID") && tag.getUUID("SummonedUUID").equals(summonedUUID))) {
            writeCompoundList(stack, KEY_ACTIVE_ENTITIES, entries);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Recorded items (library)
    // ---------------------------------------------------------------------------------------------

    public static List<CompoundTag> getRecordedItems(ItemStack stack) {
        return readCompoundList(stack, KEY_RECORDED_ITEMS);
    }

    public static void addRecordedItem(ItemStack stack, CompoundTag entry) {
        List<CompoundTag> entries = readCompoundList(stack, KEY_RECORDED_ITEMS);
        if (entries.size() >= MAX_RECORDED_ITEMS) return;
        entries.add(entry.copy());
        writeCompoundList(stack, KEY_RECORDED_ITEMS, entries);
    }

    public static void removeRecordedItem(ItemStack stack, int index) {
        List<CompoundTag> entries = readCompoundList(stack, KEY_RECORDED_ITEMS);
        if (index < 0 || index >= entries.size()) return;
        entries.remove(index);
        writeCompoundList(stack, KEY_RECORDED_ITEMS, entries);
    }

    /**
     * Rebuilds an item from a library entry written by StaffItemSummonUtil.processCapture
     * (keys: ItemId, DisplayName, Count, ItemNBT). Prefers the full encoded stack (keeps components),
     * and falls back to a plain stack of the recorded item id.
     */
    public static ItemStack reconstructItem(CompoundTag entry, RegistryAccess registries) {
        if (entry.contains("ItemNBT")) {
            Tag encoded = entry.get("ItemNBT");
            Optional<ItemStack> parsed = ItemStack.CODEC
                    .parse(registries.createSerializationContext(NbtOps.INSTANCE), encoded)
                    .result();
            if (parsed.isPresent() && !parsed.get().isEmpty()) return parsed.get();
        }

        ResourceLocation id = ResourceLocation.tryParse(entry.getString("ItemId"));
        if (id == null) return ItemStack.EMPTY;
        Item item = BuiltInRegistries.ITEM.get(id);
        if (item == Items.AIR) return ItemStack.EMPTY;
        return new ItemStack(item, Math.max(1, entry.getInt("Count")));
    }

    // ---------------------------------------------------------------------------------------------
    // Active item summons
    // ---------------------------------------------------------------------------------------------

    public static List<ActiveItemSummon> getActiveItemSummons(ItemStack stack) {
        List<ActiveItemSummon> out = new ArrayList<>();
        for (CompoundTag tag : readCompoundList(stack, KEY_ACTIVE_ITEMS)) {
            if (tag.hasUUID("TrackingId")) out.add(ActiveItemSummon.fromTag(tag));
        }
        return out;
    }

    public static void addActiveItemSummon(ItemStack stack, ActiveItemSummon summon) {
        List<CompoundTag> entries = readCompoundList(stack, KEY_ACTIVE_ITEMS);
        entries.add(summon.toTag());
        writeCompoundList(stack, KEY_ACTIVE_ITEMS, entries);
    }

    public static void removeActiveItemSummon(ItemStack stack, UUID trackingId) {
        List<CompoundTag> entries = readCompoundList(stack, KEY_ACTIVE_ITEMS);
        if (entries.removeIf(tag -> tag.hasUUID("TrackingId") && tag.getUUID("TrackingId").equals(trackingId))) {
            writeCompoundList(stack, KEY_ACTIVE_ITEMS, entries);
        }
    }
}

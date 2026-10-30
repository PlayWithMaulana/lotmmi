package com.Maul.lotmmi.item.custom;

import com.Maul.lotmmi.data.ModDataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ScepterPrayerUtil {

    private static final String KEY_PRAYERS = "Prayers";
    public static final int MAX_PRAYERS = 10;

    public record Prayer(UUID id, String name, String dimension, double x, double y, double z, long tick) {

        CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("Id", id);
            tag.putString("Name", name);
            tag.putString("Dimension", dimension);
            tag.putDouble("X", x);
            tag.putDouble("Y", y);
            tag.putDouble("Z", z);
            tag.putLong("Tick", tick);
            return tag;
        }

        static Prayer fromTag(CompoundTag tag) {
            return new Prayer(
                    tag.getUUID("Id"),
                    tag.getString("Name"),
                    tag.getString("Dimension"),
                    tag.getDouble("X"),
                    tag.getDouble("Y"),
                    tag.getDouble("Z"),
                    tag.getLong("Tick"));
        }
    }

    private static CompoundTag get(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.SCEPTER_PRAYERS.get(), new CompoundTag()).copy();
    }

    private static void save(ItemStack stack, CompoundTag tag) {
        stack.set(ModDataComponents.SCEPTER_PRAYERS.get(), tag);
    }

    public static List<Prayer> getPrayers(ItemStack stack) {
        CompoundTag root = get(stack);
        List<Prayer> out = new ArrayList<>();
        if (!root.contains(KEY_PRAYERS)) return out;

        ListTag list = root.getList(KEY_PRAYERS, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            out.add(Prayer.fromTag(list.getCompound(i)));
        }
        return out;
    }

    private static void setPrayers(ItemStack stack, List<Prayer> prayers) {
        CompoundTag root = get(stack);
        ListTag list = new ListTag();
        for (Prayer prayer : prayers) {
            list.add(prayer.toTag());
        }
        root.put(KEY_PRAYERS, list);
        save(stack, root);
    }

    public static void recordPrayer(ItemStack stack, UUID id, String name, String dimension,
                                     double x, double y, double z, long tick) {
        List<Prayer> prayers = getPrayers(stack);
        prayers.removeIf(p -> p.id().equals(id));
        prayers.add(0, new Prayer(id, name, dimension, x, y, z, tick));
        while (prayers.size() > MAX_PRAYERS) {
            prayers.remove(prayers.size() - 1);
        }
        setPrayers(stack, prayers);
    }

    public static Prayer getPrayerById(ItemStack stack, UUID id) {
        for (Prayer prayer : getPrayers(stack)) {
            if (prayer.id().equals(id)) return prayer;
        }
        return null;
    }

    public static void removePrayerById(ItemStack stack, UUID id) {
        List<Prayer> prayers = getPrayers(stack);
        if (prayers.removeIf(p -> p.id().equals(id))) {
            setPrayers(stack, prayers);
        }
    }

}

package com.Maul.lotmmi.item.custom;

import com.Maul.lotmmi.data.ModDataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class TeleportSlotUtil {
    public static final int MAX_SLOTS = 5;

    public record Slot(ResourceKey<Level> dimension, double x, double y, double z) {}

    public static List<Slot> getSlots(ItemStack stack) {
        String raw = stack.getOrDefault(ModDataComponents.STAFF_TELEPORT_SLOTS.get(), "");
        List<Slot> slots = new ArrayList<>();
        if (raw.isEmpty()) return slots;

        for (String entry : raw.split(";", -1)) {
            if (entry.isEmpty() || entry.equals("EMPTY")) {
                slots.add(null);
                continue;
            }

            String[] parts = entry.split("\\|", -1);
            if (parts.length < 4) {
                slots.add(null);
                continue;
            }
            try {
                ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(parts[0]));
                slots.add(new Slot(dim, Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), Double.parseDouble(parts[3])));
            } catch (RuntimeException e) {
                slots.add(null);
            }
        }

        while (slots.size() > MAX_SLOTS) {
            slots.remove(slots.size() - 1);
        }
        while (!slots.isEmpty() && slots.get(slots.size() - 1) == null) {
            slots.remove(slots.size() - 1);
        }
        return slots;
    }

    private static void saveSlots(ItemStack stack, List<Slot> slots) {
        StringBuilder sb = new StringBuilder();
        for (Slot slot : slots) {
            if (slot == null) {
                sb.append("EMPTY;");
            } else {
                sb.append(slot.dimension().location()).append("|")
                        .append(slot.x()).append("|").append(slot.y()).append("|").append(slot.z()).append(";");
            }
        }
        stack.set(ModDataComponents.STAFF_TELEPORT_SLOTS.get(), sb.toString());
    }

    public static void recordSlot(ItemStack stack, int index, ResourceKey<Level> dimension, Vec3 pos) {
        if (index < 0 || index >= MAX_SLOTS) return;
        List<Slot> slots = new ArrayList<>(getSlots(stack));
        while (slots.size() <= index) slots.add(null);

        slots.set(index, new Slot(dimension, pos.x, pos.y, pos.z));
        saveSlots(stack, slots);
    }

    public static void clearSlot(ItemStack stack, int index) {
        List<Slot> slots = new ArrayList<>(getSlots(stack));
        if (index < 0 || index >= slots.size()) return;

        slots.set(index, null);
        while (!slots.isEmpty() && slots.get(slots.size() - 1) == null) {
            slots.remove(slots.size() - 1);
        }
        saveSlots(stack, slots);
    }
}

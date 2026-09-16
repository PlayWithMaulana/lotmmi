package com.Maul.lotmmi.item.custom;

import com.Maul.lotmmi.data.ModDataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class TeleportSlotUtil {

    public static final int MAX_SLOTS = 3;

    public record Slot(ResourceKey<Level> dimension, double x, double y, double z, String name) {}

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
            if (parts.length != 4 && parts.length != 5) {
                slots.add(null);
                continue;
            }
            ResourceKey<Level> dim = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, ResourceLocation.parse(parts[0]));
            String name = parts.length == 5 ? parts[4] : "";
            slots.add(new Slot(dim, Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), Double.parseDouble(parts[3]), name));
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
                        .append(slot.x()).append("|").append(slot.y()).append("|").append(slot.z())
                        .append("|").append(slot.name() == null ? "" : slot.name()).append(";");
            }
        }
        stack.set(ModDataComponents.STAFF_TELEPORT_SLOTS.get(), sb.toString());
    }

    public static void recordSlot(ItemStack stack, int index, ResourceKey<Level> dimension, Vec3 pos) {
        List<Slot> slots = new ArrayList<>(getSlots(stack));
        while (slots.size() <= index) slots.add(null);

        String existingName = slots.get(index) != null ? slots.get(index).name() : "";
        slots.set(index, new Slot(dimension, pos.x, pos.y, pos.z, existingName));
        saveSlots(stack, slots);
    }

    public static void renameSlot(ItemStack stack, int index, String name) {
        List<Slot> slots = new ArrayList<>(getSlots(stack));
        if (index < 0 || index >= slots.size() || slots.get(index) == null) return;
        Slot current = slots.get(index);
        slots.set(index, new Slot(current.dimension(), current.x(), current.y(), current.z(), name));
        saveSlots(stack, slots);
    }
}

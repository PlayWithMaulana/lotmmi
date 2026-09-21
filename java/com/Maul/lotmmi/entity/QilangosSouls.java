package com.Maul.lotmmi.entity;

import com.Maul.lotmmi.item.ModItems;
import com.Maul.lotmmi.item.custom.CreepingHungerItem;
import de.jakob.lotm.LOTMCraft;
import de.jakob.lotm.beyonders.abilities.core.Ability;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class QilangosSouls {

    public record Def(String pathway, int sequence, String label, List<String> abilityIds) {}

    public record SoulAbility(Ability ability, String pathway, int sequence) {}

    public static final List<Def> DEFS = List.of(
            new Def("justiciar", 7, "Interrogator", List.of("illusionary_torture_devices_ability")),
            new Def("sun", 5, "Priest of Light", List.of("purification_halo_ability", "light_of_holiness_ability")),
            new Def("darkness", 7, "Nightmare", List.of("nightmare_ability")),
            new Def("fool", 6, "Faceless", List.of("shapeshifting_ability")),
            new Def("visionary", 7, "Psychiatrist", List.of("frenzy_ability", "psychological_cue_ability", "awe_ability"))
    );

    private static List<SoulAbility> resolved;

    private QilangosSouls() {}

    public static synchronized List<SoulAbility> resolve() {
        if (resolved != null) return resolved;

        List<SoulAbility> list = new ArrayList<>();
        for (Def def : DEFS) {
            for (String id : def.abilityIds()) {
                Ability ability = LOTMCraft.abilityHandler.getById(id);
                if (ability != null) list.add(new SoulAbility(ability, def.pathway(), def.sequence()));
            }
        }
        resolved = List.copyOf(list);
        return resolved;
    }

    public static ItemStack createCreepingHunger() {
        ItemStack stack = new ItemStack(ModItems.CREEPING_HUNGER.get());

        List<CreepingHungerItem.SoulSlot> souls = new ArrayList<>();
        for (Def def : DEFS) {
            souls.add(new CreepingHungerItem.SoulSlot(def.pathway(), def.sequence(), def.label(), "",
                    new ArrayList<>(def.abilityIds())));
        }
        CreepingHungerItem.setSouls(stack, souls);
        CreepingHungerItem.recomputeArtifactData(stack);
        return stack;
    }
}

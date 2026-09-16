package com.Maul.lotmmi.util;

import de.jakob.lotm.LOTMCraft;
import de.jakob.lotm.beyonders.abilities.core.Ability;
import de.jakob.lotm.beyonders.abilities.core.AbilityHandler;

import java.lang.reflect.Field;
import java.util.HashSet;

public class AbilityRegistrationHelper {

    private static Field abilitiesField;

    public static void registerAbility(Ability ability) {
        try {
            if (abilitiesField == null) {
                abilitiesField = AbilityHandler.class.getDeclaredField("abilities");
                abilitiesField.setAccessible(true);
            }

            @SuppressWarnings("unchecked")
            HashSet<Ability> abilities = (HashSet<Ability>) abilitiesField.get(LOTMCraft.abilityHandler);

            abilities.removeIf(a -> a.getId().equals(ability.getId()));
            abilities.add(ability);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to register custom ability '" + ability.getId() + "' into LotMCraft's AbilityHandler. " +
                    "This usually means LotMCraft changed its internal AbilityHandler field name/structure.", e);
        }
    }
}

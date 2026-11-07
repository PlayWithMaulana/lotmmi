package com.Maul.lotmmi.gui;

import de.jakob.lotm.beyonders.abilities.core.Ability;

final class AbilityStats {

    private AbilityStats() {
    }

    static int cooldown(Ability ability, int sequence) {
        try {
            return ability.getCooldown(Math.max(0, sequence));
        } catch (RuntimeException | LinkageError e) {
            return 0;
        }
    }

    static float spirituality(Ability ability, int sequence) {
        try {
            return ability.spiritualityCost(Math.max(0, sequence));
        } catch (RuntimeException | LinkageError e) {
            return 0.0F;
        }
    }
}

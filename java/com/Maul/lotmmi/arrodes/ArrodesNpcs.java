package com.Maul.lotmmi.arrodes;

import com.Maul.lotmmi.LotmMysticalItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

final class ArrodesNpcs {

    private static final int[] POUNDS = {1200, 800, 600, 400, 200, 100, 50, 25, 10, 5};
    private static final int[] SOLI = {120, 80, 60, 40, 20, 10, 5, 2, 1, 1};
    private static final int KING_POUNDS = 1000;
    private static final int KING_SOLI = 100;

    private static final String EXTRA = "lotmextra";

    enum Spawn { UNIQUE, AVATAR, COAST }

    record Npc(String name, String namespace, String path, String pathway, int sequence, boolean hostile,
               boolean king, boolean characteristic, Spawn spawn, String carries, String sequenceNote,
               String... aliases) {
    }

    private static final List<Npc> NPCS = List.of(
            new Npc("Gehrman Sparrow", EXTRA, "gehrman_sparrow", "fool", 1, false, true, true, Spawn.UNIQUE,
                    "Gehrman Sparrow carries the Awakened Creeping Hunger, and drops it when slain.", null,
                    "gehrman sparrow", "gehrman", "gerhman", "sparrow"),
            new Npc("Merlin Hermes", EXTRA, "merlin_hermes", "fool", 2, false, false, true, Spawn.UNIQUE, null, null,
                    "merlin hermes", "merlin", "hermes"),
            new Npc("Sherlock Moriarty", EXTRA, "sherlock_moriarty", "fool", 7, false, false, true, Spawn.UNIQUE, null, null,
                    "sherlock moriarty", "sherlock", "moriarty"),
            new Npc("Audrey Hall", EXTRA, "audrey_hall", "visionary", 3, false, false, true, Spawn.UNIQUE, null, null,
                    "audrey hall", "audrey"),
            new Npc("Cattleya", EXTRA, "cattleya", "hermit", 3, false, false, false, Spawn.UNIQUE, null, null,
                    "cattleya", "catleya"),
            new Npc("Danitz Dubois", EXTRA, "danitz_dubois", "red_priest", 6, false, false, true, Spawn.UNIQUE, null, null,
                    "danitz dubois", "danitz", "dubois"),
            new Npc("Derrick Berg", EXTRA, "derrick_berg", "sun", 4, false, false, true, Spawn.UNIQUE, null, null,
                    "derrick berg", "derrick"),
            new Npc("Emlyn White", EXTRA, "emlyn_white", "moon", 5, false, false, false, Spawn.UNIQUE, null, null,
                    "emlyn white", "emlyn"),
            new Npc("Fors Wall", EXTRA, "fors_wall", "door", 4, false, false, true, Spawn.UNIQUE, null, null,
                    "fors wall", "fors"),
            new Npc("Leonard Mitchell", EXTRA, "leonard_mitchell", "darkness", 4, false, false, true, Spawn.UNIQUE, null, null,
                    "leonard mitchell", "leonard", "mitchell"),
            new Npc("Ouroboros", EXTRA, "ouroboros", "wheel_of_fortune", 1, false, true, true, Spawn.UNIQUE, null, null,
                    "ouroboros"),
            new Npc("Sauron Einhorn Medici", EXTRA, "sauron_einhorn_medici", "red_priest", 1, true, true, true, Spawn.UNIQUE, null, null,
                    "sauron einhorn medici", "sauron", "einhorn", "medici"),
            new Npc("Amon's Avatar", EXTRA, "amon_avatar", "error", -1, false, false, true, Spawn.AVATAR, null,
                    "Its Sequence is not fixed. Most are Sequence 4 or 3. The mightiest of them are protected against divination and prophecy, and I cannot pry upon those.",
                    "amon avatar"),
            new Npc("Qilangos", LotmMysticalItems.MOD_ID, "qilangos", "tyrant", 6, true, false, false, Spawn.COAST,
                    "Qilangos carries a Creeping Hunger, and drops it when slain.", null,
                    "qilangos")
    );

    private ArrodesNpcs() {
    }

    static Npc find(List<String> tokens) {
        Npc best = null;
        int bestLength = 0;
        for (Npc npc : NPCS) {
            for (String alias : npc.aliases()) {
                boolean ok = true;
                for (String part : alias.split(" ")) {
                    boolean hit = false;
                    for (String token : tokens) {
                        if (ArrodesText.fuzzyEquals(token, part)) {
                            hit = true;
                            break;
                        }
                    }
                    if (!hit) {
                        ok = false;
                        break;
                    }
                }
                if (ok && alias.length() > bestLength) {
                    best = npc;
                    bestLength = alias.length();
                }
            }
        }
        return best;
    }

    static boolean isProtected(Npc npc) {
        return ArrodesKnowledge.beingProtected(npc.sequence());
    }

    static boolean exists(Npc npc) {
        return BuiltInRegistries.ENTITY_TYPE.containsKey(ResourceLocation.fromNamespaceAndPath(npc.namespace(), npc.path()));
    }

    static List<String> describe(Npc npc, boolean owner) {
        List<String> lines = new ArrayList<>();
        boolean veiled = npc.sequence() == 3;
        String pathway = veiled ? "" : ArrodesKnowledge.pathwayName(npc.pathway());
        if (veiled) {
            lines.add(npc.name() + " is a Beyonder whose path is veiled from me.");
        } else if (npc.sequence() < 0) {
            lines.add(npc.name() + " walks the " + pathway + " pathway.");
            if (npc.sequenceNote() != null) lines.add(npc.sequenceNote());
        } else {
            String sequenceName = ArrodesKnowledge.sequenceName(npc.pathway(), npc.sequence());
            lines.add(npc.name() + " walks the " + pathway + " pathway, at Sequence " + npc.sequence()
                    + (sequenceName == null ? "" : ", " + sequenceName) + ".");
        }
        if (npc.carries() != null) lines.add(npc.carries());
        lines.add(npc.hostile()
                ? npc.name() + " attacks on sight."
                : npc.name() + " does not hunt the living, and is dangerous only when provoked.");

        switch (npc.spawn()) {
            case UNIQUE -> lines.add(npc.name() + " roams the Overworld alone, below Y 100, and is exceedingly rare.");
            case AVATAR -> lines.add("Avatars roam the Overworld below Y 100. They are rare, and unlike the named ones, several may appear close together.");
            case COAST -> lines.add(npc.name() + " stalks the Overworld, a little more often along oceans and beaches than inland. It is exceedingly rare, and no two dwell within 128 blocks of each other.");
        }
        if (npc.characteristic()) {
            lines.add("Slain, " + npc.name() + " may leave a Beyonder Characteristic" + (veiled ? "." : " of the " + pathway + " pathway."));
        }
        if (owner && npc.spawn() != Spawn.COAST) {
            if (npc.sequence() >= 0) {
                int pounds = npc.king() ? KING_POUNDS : POUNDS[npc.sequence()];
                int soli = npc.king() ? KING_SOLI : SOLI[npc.sequence()];
                lines.add("A gift for my Master: slain, " + npc.name() + " drops " + pounds + " pounds and " + soli + " soli.");
            } else {
                lines.add("A gift for my Master: what an avatar drops in pounds and soli grows with the Sequence it was born to.");
            }
        }
        return lines;
    }
}

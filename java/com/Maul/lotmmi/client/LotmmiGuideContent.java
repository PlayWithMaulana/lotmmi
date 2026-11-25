package com.Maul.lotmmi.client;

import java.util.List;

/** What the lotmmi chapter of LOTMCraft's encyclopedia says. Short on purpose: one small paragraph for each thing. */
final class LotmmiGuideContent {

    record Item(String icon, String title, String tag, String text) {
    }

    record Section(String title, int color, List<Item> items) {
    }

    private static final int ARTIFACT = 0xFF7B52AB;
    private static final int BEING = 0xFF4F8F6B;
    private static final int CURIOSITY = 0xFFB0894A;

    static final String TITLE = "LOTM Mystical Items";
    static final String BLURB = "Hand-built sealed artifacts, beings and curiosities added to LOTMCraft.";

    static final List<Section> SECTIONS = List.of(
            new Section("Sealed Artifacts", ARTIFACT, List.of(
                    new Item("lotmmi:arrodes", "Arrodes", "Magic mirror",
                            "Ask it about pathways, formulas, ingredients, creatures, Beyonders and the other artifacts. Sefirah Castle owners are served first, and strangers had best be polite."),
                    new Item("minecraft:glass_pane", "Calling through glass", "Ritual",
                            "Sneak and right-click glass while holding a book with Arrodes' honorific name, and it will answer until you walk away. The book is spent. Ritual books found in chests may carry the name."),
                    new Item("lotmmi:creeping_hunger", "Creeping Hunger", "Hanged Man, Sequence 5",
                            "A glove that grazes the souls of Beyonders and lends you their abilities. It must be fed once a day, or it feeds on you."),
                    new Item("lotmmi:awakened_creeping_hunger", "Awakened Creeping Hunger", "Awakened",
                            "Holds seven souls and lets you pick the ability you keep. It prays every few minutes, and shrinks away from mushrooms."),
                    new Item("lotmmi:staff_of_the_stars", "Staff of the Stars", "Planeswalker relic",
                            "Remembers five places to return to, and replays abilities it has watched. Every misfire builds Panic, and Panic makes things strange."),
                    new Item("lotmmi:sea_god_scepter", "Sea God Scepter", "Tyrant, Sequence 3",
                            "Storm abilities, followers who pray to you, and a Gaze upon the place of a prayer. Only a demigod holds it unharmed."),
                    new Item("lotmmi:sea_god_charm", "Sea God Charm", "One use",
                            "A small charm bound from the Scepter: Sea Affinity, Electric Shock, Gale or Sea Whisper. It fades over time.")
            )),
            new Section("Beings", BEING, List.of(
                    new Item("lotmmi:qilangos_spawn_egg", "Qilangos", "Pirate",
                            "A rare Overworld wanderer that can take the shape of a player, and drops a Creeping Hunger when it falls.")
            )),
            new Section("Curiosities", CURIOSITY, List.of(
                    new Item("lotmcraft:wheel_of_fortune_uniqueness", "Die of Probability", "Wheel of Fortune",
                            "The die now shows a face that changes every so often. High faces bring luck, low faces bring misfortune, and the lowest can be fatal.")
            ))
    );

    private LotmmiGuideContent() {
    }
}

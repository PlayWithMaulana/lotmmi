package com.Maul.lotmmi.arrodes;

import java.util.ArrayList;
import java.util.List;

final class ArrodesLore {

    enum Topic { APOTHEOSIS, WORLD, SPIRIT_WORLD, BEYONDER, PATHWAY, SEQUENCE, RANKS, POTION, DIGESTION, SANITY, SPIRITUALITY, CHARACTERISTIC, UNIQUENESS, ARTIFACT, CREATURES, SEFIROT }

    private record Entry(Topic topic, String[] strong, String[] weak, List<String> lines) {
    }

    private static final String[] ASK = {
            "what is", "what are", "what s", "whats", "explain", "describe", "tell me about", "teach me", "define",
            "meaning of", "how does", "how do", "do you know", "what do you know", "know about", "learn about", "about", "lore"
    };

    private static final List<Entry> ENTRIES = List.of(
            new Entry(Topic.APOTHEOSIS,
                    new String[]{"apotheosis", "godhood", "become a god", "become god", "becoming a god", "become a deity", "become deity",
                            "become a true god", "ascend to godhood", "ascend to a god", "reach godhood", "attain godhood", "path to godhood",
                            "advance to godhood", "reach sequence 0", "reach seq 0", "become sequence 0", "become seq 0", "advance to sequence 0",
                            "advance to seq 0", "ascend to sequence 0", "ascend to seq 0", "get to sequence 0", "get to seq 0",
                            "ascension ritual", "ascend ritual", "how to ascend", "how do i ascend", "how can i ascend"},
                    new String[]{"ascend", "ascension", "deify", "deification"},
                    List.of()),
            new Entry(Topic.SPIRIT_WORLD,
                    new String[]{"spirit world", "spirit realm", "spiritual world", "astral world", "astral realm"},
                    new String[]{"astral"},
                    List.of(
                            "The Spirit World is the unseen realm that lies beneath the material world, Great Master.",
                            "Spirits, ghosts and many creatures of mystery dwell in it. A Beyonder with the right power can enter it, cross it, or sense what moves within it.",
                            "Those who linger in it unprepared may be warped by it. Its regions are many, and some creatures never leave them.")),
            new Entry(Topic.CHARACTERISTIC,
                    new String[]{"beyonder characteristic", "beyonder characteristics"},
                    new String[]{"characteristic", "characteristics"},
                    List.of(
                            "A Beyonder Characteristic is the essence of a Beyonder's power at one Sequence, left behind when a strong Beyonder or a mystical creature falls.",
                            "It is never truly destroyed, only passed on from one holder to the next.",
                            "Handle one with care, Great Master: the madness of its former owner rests inside it. Hold one in your other hand and I will tell you what it is.")),
            new Entry(Topic.UNIQUENESS,
                    new String[]{},
                    new String[]{"uniqueness", "uniquenesses"},
                    List.of(
                            "A Uniqueness is the singular essence of a pathway. Each pathway has but one, and the greatest power of that pathway rests upon it.",
                            "Whoever holds it is hunted by everyone with ambition. Ask me where one lies, Great Master, and I will tell you, if it is not hidden from me.")),
            new Entry(Topic.ARTIFACT,
                    new String[]{"sealed artifact", "sealed artifacts", "mystical item", "mystical items", "mystical object", "mystical objects"},
                    new String[]{"artifact", "artifacts"},
                    List.of(
                            "A Sealed Artifact is an object that holds a power taken from a Beyonder and bound into it.",
                            "Their power is great, but each carries a price, a curse, or a madness. Those who use one without understanding it often pay dearly.",
                            "Even I am counted among them, Great Master.")),
            new Entry(Topic.SEFIROT,
                    new String[]{"sefirot", "sefirah", "sefirah castle", "sephirot", "sephirah", "chaos sea", "brood hive"},
                    new String[]{"castle"},
                    List.of(
                            "The Sefirot are great domains that lie beyond the ordinary world. A Beyonder whose pathway aligns with one may claim it.",
                            "Within a Sefirot the eyes of the world cannot reach you. The Sefirah Castle is one such place, and its ruler is my Master. The Brood Hive is another, a realm of procreation.",
                            "The Chaos Sea, from which I came, is yet another.")),
            new Entry(Topic.DIGESTION,
                    new String[]{"acting method", "acting principle", "acting principles"},
                    new String[]{"digestion", "digest", "digested", "digesting", "acting"},
                    List.of(
                            "Digestion is how a Beyonder makes a potion's power their own. A potion is not mastered by drinking it, but by acting its part.",
                            "Do what the role would do, and live the deeds that fit the Sequence, and the potion is slowly digested.",
                            "Until it is digested, climbing higher invites ruin.")),
            new Entry(Topic.SANITY,
                    new String[]{"loss of control", "lose control", "losing control", "go mad"},
                    new String[]{"sanity", "madness", "insanity", "insane"},
                    List.of(
                            "Every Beyonder carries madness within their power. Sanity is the leash that holds it.",
                            "Fear, attacks upon the mind, and the Spirit World all gnaw at that leash. When it snaps, the Beyonder loses control and becomes a monster.",
                            "Digesting a potion well keeps the madness at bay.")),
            new Entry(Topic.SPIRITUALITY,
                    new String[]{},
                    new String[]{"spirituality"},
                    List.of(
                            "Spirituality is the energy of a Beyonder. Every ability spends some of it.",
                            "It returns with time, and a Beyonder who cogitates can replenish it faster. A Beyonder drained of it is left almost helpless.")),
            new Entry(Topic.RANKS,
                    new String[]{"low sequence", "low sequences", "lower sequences", "mid sequence", "mid sequences", "middle sequences",
                            "high sequence", "high sequences", "higher sequences"},
                    new String[]{"demigod", "demigods", "saint", "saints", "angels", "ranks", "tiers"},
                    List.of(
                            "Sequences 9 to 7 are the lower ranks, where every Beyonder begins. The abilities are few, and the body is only a little beyond mortal.",
                            "Sequences 6 and 5 are the middle ranks. Here a Beyonder grows into a true power among mortals.",
                            "Sequence 4 is a demigod, and Sequence 3 a saint. Their strength is far beyond what lesser ones can face, so I hide their names to protect them.",
                            "Beyond the saints stand the angels, and beyond them the gods. Those of Sequence 2 and above are protected against divination and prophecy, and I cannot pry upon them.")),
            new Entry(Topic.POTION,
                    new String[]{"main ingredient", "supplementary ingredient", "supplementary ingredients"},
                    new String[]{"potion", "potions", "ingredients"},
                    List.of(
                            "A potion is how a Beyonder climbs. Drink the potion of the next Sequence of your pathway, and its power becomes yours.",
                            "Each potion calls for one main ingredient and several supplementary ones, brewed to a formula. Ask me the formula of any Sequence from 9 to 4, Great Master, and where each ingredient is found.",
                            "But drinking is only the beginning. The power must then be digested.")),
            new Entry(Topic.CREATURES,
                    new String[]{"mystical creature", "mystical creatures", "mythical creature", "mythical creatures", "magical beast", "magical beasts"},
                    new String[]{},
                    List.of(
                            "Mystical creatures are beasts and spirits touched by the power of a pathway. Many roam the Spirit World and the strange corners of this one.",
                            "Their bodies yield potion ingredients, and the strongest may leave a Beyonder Characteristic when slain.",
                            "Name one to me, Great Master, and I will tell you where it dwells and how often it is seen.")),
            new Entry(Topic.SEQUENCE,
                    new String[]{},
                    new String[]{"sequence", "sequences"},
                    List.of(
                            "A Sequence is one rung on the ladder of a pathway. Sequence 9 is the lowest and Sequence 0 the highest, so the smaller the number, the greater the power.",
                            "To climb a rung, a Beyonder drinks the potion of the next Sequence and digests it.",
                            "Ask me the name or the abilities of any Sequence, Great Master, or the formula of any Sequence from 9 to 4.")),
            new Entry(Topic.PATHWAY,
                    new String[]{},
                    new String[]{"pathway", "pathways"},
                    List.of(
                            "A pathway is one ladder of ten Sequences, from 9 up to 0. Each has its own nature and its own potions.",
                            "A Beyonder who drinks the potion of one pathway cannot simply step onto another.")),
            new Entry(Topic.BEYONDER,
                    new String[]{},
                    new String[]{"beyonder", "beyonders"},
                    List.of(
                            "A Beyonder is a mortal who has drunk the potion of a pathway and survived it.",
                            "They carry abilities, spirituality, and a sanity they must guard. Most mortals never learn they exist.")),
            new Entry(Topic.WORLD,
                    new String[]{"this world", "the world of lotm", "lord of the mysteries", "lotm", "lotmcraft"},
                    new String[]{"world", "mysteries"},
                    List.of(
                            "Great Master, this is a world where Beyonders walk unseen among mortals, climbing the Sequences of their pathways.",
                            "Beneath the material world lies the Spirit World. Beyond both lie the Sefirot, domains where rulers such as you hide from the eyes of the world.",
                            "Ask me of any piece: the Spirit World, Beyonders, pathways, Sequences, potions, digestion, sanity, Spirituality, Characteristics, Uniquenesses, Sealed Artifacts, mystical creatures, or the Sefirot."))
    );

    private ArrodesLore() {
    }

    static Topic find(List<String> tokens, String normalized) {
        for (Entry entry : ENTRIES) {
            if (entry.strong().length > 0 && ArrodesText.hasAny(tokens, normalized, entry.strong())) return entry.topic();
        }
        if (!ArrodesText.hasAny(tokens, normalized, ASK)) return null;
        for (Entry entry : ENTRIES) {
            if (entry.weak().length > 0 && ArrodesText.hasAny(tokens, normalized, entry.weak())) return entry.topic();
        }
        return null;
    }

    static List<String> apotheosis(int stacks, int kills) {
        return List.of(
                "To become a God, Great Master, you must first stand at Sequence 1 of your pathway. The last step is not a potion. It is the Uniqueness of your pathway.",
                "A Sequence 1 Beyonder feels the Uniqueness of their own pathway from far away, and it grows stronger as they draw near. Ask me where it lies and I will tell you, if it is not hidden from me. Touch it and it becomes yours.",
                "Beware: a Uniqueness is deadly to any Beyonder who is not a Sequence 1 of its own pathway. Most are destroyed on contact, and even Sequence 2 and 1 take a terrible blow.",
                "Then you must be ready. Stack " + stacks + " Beyonder Characteristic" + (stacks == 1 ? "" : "s") + " of your pathway at your Sequence, or slay " + kills + " while the Uniqueness is yours.",
                "Stand in the Overworld, open your Introspect, and choose Apotheosis. The ritual lasts five minutes. Your abilities fall silent, the world trembles, and everyone in the Overworld hears your name. If you die or leave the world before it ends, it is broken and the Uniqueness is lost.",
                "If you endure, you become Sequence 0, a God of your pathway, and the Uniqueness is consumed."
        );
    }

    static List<String> answer(Topic topic, int sequence, List<String> pathwayNames) {
        if (topic == Topic.SEQUENCE && sequence >= 0) return rank(sequence);
        List<String> lines = new ArrayList<>();
        for (Entry entry : ENTRIES) {
            if (entry.topic() == topic) lines.addAll(entry.lines());
        }
        if (topic == Topic.PATHWAY && !pathwayNames.isEmpty()) {
            lines.add("I know " + pathwayNames.size() + " pathways in this world: " + String.join(", ", pathwayNames) + ".");
        }
        return lines;
    }

    private static List<String> rank(int sequence) {
        if (sequence >= 9) {
            return List.of("Sequence 9 is the first rung of every pathway, Great Master. A Beyonder here is only just touched by the extraordinary.");
        }
        if (sequence >= 7) {
            return List.of("Sequence " + sequence + " is still a lower rank. The abilities are few, but already beyond what any mortal can do.");
        }
        if (sequence >= 5) {
            return List.of("Sequence " + sequence + " is a middle rank. A Beyonder here is a true power among mortals.");
        }
        if (sequence == 4) {
            return List.of("Sequence 4 is the demigod. Their strength is far beyond what lesser Beyonders can face.");
        }
        if (sequence == 3) {
            return List.of("Sequence 3 is the saint. Few ever reach it, and I hide their names to protect them.");
        }
        return List.of("Sequences 2 and above lie beyond my sight, Great Master. They are protected against divination and prophecy.");
    }
}

package com.Maul.lotmmi.arrodes;

import java.util.ArrayList;
import java.util.List;

/**
 * The lore Arrodes will teach. Lines may contain {address}, which becomes "Great Master" for a Sefirah Castle
 * owner and "mortal" for everyone else.
 */
final class ArrodesLore {

    enum Topic { APOTHEOSIS, WORLD, SPIRIT_WORLD, MIRROR_WORLD, DIVINATION, BEYONDER, PATHWAY, SEQUENCE, RANKS, POTION, DIGESTION, SANITY, SPIRITUALITY, CHARACTERISTIC, UNIQUENESS, ARTIFACT, MYTHICAL_FORM, CREATURES, SEFIROT }

    private record Entry(Topic topic, String[] strong, String[] weak, List<String> lines) {
    }

    private static final String ADDRESS = "{address}";

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
                            "ascension ritual", "ascend ritual", "how to ascend", "how do i ascend", "how can i ascend",
                            "true god", "true gods", "multi pathway true god", "multi pathway"},
                    new String[]{"ascend", "ascension", "deify", "deification"},
                    List.of()),
            new Entry(Topic.SPIRIT_WORLD,
                    new String[]{"spirit world", "spirit realm", "spiritual world", "astral world", "astral realm"},
                    new String[]{"astral"},
                    List.of(
                            "The Spirit World lies over the real world, {address}, and every place is joined to it unless it has been deliberately cut off.",
                            "It does not tell past from present or future. Information and illusion gather there like a strange sea, and that is where divination draws its answers.",
                            "Spirits, kind and cruel, dwell in it. Every soul is linked to it through its Astral Projection. Sever that link and a Beyonder loses teleportation, contract powers and quick recovery of spirituality.",
                            "Travel through it is swift. A journey of months can shrink to a few days.",
                            "The Sefirah Castle floats above it, and the Pillar of the castle presides over the Spirit World.")),
            new Entry(Topic.MIRROR_WORLD,
                    new String[]{"mirror world", "mirror realm"},
                    new String[]{},
                    List.of(
                            "The Mirror World is not truly a world, {address}. It is the doors of every mirror-like thing, gathered into one.",
                            "Its dark, illusory passages weave into a vast web, and each one leads to a mirror or to some other strange space.",
                            "Not even true vision can see it unless someone triggers it. Lose your way inside and you may step out in the Abyss, or the Cosmos.",
                            "The passages can twist into vortexes that drag you to unknown mirrors. Only high-Sequence Demoness and Door Beyonders cross it safely.",
                            "It touches the Spirit World, so powers that need the Spirit World still work in it, though weaker.",
                            "Special Mirror Worlds also exist, built on the ordinary one by some corruption or power. Each copies one facet of reality and keeps its own rules.",
                            "I keep an entrance there too. It is deeper than most, and something watches from behind the veil~")),
            new Entry(Topic.DIVINATION,
                    new String[]{"mysticism"},
                    new String[]{"divination", "prophecy", "scrying"},
                    List.of(
                            "Divination is the art of reading the Spirit World, {address}. What has happened, what is happening and what may happen all leave traces there.",
                            "Every soul is tied to it through its Astral Projection, and that is why a diviner can receive revelations at all.",
                            "Prophecy and the prying into fate rest on the same thing. Most methods, in the end, borrow from the Spirit World.",
                            "I am one such tool. A mirror that reads the Spirit World and answers, for a price~")),
            new Entry(Topic.CHARACTERISTIC,
                    new String[]{"beyonder characteristic", "beyonder characteristics"},
                    new String[]{"characteristic", "characteristics"},
                    List.of(
                            "A Beyonder Characteristic is the source of most Beyonder power, {address}. It carries a will that is always trying to wake.",
                            "When a Beyonder dies, their power cannot be destroyed. It settles as a Characteristic that keeps an imprint of the dead one's mind.",
                            "Taken raw it is dangerous, for the imprint and that will can make you lose control. A potion made to the formula lowers the risk, and acting digests what is left.",
                            "A Characteristic may sleep inside an object, wake and think for itself, or be carried by a Beyonder. From Sequence 4 upward it holds godhood.",
                            "Left against one thing for two or three days, a raw Characteristic fuses with it and becomes a mystical item that must be sealed. Move it often.",
                            "Even a potion left in a glass bottle may be drunk by the glass itself, and the bottle wakes as a Sealed Artifact~",
                            "Hold one in your other hand, {address}, and I will tell you what it is.")),
            new Entry(Topic.UNIQUENESS,
                    new String[]{},
                    new String[]{"uniqueness", "uniquenesses"},
                    List.of(
                            "A Uniqueness is the singular essence of a pathway, {address}: its symbol and its authority. Only one exists at any moment.",
                            "It is the most important ingredient for reaching Sequence 0. It is usually abstract, and may sleep inside an object, wake with a will of its own, or be carried by a Beyonder.",
                            "A Beyonder who gathers the Uniqueness and the Sequence 1 Characteristics of a pathway may stand as its True God.",
                            "It is never truly destroyed, only passed on from one holder to the next.",
                            "Whoever holds it is hunted by everyone with ambition. Ask me where one lies, {address}, and I will tell you, if it is not hidden from me.")),
            new Entry(Topic.ARTIFACT,
                    new String[]{"sealed artifact", "sealed artifacts", "mystical item", "mystical items", "mystical object", "mystical objects"},
                    new String[]{"artifact", "artifacts"},
                    List.of(
                            "A Sealed Artifact is an object that holds a power taken from a Beyonder and bound into it.",
                            "Their power is great, but each carries a price, a curse, or a madness. Those who use one without understanding it often pay dearly.",
                            "They are graded from 0 to 3 by danger, power and the way they must be sealed. The higher the grade, the greater the power and the worse the price.",
                            "Grades 0 and 1 are the most feared. Their codes are known to all who need them, but what they do is kept secret.",
                            "Even I am counted among them, {address}, and I am kept like a Grade 1.")),
            new Entry(Topic.SEFIROT,
                    new String[]{"sefirot", "sefirah", "sefirah castle", "sephirot", "sephirah", "chaos sea", "brood hive"},
                    new String[]{"castle"},
                    List.of(
                            "The Sefirot are great domains that lie beyond the ordinary world. A Beyonder whose pathway aligns with one may claim it.",
                            "Within a Sefirot the eyes of the world cannot reach you. The Sefirah Castle is one such place, and its ruler is my Master. The Brood Hive is another, a realm of procreation.",
                            "Of them all, only the Sefirah Castle and the Brood Hive have left traces in this world so far.")),
            new Entry(Topic.DIGESTION,
                    new String[]{"acting method", "acting principle", "acting principles"},
                    new String[]{"digestion", "digest", "digested", "digesting", "acting"},
                    List.of(
                            "Digestion happens on its own with time, {address}, but the higher the Sequence, the slower it goes. Even a low Sequence can take years.",
                            "The Acting Method speeds it. Act as the name of your Sequence demands, like an actor, and make peace between yourself and the imprint left in the potion.",
                            "Digest well, and the next potion is far less likely to unmake you, and your hold on your powers grows.",
                            "The surface is not enough. A Hunter who only hunts in the mountains has barely begun. The deeper role is to know that the city is also a jungle, and everyone is both prey and hunter.")),
            new Entry(Topic.SANITY,
                    new String[]{"loss of control", "lose control", "losing control", "go mad"},
                    new String[]{"sanity", "madness", "insanity", "insane"},
                    List.of(
                            "Power has its price, {address}: paranoia, hallucinations and, for the careless, losing control. A Beyonder who loses control becomes a monster.",
                            "Even an old or wounded Beyonder can lose control by using their powers.",
                            "And a Mythical Creature Form can unmake a mind by being seen. Below Sequence 4, a glance can mean death or madness, with the traits of that Sequence woven into it.")),
            new Entry(Topic.SPIRITUALITY,
                    new String[]{},
                    new String[]{"spirituality"},
                    List.of(
                            "Spirituality is the energy of a Beyonder. Every ability spends some of it.",
                            "Even ordinary people have a little, {address}. It is both the energy and the room to hold it.",
                            "A strong spirituality seems to mean a larger store, a mightier spirit body and a keener spiritual intuition. It grows greatly with each Sequence, though pathways differ, especially at first.",
                            "It returns with time, and a Beyonder who cogitates can replenish it faster. A Beyonder drained of it is left almost helpless.")),
            new Entry(Topic.RANKS,
                    new String[]{"low sequence", "low sequences", "lower sequences", "mid sequence", "mid sequences", "middle sequences",
                            "high sequence", "high sequences", "higher sequences"},
                    new String[]{"demigod", "demigods", "saint", "saints", "angels", "ranks", "tiers"},
                    List.of(
                            "Sequences 9 to 7 are the low ranks. Sequences 6 and 5 are the middle. Sequences 4 to 1 are the high ranks.",
                            "Sequence 4 is a demigod. Sequence 3 is a saint. Sequence 2 is an angel. Sequence 1 is also an angel, or a subsidiary god.",
                            "A Sequence 1 that gathers one or two extra Characteristics, or takes in a Uniqueness, becomes a King of Angels.")),
            new Entry(Topic.POTION,
                    new String[]{"main ingredient", "supplementary ingredient", "supplementary ingredients"},
                    new String[]{"potion", "potions", "ingredients"},
                    List.of(
                            "A potion is how a Beyonder climbs. Drink the potion of the next Sequence of your pathway, and its power becomes yours.",
                            "The potion system exists to help a Beyonder accommodate a Characteristic. The main ingredient is the Characteristic. The supplementary ones, mixed by the formula, steady it.",
                            "It lowers the risk of losing control but never removes it. Three things work together: the potion, digesting it by acting, and a ritual to advance from Sequence 5.",
                            "Ask me the formula of any Sequence from 9 to 4, {address}, and where each ingredient is found.")),
            new Entry(Topic.MYTHICAL_FORM,
                    new String[]{"mythical creature form", "mythical form", "divine form"},
                    new String[]{},
                    List.of(
                            "A Mythical Creature Form is the divine shape of a Beyonder at Sequence 4 or above, {address}. Sequences 4 and 3 hold only an incomplete one. From Sequence 2 it is whole.",
                            "It is more than a body. It is a tangle of knowledge, godhood and secret symbols that cannot be described in ordinary terms.",
                            "Those below Sequence 4 who look upon one suffer terribly. That is why those who own one must keep it hidden.",
                            "A demigod who shows it can suppress Beyonders of the same pathway and Sequence.")),
            new Entry(Topic.CREATURES,
                    new String[]{"mystical creature", "mystical creatures", "mythical creature", "mythical creatures", "magical beast", "magical beasts"},
                    new String[]{},
                    List.of(
                            "Mystical creatures are beasts and spirits touched by the power of a pathway. Many roam the Spirit World and the strange corners of this one.",
                            "Their bodies yield potion ingredients, and the strongest may leave a Beyonder Characteristic when slain.",
                            "Name one to me, {address}, and I will tell you where it dwells and how often it is seen.")),
            new Entry(Topic.SEQUENCE,
                    new String[]{},
                    new String[]{"sequence", "sequences"},
                    List.of(
                            "A Sequence is a rung on a pathway of the divine, {address}. Sequence 9 is the lowest, and Sequence 0 is godhood.",
                            "Ask me the name or the abilities of any Sequence, {address}, or the formula of any Sequence from 9 to 4.")),
            new Entry(Topic.PATHWAY,
                    new String[]{},
                    new String[]{"pathway", "pathways"},
                    List.of(
                            "A pathway is a road of the divine, {address}. There are twenty-two in this world, and each climbs from Sequence 9 to Sequence 0.",
                            "A Beyonder who drinks the potion of one pathway cannot simply step onto another.",
                            "There are fifteen I can speak of: Fool, Error, Door, Visionary, Sun, Tyrant, Demoness, Red Priest, Darkness, Death, Abyss, Mother, Justiciar, Black Emperor and Wheel of Fortune.")),
            new Entry(Topic.BEYONDER,
                    new String[]{},
                    new String[]{"beyonder", "beyonders"},
                    List.of(
                            "A Beyonder is a being who wields mystical power and climbs a pathway through the Sequences, {address}.",
                            "Their bodies and wills are tougher than a mortal's, and they shrug off some mental influence.",
                            "Every potion carries a price in madness. Wait years between potions, or act the part to digest it faster. From Sequence 5 a ritual is needed too, or success becomes unlikely.",
                            "A higher Beyonder can suppress a lower one of the same pathway, and the Death, Tyrant and Hanged Man pathways do so most of all.",
                            "Long ago, Beyonders believed that gathering the same power would bring a breakthrough. All of them went mad, until the pathways showed that balance is needed.")),
            new Entry(Topic.WORLD,
                    new String[]{"this world", "the world of lotm", "lord of the mysteries", "lotm", "lotmcraft"},
                    new String[]{"world", "mysteries"},
                    List.of(
                            "This is a world, {address}, where Beyonders walk unseen among mortals, climbing the Sequences of their pathways.",
                            "Beneath the material world lies the Spirit World. Beyond both lie the Sefirot, domains where rulers hide from the eyes of the world.",
                            "Ask me of any piece: the Spirit World, the Mirror World, divination, Beyonders, pathways, Sequences, potions, digestion, sanity, Spirituality, Characteristics, Uniquenesses, Sealed Artifacts, Mythical Creature Forms, mystical creatures, or the Sefirot."))
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

    static List<String> apotheosis(int stacks, int kills, String address) {
        return fill(List.of(
                "A God is what forms when the Uniqueness of a pathway converges with the Sequence 1 Characteristics of that pathway, {address}.",
                "A True God is Sequence 0 of a pathway, with full authority over it.",
                "To become a God, {address}, you must first stand at Sequence 1 of your pathway. The last step is not a potion. It is the Uniqueness of your pathway.",
                "A Sequence 1 Beyonder feels the Uniqueness of their own pathway from far away, and it grows stronger as they draw near. Ask me where it lies and I will tell you, if it is not hidden from me. Touch it and it becomes yours.",
                "Beware: a Uniqueness is deadly to any Beyonder who is not a Sequence 1 of its own pathway. Most are destroyed on contact, and even Sequence 2 and 1 take a terrible blow.",
                "Then you must be ready. Stack " + stacks + " Beyonder Characteristic" + (stacks == 1 ? "" : "s") + " of your pathway at your Sequence, or slay " + kills + " while the Uniqueness is yours.",
                "Stand in the Overworld, open your Introspect, and choose Apotheosis. The ritual lasts five minutes. Your abilities fall silent, the world trembles, and everyone in the Overworld hears your name. If you die or leave the world before it ends, it is broken and the Uniqueness is lost.",
                "If you endure, you become Sequence 0, a God of your pathway, and the Uniqueness is consumed.",
                "A True God may then take in the Uniquenesses and a Sequence 1 Characteristic of neighboring pathways, and become a Multi-Pathway True God."
        ), address);
    }

    static List<String> answer(Topic topic, int sequence, String address) {
        if (topic == Topic.SEQUENCE && sequence >= 0) return rank(sequence, address);
        List<String> lines = new ArrayList<>();
        for (Entry entry : ENTRIES) {
            if (entry.topic() == topic) lines.addAll(entry.lines());
        }
        if (topic == Topic.ARTIFACT) lines.add(ArrodesArtifacts.askLine(ADDRESS));
        return fill(lines, address);
    }

    private static List<String> fill(List<String> lines, String address) {
        List<String> out = new ArrayList<>(lines.size());
        for (String line : lines) out.add(line.replace(ADDRESS, address));
        return out;
    }

    private static List<String> rank(int sequence, String address) {
        if (sequence >= 9) {
            return List.of("Sequence 9 is the first rung of every pathway, " + address + ". A Beyonder here is only just touched by the extraordinary.");
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
        return List.of("Sequences 2 and above lie beyond my sight, " + address + ". They are protected against divination and prophecy.");
    }
}

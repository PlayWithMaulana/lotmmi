package com.Maul.lotmmi.arrodes;

import com.Maul.lotmmi.config.LotmmiConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * What Arrodes says about the other sealed artifacts of this addon. Everyone hears what each one is and what it does.
 * What it costs its bearer is told to Sefirah Castle owners only, like the other dangers.
 */
final class ArrodesArtifacts {

    enum Id {
        CREEPING_HUNGER, AWAKENED, STAFF, SCEPTER, CHARM
    }

    record Artifact(Id id, String shown, String... aliases) {
    }

    private static final List<Artifact> ARTIFACTS = List.of(
            new Artifact(Id.CREEPING_HUNGER, "Creeping Hunger",
                    "creeping hunger", "creepinghunger", "creeping"),
            new Artifact(Id.AWAKENED, "the Awakened Creeping Hunger",
                    "awakened creeping hunger", "awakened creeping", "awakened hunger", "awakened ch"),
            new Artifact(Id.STAFF, "the Staff of the Stars",
                    "staff of the stars", "staff of stars", "star staff", "stars staff", "staff"),
            new Artifact(Id.SCEPTER, "the Sea God Scepter",
                    "sea god scepter", "sea god sceptre", "sea gods scepter", "sea god s scepter", "scepter", "sceptre"),
            new Artifact(Id.CHARM, "the Sea God Charm",
                    "sea god charm", "sea god s charm", "sea gods charm", "charm")
    );

    private ArrodesArtifacts() {
    }

    /** Longest matching alias wins. A lone short word must match exactly, so "stuff" is never taken for "staff"; inside a longer name a small typo is forgiven. */
    static Artifact find(List<String> tokens) {
        Artifact best = null;
        int bestLength = 0;
        for (Artifact artifact : ARTIFACTS) {
            for (String alias : artifact.aliases()) {
                boolean ok = true;
                String[] parts = alias.split(" ");
                int significant = 0;
                for (String part : parts) if (part.length() > 2 && !ArrodesText.isStop(part)) significant++;
                for (String part : parts) {
                    boolean hit = false;
                    boolean mayBlur = part.length() >= 7 || (significant >= 2 && part.length() >= 5);
                    for (String token : tokens) {
                        if (token.equals(part) || (mayBlur && ArrodesText.fuzzyEquals(token, part))) {
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
                    best = artifact;
                    bestLength = alias.length();
                }
            }
        }
        return best;
    }

    /** The tokens with every word of the artifact's names blanked, so they are not read as a pathway or a Sequence. */
    static List<String> mask(List<String> tokens, Artifact artifact) {
        List<String> out = new ArrayList<>(tokens);
        for (String alias : artifact.aliases()) {
            for (String part : alias.split(" ")) {
                if (part.length() < 2 || ArrodesText.isStop(part)) continue;
                for (int i = 0; i < out.size(); i++) {
                    String token = out.get(i);
                    if (token.equals(part) || (part.length() >= 7 && ArrodesText.fuzzyEquals(token, part))) out.set(i, "_");
                }
            }
        }
        return out;
    }

    /** The closing line of Arrodes' lore on sealed artifacts: the names it can speak of. */
    static String askLine(String address) {
        List<String> names = new ArrayList<>();
        for (Artifact artifact : ARTIFACTS) names.add(artifact.shown());
        StringBuilder joined = new StringBuilder();
        for (int i = 0; i < names.size(); i++) {
            if (i > 0) joined.append(i == names.size() - 1 ? " and " : ", ");
            joined.append(names.get(i));
        }
        return "Name one to me, " + address + ", and I will tell you what it is. I know " + joined + ".";
    }

    static List<String> describe(Artifact artifact, boolean owner, String address) {
        List<String> lines = new ArrayList<>(open(artifact.id()));
        List<String> price = price(artifact.id());
        if (!price.isEmpty()) {
            if (owner) {
                lines.addAll(price);
            } else {
                lines.add("What it costs its bearer is a secret I keep for my Master.");
            }
        }
        List<String> out = new ArrayList<>(lines.size());
        for (String line : lines) out.add(line.replace("{address}", address));
        return out;
    }

    private static List<String> open(Id id) {
        return switch (id) {
            case CREEPING_HUNGER -> List.of(
                    "Creeping Hunger is a glove that feeds on souls, {address}. It answers to Sequence 5 of the Hanged Man pathway.",
                    "Graze a Beyonder's soul with it and it keeps that soul, and lends you one to three of its abilities. It holds five souls at a time.",
                    "It cannot graze the soul of a demigod, Sequence 4 or stronger.",
                    "Let a soul go and its Characteristic is spat back out.");
            case AWAKENED -> List.of(
                    "The Awakened Creeping Hunger is the glove grown greater, {address}. It holds seven souls instead of five, and each soul gives it two or three abilities.",
                    "When it grazes a soul, you choose which of its abilities to keep.",
                    "It will take a demigod's soul where the lesser glove cannot, though not many.",
                    "It is forged from a Creeping Hunger, the Characteristics of a Cataclysmic Interrer, a Manipulator and an Unshadowed, and five One Pounds.");
            case STAFF -> List.of(
                    "The Staff of the Stars is a relic of the Planeswalker, {address}. It is forged round a Planeswalker's Characteristic and eight One Pounds.",
                    "Crouch and use it to open its five remembered places. Save a place, and the Staff will carry you back to that exact spot, from anywhere.",
                    "It also watches. Abilities cast in its sight are learned, and a creature stared at for a while leaves its likeness in the Staff. Both can be called back, a little weaker than the original.",
                    "Strike with it and the target changes strangely: it may mutate, slow, float, reel, glow, go blind, or be flung aside.");
            case SCEPTER -> List.of(
                    "The Sea God Scepter is a Tyrant's relic of Sequence 3, {address}. It is forged round a Sea King's Characteristic and eight One Pounds.",
                    "Storms answer it: wind, lightning, tsunami, hurricane, water, downpour and the wrath of the ocean are bound inside.",
                    "Crouch and use it on a weaker Beyonder and they become your follower. A follower can be blessed, sent to attack, or called to your side, and their prayers can be answered.",
                    "You may even Gaze upon the place where a follower prayed.",
                    "Held, it lends strength, breath beneath the water and the power of a conduit.");
            case CHARM -> List.of(
                    "The Sea God Charm is a small charm bound from the Scepter's power, {address}. It serves once, and is spent.",
                    "There are four kinds: Sea Affinity to swim fast and breathe underwater for a while, Electric Shock, Gale, and Sea Whisper, which calls the creatures of the sea and calms the hostile ones.",
                    "Its spirituality fades over about " + LotmmiConfig.CHARM_LIFETIME_DAYS.get() + " days, and a faded charm does nothing.");
        };
    }

    private static List<String> price(Id id) {
        return switch (id) {
            case CREEPING_HUNGER -> List.of(
                    "It must be fed once a day. Leave a soul in its reserve and it devours that one instead, and spits out its Characteristic.",
                    "With the reserve empty it feeds on its bearer. It means to kill. Never carry it hungry.");
            case AWAKENED -> List.of(
                    "It hungers as the lesser glove does, and it prays as well. Every few minutes it recites to its Lord, and while you hold it the prayer takes a little blood and a little of your sanity.",
                    "It shrinks from mushrooms. Stand within five blocks of one and it will not answer you.");
            case STAFF -> List.of(
                    "Its price is Panic. Every misfire and every misremembered journey adds a level of it, up to five, and the higher it climbs the likelier the next misfire becomes, and the crueler the anomalies: a cursed shape, a blank mind, a journey that ends in the Spirit World or the Cosmos.",
                    "Panic fades after a minute of calm, and the Sea God Scepter keeps it away entirely.",
                    "Only a Sefirah Castle seals the Staff. Anywhere else it is never calm, and now and then, very rarely, it slips away on its own.");
            case SCEPTER -> List.of(
                    "Below Sequence 4, whoever holds it loses their sanity. A Beyonder loses it slowly, while a mortal or a Sequence 9 is left on the edge of madness at once. Only a demigod holds it unharmed.",
                    "Every " + LotmmiConfig.BLOOD_DRAIN_INTERVAL_MINUTES.get() + " minutes it drains the blood of every living thing within thirty-two blocks of its bearer. Your own castle shelters you from that, and from its madness.",
                    "While you Gaze, your body is left behind and your spirituality bleeds every second. If the body is struck you are pulled back, and if it is killed, you die with it.");
            case CHARM -> List.of();
        };
    }
}

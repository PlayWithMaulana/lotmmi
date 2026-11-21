package com.Maul.lotmmi.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class LotmmiConfig {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue RESTRICT_DEMIGOD_GRAZING = BUILDER
            .comment("Creeping Hunger cannot graze souls of Sequence 4 or higher. The Awakened version may still hold a limited number of Sequence 4 souls.")
            .define("restrictDemigodGrazing", true);

    public static final ModConfigSpec.IntValue AWAKENED_MIN_GRAZIABLE_SEQUENCE = BUILDER
            .comment("The strongest (lowest-numbered) demigod Sequence the Awakened Creeping Hunger can graze. 4 = Sequence 4 only (default); 0 = any Sequence from 4 down to 0. Only used when restrictDemigodGrazing is enabled.")
            .defineInRange("awakenedMinGraziableSequence", 4, 0, 4);

    public static final ModConfigSpec.IntValue AWAKENED_DEMIGOD_SOULS = BUILDER
            .comment("How many demigod-tier souls (Sequence 4 down to awakenedMinGraziableSequence) the Awakened Creeping Hunger can hold at once. Only used when restrictDemigodGrazing is enabled.")
            .defineInRange("awakenedDemigodSouls", 1, 0, 7);

    public static final ModConfigSpec.DoubleValue CHARM_SPIRITUALITY_COST = BUILDER
            .comment("Spirituality spent to bind one Sea God Charm. The default of 100 is about twice an average Sequence 7 ability (55).")
            .defineInRange("charmSpiritualityCost", 100.0D, 0.0D, 1000000.0D);

    public static final ModConfigSpec.IntValue CHARM_LIFETIME_DAYS = BUILDER
            .comment("Days until a Sea God Charm loses all its spirituality (1 day = 24000 ticks). 90 = three months of 30 days.")
            .defineInRange("charmLifetimeDays", 90, 1, 3650);

    public static final ModConfigSpec.IntValue GAZE_DURATION_SECONDS = BUILDER
            .comment("How many seconds the Sea God Scepter's Gaze lasts before you are pulled back to where you started.")
            .defineInRange("gazeDurationSeconds", 60, 5, 600);

    public static final ModConfigSpec.DoubleValue GAZE_SPIRITUALITY_PER_SECOND = BUILDER
            .comment("Spirituality drained per second while gazing. The default is about one median Sequence 5 ability (150) per second.")
            .defineInRange("gazeSpiritualityPerSecond", 150.0D, 0.0D, 1000000.0D);

    public static final ModConfigSpec.BooleanValue GAZE_REQUIRES_CASTLE = BUILDER
            .comment("If true, Gaze only works inside your own Sefirah Castle. If false (default), it works anywhere.")
            .define("gazeRequiresSefirahCastle", false);

    public static final ModConfigSpec.BooleanValue STAFF_ANOMALY_POWER = BUILDER
            .comment("Staff of the Stars anomalies: power (wrong power, backlash, shuffled ability wheel).")
            .define("staffAnomalyPower", true);
    public static final ModConfigSpec.BooleanValue STAFF_ANOMALY_MIND = BUILDER
            .comment("Staff of the Stars anomalies: mind (blanking out, Mental Plague).")
            .define("staffAnomalyMind", true);
    public static final ModConfigSpec.BooleanValue STAFF_ANOMALY_BODY = BUILDER
            .comment("Staff of the Stars anomalies: body (Cursed animal form, shrinking, growing, Mutated).")
            .define("staffAnomalyBody", true);
    public static final ModConfigSpec.BooleanValue STAFF_ANOMALY_SPACE = BUILDER
            .comment("Staff of the Stars anomalies: space (displacement, swapping places).")
            .define("staffAnomalySpace", true);
    public static final ModConfigSpec.BooleanValue STAFF_ANOMALY_FORTUNE = BUILDER
            .comment("Staff of the Stars anomalies: fortune (bad luck, stray lightning, a sudden storm).")
            .define("staffAnomalyFortune", true);
    public static final ModConfigSpec.BooleanValue STAFF_ANOMALY_ECHO = BUILDER
            .comment("Staff of the Stars anomalies: hostile echoes of figures the staff has stared at (high Panic only).")
            .define("staffAnomalyEchoFigures", true);
    public static final ModConfigSpec.BooleanValue STAFF_AMBIENT_ANOMALIES = BUILDER
            .comment("Strange things happen around an unsealed Staff of the Stars that nobody is holding. Inside a Sefirah Castle it is sealed and calm.")
            .define("staffAmbientAnomalies", true);
    public static final ModConfigSpec.DoubleValue STAFF_SLIP_CHANCE_DROPPED = BUILDER
            .comment("Chance per minute that a dropped, unsealed Staff of the Stars teleports far away. Very small by default. 0 turns it off.")
            .defineInRange("staffSlipChanceDropped", 0.005D, 0.0D, 1.0D);
    public static final ModConfigSpec.DoubleValue STAFF_SLIP_CHANCE_STOWED = BUILDER
            .comment("Chance per minute that an unsealed Staff of the Stars carried in a pack (not in hand) slips away. Very small by default. 0 turns it off.")
            .defineInRange("staffSlipChanceStowed", 0.0005D, 0.0D, 1.0D);
    public static final ModConfigSpec.IntValue BLOOD_DRAIN_INTERVAL_MINUTES = BUILDER
            .comment("Minutes between the Sea God Scepter's Blood Drain pulses while it is carried (20 ticks per second).")
            .defineInRange("bloodDrainIntervalMinutes", 35, 1, 100000);

    public static final ModConfigSpec.DoubleValue QILANGOS_SPAWN_RATE = BUILDER
            .comment("How often Qilangos spawns naturally in the Overworld. 1.0 (default) is about as rare as an ocean monument. "
                    + "0 = never, 0.1 = almost never, 0.5 = rarer, 2 = twice as common, 4 = any region (still uncommon), "
                    + "20 = fairly common, 100+ = very common. Spawn eggs, commands and spawners are not affected.")
            .defineInRange("qilangosSpawnRate", 1.0D, 0.0D, 1000.0D);

    public static final ModConfigSpec.BooleanValue DIE_ENABLED = BUILDER
            .comment("Enables the Die of Probability behaviour on the base mod's Dice of Probability item.")
            .define("dieEnabled", true);

    public static final ModConfigSpec.IntValue DIE_REROLL_MIN_MINUTES = BUILDER
            .comment("Shortest time in game minutes before the die changes its face.")
            .defineInRange("dieRerollMinMinutes", 10, 1, 100000);

    public static final ModConfigSpec.IntValue DIE_REROLL_MAX_MINUTES = BUILDER
            .comment("Longest time in game minutes before the die changes its face.")
            .defineInRange("dieRerollMaxMinutes", 40, 1, 100000);

    public static final ModConfigSpec.IntValue DIE_LUCK_PER_FACE = BUILDER
            .comment("Luck added or removed for each step away from the middle of the die. Faces 1-3 subtract, faces 4-6 add. Face 1 and 6 are the strongest.")
            .defineInRange("dieLuckPerFace", 150, 0, 100000);

    public static final ModConfigSpec.IntValue DIE_LEAK_RADIUS = BUILDER
            .comment("Blocks around a carried die whose creatures are also touched by its luck. 0 turns the leak off.")
            .defineInRange("dieLeakRadius", 16, 0, 128);

    public static final ModConfigSpec.DoubleValue DIE_LEAK_FRACTION = BUILDER
            .comment("Share of the holder's luck swing that leaks onto nearby creatures.")
            .defineInRange("dieLeakFraction", 0.4D, 0.0D, 1.0D);

    public static final ModConfigSpec.DoubleValue DIE_FACE_ONE_DEATH_PER_MINUTE = BUILDER
            .comment("Chance per minute that a holder showing face 1 is struck by a calamity: lightning, a falling anvil and a blast, all at once. Almost always fatal. A totem of undying still works. Blocks are not destroyed.")
            .defineInRange("dieFaceOneDeathPerMinute", 0.03D, 0.0D, 1.0D);

    public static final ModConfigSpec.DoubleValue DIE_FACE_TWO_ACCIDENT_PER_MINUTE = BUILDER
            .comment("Chance per minute that a holder showing face 2 is struck by one random calamity (lightning, a falling anvil or a blast). Lethal if already hurt.")
            .defineInRange("dieFaceTwoAccidentPerMinute", 0.02D, 0.0D, 1.0D);

    public static final ModConfigSpec.DoubleValue ARRODES_SPIRITUALITY_COST = BUILDER
            .comment("Spirituality a Beyonder pays for every answer Arrodes gives. Refusals, clarifying questions and punishments are free, and so is anyone who is not a Beyonder. The default of 13 is about an average Sequence 9 ability (the active Sequence 9 abilities cost 10 to 30, averaging about 13). 0 makes Arrodes free.")
            .defineInRange("arrodesSpiritualityCost", 13.0D, 0.0D, 1000000.0D);

    public static final ModConfigSpec.IntValue ARRODES_QUESTIONS_PER_USE = BUILDER
            .comment("How many questions can be settled (asked and paid for) before Arrodes rests. The count also resets after a rest-length pause.")
            .defineInRange("arrodesQuestionsPerUse", 5, 1, 1000);

    public static final ModConfigSpec.IntValue ARRODES_COOLDOWN_SECONDS = BUILDER
            .comment("Seconds Arrodes rests after the question limit is used up.")
            .defineInRange("arrodesCooldownSeconds", 60, 0, 86400);

    public static final ModConfigSpec.IntValue ARRODES_ANSWER_SECONDS = BUILDER
            .comment("Seconds the holder has to answer Arrodes' question in chat before being struck by lightning.")
            .defineInRange("arrodesAnswerSeconds", 15, 5, 3600);

    public static final ModConfigSpec.IntValue ARRODES_BROADCAST_RADIUS = BUILDER
            .comment("Blocks around the holder that hear Arrodes' question. 0 broadcasts to the whole server.")
            .defineInRange("arrodesBroadcastRadius", 32, 0, 100000);

    public static final ModConfigSpec.DoubleValue ARRODES_STRIKE_DAMAGE = BUILDER
            .comment("Lightning damage to a Beyonder of Sequence 7 or stronger who fails to answer Arrodes. The default of 16 is about what a Tyrant's Sequence 5 Lightning deals (the base mod rolls 8.3 to 17.1 for that sequence).")
            .defineInRange("arrodesLightningDamage", 16.0D, 0.0D, 1000.0D);

    public static final ModConfigSpec.DoubleValue ARRODES_WEAK_KILL_CHANCE = BUILDER
            .comment("Chance that the lightning kills a non-Beyonder or a Beyonder of Sequence 8 or 9 who fails to answer Arrodes. Those who survive the chance are left clinging to a sliver of health, never killed by the bolt itself.")
            .defineInRange("arrodesWeakKillChance", 0.5D, 0.0D, 1.0D);

    public static final ModConfigSpec.DoubleValue ARRODES_OWNER_STRIKE_DAMAGE = BUILDER
            .comment("Lightning damage to a Sefirah Castle owner who ignores Arrodes' easy question.")
            .defineInRange("arrodesOwnerStrikeDamage", 2.0D, 0.0D, 1000.0D);

    public static final ModConfigSpec.BooleanValue ARRODES_FIND_PLAYER = BUILDER
            .comment("Lets Arrodes tell where an online player roughly is, and which pathway and sequence they walk (never for Sequence 0 to 2, who are protected against divination, and the path of a Sequence 3 stays veiled). Turn off to prevent using it to hunt players.")
            .define("arrodesFindPlayer", true);

    public static final ModConfigSpec.BooleanValue ARRODES_SMITE_ILL_INTENT = BUILDER
            .comment("Arrodes punishes a non-owner who asks it to help hunt, rob or harm players, or who tries to trick it, with a lightning strike or a curse. The punishment is free: it does not count as one of the settled questions. Sefirah Castle owners only get a polite refusal. Turn off to make everyone get a polite refusal.")
            .define("arrodesSmiteIllIntent", true);

    public static final ModConfigSpec.DoubleValue ARRODES_CURSE_CHANCE = BUILDER
            .comment("When Arrodes punishes ill intent, the chance that it is a curse (Bad Luck, Weakness, Slowness, Darkness and Nausea, never lethal) instead of a lightning strike.")
            .defineInRange("arrodesCurseChance", 0.4D, 0.0D, 1.0D);

    public static final ModConfigSpec.DoubleValue ARRODES_CHEST_RARITY = BUILDER
            .comment("How often Arrodes turns up in generated chests, compared with the Staff of the Stars (which is found with the same roll: about 2.6% of chests). 1 = exactly as often as the Staff. Lower is rarer, 0 turns it off. It cannot be made more common than the Staff. This replaces the old arrodesChestChance setting, which is no longer read.")
            .defineInRange("arrodesChestRarity", 1.0D, 0.0D, 1.0D);

    public static final ModConfigSpec.BooleanValue ARRODES_WARN_FIRST = BUILDER
            .comment("When a non-owner's question smells of ill intent, Arrodes warns them the first time (within about ten minutes) and only punishes the second. Tricks and loopholes are always punished at once.")
            .define("arrodesWarnFirst", true);

    public static final ModConfigSpec.DoubleValue ARRODES_OWNER_REMARK_CHANCE = BUILDER
            .comment("Chance that Arrodes adds a free remark after an answer to a Sefirah Castle owner. It never remarks twice in a row.")
            .defineInRange("arrodesOwnerRemarkChance", 0.3D, 0.0D, 1.0D);

    public static final ModConfigSpec.IntValue ARRODES_WARNING_COOLDOWN_SECONDS = BUILDER
            .comment("After Arrodes warns a Sefirah Castle owner of a real danger nearby (hostile creatures, a stronger Beyonder, a storm), the same kind of danger is not mentioned again for this many seconds. A different kind of danger may speak after a third of it. Asking Arrodes about danger directly always gets a full answer. 0 warns after every answer.")
            .defineInRange("arrodesWarningCooldownSeconds", 600, 0, 86400);

    public static final ModConfigSpec.BooleanValue ARRODES_MIRROR_ENABLED = BUILDER
            .comment("Lets anyone summon Arrodes through a mirror (glass) by sneaking and right-clicking it with a signed book that holds Arrodes' honorific name. The book is the price and is spent when he comes.")
            .define("arrodesMirrorEnabled", true);

    public static final ModConfigSpec.DoubleValue ARRODES_MIRROR_SUMMON_COST = BUILDER
            .comment("Spirituality a Beyonder pays once to summon Arrodes through a mirror, on top of the normal cost of each answer. Non-Beyonders pay nothing. A summon the player cannot afford does nothing.")
            .defineInRange("arrodesMirrorSummonCost", 30.0D, 0.0D, 1000000.0D);

    public static final ModConfigSpec.IntValue ARRODES_MIRROR_QUESTIONS_PER_DAY = BUILDER
            .comment("How many questions Arrodes settles for one player through mirrors before the mirror goes quiet. This is separate from the item's own limit.")
            .defineInRange("arrodesMirrorQuestionsPerDay", 5, 1, 1000);

    public static final ModConfigSpec.IntValue ARRODES_MIRROR_COOLDOWN_TICKS = BUILDER
            .comment("Ticks until a player's mirror questions return, counted from their first question (24000 = one Minecraft day, which is 20 real minutes). Sleeping does not shorten it.")
            .defineInRange("arrodesMirrorCooldownTicks", 24000, 0, 2400000);

    public static final ModConfigSpec.IntValue ARRODES_MIRROR_IDLE_SECONDS = BUILDER
            .comment("Seconds Arrodes waits at a mirror for a question before it withdraws.")
            .defineInRange("arrodesMirrorIdleSeconds", 180, 10, 3600);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private LotmmiConfig() {
    }
}

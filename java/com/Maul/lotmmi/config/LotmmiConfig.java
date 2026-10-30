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

    public static final ModConfigSpec.IntValue BLOOD_DRAIN_INTERVAL_MINUTES = BUILDER
            .comment("Minutes between the Sea God Scepter's Blood Drain pulses while it is carried (20 ticks per second).")
            .defineInRange("bloodDrainIntervalMinutes", 35, 1, 100000);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private LotmmiConfig() {
    }
}

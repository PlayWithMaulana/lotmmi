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

    public static final ModConfigSpec SPEC = BUILDER.build();

    private LotmmiConfig() {
    }
}

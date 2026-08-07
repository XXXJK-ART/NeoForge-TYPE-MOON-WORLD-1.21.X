package com.example.typemoonaddon.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Limits for the server-only addon administration commands. */
public final class AddonCommandConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue PERMISSION_LEVEL = BUILDER
            .comment("Minimum permission level required for addon administration commands.")
            .defineInRange("commands.permissionLevel", 2, 0, 4);
    public static final ModConfigSpec.DoubleValue MAXIMUM_MANA = BUILDER
            .comment("Maximum mana applied by unlock_all_max. The default matches the main mod max-level command.")
            .defineInRange("commands.unlockAllMax.maximumMana", 100000.0D, 0.0D, 1000000.0D);
    public static final ModConfigSpec.DoubleValue MAXIMUM_MANA_REGENERATION = BUILDER
            .comment("Mana restored per recovery settlement applied by unlock_all_max.")
            .defineInRange("commands.unlockAllMax.maximumManaRegeneration", 100.0D, 0.0D, 1000.0D);
    public static final ModConfigSpec.DoubleValue MAXIMUM_MANA_REGENERATION_INTERVAL = BUILDER
            .comment("Recovery interval in ticks applied by unlock_all_max. One tick is the main mod minimum.")
            .defineInRange("commands.unlockAllMax.maximumManaRegenerationInterval", 1.0D, 1.0D, 10000.0D);
    public static final ModConfigSpec.IntValue MAXIMUM_TARGETS = BUILDER
            .comment("Maximum number of online players accepted by one unlock_all_max command.")
            .defineInRange("commands.unlockAllMax.maximumTargets", 64, 1, 256);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private AddonCommandConfig() {
    }
}

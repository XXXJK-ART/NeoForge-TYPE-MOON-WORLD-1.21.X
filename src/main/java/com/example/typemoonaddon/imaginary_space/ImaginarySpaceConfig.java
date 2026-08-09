package com.example.typemoonaddon.imaginary_space;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Server/common safety filters and strict per-instance generation budgets. */
public final class ImaginarySpaceConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.ConfigValue<List<? extends String>> NAMESPACE_BLACKLIST = BUILDER
            .comment("Namespaces excluded from imaginary-space item, block, and mob pools.")
            .defineList("imaginarySpace.namespaceBlacklist", List.of(), ImaginarySpaceConfig::validNamespace);
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ID_BLACKLIST = BUILDER
            .comment("Full registry IDs excluded from imaginary-space generation.")
            .defineList("imaginarySpace.idBlacklist", List.of(
                    "minecraft:air",
                    "minecraft:barrier",
                    "minecraft:bedrock",
                    "minecraft:command_block",
                    "minecraft:chain_command_block",
                    "minecraft:repeating_command_block",
                    "minecraft:structure_block",
                    "minecraft:structure_void",
                    "minecraft:jigsaw",
                    "minecraft:spawner",
                    "minecraft:trial_spawner",
                    "minecraft:vault",
                    "minecraft:end_portal",
                    "minecraft:end_gateway",
                    "minecraft:nether_portal"
            ), ImaginarySpaceConfig::validId);
    public static final ModConfigSpec.ConfigValue<List<? extends String>> NAMESPACE_WHITELIST = BUILDER
            .comment("Optional namespace allowlist. Empty means all namespaces are allowed.")
            .defineList("imaginarySpace.namespaceWhitelist", List.of(), ImaginarySpaceConfig::validNamespace);
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ID_WHITELIST = BUILDER
            .comment("Optional full-ID allowlist. Empty means all IDs passing safety checks are allowed.")
            .defineList("imaginarySpace.idWhitelist", List.of(), ImaginarySpaceConfig::validId);

    public static final ModConfigSpec.IntValue MAX_BLOCKS = BUILDER
            .defineInRange("imaginarySpace.maxBlocksPerInstance", 8192, 0, 16384);
    public static final ModConfigSpec.IntValue MAX_ITEMS = BUILDER
            .defineInRange("imaginarySpace.maxItemsPerInstance", 1024, 0, 4096);
    public static final ModConfigSpec.IntValue MAX_MOBS = BUILDER
            .defineInRange("imaginarySpace.maxMobsPerInstance", 256, 0, 1024);
    public static final ModConfigSpec.IntValue MAX_PER_NAMESPACE = BUILDER
            .defineInRange("imaginarySpace.maxEntriesPerNamespace", 4096, 1, 8192);
    public static final ModConfigSpec.IntValue TASKS_PER_TICK = BUILDER
            .defineInRange("imaginarySpace.generationTasksPerTick", 64, 1, 128);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private ImaginarySpaceConfig() {
    }

    public static boolean allows(ResourceLocation id) {
        if (id == null) {
            return false;
        }
        String namespace = id.getNamespace();
        String fullId = id.toString();
        if (NAMESPACE_BLACKLIST.get().contains(namespace) || ID_BLACKLIST.get().contains(fullId)) {
            return false;
        }
        List<? extends String> namespaceWhitelist = NAMESPACE_WHITELIST.get();
        List<? extends String> idWhitelist = ID_WHITELIST.get();
        return namespaceWhitelist.isEmpty() && idWhitelist.isEmpty()
                || namespaceWhitelist.contains(namespace)
                || idWhitelist.contains(fullId);
    }

    public static int signature() {
        int result = NAMESPACE_BLACKLIST.get().hashCode();
        result = 31 * result + ID_BLACKLIST.get().hashCode();
        result = 31 * result + NAMESPACE_WHITELIST.get().hashCode();
        result = 31 * result + ID_WHITELIST.get().hashCode();
        return result;
    }

    private static boolean validNamespace(Object value) {
        return value instanceof String namespace
                && namespace.matches("[a-z0-9_.-]+");
    }

    private static boolean validId(Object value) {
        return value instanceof String id && ResourceLocation.tryParse(id) != null;
    }
}

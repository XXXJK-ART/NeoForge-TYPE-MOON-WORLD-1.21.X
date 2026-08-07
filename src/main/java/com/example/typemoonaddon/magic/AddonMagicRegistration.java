package com.example.typemoonaddon.magic;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.api.MagicDefinitionRegistry;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.IMagicExecutor;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.IMagicRegistry;
import net.xxxjk.typemoonworld.api.MagicDefinitionData;

public final class AddonMagicRegistration {
    private static final Set<String> REGISTERED_MAGIC_IDS = ConcurrentHashMap.newKeySet();

    private AddonMagicRegistration() {
    }

    /** Returns only magic IDs whose executor registration succeeded in this addon. */
    public static Set<String> registeredMagicIds() {
        return Set.copyOf(REGISTERED_MAGIC_IDS);
    }

    static boolean register(
            IMagicRegistry registry,
            String magicId,
            IMagicExecutor executor,
            String providerId,
            String category,
            String nameTranslationKey
    ) {
        boolean registered = registry.register(magicId, executor, providerId);
        if (!registered) {
            return false;
        }
        REGISTERED_MAGIC_IDS.add(magicId);
        ResourceLocation definitionId = magicId.indexOf(':') >= 0
                ? ResourceLocation.tryParse(magicId)
                : ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, magicId);
        if (definitionId != null) {
            ResourceLocation categoryId = ResourceLocation.fromNamespaceAndPath(
                    TYPE_MOON_WORLD.MOD_ID, category);
            ResourceLocation schoolId = ResourceLocation.fromNamespaceAndPath(
                    TYPE_MOON_WORLD.MOD_ID, "none");
            MagicDefinitionRegistry.register(new MagicDefinitionData(
                    definitionId,
                    nameTranslationKey,
                    categoryId,
                    schoolId,
                    0.0D,
                    0,
                    true,
                    true,
                    false,
                    false,
                    false,
                    0,
                    0
            ), providerId);
        }
        return registered;
    }
}

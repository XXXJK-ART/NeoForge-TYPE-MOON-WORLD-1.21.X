package com.example.typemoonaddon.magic;

import com.example.typemoonaddon.TypeMoonAddon;
import net.xxxjk.TYPE_MOON_WORLD.magic.MagicDisplayMetadata;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.IMagicAddonEntrypoint;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.IMagicRegistry;

public final class NegaSummonMagicEntrypoint implements IMagicAddonEntrypoint {
    @Override
    public String providerId() {
        return TypeMoonAddon.MOD_ID;
    }

    @Override
    public void registerMagics(IMagicRegistry registry) {
        boolean registered = AddonMagicRegistration.register(
                registry,
                NegaSummonMagic.MAGIC_ID,
                NegaSummonMagic::execute,
                providerId(),
                MagicDisplayMetadata.CATEGORY_SPECIAL,
                "magic.typemoonworld.nega_summon.name"
        );
        if (!registered) {
            TypeMoonAddon.LOGGER.error(
                    "Magic id '{}' is already registered; Nega Summon was not installed",
                    NegaSummonMagic.MAGIC_ID
            );
        }
    }
}

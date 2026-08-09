package com.example.typemoonaddon.magic;

import com.example.typemoonaddon.TypeMoonAddon;
import net.xxxjk.TYPE_MOON_WORLD.magic.MagicDisplayMetadata;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.IMagicAddonEntrypoint;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.IMagicRegistry;

public final class DemonGodGazeMagicEntrypoint implements IMagicAddonEntrypoint {
    @Override
    public String providerId() {
        return TypeMoonAddon.MOD_ID;
    }

    @Override
    public void registerMagics(IMagicRegistry registry) {
        boolean registered = AddonMagicRegistration.register(
                registry,
                DemonGodGazeMagic.MAGIC_ID,
                DemonGodGazeMagic::execute,
                providerId(),
                MagicDisplayMetadata.CATEGORY_SPECIAL,
                "magic.typemoonworld.demon_god_gaze.name"
        );
        if (!registered) {
            TypeMoonAddon.LOGGER.error(
                    "Magic id '{}' is already registered; Demon God Gaze was not installed",
                    DemonGodGazeMagic.MAGIC_ID
            );
        }
    }
}

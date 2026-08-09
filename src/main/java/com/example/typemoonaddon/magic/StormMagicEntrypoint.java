package com.example.typemoonaddon.magic;

import com.example.typemoonaddon.TypeMoonAddon;
import net.xxxjk.TYPE_MOON_WORLD.magic.MagicDisplayMetadata;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.IMagicAddonEntrypoint;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.IMagicRegistry;

public final class StormMagicEntrypoint implements IMagicAddonEntrypoint {
    @Override
    public String providerId() {
        return TypeMoonAddon.MOD_ID;
    }

    @Override
    public void registerMagics(IMagicRegistry registry) {
        boolean registered = AddonMagicRegistration.register(
                registry,
                StormMagic.MAGIC_ID,
                StormMagic::execute,
                providerId(),
                MagicDisplayMetadata.CATEGORY_ELEMENTAL,
                "magic.typemoonworld.storm.name"
        );
        StormMagic.setRegistered(registered);
        if (!registered) {
            TypeMoonAddon.LOGGER.error("Unable to register addon magic id '{}' because it is already occupied",
                    StormMagic.MAGIC_ID);
        }
    }
}

package com.example.typemoonaddon.magic;

import com.example.typemoonaddon.TypeMoonAddon;
import net.xxxjk.TYPE_MOON_WORLD.magic.MagicDisplayMetadata;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.IMagicAddonEntrypoint;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.IMagicRegistry;

public final class ImaginarySpaceMagicEntrypoint implements IMagicAddonEntrypoint {
    @Override
    public String providerId() {
        return TypeMoonAddon.MOD_ID;
    }

    @Override
    public void registerMagics(IMagicRegistry registry) {
        boolean registered = AddonMagicRegistration.register(
                registry,
                ImaginarySpaceMagic.MAGIC_ID,
                ImaginarySpaceMagic::execute,
                providerId(),
                MagicDisplayMetadata.CATEGORY_SPECIAL,
                "magic.typemoonworld.imaginary_space.name");
        if (!registered) {
            TypeMoonAddon.LOGGER.error(
                    "Magic id '{}' is already registered; Imaginary Space was not installed",
                    ImaginarySpaceMagic.MAGIC_ID);
        }
    }
}

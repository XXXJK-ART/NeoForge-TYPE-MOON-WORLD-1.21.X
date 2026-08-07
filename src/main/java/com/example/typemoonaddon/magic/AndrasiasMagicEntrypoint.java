package com.example.typemoonaddon.magic;

import com.example.typemoonaddon.TypeMoonAddon;
import net.xxxjk.TYPE_MOON_WORLD.magic.MagicDisplayMetadata;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.IMagicAddonEntrypoint;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.IMagicRegistry;

public final class AndrasiasMagicEntrypoint implements IMagicAddonEntrypoint {
    @Override
    public String providerId() {
        return TypeMoonAddon.MOD_ID;
    }

    @Override
    public void registerMagics(IMagicRegistry registry) {
        boolean registered = AddonMagicRegistration.register(
                registry,
                AndrasiasMagic.MAGIC_ID,
                AndrasiasMagic::execute,
                providerId(),
                MagicDisplayMetadata.CATEGORY_ELEMENTAL,
                "magic.typemoonworld.andrasias.name"
        );
        if (!registered) {
            TypeMoonAddon.LOGGER.error("Magic id '{}' is already registered; Andrasias was not installed",
                    AndrasiasMagic.MAGIC_ID);
        }
    }
}

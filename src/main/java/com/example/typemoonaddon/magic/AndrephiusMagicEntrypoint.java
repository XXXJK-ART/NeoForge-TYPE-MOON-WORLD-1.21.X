package com.example.typemoonaddon.magic;

import com.example.typemoonaddon.TypeMoonAddon;
import net.xxxjk.TYPE_MOON_WORLD.magic.MagicDisplayMetadata;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.IMagicAddonEntrypoint;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.IMagicRegistry;

public final class AndrephiusMagicEntrypoint implements IMagicAddonEntrypoint {
    @Override
    public String providerId() {
        return TypeMoonAddon.MOD_ID;
    }

    @Override
    public void registerMagics(IMagicRegistry registry) {
        AddonMagicRegistration.register(
                registry,
                AndrephiusMagic.MAGIC_ID,
                AndrephiusMagic::execute,
                providerId(),
                MagicDisplayMetadata.CATEGORY_ELEMENTAL,
                "magic.typemoonworld.andrephius.name"
        );
    }
}

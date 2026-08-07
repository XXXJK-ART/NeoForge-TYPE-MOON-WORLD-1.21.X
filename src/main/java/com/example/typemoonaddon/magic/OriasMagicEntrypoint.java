package com.example.typemoonaddon.magic;

import com.example.typemoonaddon.TypeMoonAddon;
import net.xxxjk.TYPE_MOON_WORLD.magic.MagicDisplayMetadata;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.IMagicAddonEntrypoint;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.IMagicRegistry;

public final class OriasMagicEntrypoint implements IMagicAddonEntrypoint {
    @Override
    public String providerId() {
        return TypeMoonAddon.MOD_ID;
    }

    @Override
    public void registerMagics(IMagicRegistry registry) {
        AddonMagicRegistration.register(
                registry,
                OriasMagic.MAGIC_ID,
                OriasMagic::execute,
                providerId(),
                MagicDisplayMetadata.CATEGORY_ELEMENTAL,
                "magic.typemoonworld.orias.name"
        );
    }
}

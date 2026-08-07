package com.example.typemoonaddon.magic;

import com.example.typemoonaddon.TypeMoonAddon;
import net.xxxjk.TYPE_MOON_WORLD.magic.MagicDisplayMetadata;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.IMagicAddonEntrypoint;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.IMagicRegistry;

public final class AbsorptionMagicEntrypoint implements IMagicAddonEntrypoint {
    @Override
    public String providerId() {
        return TypeMoonAddon.MOD_ID;
    }

    @Override
    public void registerMagics(IMagicRegistry registry) {
        AddonMagicRegistration.register(
                registry,
                AbsorptionMagic.MAGIC_ID,
                AbsorptionMagic::execute,
                providerId(),
                MagicDisplayMetadata.CATEGORY_SPECIAL,
                "magic.typemoonworld.absorption.name"
        );
    }
}

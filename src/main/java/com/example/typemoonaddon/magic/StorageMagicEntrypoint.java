package com.example.typemoonaddon.magic;

import com.example.typemoonaddon.TypeMoonAddon;
import net.xxxjk.TYPE_MOON_WORLD.magic.MagicDisplayMetadata;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.IMagicAddonEntrypoint;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.IMagicRegistry;

public final class StorageMagicEntrypoint implements IMagicAddonEntrypoint {
    @Override
    public String providerId() {
        return TypeMoonAddon.MOD_ID;
    }

    @Override
    public void registerMagics(IMagicRegistry registry) {
        AddonMagicRegistration.register(
                registry,
                StorageMagic.MAGIC_ID,
                StorageMagic::execute,
                providerId(),
                MagicDisplayMetadata.CATEGORY_SPECIAL,
                "magic.typemoonworld.storage.name"
        );
    }
}

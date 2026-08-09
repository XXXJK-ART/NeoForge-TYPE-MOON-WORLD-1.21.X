package com.example.typemoonaddon.magic;

import com.example.typemoonaddon.TypeMoonAddon;
import net.xxxjk.TYPE_MOON_WORLD.magic.MagicDisplayMetadata;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.IMagicAddonEntrypoint;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.IMagicRegistry;

public final class AirflowBladeMagicEntrypoint implements IMagicAddonEntrypoint {
    @Override
    public String providerId() {
        return TypeMoonAddon.MOD_ID;
    }

    @Override
    public void registerMagics(IMagicRegistry registry) {
        boolean registered = AddonMagicRegistration.register(
                registry,
                AirflowBladeMagic.MAGIC_ID,
                AirflowBladeMagic::execute,
                providerId(),
                MagicDisplayMetadata.CATEGORY_ELEMENTAL,
                "magic.typemoonworld.airflow_blade.name"
        );
        if (!registered) {
            TypeMoonAddon.LOGGER.error("Magic id '{}' is already registered; Airflow Blade was not installed",
                    AirflowBladeMagic.MAGIC_ID);
        }
    }
}

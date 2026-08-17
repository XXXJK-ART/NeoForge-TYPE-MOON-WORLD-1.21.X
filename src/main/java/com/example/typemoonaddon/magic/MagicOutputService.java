package com.example.typemoonaddon.magic;

import com.example.typemoonaddon.TypeMoonAddon;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;

public final class MagicOutputService {
    public static final ResourceKey<DamageType> DAMAGE_TYPE =
            ResourceKey.create(Registries.DAMAGE_TYPE, TypeMoonAddon.id("magic_output"));

    public static boolean isDamage(DamageSource source) {
        return source != null && source.is(DAMAGE_TYPE);
    }

    private MagicOutputService() {
    }
}

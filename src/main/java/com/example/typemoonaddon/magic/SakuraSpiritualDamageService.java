package com.example.typemoonaddon.magic;

import com.example.typemoonaddon.TypeMoonAddon;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;

public final class SakuraSpiritualDamageService {
    public static final ResourceKey<DamageType> COLLAPSE =
            ResourceKey.create(Registries.DAMAGE_TYPE, TypeMoonAddon.id("spiritual_collapse"));

    public static boolean isCollapse(DamageSource source) {
        return source != null && source.is(COLLAPSE);
    }

    private SakuraSpiritualDamageService() {
    }
}

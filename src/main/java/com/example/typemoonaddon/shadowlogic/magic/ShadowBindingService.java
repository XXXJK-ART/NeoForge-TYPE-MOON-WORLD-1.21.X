package com.example.typemoonaddon.shadowlogic.magic;

import com.example.typemoonaddon.magic.SakuraShadowBindingService;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

public final class ShadowBindingService {
    public static boolean isFriendlyDamage(LivingEntity target, DamageSource source) {
        return SakuraShadowBindingService.isFriendlyDamage(target, source);
    }

    private ShadowBindingService() {
    }
}

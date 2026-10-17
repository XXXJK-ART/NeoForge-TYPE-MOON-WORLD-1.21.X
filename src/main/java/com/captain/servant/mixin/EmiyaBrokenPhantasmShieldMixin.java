package com.captain.servant.mixin;

import com.captain.servant.emiya.EmiyaSupportService;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.xxxjk.TYPE_MOON_WORLD.magic.broken_phantasm.UBWBrokenPhantasmExplosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = UBWBrokenPhantasmExplosion.class, remap = false)
public abstract class EmiyaBrokenPhantasmShieldMixin {
    @Redirect(
            method = "processWave",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"
            )
    )
    private static boolean fatesupport$penetrateVanillaShield(
            LivingEntity target,
            DamageSource damageSource,
            float damage
    ) {
        return EmiyaSupportService.hurtBrokenPhantasmTarget(target, damageSource, damage);
    }
}

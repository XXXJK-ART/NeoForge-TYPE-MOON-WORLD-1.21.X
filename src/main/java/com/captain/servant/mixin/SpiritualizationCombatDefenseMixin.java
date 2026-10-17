package com.captain.servant.mixin;

import com.captain.servant.spiritual.SpiritualizationService;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatDefenseService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The current core defense system also needs the old addon's spirit-form restrictions. */
@Mixin(value = ServantCombatDefenseService.class, remap = false)
public abstract class SpiritualizationCombatDefenseMixin {
    @Inject(method = {"canAct", "trySkillAutoDodge", "isGuarding"}, at = @At("HEAD"), cancellable = true)
    private static void fatesupport$blockDefense(LivingEntity entity, CallbackInfoReturnable<Boolean> callback) {
        if (SpiritualizationService.isSpiritualized(entity)) {
            callback.setReturnValue(false);
        }
    }

    @Inject(method = "applyGuardHit", at = @At("HEAD"), cancellable = true)
    private static void fatesupport$blockGuardHit(LivingEntity entity, DamageSource source, float amount,
            CallbackInfoReturnable<Float> callback) {
        if (SpiritualizationService.isSpiritualized(entity)) {
            callback.setReturnValue(null);
        }
    }
}

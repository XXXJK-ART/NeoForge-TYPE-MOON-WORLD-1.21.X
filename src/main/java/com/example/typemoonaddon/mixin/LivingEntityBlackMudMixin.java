package com.example.typemoonaddon.mixin;

import com.example.typemoonaddon.magic.SakuraBlackMudService;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Gives grail-aligned entities a real collision surface on otherwise non-solid black mud. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityBlackMudMixin {
    @Inject(method = "canStandOnFluid", at = @At("HEAD"), cancellable = true)
    private void typemoonaddon$standOnBlackMud(
        FluidState fluidState,
        CallbackInfoReturnable<Boolean> callback
    ) {
        LivingEntity entity = (LivingEntity)(Object)this;
        if (SakuraBlackMudService.canWalkOnSurface(entity, fluidState)) {
            callback.setReturnValue(true);
        }
    }
}

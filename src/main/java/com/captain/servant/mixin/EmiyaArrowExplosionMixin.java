package com.captain.servant.mixin;

import com.captain.servant.emiya.EmiyaSupportService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.magic.broken_phantasm.UBWBrokenPhantasmExplosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = UBWBrokenPhantasmExplosion.class, remap = false)
public abstract class EmiyaArrowExplosionMixin {
    @Inject(
            method = "explode(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/phys/Vec3;F)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void fatesupport$explodeExcaliburArrow(
            Level level,
            Entity projectile,
            Entity owner,
            ItemStack payload,
            Vec3 center,
            float originalMultiplier,
            CallbackInfo callbackInfo
    ) {
        if (level instanceof ServerLevel serverLevel && EmiyaSupportService.isExcaliburArrowExplosion(owner, payload)) {
            EmiyaSupportService.explodeExcaliburArrow(serverLevel, projectile, owner, center);
            callbackInfo.cancel();
        }
    }
}

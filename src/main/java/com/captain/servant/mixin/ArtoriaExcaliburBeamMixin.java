package com.captain.servant.mixin;

import com.captain.servant.compat.superbwarfare.SuperbWarfareLoader;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.entity.ArtoriaExcaliburBeamEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.xxxjk.TYPE_MOON_WORLD.entity.ArtoriaExcaliburBeamEntity", remap = false)
public abstract class ArtoriaExcaliburBeamMixin {
    @Shadow
    public abstract Vec3 getEndPos();

    @Shadow
    public abstract double beamHalfWidth();

    @Shadow
    public abstract double beamHalfHeight();

    @Inject(
            method = "applyBeamDamage(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;)V",
            at = @At("TAIL")
    )
    private void fatesupport$damageVehiclesInBeam(
            ServerLevel level,
            LivingEntity owner,
            CallbackInfo callbackInfo
    ) {
        Entity beam = (Entity) (Object) this;
        SuperbWarfareLoader.damageVehiclesInBeam(
                level,
                beam,
                owner,
                beam.position(),
                getEndPos(),
                beamHalfWidth(),
                beamHalfHeight(),
                133.33333F
        );
    }

    @Inject(
            method = "damageCraterSlice(Lnet/xxxjk/TYPE_MOON_WORLD/entity/ArtoriaExcaliburBeamEntity;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/phys/Vec3;IIIIF)V",
            at = @At("TAIL")
    )
    private static void fatesupport$damageVehiclesInCrater(
            ArtoriaExcaliburBeamEntity beam,
            ServerLevel level,
            LivingEntity owner,
            Vec3 center,
            int radius,
            int halfHeight,
            int fromY,
            int batchHeight,
            float powerScale,
            CallbackInfo callbackInfo
    ) {
        Entity beamEntity = (Entity) (Object) beam;
        SuperbWarfareLoader.damageVehiclesInBox(
                level,
                new net.minecraft.world.phys.AABB(
                        center.x - radius,
                        center.y + fromY - 1.5D,
                        center.z - radius,
                        center.x + radius,
                        center.y + Math.min(halfHeight, fromY + batchHeight) + 1.5D,
                        center.z + radius
                ),
                owner,
                level.damageSources().explosion(beamEntity, owner),
                180.0F * powerScale
        );
    }
}

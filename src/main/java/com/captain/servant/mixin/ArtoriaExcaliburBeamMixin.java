package com.captain.servant.mixin;

import com.captain.servant.compat.superbwarfare.SuperbWarfareLoader;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.entity.ArtoriaExcaliburBeamEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.xxxjk.TYPE_MOON_WORLD.entity.ArtoriaExcaliburBeamEntity", remap = false)
public abstract class ArtoriaExcaliburBeamMixin {
    @Unique private boolean fatesupport$terminalVehiclesDamaged;
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
                ((ArtoriaExcaliburBeamEntity) (Object) this).pulseDamage()
        );
    }

    @Inject(
            method = "queueCrater(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/phys/Vec3;)V",
            at = @At("TAIL")
    )
    private void fatesupport$damageVehiclesInCrater(
            ServerLevel level,
            LivingEntity owner,
            Vec3 center,
            CallbackInfo callbackInfo
    ) {
        ArtoriaExcaliburBeamEntity beam = (ArtoriaExcaliburBeamEntity) (Object) this;
        if (fatesupport$terminalVehiclesDamaged || !beam.hasTerminalImpact() || beam.isGoldenVariant() || owner == null) return;
        fatesupport$terminalVehiclesDamaged = true;
        float powerScale = beam.impactPower();
        int radius = Math.max(6, (int)Math.floor(28 * (.25 + powerScale * .75)));
        int halfHeight = Math.max(18, (int)Math.floor(112 * (.22 + powerScale * .78)));
        Entity beamEntity = (Entity) (Object) beam;
        SuperbWarfareLoader.damageVehiclesInBox(
                level,
                new net.minecraft.world.phys.AABB(
                        center.x - radius,
                        center.y - halfHeight,
                        center.z - radius,
                        center.x + radius,
                        center.y + halfHeight,
                        center.z + radius
                ),
                owner,
                level.damageSources().explosion(beamEntity, owner),
                180.0F * powerScale
        );
    }
}

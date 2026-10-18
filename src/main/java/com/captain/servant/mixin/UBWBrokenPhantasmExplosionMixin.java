package com.captain.servant.mixin;

import com.captain.servant.compat.superbwarfare.SuperbWarfareLoader;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.xxxjk.TYPE_MOON_WORLD.magic.broken_phantasm.UBWBrokenPhantasmExplosion", remap = false)
public abstract class UBWBrokenPhantasmExplosionMixin {
    @Inject(
            method = "processWave(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/phys/Vec3;DDLnet/minecraft/world/damagesource/DamageSource;Lnet/minecraft/world/entity/Entity;Ljava/util/Set;FDZ)V",
            at = @At("TAIL")
    )
    private static void fatesupport$damageVehiclesInWave(
            ServerLevel level,
            Vec3 center,
            double currentRadius,
            double previousRadius,
            DamageSource source,
            Entity owner,
            Set<Integer> damagedEntities,
            float damage,
            double maximumRadius,
            boolean emiyaDistanceFalloff,
            CallbackInfo callbackInfo
    ) {
        SuperbWarfareLoader.damageVehiclesInAnnulus(
                level, center, currentRadius, previousRadius, owner, source, damage, damagedEntities
        );
    }
}

package com.captain.servant.mixin;

import com.captain.servant.compat.superbwarfare.SuperbWarfareLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity", remap = false)
public abstract class SuperbWarfareVehicleMixin {
    private static final String RENDER_MARKER = "\"FateSupportKnightOfOwner\":true";

    @Shadow(remap = false)
    public abstract String getOverride();

    @Inject(method = "canShoot", at = @At("HEAD"), cancellable = true, require = 1)
    private void fatesupport$disableRammingAircraftTrigger(CallbackInfoReturnable<Boolean> callbackInfo) {
        if (fatesupport$isRammingAircraft()) callbackInfo.setReturnValue(false);
    }

    @Inject(method = {
            "vehicleShoot(Lnet/minecraft/world/entity/LivingEntity;Ljava/lang/String;Lnet/minecraft/world/phys/Vec3;)V",
            "vehicleShoot(Lnet/minecraft/world/entity/LivingEntity;Ljava/lang/String;Ljava/util/UUID;Lnet/minecraft/world/phys/Vec3;)V",
            "vehicleShoot(Lnet/minecraft/world/entity/LivingEntity;Ljava/util/UUID;Lnet/minecraft/world/phys/Vec3;)V"
    }, at = @At("HEAD"), cancellable = true, require = 3)
    private void fatesupport$disableRammingAircraftWeapons(CallbackInfo callbackInfo) {
        if (fatesupport$isRammingAircraft()) callbackInfo.cancel();
    }

    private boolean fatesupport$isRammingAircraft() {
        Entity vehicle = (Entity) (Object) this;
        return vehicle.getPersistentData().getBoolean(SuperbWarfareLoader.VEHICLE_TAG)
                && vehicle.getPersistentData().getBoolean(SuperbWarfareLoader.VEHICLE_A10_TAG);
    }

    @Inject(method = "getMaxHealth", at = @At("RETURN"), cancellable = true, require = 0)
    private void fatesupport$doubleOwnerizedMaxHealth(CallbackInfoReturnable<Float> callbackInfo) {
        Entity vehicle = (Entity) (Object) this;
        if (fatesupport$isOwnerized(vehicle)) {
            float original = vehicle.getPersistentData().getFloat(
                    SuperbWarfareLoader.VEHICLE_ORIGINAL_HEALTH_TAG
            );
            callbackInfo.setReturnValue(original > 0.0F ? original * 2.0F : callbackInfo.getReturnValueF() * 2.0F);
        }
    }

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true, require = 0)
    private void fatesupport$allowTypeMoonDamage(
            DamageSource source,
            float amount,
            CallbackInfoReturnable<Boolean> callbackInfo
    ) {
        if (SuperbWarfareLoader.forceTypeMoonDamage((Entity) (Object) this, source, amount)) {
            callbackInfo.setReturnValue(true);
        }
    }

    @Inject(method = "positionRider", at = @At("HEAD"), cancellable = true, require = 0)
    private void fatesupport$positionA10Owner(Entity passenger, Entity.MoveFunction move, CallbackInfo callbackInfo) {
        Entity vehicle = (Entity) (Object) this;
        if (!fatesupport$isOwnerized(vehicle)
                || !"superbwarfare:a_10a".equals(
                        BuiltInRegistries.ENTITY_TYPE.getKey(vehicle.getType()).toString())
                || !fatesupport$isOwner(vehicle, passenger)) {
            return;
        }
        move.accept(passenger, vehicle.getX(), vehicle.getY() + vehicle.getBbHeight() + 0.35D, vehicle.getZ());
        callbackInfo.cancel();
    }

    private boolean fatesupport$isOwner(Entity vehicle, Entity passenger) {
        if (vehicle.getPersistentData().hasUUID(SuperbWarfareLoader.VEHICLE_OWNER_TAG)) {
            return passenger.getUUID().equals(
                    vehicle.getPersistentData().getUUID(SuperbWarfareLoader.VEHICLE_OWNER_TAG));
        }
        if (vehicle.getEntityData() == null) {
            return false;
        }
        String override = getOverride();
        return override != null && override.contains(
                "\"FateSupportKnightOwner\":\"" + passenger.getUUID() + "\""
        );
    }

    private boolean fatesupport$isOwnerized(Entity vehicle) {
        if (vehicle.getPersistentData().getBoolean(SuperbWarfareLoader.VEHICLE_TAG)) {
            return true;
        }
        // VehicleEntity calls getMaxHealth while its synced data is still being constructed.
        if (vehicle.getEntityData() == null) {
            return false;
        }
        String override = getOverride();
        return override != null && override.contains(RENDER_MARKER);
    }
}

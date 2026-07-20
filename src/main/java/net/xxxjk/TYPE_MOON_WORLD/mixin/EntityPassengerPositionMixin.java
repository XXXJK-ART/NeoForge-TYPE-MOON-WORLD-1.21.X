package net.xxxjk.TYPE_MOON_WORLD.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantMasterCarryService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityPassengerPositionMixin {
   @Inject(method = "positionRider(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity$MoveFunction;)V", at = @At("HEAD"), cancellable = true)
   private void typemoonworld$positionCarriedMaster(Entity passenger, Entity.MoveFunction callback, CallbackInfo ci) {
      Entity vehicle = (Entity)(Object)this;
      if (!(vehicle instanceof Player servant) || !ServantMasterCarryService.isCarryPair(servant, passenger)) return;
      Vec3 forward = servant.getLookAngle().multiply(1.0, 0.0, 1.0);
      if (forward.lengthSqr() < 1.0E-5) forward = new Vec3(0.0, 0.0, 1.0);
      forward = forward.normalize();
      Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
      Vec3 position = servant.position().add(forward.scale(0.72)).add(right.scale(0.12)).add(0.0, 0.42, 0.0);
      callback.accept(passenger, position.x, position.y, position.z);
      ci.cancel();
   }
}

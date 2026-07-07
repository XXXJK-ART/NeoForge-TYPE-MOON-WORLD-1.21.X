package net.xxxjk.TYPE_MOON_WORLD.mixin;

import net.minecraft.world.entity.player.Player;
import net.xxxjk.TYPE_MOON_WORLD.entity.MedusaPegasusEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.OdaMatchlockGunEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerPegasusDismountMixin {
   @Inject(
      method = "wantsToStopRiding",
      at = @At("HEAD"),
      cancellable = true
   )
   private void typemoonworld$keepRidingMedusaPegasus(CallbackInfoReturnable<Boolean> cir) {
      Player player = (Player)(Object)this;
      if (player.getVehicle() instanceof MedusaPegasusEntity || player.getVehicle() instanceof OdaMatchlockGunEntity gun && gun.isMountMode()) {
         cir.setReturnValue(false);
      }
   }
}

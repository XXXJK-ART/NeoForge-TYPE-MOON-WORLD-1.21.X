package net.xxxjk.TYPE_MOON_WORLD.mixin.client;

import net.minecraft.client.player.LocalPlayer;
import net.xxxjk.TYPE_MOON_WORLD.entity.ZhaoYunHakuryuEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vanilla clears the local Shift input while riding and waits for the server
 * to acknowledge a dismount. Hakuryu's secondary seat could therefore remain
 * visually attached for a tick or indefinitely when the server-side path had
 * already succeeded. Predict the same dismount locally; the server remains
 * authoritative and will reconcile invalid requests.
 */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerHakuryuDismountMixin {
   @Inject(method = "rideTick", at = @At("HEAD"))
   private void typemoonworld$predictHakuryuDismount(CallbackInfo ci) {
      LocalPlayer player = (LocalPlayer)(Object)this;
      if (!(player.getVehicle() instanceof ZhaoYunHakuryuEntity mount)) return;
      // A SetPassengers packet can arrive after the server has already
      // removed the player from the horse. Reconcile that stale local vehicle
      // relation even if the key was released before the packet arrived.
      if ((mount.tickCount > 5 && !mount.hasPassenger(player))
         || player.isShiftKeyDown() || player.input.shiftKeyDown) {
         player.stopRiding();
      }
   }
}

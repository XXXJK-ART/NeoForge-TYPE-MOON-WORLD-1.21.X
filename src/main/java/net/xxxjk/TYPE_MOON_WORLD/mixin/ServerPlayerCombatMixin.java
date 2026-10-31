package net.xxxjk.TYPE_MOON_WORLD.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantMasterTargeting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerCombatMixin {
   // IncomingDamage is too late: Player.hurt may already scale damage or reject it using the master's state.
   @com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod(method = "hurt")
   private boolean typemoonworld$routeMasterDamage(net.minecraft.world.damagesource.DamageSource source, float amount,
      com.llamalad7.mixinextras.injector.wrapoperation.Operation<Boolean> original) {
      return net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantMasterDamageRouter.hurt(
         (ServerPlayer)(Object)this, source, amount, () -> original.call(source, amount));
   }
   @Inject(method = "canHarmPlayer", at = @At("HEAD"), cancellable = true)
   private void typemoonworld$allowServantCardCombat(Player other, CallbackInfoReturnable<Boolean> cir) {
      if (ServantMasterTargeting.canServantCardPlayersHarm((ServerPlayer)(Object)this, other)) {
         cir.setReturnValue(true);
      }
   }
}

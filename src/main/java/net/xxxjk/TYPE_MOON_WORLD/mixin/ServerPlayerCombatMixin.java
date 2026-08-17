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
   @Inject(method = "canHarmPlayer", at = @At("HEAD"), cancellable = true)
   private void typemoonworld$allowServantCardCombat(Player other, CallbackInfoReturnable<Boolean> cir) {
      if (ServantMasterTargeting.canServantCardPlayersHarm((ServerPlayer)(Object)this, other)) {
         cir.setReturnValue(true);
      }
   }
}

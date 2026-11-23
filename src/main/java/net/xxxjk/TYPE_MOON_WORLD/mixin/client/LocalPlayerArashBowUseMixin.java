package net.xxxjk.TYPE_MOON_WORLD.mixin.client;

import net.minecraft.client.player.LocalPlayer;
import net.xxxjk.TYPE_MOON_WORLD.client.ServantCardSkillInputController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerArashBowUseMixin {
   @Inject(method = "stopUsingItem", at = @At("TAIL"))
   private void typemoonworld$clearRainUseOwnership(CallbackInfo ci) {
      ServantCardSkillInputController.onLocalItemUseStopped();
   }
}

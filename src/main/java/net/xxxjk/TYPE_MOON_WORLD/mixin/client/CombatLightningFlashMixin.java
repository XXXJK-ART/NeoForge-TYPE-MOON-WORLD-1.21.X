package net.xxxjk.TYPE_MOON_WORLD.mixin.client;

import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Vanilla has no event for suppressing one lightning bolt's scene-wide flash. */
@Mixin(LightningBolt.class)
public abstract class CombatLightningFlashMixin {
   @Redirect(method = "tick", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/world/level/Level;setSkyFlashTime(I)V"))
   private void typemoonworld$skipCombatSkyFlash(Level level, int ticks) {
      // Mod-created bolts use the synced no-gravity flag as a visual marker.
      // Lightning never moves under gravity, so this does not change damage,
      // fire, lifetime or the bolt itself. Natural lightning keeps its behavior.
      if (!((LightningBolt)(Object)this).isNoGravity()) {
         level.setSkyFlashTime(ticks);
      }
   }
}

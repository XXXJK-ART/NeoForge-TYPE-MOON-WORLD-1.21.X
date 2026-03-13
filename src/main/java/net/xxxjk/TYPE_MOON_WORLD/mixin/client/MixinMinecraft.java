package net.xxxjk.TYPE_MOON_WORLD.mixin.client;

import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Minecraft.class})
public abstract class MixinMinecraft {
   @Shadow
   @Nullable
   public LocalPlayer player;

   @Inject(
      method = {"setScreen"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onSetScreen(Screen screen, CallbackInfo ci) {
      if (screen instanceof ReceivingLevelScreen) {
         try {
            if (this.player != null && this.player.level() != null) {
               ResourceLocation currentDim = this.player.level().dimension().location();
               boolean currentlyInUBW = currentDim.toString().equals("typemoonworld:unlimited_blade_works");
               TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)this.player
                  .getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
               boolean chanting = vars.is_chanting_ubw;
               boolean inUBW = vars.is_in_ubw;
               if (currentlyInUBW || chanting || inUBW) {
                  ci.cancel();
               }
            }
         } catch (Exception var8) {
         }
      }
   }
}

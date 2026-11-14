package net.xxxjk.TYPE_MOON_WORLD.mixin.client;

import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import net.xxxjk.TYPE_MOON_WORLD.client.ServantCardSkillInputController;
import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.world.dimension.ModDimensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Minecraft.class})
public abstract class MixinMinecraft {
   @Shadow
   @Nullable
   public LocalPlayer player;

   // LivingEntityUseItemEvent.Stop still clears item use after cancellation in 1.21.1.
   // Keep only the skill-owned bow out of vanilla's RMB-release path, without changing key state.
   @Redirect(method = "handleKeybinds", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;releaseUsingItem(Lnet/minecraft/world/entity/player/Player;)V"))
   private void retainArrowRainBowUse(MultiPlayerGameMode gameMode, Player player) {
      if (!ServantCardSkillInputController.ownsArrowRainUse(player)) gameMode.releaseUsingItem(player);
   }

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
               boolean currentlyInUBW = ModDimensions.isUbwDimension(currentDim);
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

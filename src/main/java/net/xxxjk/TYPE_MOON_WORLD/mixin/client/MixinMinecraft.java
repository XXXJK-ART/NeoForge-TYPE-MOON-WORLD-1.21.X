package net.xxxjk.TYPE_MOON_WORLD.mixin.client;

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

import javax.annotation.Nullable;

@Mixin(Minecraft.class)
public abstract class MixinMinecraft {

    @Shadow @Nullable public LocalPlayer player;

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void onSetScreen(Screen screen, CallbackInfo ci) {
        // Check if the screen being set is the dirt loading screen
        if (screen instanceof ReceivingLevelScreen) {
            try {
                if (this.player != null && this.player.level() != null) {
                    ResourceLocation currentDim = this.player.level().dimension().location();
                    boolean currentlyInUBW = currentDim.toString().equals("typemoonworld:unlimited_blade_works");

                    TypeMoonWorldModVariables.PlayerVariables vars = this.player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
                    boolean chanting = vars.is_chanting_ubw;
                    boolean inUBW = vars.is_in_ubw;

                    // If we are exiting UBW (currentlyInUBW) or entering UBW (chanting or already flagged as in UBW), suppress the loading screen
                    if (currentlyInUBW || chanting || inUBW) {
                        // Cancel the setScreen call. 
                        // The previous screen (likely null/game view) will remain.
                        ci.cancel();
                    }
                }
            } catch (Exception e) {
                // Fallback: allow screen if error
            }
        }
    }
}

package com.captain.servant.mixin.client;

import com.captain.servant.client.SpiritualizationClient;
import com.captain.servant.client.TelepathyClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.xxxjk.TYPE_MOON_WORLD.client.screens.ServantCardHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ServantCardHud.class, remap = false)
public abstract class ServantCardHudMixin {
    @Inject(method = "onRenderGui", at = @At("HEAD"))
    private static void fatesupport$drawSpiritualization(RenderGuiEvent.Pre event, CallbackInfo callbackInfo) {
        SpiritualizationClient.drawHud(event.getGuiGraphics(), Minecraft.getInstance());
        TelepathyClient.drawHud(event.getGuiGraphics(), Minecraft.getInstance());
    }
}

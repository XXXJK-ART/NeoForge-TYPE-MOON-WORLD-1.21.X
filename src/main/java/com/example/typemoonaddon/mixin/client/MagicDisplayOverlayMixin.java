package com.example.typemoonaddon.mixin.client;

import com.example.typemoonaddon.client.TypeMoonClientModeBridge;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Slice;

@Mixin(targets = "net.xxxjk.TYPE_MOON_WORLD.client.screens.Magic_display_Overlay", remap = false)
public abstract class MagicDisplayOverlayMixin {
    @ModifyArg(
            method = "eventHandler",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)I",
                    ordinal = 1,
                    remap = true
            ),
            slice = @Slice(
                    from = @At(value = "CONSTANT", args = "stringValue=gui.typemoonworld.overlay.current_magic")
            ),
            index = 1
    )
    private static Component typemoonworld$appendProtectionMode(Component original) {
        if (!TypeMoonClientModeBridge.shouldShowProtectionSuffix()) {
            return original;
        }
        return original.copy().append(Component.translatable("label.typemoonworld.protection_suffix"));
    }
}

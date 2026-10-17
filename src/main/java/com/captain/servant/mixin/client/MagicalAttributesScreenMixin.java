package com.captain.servant.mixin.client;

import net.xxxjk.TYPE_MOON_WORLD.client.gui.Magical_attributes_Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Magical_attributes_Screen.class, remap = false)
public abstract class MagicalAttributesScreenMixin {
    @Shadow
    private void addMagic(String magicId, String nameKey, String categories, int color) {
        throw new AssertionError();
    }

    @Inject(method = "initMagicCatalog", at = @At("TAIL"))
    private void fatesupport$addMessageMagic(CallbackInfo callbackInfo) {
        addMagic(
                "fatesupport:message_magic",
                "magic.fatesupport.message_magic.name",
                "basic",
                0
        );
    }
}

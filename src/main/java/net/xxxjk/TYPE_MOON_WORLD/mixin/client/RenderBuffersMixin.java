package net.xxxjk.TYPE_MOON_WORLD.mixin.client;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.RenderType;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.ReinforcementRenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderBuffers.class)
public class RenderBuffersMixin {

    @Inject(method = "lambda$new$0", at = @At("TAIL"), require = 0)
    private static void addReinforcementGlintTypes(Object2ObjectLinkedOpenHashMap<RenderType, ByteBufferBuilder> map, RenderType type, CallbackInfo ci) {
        ReinforcementRenderType.addGlintTypes(map);
    }
}

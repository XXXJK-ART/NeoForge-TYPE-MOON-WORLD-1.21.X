package net.xxxjk.TYPE_MOON_WORLD.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.xxxjk.TYPE_MOON_WORLD.client.ServantCardConcealmentClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Optional compatibility for Neat's world-space health/name plate renderer. */
@Pseudo
@Mixin(targets = "vazkii.neat.HealthBarRenderer", remap = false)
public abstract class NeatHealthBarRendererMixin {
   @Inject(method = "hookRender", at = @At("HEAD"), cancellable = true, remap = false)
   private static void typemoonworld$hideConcealedPlate(
      Entity entity, PoseStack poseStack, MultiBufferSource buffers, Camera camera,
      EntityRenderer<? super Entity> renderer, float partialTick,
      double x, double y, double z, CallbackInfo ci
   ) {
      if (ServantCardConcealmentClient.isPerfectlyConcealed(entity)) ci.cancel();
   }
}

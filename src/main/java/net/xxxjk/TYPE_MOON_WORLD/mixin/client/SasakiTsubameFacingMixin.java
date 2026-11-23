package net.xxxjk.TYPE_MOON_WORLD.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.xxxjk.TYPE_MOON_WORLD.client.SasakiTsubameAfterimageClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** No render event exposes the body-yaw argument used by both player and NPC renderers. */
@Mixin(LivingEntityRenderer.class)
public abstract class SasakiTsubameFacingMixin {
   @ModifyVariable(method = "setupRotations", at = @At("HEAD"), argsOnly = true, ordinal = 1)
   private float tsubameFacing(float yaw, LivingEntity entity, PoseStack poses, float bob,
                               float bodyYaw, float partialTick, float scale) {
      return SasakiTsubameAfterimageClient.facingYaw(entity, yaw);
   }
}

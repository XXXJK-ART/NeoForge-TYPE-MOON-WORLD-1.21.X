package net.xxxjk.TYPE_MOON_WORLD.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.xxxjk.TYPE_MOON_WORLD.init.TypeMoonWorldModKeyMappings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ItemInHandRenderer.class})
public abstract class ItemInHandRendererMixin {
   @Invoker("renderPlayerArm")
   protected abstract void typemoonworld$renderPlayerArm(PoseStack var1, MultiBufferSource var2, int var3, float var4, float var5, HumanoidArm var6);

   @Inject(
      method = {"renderArmWithItem(Lnet/minecraft/client/player/AbstractClientPlayer;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void forceRenderEmptyCastingOffhandArm(
      AbstractClientPlayer player,
      float partialTicks,
      float pitch,
      InteractionHand hand,
      float swingProgress,
      ItemStack stack,
      float equippedProgress,
      PoseStack poseStack,
      MultiBufferSource buffer,
      int combinedLight,
      CallbackInfo ci
   ) {
      if (!player.isScoping() && stack.isEmpty() && hand == InteractionHand.OFF_HAND && !player.isInvisible()) {
         boolean ganderCharging = TypeMoonWorldModKeyMappings.KeyEventListener.isLocalGanderCharging();
         boolean gandrMachineGunCasting = TypeMoonWorldModKeyMappings.KeyEventListener.isLocalGandrMachineGunCasting();
         boolean machineGunFiringPose = TypeMoonWorldModKeyMappings.KeyEventListener.isLocalMachineGunFiringPoseActive();
         if (ganderCharging || gandrMachineGunCasting || machineGunFiringPose) {
            HumanoidArm offhandArm = player.getMainArm().getOpposite();
            HumanoidArm castingArm = TypeMoonWorldModKeyMappings.KeyEventListener.getLocalCastingArm();
            if (castingArm == offhandArm) {
               poseStack.pushPose();
               this.typemoonworld$renderPlayerArm(poseStack, buffer, combinedLight, equippedProgress, swingProgress, offhandArm);
               poseStack.popPose();
               ci.cancel();
            }
         }
      }
   }
}

package net.xxxjk.TYPE_MOON_WORLD.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.xxxjk.TYPE_MOON_WORLD.client.SasakiTsubameAfterimageClient;
import net.xxxjk.TYPE_MOON_WORLD.client.SasakiKojiroActionAnimationClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The vanilla hand layer has no event for an individual item's grip transform. */
@Mixin(ItemInHandLayer.class)
public abstract class SasakiTsubameHeldItemMixin<T extends LivingEntity, M extends EntityModel<T> & ArmedModel>
   extends RenderLayer<T, M> {
   protected SasakiTsubameHeldItemMixin(RenderLayerParent<T, M> parent) { super(parent); }

   @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
   private void tsubameGrip(LivingEntity entity, ItemStack stack, ItemDisplayContext display, HumanoidArm arm,
                            PoseStack poses, MultiBufferSource buffers, int light, CallbackInfo ci) {
      if (getParentModel() instanceof HumanoidModel<?> model
         && (SasakiTsubameAfterimageClient.renderHeldItem(entity, model, arm, stack, display, poses, buffers, light)
            || SasakiKojiroActionAnimationClient.renderImportedHeldItem(entity, model, arm, stack, display, poses, buffers, light)
            || net.xxxjk.TYPE_MOON_WORLD.client.DiarmuidActionAnimationClient.renderImportedHeldItem(entity, model, arm, stack, display, poses, buffers, light))) {
         ci.cancel();
      }
   }
}

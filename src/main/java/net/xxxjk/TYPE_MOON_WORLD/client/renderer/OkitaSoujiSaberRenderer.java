package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.xxxjk.TYPE_MOON_WORLD.client.model.OkitaSoujiSaberModel;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.OkitaSoujiSaberEntity;
import software.bernie.geckolib.cache.object.GeoBone;

public class OkitaSoujiSaberRenderer extends BaseServantRenderer<OkitaSoujiSaberEntity> {
   public OkitaSoujiSaberRenderer(Context renderManager) {
      super(renderManager, new OkitaSoujiSaberModel(), 0.95F);
   }

   @Override
   protected ItemStack getStackForBone(GeoBone bone, OkitaSoujiSaberEntity animatable) {
      return switch (bone.getName()) {
         case "bipedRightArm", "armorRightArm" -> animatable.getMainHandItem();
         case "bipedLeftArm", "armorLeftArm" -> animatable.getOffhandItem();
         default -> super.getStackForBone(bone, animatable);
      };
   }

   @Override
   protected ItemDisplayContext getTransformTypeForStack(GeoBone bone, ItemStack stack, OkitaSoujiSaberEntity animatable) {
      return bone.getName().contains("Left") ? ItemDisplayContext.THIRD_PERSON_LEFT_HAND : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
   }

   @Override
   protected void preRenderStackForBone(PoseStack poseStack, GeoBone bone, ItemStack stack, OkitaSoujiSaberEntity animatable) {
      super.preRenderStackForBone(poseStack, bone, stack, animatable);
      poseStack.translate(0.0, -0.12, 0.02);
   }
}

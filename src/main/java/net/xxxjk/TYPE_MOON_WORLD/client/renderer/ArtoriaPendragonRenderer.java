package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.world.item.ItemStack;
import net.xxxjk.TYPE_MOON_WORLD.client.model.ArtoriaPendragonModel;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ArtoriaPendragonCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ArtoriaPendragonEntity;
import software.bernie.geckolib.cache.object.GeoBone;

public class ArtoriaPendragonRenderer extends BaseServantRenderer<ArtoriaPendragonEntity> {
   public ArtoriaPendragonRenderer(Context renderManager) {
      super(renderManager, new ArtoriaPendragonModel(), 1.0F);
   }

   @Override
   protected ItemStack getStackForBone(GeoBone bone, ArtoriaPendragonEntity animatable) {
      if ("right arm".equals(bone.getName()) && !animatable.isExcaliburVisible()) {
         return ItemStack.EMPTY;
      }
      if ("left arm".equals(bone.getName()) && ArtoriaPendragonCombatHelper.hasAvalon(animatable)) {
         return new ItemStack(ModItems.AVALON.get());
      }
      return super.getStackForBone(bone, animatable);
   }

   @Override
   protected void preRenderStackForBone(PoseStack poseStack, GeoBone bone, ItemStack stack, ArtoriaPendragonEntity animatable) {
      if ("left arm".equals(bone.getName()) && stack.is(ModItems.AVALON.get())) {
         poseStack.translate(-0.08, -0.74, 0.03);
         poseStack.mulPose(Axis.XP.rotationDegrees(-92.0F));
         poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
         poseStack.mulPose(Axis.ZP.rotationDegrees(12.0F));
         poseStack.scale(0.86F, 0.86F, 0.86F);
         return;
      }
      super.preRenderStackForBone(poseStack, bone, stack, animatable);
   }
}

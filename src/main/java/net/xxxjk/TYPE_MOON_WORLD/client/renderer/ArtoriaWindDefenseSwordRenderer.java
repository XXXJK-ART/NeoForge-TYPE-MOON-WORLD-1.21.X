package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.nbt.CompoundTag;
import net.xxxjk.TYPE_MOON_WORLD.entity.ArtoriaWindDefenseSwordEntity;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;

public final class ArtoriaWindDefenseSwordRenderer extends EntityRenderer<ArtoriaWindDefenseSwordEntity> {
   public ArtoriaWindDefenseSwordRenderer(EntityRendererProvider.Context context) {
      super(context);
   }

   @Override
   public void render(ArtoriaWindDefenseSwordEntity entity, float entityYaw, float partialTicks, PoseStack poses,
                      MultiBufferSource buffers, int packedLight) {
      poses.pushPose();
      poses.mulPose(Axis.YP.rotationDegrees(entity.getYRot()));
      poses.mulPose(Axis.ZP.rotationDegrees(-90.0F));
      poses.translate(0.0, 0.72, 0.0);
      poses.scale(1.15F, 1.15F, 1.15F);
      ItemStack displayStack = new ItemStack(ModItems.EXCALIBUR.get());
      CompoundTag state = new CompoundTag();
      state.putBoolean("ServantCardArtoriaWindVeiled", true);
      displayStack.set(DataComponents.CUSTOM_DATA, CustomData.of(state));
      Minecraft.getInstance().getItemRenderer().renderStatic(displayStack,
         ItemDisplayContext.GROUND, 15728880, OverlayTexture.NO_OVERLAY, poses, buffers, entity.level(), entity.getId());
      poses.popPose();
      super.render(entity, entityYaw, partialTicks, poses, buffers, packedLight);
   }

   @Override
   public ResourceLocation getTextureLocation(ArtoriaWindDefenseSwordEntity entity) {
      return InventoryMenu.BLOCK_ATLAS;
   }
}

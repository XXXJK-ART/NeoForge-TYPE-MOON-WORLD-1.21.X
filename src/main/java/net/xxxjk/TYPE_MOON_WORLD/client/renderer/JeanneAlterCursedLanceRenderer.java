package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.servant.jeanne.JeanneAlterCursedLanceEntity;

public final class JeanneAlterCursedLanceRenderer extends EntityRenderer<JeanneAlterCursedLanceEntity> {
   private final ItemRenderer itemRenderer;
   public JeanneAlterCursedLanceRenderer(EntityRendererProvider.Context context) { super(context); itemRenderer = context.getItemRenderer(); shadowRadius = 0.0F; }
   @Override public void render(JeanneAlterCursedLanceEntity entity, float yaw, float partial, PoseStack pose, MultiBufferSource buffer, int light) {
      pose.pushPose(); pose.mulPose(Axis.YP.rotationDegrees(entity.getYRot())); pose.mulPose(Axis.ZP.rotationDegrees(entity.getXRot()));
      float scale = 1.45F * entity.modelScale(); pose.scale(scale, scale, scale);
      itemRenderer.renderStatic(new ItemStack(ModItems.JEANNE_ALTER_CURSED_LANCE.get()), ItemDisplayContext.NONE, 15728880, OverlayTexture.NO_OVERLAY, pose, buffer, entity.level(), entity.getId());
      pose.popPose(); super.render(entity, yaw, partial, pose, buffer, light);
   }
   @Override public ResourceLocation getTextureLocation(JeanneAlterCursedLanceEntity entity) { return InventoryMenu.BLOCK_ATLAS; }
}

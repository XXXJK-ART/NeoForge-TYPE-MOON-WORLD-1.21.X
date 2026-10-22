package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.xxxjk.TYPE_MOON_WORLD.entity.CuSpearEntity;

public final class CuSpearRenderer extends EntityRenderer<CuSpearEntity> {
   private final GaeBulgRenderer item=new GaeBulgRenderer();
   public CuSpearRenderer(EntityRendererProvider.Context context){super(context);}
   @Override public void render(CuSpearEntity spear,float yaw,float partial,PoseStack poses,MultiBufferSource buffers,int light) {
      poses.pushPose();poses.mulPose(Axis.YP.rotationDegrees(spear.getYRot()));poses.mulPose(Axis.XP.rotationDegrees(-spear.getXRot()));poses.mulPose(Axis.YP.rotationDegrees(180));
      item.renderByItem(spear.appearance(),ItemDisplayContext.NONE,poses,buffers,light,OverlayTexture.NO_OVERLAY);poses.popPose();
      if(spear.state()!=1 && spear.trail.size()>1)ProjectileVisualEffectHelper.renderRibbonTrail(spear.trail,spear.getPosition(partial),entityRenderDispatcher.camera.getPosition(),poses,buffers,ResourceLocation.withDefaultNamespace("textures/entity/beacon_beam.png"),spear.state()==2?.25F:.15F,0xD50920,1);
      super.render(spear,yaw,partial,poses,buffers,light);
   }
   @Override public ResourceLocation getTextureLocation(CuSpearEntity spear){return InventoryMenu.BLOCK_ATLAS;}
}

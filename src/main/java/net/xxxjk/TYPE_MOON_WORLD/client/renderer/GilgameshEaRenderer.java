package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.util.Mth;
import net.xxxjk.TYPE_MOON_WORLD.client.model.GilgameshEaModel;
import net.xxxjk.TYPE_MOON_WORLD.entity.GilgameshEaBeamEntity;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class GilgameshEaRenderer extends GeoEntityRenderer<GilgameshEaBeamEntity> {
   public GilgameshEaRenderer(Context context) { super(context, new GilgameshEaModel()); }
   @Override public void render(GilgameshEaBeamEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
      pose.pushPose();
      var direction=e.beamEnd().subtract(e.beamStart()).normalize();
      float ryaw=(float)(Mth.atan2(direction.x,direction.z)*180.0/Math.PI);
      float pitch=(float)(Mth.atan2(direction.y,direction.horizontalDistance())*180.0/Math.PI);
      pose.mulPose(Axis.YP.rotationDegrees(ryaw)); pose.mulPose(Axis.XP.rotationDegrees(-pitch)); pose.mulPose(Axis.ZP.rotationDegrees(90));
      pose.scale(2.8F,2.8F,2.8F);
      super.render(e,yaw,partial,pose,buffers,light); pose.popPose();
   }
}

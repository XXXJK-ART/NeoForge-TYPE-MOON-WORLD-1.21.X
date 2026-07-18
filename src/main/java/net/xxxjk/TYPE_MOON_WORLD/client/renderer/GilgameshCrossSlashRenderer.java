package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.util.Mth;
import net.xxxjk.TYPE_MOON_WORLD.client.model.GilgameshCrossSlashModel;
import net.xxxjk.TYPE_MOON_WORLD.entity.GilgameshCrossSlashEntity;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class GilgameshCrossSlashRenderer extends GeoEntityRenderer<GilgameshCrossSlashEntity> {
   public GilgameshCrossSlashRenderer(Context context) { super(context, new GilgameshCrossSlashModel()); }
   @Override public void render(GilgameshCrossSlashEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
      pose.pushPose();
      var direction = e.getSlashDirection();
      float ryaw = (float)(Mth.atan2(direction.x, direction.z) * 180.0 / Math.PI);
      pose.mulPose(Axis.YP.rotationDegrees(ryaw));
      pose.mulPose(Axis.ZP.rotationDegrees(e.getSlashType() == GilgameshCrossSlashEntity.SlashType.IGALIMA ? 45.0F : -45.0F));
      pose.translate(0.0, 1.5, -25.0);
      pose.scale(14.0F, 14.0F, 14.0F);
      super.render(e, yaw, partial, pose, buffers, light);
      pose.popPose();
   }
}

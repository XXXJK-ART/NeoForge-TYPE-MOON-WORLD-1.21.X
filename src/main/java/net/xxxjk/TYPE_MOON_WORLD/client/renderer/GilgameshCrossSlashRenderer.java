package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.util.Mth;
import net.xxxjk.TYPE_MOON_WORLD.client.model.GilgameshCrossSlashModel;
import net.xxxjk.TYPE_MOON_WORLD.entity.GilgameshCrossSlashEntity;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.util.Color;

public class GilgameshCrossSlashRenderer extends GeoEntityRenderer<GilgameshCrossSlashEntity> {
   public GilgameshCrossSlashRenderer(Context context) { super(context, new GilgameshCrossSlashModel()); }
   @Override public void render(GilgameshCrossSlashEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
      pose.pushPose();
      var direction = e.getSlashDirection();
      float ryaw = (float)(Mth.atan2(direction.x, direction.z) * 180.0 / Math.PI);
      float age = e.tickCount + partial;
      float appear = smooth(age / GilgameshCrossSlashEntity.MANIFEST_TICKS);
      float scale = (e.getSlashType() == GilgameshCrossSlashEntity.SlashType.IGALIMA ? 14.45F : 4.51F)
         * Math.max(0.001F, appear);
      pose.mulPose(Axis.YP.rotationDegrees(ryaw));
      pose.translate(0.0, -2.0, 0.0);
      pose.mulPose(Axis.XP.rotationDegrees(70.0F));
      pose.mulPose(Axis.ZP.rotationDegrees(e.getSlashType() == GilgameshCrossSlashEntity.SlashType.IGALIMA ? 45.0F : -45.0F));
      pose.scale(scale, scale, scale);
      super.render(e, yaw, partial, pose, buffers, light);
      pose.popPose();
   }

   @Override
   public Color getRenderColor(GilgameshCrossSlashEntity entity, float partialTick, int packedLight) {
      int alpha = Mth.clamp(Math.round(entity.getModelAlpha(partialTick) * 255.0F), 0, 255);
      return Color.ofARGB(alpha, 255, 255, 255);
   }

   private static float smooth(float value) {
      float clamped = Mth.clamp(value, 0.0F, 1.0F);
      return clamped * clamped * (3.0F - 2.0F * clamped);
   }
}

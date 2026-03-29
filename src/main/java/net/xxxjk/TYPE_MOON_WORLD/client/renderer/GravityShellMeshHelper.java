package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;

public final class GravityShellMeshHelper {
   private static final int STACKS = 14;
   private static final int SLICES = 28;
   private static final float HALF_PI = (float)(Math.PI * 0.5);
   private static final float LIGHT_X = -0.35F;
   private static final float LIGHT_Y = 0.91F;
   private static final float LIGHT_Z = -0.21F;

   private GravityShellMeshHelper() {
   }

   public static void drawUpperHemisphere(Pose pose, VertexConsumer consumer, float radiusXZ, float radiusY, float red, float green, float blue, float alpha) {
      for (int stack = 0; stack < STACKS; stack++) {
         float v0 = (float)stack / (float)STACKS;
         float v1 = (float)(stack + 1) / (float)STACKS;
         float elevation0 = v0 * HALF_PI;
         float elevation1 = v1 * HALF_PI;
         float ringRadius0 = Mth.cos(elevation0) * radiusXZ;
         float ringRadius1 = Mth.cos(elevation1) * radiusXZ;
         float y0 = Mth.sin(elevation0) * radiusY;
         float y1 = Mth.sin(elevation1) * radiusY;

         for (int slice = 0; slice < SLICES; slice++) {
            float u0 = (float)slice / (float)SLICES;
            float u1 = (float)(slice + 1) / (float)SLICES;
            float angle0 = u0 * Mth.TWO_PI;
            float angle1 = u1 * Mth.TWO_PI;

            addVertex(pose, consumer, Mth.cos(angle0) * ringRadius0, y0, Mth.sin(angle0) * ringRadius0, u0, v0, radiusXZ, radiusY, red, green, blue, alpha);
            addVertex(pose, consumer, Mth.cos(angle1) * ringRadius0, y0, Mth.sin(angle1) * ringRadius0, u1, v0, radiusXZ, radiusY, red, green, blue, alpha);
            addVertex(pose, consumer, Mth.cos(angle1) * ringRadius1, y1, Mth.sin(angle1) * ringRadius1, u1, v1, radiusXZ, radiusY, red, green, blue, alpha);
            addVertex(pose, consumer, Mth.cos(angle0) * ringRadius1, y1, Mth.sin(angle0) * ringRadius1, u0, v1, radiusXZ, radiusY, red, green, blue, alpha);
         }
      }
   }

   private static void addVertex(
      Pose pose, VertexConsumer consumer, float x, float y, float z, float u, float v, float radiusXZ, float radiusY, float red, float green, float blue, float alpha
   ) {
      float normalX = radiusXZ <= 1.0E-4F ? 0.0F : x / radiusXZ;
      float normalY = radiusY <= 1.0E-4F ? 1.0F : y / radiusY;
      float normalZ = radiusXZ <= 1.0E-4F ? 0.0F : z / radiusXZ;
      float normalLength = Mth.sqrt(normalX * normalX + normalY * normalY + normalZ * normalZ);
      if (normalLength <= 1.0E-4F) {
         normalX = 0.0F;
         normalY = 1.0F;
         normalZ = 0.0F;
      } else {
         normalX /= normalLength;
         normalY /= normalLength;
         normalZ /= normalLength;
      }

      float lightDot = Math.max(0.0F, normalX * LIGHT_X + normalY * LIGHT_Y + normalZ * LIGHT_Z);
      float horizon = 1.0F - normalY;
      float rimBoost = (float)Math.pow(horizon, 1.55);
      float brightness = 0.42F + lightDot * 0.34F + rimBoost * 0.19F;
      float alphaBoost = 0.9F + rimBoost * 0.08F;
      consumer.addVertex(pose, x, y, z)
         .setColor(
            Mth.clamp(red * brightness, 0.0F, 1.0F),
            Mth.clamp(green * brightness, 0.0F, 1.0F),
            Mth.clamp(blue * brightness, 0.0F, 1.0F),
            Mth.clamp(alpha * alphaBoost, 0.0F, 1.0F)
         )
         .setUv(u, v)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(15728880)
         .setNormal(pose, normalX, normalY, normalZ);
   }
}

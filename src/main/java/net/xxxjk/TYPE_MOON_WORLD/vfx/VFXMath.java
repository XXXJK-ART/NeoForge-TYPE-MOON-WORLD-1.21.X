package net.xxxjk.TYPE_MOON_WORLD.vfx;

import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class VFXMath {
   private VFXMath() {
   }

   public static Vector3f cubicBezier(Vector3f p0, Vector3f p1, Vector3f p2, Vector3f p3, float t, Vector3f out) {
      float u = 1.0F - t;
      float b0 = u * u * u;
      float b1 = 3.0F * u * u * t;
      float b2 = 3.0F * u * t * t;
      float b3 = t * t * t;
      return out.set(
         b0 * p0.x + b1 * p1.x + b2 * p2.x + b3 * p3.x,
         b0 * p0.y + b1 * p1.y + b2 * p2.y + b3 * p3.y,
         b0 * p0.z + b1 * p1.z + b2 * p2.z + b3 * p3.z
      );
   }

   public static Vector3f sphericalToCartesian(float radius, float theta, float phi, Vector3f out) {
      float sinPhi = Mth.sin(phi);
      return out.set(radius * sinPhi * Mth.cos(theta), radius * Mth.cos(phi), radius * sinPhi * Mth.sin(theta));
   }

   public static float noise3D(float x, float y, float z) {
      int xi = Mth.floor(x);
      int yi = Mth.floor(y);
      int zi = Mth.floor(z);
      float xf = x - xi;
      float yf = y - yi;
      float zf = z - zi;
      float u = fade(xf);
      float v = fade(yf);
      float w = fade(zf);
      float x00 = Mth.lerp(u, grad(hash(xi, yi, zi), xf, yf, zf), grad(hash(xi + 1, yi, zi), xf - 1.0F, yf, zf));
      float x10 = Mth.lerp(u, grad(hash(xi, yi + 1, zi), xf, yf - 1.0F, zf), grad(hash(xi + 1, yi + 1, zi), xf - 1.0F, yf - 1.0F, zf));
      float x01 = Mth.lerp(u, grad(hash(xi, yi, zi + 1), xf, yf, zf - 1.0F), grad(hash(xi + 1, yi, zi + 1), xf - 1.0F, yf, zf - 1.0F));
      float x11 = Mth.lerp(u, grad(hash(xi, yi + 1, zi + 1), xf, yf - 1.0F, zf - 1.0F), grad(hash(xi + 1, yi + 1, zi + 1), xf - 1.0F, yf - 1.0F, zf - 1.0F));
      return Mth.lerp(w, Mth.lerp(v, x00, x10), Mth.lerp(v, x01, x11));
   }

   public static Quaternionf slerp(Quaternionf a, Quaternionf b, float t, Quaternionf out) {
      return out.set(a).slerp(b, t);
   }

   private static float fade(float t) {
      return t * t * t * (t * (t * 6.0F - 15.0F) + 10.0F);
   }

   private static int hash(int x, int y, int z) {
      int h = x * 374761393 + y * 668265263 + z * 2147483647;
      h = (h ^ h >> 13) * 1274126177;
      return h ^ h >> 16;
   }

   private static float grad(int hash, float x, float y, float z) {
      return switch (hash & 15) {
         case 0 -> x + y;
         case 1 -> -x + y;
         case 2 -> x - y;
         case 3 -> -x - y;
         case 4 -> x + z;
         case 5 -> -x + z;
         case 6 -> x - z;
         case 7 -> -x - z;
         case 8 -> y + z;
         case 9 -> -y + z;
         case 10 -> y - z;
         case 11 -> -y - z;
         case 12 -> x + y;
         case 13 -> -x + y;
         case 14 -> -y + z;
         default -> -y - z;
      };
   }
}

package net.xxxjk.TYPE_MOON_WORLD.vfx.component.surface;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.List;
import javax.imageio.ImageIO;
import net.minecraft.util.Mth;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXParticle;

public class ImageMaskSurface extends AbstractSurfaceComponent {
   private final float width;
   private final float height;
   private final String texture;
   private final float threshold;
   private final Reveal reveal;
   private final BufferedImage image;

   public ImageMaskSurface(String texture, float width, float height, int uSegments, int vSegments, float threshold) {
      this(texture, width, height, uSegments, vSegments, threshold, Reveal.NONE);
   }

   public ImageMaskSurface(String texture, float width, float height, int uSegments, int vSegments, float threshold, Reveal reveal) {
      super(uSegments, vSegments, 0.0F);
      this.texture = texture;
      this.width = width;
      this.height = height;
      this.threshold = threshold;
      this.reveal = reveal == null ? Reveal.NONE : reveal;
      this.image = loadImage(texture);
   }

   @Override
   public void update(float deltaTime, float lifeProgress, List<VFXParticle> out) {
      for (int u = 0; u < this.uSegments; u++) {
         float x = (u / (float)(this.uSegments - 1) - 0.5F) * this.width;
         for (int v = 0; v < this.vSegments; v++) {
            float revealT = v / (float)(this.vSegments - 1);
            if (!isRevealed(revealT, lifeProgress)) {
               continue;
            }
            float y = (v / (float)(this.vSegments - 1)) * this.height;
            float mask = sampleMask(x / this.width + 0.5F, y / this.height);
            if (mask >= this.threshold) {
               sample(out, x, y, 0.0F);
            }
         }
      }
   }

   private boolean isRevealed(float pointProgress, float lifeProgress) {
      return switch (this.reveal) {
         case NONE -> true;
         case BOTTOM_TO_TOP -> pointProgress <= lifeProgress;
         case BOTTOM_TO_TOP_THEN_TOP_TO_BOTTOM -> lifeProgress < 0.5F
            ? pointProgress <= lifeProgress * 2.0F
            : pointProgress <= 1.0F - (lifeProgress - 0.5F) * 2.0F;
         case TOP_TO_BOTTOM -> pointProgress >= 1.0F - lifeProgress;
         case CENTER_OUT -> Math.abs(pointProgress - 0.5F) <= lifeProgress * 0.5F;
      };
   }

   private float sampleMask(float u, float v) {
      if (this.image != null) {
         int x = Mth.clamp((int)(u * (this.image.getWidth() - 1)), 0, this.image.getWidth() - 1);
         int y = Mth.clamp((int)((1.0F - v) * (this.image.getHeight() - 1)), 0, this.image.getHeight() - 1);
         int argb = this.image.getRGB(x, y);
         return ((argb >>> 24) & 255) / 255.0F;
      }
      float cx = u - 0.5F;
      float cy = v - 0.5F;
      float trunk = 1.0F - Mth.clamp(Math.abs(cx) * 4.5F, 0.0F, 1.0F);
      float canopy = 1.0F - Mth.clamp((Math.abs(cx) * 2.2F + Math.max(0.0F, cy - 0.15F) * 3.2F), 0.0F, 1.0F);
      float root = 1.0F - Mth.clamp((Math.abs(cx) * 7.0F + Math.max(0.0F, 0.30F - cy) * 8.0F), 0.0F, 1.0F);
      float total = Math.max(trunk, Math.max(canopy * (cy < 0.45F ? 1.0F : 0.0F), root));
      if (cy > 0.86F) {
         total *= 1.0F - Mth.clamp((cy - 0.86F) * 8.0F, 0.0F, 1.0F);
      }
      return total;
   }

   private static BufferedImage loadImage(String texture) {
      if (texture == null || texture.isBlank()) {
         return null;
      }
      String path = texture;
      int colon = path.indexOf(':');
      if (colon >= 0) {
         path = "assets/" + path.substring(0, colon) + "/" + path.substring(colon + 1);
      }
      try (InputStream stream = ImageMaskSurface.class.getClassLoader().getResourceAsStream(path)) {
         return stream == null ? null : ImageIO.read(stream);
      } catch (Exception ignored) {
         return null;
      }
   }

   public enum Reveal {
      NONE,
      BOTTOM_TO_TOP,
      BOTTOM_TO_TOP_THEN_TOP_TO_BOTTOM,
      TOP_TO_BOTTOM,
      CENTER_OUT
   }
}

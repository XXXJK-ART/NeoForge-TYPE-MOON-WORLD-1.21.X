package net.xxxjk.TYPE_MOON_WORLD.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

public class RuneSigilParticle extends TextureSheetParticle {
   private final SpriteSet sprites;

   protected RuneSigilParticle(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed,
                               SpriteSet sprites, float red, float green, float blue, float quadScale, int lifetime) {
      super(level, x, y, z, xSpeed, ySpeed, zSpeed);
      this.sprites = sprites;
      this.friction = 0.92F;
      this.gravity = 0.0F;
      this.speedUpWhenYMotionIsBlocked = false;
      this.quadSize *= quadScale;
      this.lifetime = lifetime;
      this.rCol = red;
      this.gCol = green;
      this.bCol = blue;
      this.alpha = 0.95F;
      this.hasPhysics = false;
      this.pickSprite(sprites);
   }

   @Override
   public void tick() {
      super.tick();
      if (!this.removed) {
         this.setSpriteFromAge(this.sprites);
         this.alpha = Math.max(0.0F, 1.0F - (float)this.age / (float)this.lifetime);
      }
   }

   @Override
   public ParticleRenderType getRenderType() {
      return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
   }

   @Override
   public int getLightColor(float partialTick) {
      return 240;
   }

   public static class Provider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;
      private final float red;
      private final float green;
      private final float blue;
      private final float quadScale;
      private final int lifetime;
      private final float alpha;

      public Provider(SpriteSet sprites, float red, float green, float blue, float quadScale, int lifetime) {
         this(sprites, red, green, blue, quadScale, lifetime, 0.95F);
      }

      public Provider(SpriteSet sprites, float red, float green, float blue, float quadScale, int lifetime, float alpha) {
         this.sprites = sprites;
         this.red = red;
         this.green = green;
         this.blue = blue;
         this.quadScale = quadScale;
         this.lifetime = lifetime;
         this.alpha = alpha;
      }

      @Override
      public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                     double xSpeed, double ySpeed, double zSpeed) {
         RuneSigilParticle particle = new RuneSigilParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites, this.red, this.green, this.blue, this.quadScale, this.lifetime);
         particle.alpha = this.alpha;
         return particle;
      }
   }
}

package net.xxxjk.TYPE_MOON_WORLD.vfx.client;

import org.joml.Vector3f;

public class VFXRingShockwave {
   public final Vector3f center = new Vector3f();
   public int color;
   public float startRadius;
   public float endRadius;
   public float age;
   public float lifetime;

   public VFXRingShockwave(Vector3f center, int color, float startRadius, float endRadius, float lifetime) {
      this.center.set(center);
      this.color = color;
      this.startRadius = startRadius;
      this.endRadius = endRadius;
      this.lifetime = Math.max(0.001F, lifetime);
   }

   public boolean tick(float deltaTime) {
      this.age += deltaTime;
      return this.age < this.lifetime;
   }

   public float progress() {
      return Math.min(1.0F, this.age / this.lifetime);
   }

   public float alpha() {
      return Math.max(0.0F, 1.0F - progress());
   }

   public float radius() {
      return this.startRadius + (this.endRadius - this.startRadius) * progress();
   }
}

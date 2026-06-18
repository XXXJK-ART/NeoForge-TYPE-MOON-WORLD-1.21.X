package net.xxxjk.TYPE_MOON_WORLD.vfx.client;

import org.joml.Vector3f;

public class VFXBeam {
   public final Vector3f start = new Vector3f();
   public final Vector3f end = new Vector3f();
   public int color;
   public float widthStart;
   public float widthEnd;
   public float age;
   public float lifetime;

   public VFXBeam(Vector3f start, Vector3f end, int color, float widthStart, float widthEnd, float lifetime) {
      this.start.set(start);
      this.end.set(end);
      this.color = color;
      this.widthStart = widthStart;
      this.widthEnd = widthEnd;
      this.lifetime = Math.max(0.001F, lifetime);
   }

   public boolean tick(float deltaTime) {
      this.age += deltaTime;
      return this.age < this.lifetime;
   }

   public float alpha() {
      return Math.max(0.0F, 1.0F - this.age / this.lifetime);
   }
}

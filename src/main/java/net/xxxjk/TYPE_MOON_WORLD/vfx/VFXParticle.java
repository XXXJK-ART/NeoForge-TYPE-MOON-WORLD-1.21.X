package net.xxxjk.TYPE_MOON_WORLD.vfx;

import java.util.ArrayDeque;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;

public class VFXParticle {
   private static final int INITIAL_POOL_SIZE = 4096;
   private static final int MAX_POOL_SIZE = 16384;
   private static final ArrayDeque<VFXParticle> POOL = new ArrayDeque<>(INITIAL_POOL_SIZE);

   static {
      for (int i = 0; i < INITIAL_POOL_SIZE; i++) {
         POOL.addLast(new VFXParticle());
      }
   }

   public final Vector3f position = new Vector3f();
   public final Vector3f previousPosition = new Vector3f();
   public final Vector3f velocity = new Vector3f();
   public int color = 0xFFFFFFFF;
   public float size = 0.08F;
   public float billboardScale = 1.0F;
   public float currentLife = 0.0F;
   public float totalLife = 1.0F;
   public boolean additive = true;
   public ResourceLocation texture;

   private VFXParticle() {
   }

   public static VFXParticle acquire() {
      VFXParticle particle = POOL.pollFirst();
      return particle == null ? new VFXParticle() : particle.clear();
   }

   public static void release(VFXParticle particle) {
      if (particle != null && POOL.size() < MAX_POOL_SIZE) {
         POOL.addLast(particle.clear());
      }
   }

   public VFXParticle clear() {
      this.position.zero();
      this.previousPosition.zero();
      this.velocity.zero();
      this.color = 0xFFFFFFFF;
      this.size = 0.08F;
      this.billboardScale = 1.0F;
      this.currentLife = 0.0F;
      this.totalLife = 1.0F;
      this.additive = true;
      this.texture = null;
      return this;
   }

   public boolean tick(float deltaTime) {
      this.currentLife += deltaTime;
      this.previousPosition.set(this.position);
      this.position.fma(deltaTime, this.velocity);
      return this.currentLife < this.totalLife;
   }

   public float lifeProgress() {
      return this.totalLife <= 0.0F ? 1.0F : Math.min(1.0F, this.currentLife / this.totalLife);
   }

   public static int pooledCount() {
      return POOL.size();
   }
}

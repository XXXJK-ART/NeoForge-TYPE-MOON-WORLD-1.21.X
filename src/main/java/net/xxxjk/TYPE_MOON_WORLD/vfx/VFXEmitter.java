package net.xxxjk.TYPE_MOON_WORLD.vfx;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import net.minecraft.util.Mth;
import net.xxxjk.TYPE_MOON_WORLD.vfx.condition.VFXCondition;
import net.xxxjk.TYPE_MOON_WORLD.vfx.data.VFXVanillaParticleDefinition;
import net.xxxjk.TYPE_MOON_WORLD.vfx.data.VFXVanillaParticleSpawn;
import net.xxxjk.TYPE_MOON_WORLD.vfx.keyframe.ColorKeyFrame;
import net.xxxjk.TYPE_MOON_WORLD.vfx.keyframe.SizeKeyFrame;
import net.xxxjk.TYPE_MOON_WORLD.vfx.keyframe.TransformKeyFrame;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class VFXEmitter {
   private final List<IVFXComponent> components = new ArrayList<>();
   private final List<VFXParticle> particles = new ArrayList<>();
   private final List<VFXParticle> samplePoints = new ArrayList<>();
   private final List<TransformKeyFrame> transformKeyFrames = new ArrayList<>();
   private final List<ColorKeyFrame> colorKeyFrames = new ArrayList<>();
   private final List<SizeKeyFrame> sizeKeyFrames = new ArrayList<>();
   private final List<VFXVanillaParticleDefinition> vanillaParticles = new ArrayList<>();
   private final List<VFXVanillaParticleSpawn> vanillaParticleSpawns = new ArrayList<>();
   private final Random random;
   private final float duration;
   private final float rate;
   private final float particleLifetime;
   private final float sizeVariance;
   private final float lifetimeVariance;
   private final float positionVariance;
   private final float velocityVariance;
   private final Vector3f baseVelocity = new Vector3f();
   private final VFXCondition visibilityCondition;
   private final VFXBlendMode blendMode;
   private final float effectDuration;
   private final float startTime;
   private final float endTime;
   private final float phaseDuration;
   private final int maxVanillaParticleSpawnsPerTick;
   private final Vector3f origin = new Vector3f();
   private final List<String> onStart = new ArrayList<>();
   private final List<String> onTick = new ArrayList<>();
   private final List<String> onEnd = new ArrayList<>();
   private final List<String> triggeredSubEffects = new ArrayList<>();
   private boolean started;
   private boolean ended;
   private boolean endHooksFired;
   private float age;
   private float emissionAccumulator;
   private boolean enableTrail;

   public VFXEmitter(
      float duration,
      float rate,
      float particleLifetime,
      float sizeVariance,
      float lifetimeVariance,
      float positionVariance,
      Vector3f baseVelocity,
      float velocityVariance,
      VFXCondition visibilityCondition,
      VFXBlendMode blendMode,
      float effectDuration,
      float startTime,
      float endTime,
      int maxVanillaParticleSpawnsPerTick,
      long seed
   ) {
      this.duration = Math.max(0.001F, duration);
      this.rate = Math.max(0.0F, rate);
      this.particleLifetime = Math.max(0.001F, particleLifetime);
      this.sizeVariance = Mth.clamp(sizeVariance, 0.0F, 1.0F);
      this.lifetimeVariance = Mth.clamp(lifetimeVariance, 0.0F, 1.0F);
      this.positionVariance = Math.max(0.0F, positionVariance);
      this.velocityVariance = Math.max(0.0F, velocityVariance);
      if (baseVelocity != null) {
         this.baseVelocity.set(baseVelocity);
      }
      this.visibilityCondition = visibilityCondition == null ? VFXCondition.ALWAYS : visibilityCondition;
      this.blendMode = blendMode == null ? VFXBlendMode.ADDITIVE : blendMode;
      this.effectDuration = Math.max(0.001F, effectDuration);
      this.startTime = Mth.clamp(startTime, 0.0F, this.effectDuration);
      this.endTime = Mth.clamp(endTime <= 0.0F ? this.effectDuration : endTime, this.startTime, this.effectDuration);
      this.phaseDuration = Math.max(0.001F, this.endTime - this.startTime);
      this.maxVanillaParticleSpawnsPerTick = Math.max(0, maxVanillaParticleSpawnsPerTick);
      this.random = new Random(seed);
   }

   public void setOrigin(float x, float y, float z) {
      this.origin.set(x, y, z);
   }

   public void addComponent(IVFXComponent component) {
      if (component != null) {
         this.components.add(component);
      }
   }

   public void addTransformKeyFrame(TransformKeyFrame keyFrame) {
      this.transformKeyFrames.add(keyFrame);
      this.transformKeyFrames.sort(Comparator.comparing(TransformKeyFrame::t));
   }

   public void addColorKeyFrame(ColorKeyFrame keyFrame) {
      this.colorKeyFrames.add(keyFrame);
      this.colorKeyFrames.sort(Comparator.comparing(ColorKeyFrame::t));
   }

   public void addSizeKeyFrame(SizeKeyFrame keyFrame) {
      this.sizeKeyFrames.add(keyFrame);
      this.sizeKeyFrames.sort(Comparator.comparing(SizeKeyFrame::t));
   }

   public void enableTrail(boolean enableTrail) {
      this.enableTrail = enableTrail;
   }

   public void addVanillaParticle(VFXVanillaParticleDefinition particle) {
      if (particle != null) {
         this.vanillaParticles.add(particle);
      }
   }

   public boolean isTrailEnabled() {
      return this.enableTrail;
   }

   public List<String> onStartHooks() {
      return this.onStart;
   }

   public List<String> onTickHooks() {
      return this.onTick;
   }

   public List<String> onEndHooks() {
      return this.onEnd;
   }

   public boolean tick(float deltaTime) {
      if (this.ended) {
         return false;
      }
      if (!this.started) {
         this.started = true;
         trigger(this.onStart);
      }
      this.age += deltaTime;
      float progress = localProgress();
      for (int i = this.particles.size() - 1; i >= 0; i--) {
         VFXParticle particle = this.particles.get(i);
         if (!particle.tick(deltaTime)) {
            this.particles.remove(i);
            VFXParticle.release(particle);
         }
      }
      if (this.age >= this.startTime && this.age <= this.endTime && this.visibilityCondition.test(progress, this.age, this.effectDuration, this.random)) {
         emit(deltaTime, progress);
         trigger(this.onTick);
      }
      if (this.age > this.endTime && this.particles.isEmpty()) {
         this.ended = true;
         triggerEnd();
      }
      return !this.ended;
   }

   public List<VFXParticle> particles() {
      return this.particles;
   }

   public VFXBlendMode blendMode() {
      return this.blendMode;
   }

   public Vector3f origin() {
      return new Vector3f(this.origin);
   }

   public float progress() {
      return localProgress();
   }

   public void releaseAll() {
      for (VFXParticle particle : this.particles) {
         VFXParticle.release(particle);
      }
      this.particles.clear();
      clearSamples();
      this.vanillaParticleSpawns.clear();
   }

   public List<VFXVanillaParticleSpawn> drainVanillaParticleSpawns() {
      List<VFXVanillaParticleSpawn> result = new ArrayList<>(this.vanillaParticleSpawns);
      this.vanillaParticleSpawns.clear();
      return result;
   }

   public List<String> drainTriggeredSubEffects() {
      List<String> result = new ArrayList<>(this.triggeredSubEffects);
      this.triggeredSubEffects.clear();
      return result;
   }

   private void trigger(List<String> hooks) {
      if (!hooks.isEmpty()) {
         this.triggeredSubEffects.addAll(hooks);
      }
   }

   private void triggerEnd() {
      if (!this.endHooksFired) {
         this.endHooksFired = true;
         trigger(this.onEnd);
      }
   }

   private void emit(float deltaTime, float progress) {
      this.vanillaParticleSpawns.clear();
      clearSamples();
      for (IVFXComponent component : this.components) {
         component.update(deltaTime, progress, this.samplePoints);
      }
      if (this.samplePoints.isEmpty()) {
         return;
      }
      this.emissionAccumulator += this.rate * deltaTime;
      int count = (int)this.emissionAccumulator;
      this.emissionAccumulator -= count;
      Vector3f translation = interpolatePosition(progress);
      Quaternionf rotation = interpolateRotation(progress);
      Vector3f scale = interpolateScale(progress);
      int color = interpolateColor(progress);
      float size = interpolateSize(progress);
      for (int i = 0; i < count; i++) {
         VFXParticle sample = this.samplePoints.get(this.random.nextInt(this.samplePoints.size()));
         VFXParticle particle = VFXParticle.acquire();
         particle.position.set(sample.position).mul(scale).rotate(rotation).add(this.origin).add(translation);
         if (this.positionVariance > 0.0F) {
            particle.position.add(randomSigned() * this.positionVariance, randomSigned() * this.positionVariance, randomSigned() * this.positionVariance);
         }
         particle.previousPosition.set(particle.position);
         particle.velocity
            .set(sample.velocity)
            .add(this.baseVelocity)
            .add(randomSigned() * this.velocityVariance, randomSigned() * this.velocityVariance, randomSigned() * this.velocityVariance);
         particle.color = color;
         float variance = this.sizeVariance <= 0.0F ? 1.0F : 1.0F + (this.random.nextFloat() * 2.0F - 1.0F) * this.sizeVariance;
         particle.size = Math.max(0.001F, size * variance);
         float lifetimeScale = this.lifetimeVariance <= 0.0F ? 1.0F : 1.0F + randomSigned() * this.lifetimeVariance;
         particle.totalLife = Math.max(0.001F, this.particleLifetime * lifetimeScale);
         particle.additive = this.blendMode == VFXBlendMode.ADDITIVE;
         this.particles.add(particle);
         if (!this.vanillaParticles.isEmpty() && this.vanillaParticleSpawns.size() < this.maxVanillaParticleSpawnsPerTick) {
            for (VFXVanillaParticleDefinition vanillaParticle : this.vanillaParticles) {
               if (this.vanillaParticleSpawns.size() >= this.maxVanillaParticleSpawnsPerTick) {
                  break;
               }
               int before = this.vanillaParticleSpawns.size();
               vanillaParticle.emit(this.random, particle.position, this.vanillaParticleSpawns);
               if (this.vanillaParticleSpawns.size() > this.maxVanillaParticleSpawnsPerTick) {
                  this.vanillaParticleSpawns.subList(this.maxVanillaParticleSpawnsPerTick, this.vanillaParticleSpawns.size()).clear();
               }
               if (before == this.vanillaParticleSpawns.size() && this.maxVanillaParticleSpawnsPerTick == 0) {
                  break;
               }
            }
         }
      }
      clearSamples();
   }

   private float localProgress() {
      return Mth.clamp((this.age - this.startTime) / this.phaseDuration, 0.0F, 1.0F);
   }

   private void clearSamples() {
      for (VFXParticle sample : this.samplePoints) {
         VFXParticle.release(sample);
      }
      this.samplePoints.clear();
   }

   private Vector3f interpolatePosition(float progress) {
      TransformKeyFrame frame = frameAt(this.transformKeyFrames, progress);
      if (frame == null) {
         return new Vector3f();
      }
      TransformKeyFrame next = nextFrame(this.transformKeyFrames, progress);
      if (next == null || next == frame) {
         return new Vector3f(frame.position());
      }
      float t = keyedProgress(frame.t(), next.t(), progress, frame.easing());
      return new Vector3f(frame.position()).lerp(next.position(), t);
   }

   private Quaternionf interpolateRotation(float progress) {
      TransformKeyFrame frame = frameAt(this.transformKeyFrames, progress);
      if (frame == null) {
         return new Quaternionf();
      }
      TransformKeyFrame next = nextFrame(this.transformKeyFrames, progress);
      if (next == null || next == frame) {
         return new Quaternionf(frame.rotation());
      }
      float t = keyedProgress(frame.t(), next.t(), progress, frame.easing());
      return new Quaternionf(frame.rotation()).slerp(next.rotation(), t);
   }

   private Vector3f interpolateScale(float progress) {
      TransformKeyFrame frame = frameAt(this.transformKeyFrames, progress);
      if (frame == null) {
         return new Vector3f(1.0F);
      }
      TransformKeyFrame next = nextFrame(this.transformKeyFrames, progress);
      if (next == null || next == frame) {
         return new Vector3f(frame.scale());
      }
      float t = keyedProgress(frame.t(), next.t(), progress, frame.easing());
      return new Vector3f(frame.scale()).lerp(next.scale(), t);
   }

   private int interpolateColor(float progress) {
      ColorKeyFrame frame = frameAt(this.colorKeyFrames, progress);
      if (frame == null) {
         return 0xFFFFFFFF;
      }
      ColorKeyFrame next = nextFrame(this.colorKeyFrames, progress);
      if (next == null || next == frame) {
         return frame.color();
      }
      float t = keyedProgress(frame.t(), next.t(), progress, frame.easing());
      return lerpArgb(frame.color(), next.color(), t);
   }

   private float interpolateSize(float progress) {
      SizeKeyFrame frame = frameAt(this.sizeKeyFrames, progress);
      if (frame == null) {
         return 0.08F;
      }
      SizeKeyFrame next = nextFrame(this.sizeKeyFrames, progress);
      if (next == null || next == frame) {
         return frame.size();
      }
      float t = keyedProgress(frame.t(), next.t(), progress, frame.easing());
      return Mth.lerp(t, frame.size(), next.size());
   }

   private static float keyedProgress(float start, float end, float progress, Easing easing) {
      if (end <= start) {
         return 1.0F;
      }
      return (easing == null ? Easing.LINEAR : easing).apply(Mth.clamp((progress - start) / (end - start), 0.0F, 1.0F));
   }

   private float randomSigned() {
      return this.random.nextFloat() * 2.0F - 1.0F;
   }

   private static int lerpArgb(int a, int b, float t) {
      int aa = a >>> 24 & 255;
      int ar = a >>> 16 & 255;
      int ag = a >>> 8 & 255;
      int ab = a & 255;
      int ba = b >>> 24 & 255;
      int br = b >>> 16 & 255;
      int bg = b >>> 8 & 255;
      int bb = b & 255;
      return (Mth.lerpInt(t, aa, ba) & 255) << 24
         | (Mth.lerpInt(t, ar, br) & 255) << 16
         | (Mth.lerpInt(t, ag, bg) & 255) << 8
         | Mth.lerpInt(t, ab, bb) & 255;
   }

   private static <T> T frameAt(List<T> frames, float progress) {
      T result = null;
      for (T frame : frames) {
         float t = frameT(frame);
         if (t <= progress) {
            result = frame;
         } else {
            break;
         }
      }
      return result == null && !frames.isEmpty() ? frames.get(0) : result;
   }

   private static <T> T nextFrame(List<T> frames, float progress) {
      for (T frame : frames) {
         if (frameT(frame) > progress) {
            return frame;
         }
      }
      return frames.isEmpty() ? null : frames.get(frames.size() - 1);
   }

   private static float frameT(Object frame) {
      if (frame instanceof TransformKeyFrame transformKeyFrame) {
         return transformKeyFrame.t();
      }
      if (frame instanceof ColorKeyFrame colorKeyFrame) {
         return colorKeyFrame.t();
      }
      return ((SizeKeyFrame)frame).t();
   }
}

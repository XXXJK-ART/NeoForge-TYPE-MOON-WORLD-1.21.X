package net.xxxjk.TYPE_MOON_WORLD.vfx.data;

import java.util.ArrayList;
import java.util.List;
import net.xxxjk.TYPE_MOON_WORLD.vfx.IVFXComponent;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXBlendMode;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXEmitter;
import net.xxxjk.TYPE_MOON_WORLD.vfx.condition.VFXConditionParser;
import net.xxxjk.TYPE_MOON_WORLD.vfx.keyframe.ColorKeyFrame;
import net.xxxjk.TYPE_MOON_WORLD.vfx.keyframe.SizeKeyFrame;
import net.xxxjk.TYPE_MOON_WORLD.vfx.keyframe.TransformKeyFrame;

public class VFXEffectDefinition {
   private final float duration;
   private final List<EmitterDefinition> emitters;
   private final List<VFXEnvironmentDefinition> environments;

   public VFXEffectDefinition(float duration, List<EmitterDefinition> emitters, List<VFXEnvironmentDefinition> environments) {
      this.duration = duration;
      this.emitters = List.copyOf(emitters);
      this.environments = List.copyOf(environments);
   }

   public List<VFXEmitter> createEmitters(float x, float y, float z, long seed) {
      List<VFXEmitter> result = new ArrayList<>();
      for (int i = 0; i < this.emitters.size(); i++) {
         EmitterDefinition definition = this.emitters.get(i);
         VFXEmitter emitter = new VFXEmitter(
            this.duration,
            definition.rate,
            definition.particleLifetime,
            definition.sizeVariance,
            definition.lifetimeVariance,
            definition.positionVariance,
            definition.velocity,
            definition.velocityVariance,
            VFXConditionParser.parse(definition.visibilityCondition),
            definition.blendMode,
            this.duration,
            definition.startTime,
            definition.endTime,
            definition.maxVanillaParticleSpawnsPerTick,
            seed + i * 31L
         );
         emitter.setOrigin(x, y, z);
         emitter.addComponent(definition.component);
         for (TransformKeyFrame keyFrame : definition.transformKeyFrames) {
            emitter.addTransformKeyFrame(keyFrame);
         }
         for (ColorKeyFrame keyFrame : definition.colorKeyFrames) {
            emitter.addColorKeyFrame(keyFrame);
         }
         for (SizeKeyFrame keyFrame : definition.sizeKeyFrames) {
            emitter.addSizeKeyFrame(keyFrame);
         }
         for (VFXVanillaParticleDefinition vanillaParticle : definition.vanillaParticles) {
            emitter.addVanillaParticle(vanillaParticle);
         }
         emitter.enableTrail(definition.enableTrail);
         emitter.onStartHooks().addAll(definition.onStart);
         emitter.onTickHooks().addAll(definition.onTick);
         emitter.onEndHooks().addAll(definition.onEnd);
         result.add(emitter);
      }
      return result;
   }

   public float duration() {
      return this.duration;
   }

   public List<VFXEnvironmentDefinition> environments() {
      return this.environments;
   }

   public record EmitterDefinition(
      float rate,
      float particleLifetime,
      float sizeVariance,
      float lifetimeVariance,
      float positionVariance,
      org.joml.Vector3f velocity,
      float velocityVariance,
      float startTime,
      float endTime,
      int maxVanillaParticleSpawnsPerTick,
      IVFXComponent component,
      List<TransformKeyFrame> transformKeyFrames,
      List<ColorKeyFrame> colorKeyFrames,
      List<SizeKeyFrame> sizeKeyFrames,
      String visibilityCondition,
      VFXBlendMode blendMode,
      String particleRole,
      List<VFXVanillaParticleDefinition> vanillaParticles,
      boolean enableTrail,
      List<String> onStart,
      List<String> onTick,
      List<String> onEnd
   ) {
   }
}

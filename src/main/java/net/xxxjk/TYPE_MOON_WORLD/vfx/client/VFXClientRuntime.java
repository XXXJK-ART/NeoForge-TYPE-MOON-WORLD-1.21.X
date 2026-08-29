package net.xxxjk.TYPE_MOON_WORLD.vfx.client;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXEmitter;
import net.xxxjk.TYPE_MOON_WORLD.vfx.data.EffectLibrary;
import net.xxxjk.TYPE_MOON_WORLD.vfx.data.VFXEffectDefinition;
import net.xxxjk.TYPE_MOON_WORLD.vfx.data.VFXEnvironmentDefinition;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class VFXClientRuntime {
   private static final int MAX_TARGET_CACHE_SIZE = 256;
   private static final Map<UUID, Integer> TARGET_ENTITY_IDS = new ConcurrentHashMap<>();

   private VFXClientRuntime() {
   }

   public static void spawn(String effectId, double x, double y, double z, int targetEntityId, long seed) {
      VFXEffectDefinition definition = EffectLibrary.INSTANCE.get(effectId);
      if (definition == null) {
         TYPE_MOON_WORLD.LOGGER.warn("Unknown VFX effect '{}'", effectId);
         return;
      }
      for (VFXEnvironmentDefinition environment : definition.environments()) {
         VFXEnvironmentManager.add(environment, x, y, z, null);
      }
      List<VFXEmitter> emitters = definition.createEmitters((float)x, (float)y, (float)z, seed);
      if (targetEntityId >= 0) {
         Minecraft mc = Minecraft.getInstance();
         Entity entity = mc.level == null ? null : mc.level.getEntity(targetEntityId);
         if (entity != null) {
            for (VFXEmitter emitter : emitters) {
               emitter.setOrigin((float)entity.getX(), (float)entity.getY(), (float)entity.getZ());
               bindToTarget(emitter, entity);
            }
         }
      }
      for (VFXEmitter emitter : emitters) {
         VFXRenderManager.addEmitter(emitter);
      }
   }

   public static void spawn(String effectId, double x, double y, double z, Optional<UUID> targetEntityUuid, long seed) {
      spawn(effectId, x, y, z, targetEntityUuid, seed, Optional.empty(), 1.0F);
   }

   public static void spawn(String effectId, double x, double y, double z, Optional<UUID> targetEntityUuid, long seed, Optional<Vec3> direction) {
      spawn(effectId, x, y, z, targetEntityUuid, seed, direction, 1.0F);
   }

   public static void spawn(String effectId, double x, double y, double z, Optional<UUID> targetEntityUuid, long seed, Optional<Vec3> direction, float scale) {
      VFXEffectDefinition definition = EffectLibrary.INSTANCE.get(effectId);
      if (definition == null) {
         TYPE_MOON_WORLD.LOGGER.warn("Unknown VFX effect '{}'", effectId);
         return;
      }
      Minecraft mc = Minecraft.getInstance();
      Entity target = null;
      if (targetEntityUuid.isPresent() && mc.level != null) {
         target = findTargetByUuid(mc, targetEntityUuid.get());
      }
      double originX = x;
      double originY = y;
      double originZ = z;
      if (target != null) {
         originX = target.getX();
         originY = target.getY();
         originZ = target.getZ();
      }
      for (VFXEnvironmentDefinition environment : definition.environments()) {
         VFXEnvironmentManager.add(environment, originX, originY, originZ, targetEntityUuid.orElse(null));
      }
      List<VFXEmitter> emitters = definition.createEmitters((float)originX, (float)originY, (float)originZ, seed);
      for (VFXEmitter emitter : emitters) {
         emitter.setUniformScale(scale);
      }
      if (direction.isPresent() && target == null) {
         Quaternionf rotation = rotationFromForward(direction.get());
         float fixedX = (float)originX, fixedY = (float)originY, fixedZ = (float)originZ;
         for (VFXEmitter emitter : emitters) {
            emitter.setDynamicTransform(() -> new Vector3f(fixedX, fixedY, fixedZ), () -> new Quaternionf(rotation));
         }
      }
      if (target != null) {
         Entity boundTarget = target;
         for (VFXEmitter emitter : emitters) {
            bindToTarget(emitter, boundTarget);
         }
      }
      for (VFXEmitter emitter : emitters) {
         VFXRenderManager.addEmitter(emitter);
      }
   }

   private static Quaternionf rotationFromForward(Vec3 forward) {
      Vec3 f = forward.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : forward.normalize();
      return new Quaternionf().rotationTo(new Vector3f(0.0F, 0.0F, 1.0F), new Vector3f((float)f.x, (float)f.y, (float)f.z));
   }

   public static void spawnTest(double x, double y, double z) {
      VFXRenderManager.spawnTest((float)x, (float)y, (float)z);
   }

   private static void bindToTarget(VFXEmitter emitter, Entity target) {
      VFXEffectDefinition.BindingDefinition binding = emitter.binding();
      if (binding == null || binding.mode().isBlank() || target == null) {
         return;
      }
      String mode = binding.mode();
      if ("artoria_blade".equals(mode) || "entity_yaw".equals(mode) || "entity_look".equals(mode)) {
         emitter.setDynamicTransform(
            () -> bindingOrigin(target, binding),
            () -> binding.rotateWithEntity() ? ("entity_look".equals(mode) ? rotationFromForward(target.getLookAngle()) : yawRotation(target)) : new Quaternionf()
         );
      }
   }

   private static Entity findTargetByUuid(Minecraft mc, UUID targetUuid) {
      Integer cachedId = TARGET_ENTITY_IDS.get(targetUuid);
      if (cachedId != null) {
         Entity cached = mc.level.getEntity(cachedId);
         if (cached != null && cached.isAlive() && targetUuid.equals(cached.getUUID())) {
            return cached;
         }
         TARGET_ENTITY_IDS.remove(targetUuid);
      }

      for (Entity entity : mc.level.entitiesForRendering()) {
         if (targetUuid.equals(entity.getUUID())) {
            rememberTarget(entity);
            return entity;
         }
      }
      return null;
   }

   private static void rememberTarget(Entity entity) {
      if (TARGET_ENTITY_IDS.size() >= MAX_TARGET_CACHE_SIZE) {
         TARGET_ENTITY_IDS.clear();
      }
      TARGET_ENTITY_IDS.put(entity.getUUID(), entity.getId());
   }

   private static Vector3f bindingOrigin(Entity target, VFXEffectDefinition.BindingDefinition binding) {
      Vec3 look = target.getLookAngle();
      Vec3 forward = new Vec3(look.x, 0.0, look.z);
      if (forward.lengthSqr() < 1.0E-6) {
         forward = Vec3.directionFromRotation(0.0F, target.getYRot());
      }
      forward = forward.normalize();
      Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
      Vector3f offset = binding.offset();
      double baseY = target instanceof LivingEntity living ? living.getY() + living.getBbHeight() * 0.58 : target.getY() + target.getBbHeight() * 0.55;
      Vec3 origin = new Vec3(target.getX(), baseY, target.getZ())
         .add(right.scale(offset.x))
         .add(0.0, offset.y, 0.0)
         .add(forward.scale(offset.z));
      return new Vector3f((float)origin.x, (float)origin.y, (float)origin.z);
   }

   private static Quaternionf yawRotation(Entity target) {
      float radians = (float)Math.toRadians(-target.getYRot());
      return new Quaternionf().rotateY(radians);
   }
}

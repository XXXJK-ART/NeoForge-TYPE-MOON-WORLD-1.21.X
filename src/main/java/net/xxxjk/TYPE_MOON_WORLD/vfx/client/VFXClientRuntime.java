package net.xxxjk.TYPE_MOON_WORLD.vfx.client;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXEmitter;
import net.xxxjk.TYPE_MOON_WORLD.vfx.data.EffectLibrary;
import net.xxxjk.TYPE_MOON_WORLD.vfx.data.VFXEffectDefinition;
import net.xxxjk.TYPE_MOON_WORLD.vfx.data.VFXEnvironmentDefinition;

public final class VFXClientRuntime {
   private VFXClientRuntime() {
   }

   public static void spawn(String effectId, double x, double y, double z, int targetEntityId, long seed) {
      VFXEffectDefinition definition = EffectLibrary.INSTANCE.get(effectId);
      if (definition == null) {
         TYPE_MOON_WORLD.LOGGER.warn("Unknown VFX effect '{}'", effectId);
         return;
      }
      for (VFXEnvironmentDefinition environment : definition.environments()) {
         VFXEnvironmentManager.add(environment, x, y, z);
      }
      List<VFXEmitter> emitters = definition.createEmitters((float)x, (float)y, (float)z, seed);
      if (targetEntityId >= 0) {
         Minecraft mc = Minecraft.getInstance();
         Entity entity = mc.level == null ? null : mc.level.getEntity(targetEntityId);
         if (entity != null) {
            for (VFXEmitter emitter : emitters) {
               emitter.setOrigin((float)entity.getX(), (float)entity.getY(), (float)entity.getZ());
            }
         }
      }
      for (VFXEmitter emitter : emitters) {
         VFXRenderManager.addEmitter(emitter);
      }
   }

   public static void spawn(String effectId, double x, double y, double z, Optional<UUID> targetEntityUuid, long seed) {
      VFXEffectDefinition definition = EffectLibrary.INSTANCE.get(effectId);
      if (definition == null) {
         TYPE_MOON_WORLD.LOGGER.warn("Unknown VFX effect '{}'", effectId);
         return;
      }
      Minecraft mc = Minecraft.getInstance();
      Entity target = null;
      if (targetEntityUuid.isPresent() && mc.level != null) {
         for (Entity entity : mc.level.entitiesForRendering()) {
            if (targetEntityUuid.get().equals(entity.getUUID())) {
               target = entity;
               break;
            }
         }
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
         VFXEnvironmentManager.add(environment, originX, originY, originZ);
      }
      List<VFXEmitter> emitters = definition.createEmitters((float)originX, (float)originY, (float)originZ, seed);
      for (VFXEmitter emitter : emitters) {
         VFXRenderManager.addEmitter(emitter);
      }
   }

   public static void spawnTest(double x, double y, double z) {
      VFXRenderManager.spawnTest((float)x, (float)y, (float)z);
   }
}

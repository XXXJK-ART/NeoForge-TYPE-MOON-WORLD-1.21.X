package net.xxxjk.TYPE_MOON_WORLD.vfx.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.NeoForgeRenderTypes;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.Config;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXBlendMode;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXEmitter;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXParticle;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXPerformanceBudget;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXPriority;
import net.xxxjk.TYPE_MOON_WORLD.vfx.component.curve.LineCurve;
import net.xxxjk.TYPE_MOON_WORLD.vfx.condition.VFXCondition;
import net.xxxjk.TYPE_MOON_WORLD.vfx.data.VFXVanillaParticleSpawn;
import net.xxxjk.TYPE_MOON_WORLD.vfx.keyframe.ColorKeyFrame;
import net.xxxjk.TYPE_MOON_WORLD.vfx.keyframe.SizeKeyFrame;
import org.joml.Vector3f;

@EventBusSubscriber(
   modid = TYPE_MOON_WORLD.MOD_ID,
   value = {Dist.CLIENT}
)
public final class VFXRenderManager {
   private static final ResourceLocation PARTICLE_TEXTURE = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "textures/particle/particle_white.png");
   private static final ResourceLocation BEAM_TEXTURE = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "textures/particle/beam_gradient.png");
   private static final ResourceLocation RING_TEXTURE = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "textures/particle/ring_shockwave.png");
   private static final Map<ResourceLocation, RenderType> CUSTOM_TRANSLUCENT_TEXTURES = new HashMap<>();
   private static final Map<ResourceLocation, RenderType> CUSTOM_ADDITIVE_TEXTURES = new HashMap<>();
   private static final List<RenderType> USED_CUSTOM_RENDER_TYPES = new ArrayList<>();
   private static final List<VFXEmitter> EMITTERS = new ArrayList<>();
   private static final List<VFXBeam> BEAMS = new ArrayList<>();
   private static final List<VFXRingShockwave> RINGS = new ArrayList<>();
   private static long clientTick;
   private static double smoothedFps;
   private static final VFXQualityController QUALITY = new VFXQualityController();
   private static long updateNanos;
   private static long renderNanos;
   private static int renderedParticles;
   private static int culledParticles;
   private static int renderBatches;

   private VFXRenderManager() {
   }

   public static void addEmitter(VFXEmitter emitter) {
      if (emitter == null) return;
      if (EMITTERS.size() >= VFXPerformanceBudget.MAX_EMITTERS) {
         emitter.releaseAll();
         return;
      }
      EMITTERS.add(emitter);
   }

   public static void addBeam(VFXBeam beam) {
      if (beam != null && BEAMS.size() < VFXPerformanceBudget.MAX_BEAMS) {
         BEAMS.add(beam);
      }
   }

   public static void addRingShockwave(VFXRingShockwave ring) {
      if (ring != null && RINGS.size() < VFXPerformanceBudget.MAX_RINGS) {
         RINGS.add(ring);
      }
   }

   public static void spawnTest(float x, float y, float z) {
      VFXEmitter emitter = new VFXEmitter(
         0.25F,
         40.0F,
         1.2F,
         0.0F,
         0.0F,
         0.0F,
         new Vector3f(),
         0.0F,
         VFXCondition.ALWAYS,
         VFXBlendMode.ADDITIVE,
         0.25F,
         0.0F,
         0.25F,
         0,
         clientTick
      );
      emitter.setOrigin(x, y, z);
      emitter.addComponent(new LineCurve(new Vector3f(0.0F, 0.0F, 0.0F), new Vector3f(0.0F, 0.8F, 0.0F), 10));
      emitter.addColorKeyFrame(new ColorKeyFrame(0.0F, 0xFFFFFFFF, null));
      emitter.addSizeKeyFrame(new SizeKeyFrame(0.0F, 0.12F, null));
      addEmitter(emitter);
      addRingShockwave(new VFXRingShockwave(new Vector3f(x, y + 0.02F, z), 0xCCFFFFFF, 0.35F, 2.4F, 0.45F));
   }

   public static void clear() {
      for (VFXEmitter emitter : EMITTERS) {
         emitter.releaseAll();
      }
      EMITTERS.clear();
      BEAMS.clear();
      RINGS.clear();
      smoothedFps = 0.0;
      QUALITY.reset();
      renderedParticles = culledParticles = renderBatches = 0;
      VFXEnvironmentManager.clear();
      VFXPostProcessManager.clear();
   }

   @SubscribeEvent
   public static void onClientTick(ClientTickEvent.Post event) {
      clientTick++;
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.level == null) {
         clear();
         return;
      }
      int fps = minecraft.getFps();
      if (fps > 0) {
         smoothedFps = smoothedFps <= 0.0 ? fps : smoothedFps * 0.9 + fps * 0.1;
      }
      QUALITY.tick(smoothedFps);
      if (EMITTERS.isEmpty() && BEAMS.isEmpty() && RINGS.isEmpty()) {
         return;
      }
      float delta = 1.0F / 20.0F;
      long updateStart = System.nanoTime();
      VFXPerformanceBudget.Budget budget = VFXPerformanceBudget.resolve(
         VFXPerformanceBudget.qualityByName(Config.vfxQuality), QUALITY.pressure(), Config.vfxMaxParticles);
      int activeParticles = 0;
      for (VFXEmitter emitter : EMITTERS) {
         activeParticles += emitter.particles().size();
      }
      int vanillaParticleBudget = budget.vanillaParticlesPerTick();
      Iterator<VFXEmitter> iterator = EMITTERS.iterator();
      List<VFXEmitter> spawnedByHooks = null;
      while (iterator.hasNext()) {
         VFXEmitter emitter = iterator.next();
         if (QUALITY.pressure() != VFXPerformanceBudget.Pressure.NORMAL && emitter.priority() == VFXPriority.DECORATIVE) {
            emitter.releaseAll();
            iterator.remove();
            continue;
         }
         int particlesBefore = emitter.particles().size();
         boolean nearby = minecraft.player != null
            && emitter.originDistanceToSqr(minecraft.player.getX(), minecraft.player.getY(), minecraft.player.getZ())
               <= VFXPerformanceBudget.MAX_RENDER_DISTANCE_SQR;
         int priorityBudget = emitter.priority() == VFXPriority.CRITICAL ? budget.particles() : Math.round(budget.particles() * 0.75F);
         int particleBudget = nearby ? Math.max(0, Math.min(priorityBudget, budget.particles() - activeParticles)) : 0;
         int emitterVanillaBudget = nearby ? vanillaParticleBudget : 0;
         if (!emitter.tick(delta, particleBudget, emitterVanillaBudget)) {
            emitter.releaseAll();
            iterator.remove();
            activeParticles -= particlesBefore;
         } else {
            activeParticles += emitter.particles().size() - particlesBefore;
            for (String subEffect : emitter.drainTriggeredSubEffects()) {
               if (!nearby) continue;
               var definition = net.xxxjk.TYPE_MOON_WORLD.vfx.data.EffectLibrary.INSTANCE.get(subEffect);
               if (definition != null) {
                  if (spawnedByHooks == null) {
                     spawnedByHooks = new ArrayList<>();
                  }
                  Vector3f origin = emitter.origin();
                  spawnedByHooks.addAll(definition.createEmitters(origin.x, origin.y, origin.z, clientTick));
               }
            }
            for (VFXVanillaParticleSpawn spawn : emitter.drainVanillaParticleSpawns()) {
               if (vanillaParticleBudget > 0 && minecraft.level != null) {
                  minecraft.level.addParticle(
                     spawn.options(),
                     spawn.position().x,
                     spawn.position().y,
                     spawn.position().z,
                     spawn.velocity().x,
                     spawn.velocity().y,
                     spawn.velocity().z
                  );
                  vanillaParticleBudget--;
               }
            }
         }
      }
      if (spawnedByHooks != null) {
         for (VFXEmitter emitter : spawnedByHooks) {
            addEmitter(emitter);
         }
      }
      Iterator<VFXBeam> beamIterator = BEAMS.iterator();
      while (beamIterator.hasNext()) {
         if (!beamIterator.next().tick(delta)) {
            beamIterator.remove();
         }
      }
      Iterator<VFXRingShockwave> ringIterator = RINGS.iterator();
      while (ringIterator.hasNext()) {
         if (!ringIterator.next().tick(delta)) {
            ringIterator.remove();
         }
      }
      updateNanos = System.nanoTime() - updateStart;
   }

   @SubscribeEvent
   public static void onRenderLevelStage(RenderLevelStageEvent event) {
      if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || (EMITTERS.isEmpty() && BEAMS.isEmpty() && RINGS.isEmpty())) {
         return;
      }
      Minecraft mc = Minecraft.getInstance();
      if (mc.level == null || mc.player == null) {
         return;
      }
      // VFX continue ticking server-side while a GUI is open, but there is no
      // useful reason to rasterize their full particle graph behind the GUI.
      if (mc.screen != null) {
         return;
      }
      PoseStack poseStack = event.getPoseStack();
      long renderStart = System.nanoTime();
      renderedParticles = 0;
      culledParticles = 0;
      Vec3 camera = event.getCamera().getPosition();
      poseStack.pushPose();
      try {
       poseStack.translate(-camera.x, -camera.y, -camera.z);
      BufferSource source = mc.renderBuffers().bufferSource();
      RenderType translucent = NeoForgeRenderTypes.getUnlitTranslucent(PARTICLE_TEXTURE, false);
      RenderType additive = RenderType.entityTranslucentEmissive(PARTICLE_TEXTURE);
      RenderType beamType = RenderType.entityTranslucentEmissive(BEAM_TEXTURE);
      RenderType ringType = RenderType.entityTranslucentEmissive(RING_TEXTURE);
      USED_CUSTOM_RENDER_TYPES.clear();
      // RenderType batches are flushed after all emitters are traversed, so
      // uniforms must be frame-global rather than updated per emitter.
      VFXMaterialShaders.updateGlobals(
         (float)(clientTick + event.getPartialTick().getGameTimeDeltaTicks()),
         0.0F,
         0.85F,
         Config.vfxDistortion ? 1.0F : 0.0F
      );
      Vector3f cameraLeft = new Vector3f(-1.0F, 0.0F, 0.0F).rotate(event.getCamera().rotation());
      Vector3f cameraUp = new Vector3f(0.0F, 1.0F, 0.0F).rotate(event.getCamera().rotation());
      for (VFXEmitter emitter : EMITTERS) {
         if (emitter.originDistanceToSqr(camera.x, camera.y, camera.z) > VFXPerformanceBudget.MAX_RENDER_DISTANCE_SQR) continue;
         for (VFXParticle particle : emitter.particles()) {
            if (!particleVisible(particle, camera)) {
               culledParticles++;
               continue;
            }
            RenderType particleType = renderTypeFor(particle.texture, emitter, emitter.blendMode() == VFXBlendMode.ADDITIVE, translucent, additive);
            VertexConsumer consumer = source.getBuffer(particleType);
            switch (emitter.rendererType()) {
               case RIBBON, BEAM -> {
                  drawTrail(poseStack, consumer, particle.previousPosition, particle.position,
                     particle.color, particle.size, cameraLeft);
                  renderedParticles++;
               }
               case RING, DECAL -> {
                  drawDecal(poseStack, consumer, particle);
                  renderedParticles++;
               }
               case SHADER_QUAD, VOLUME, BILLBOARD -> {
                  drawBillboard(poseStack, consumer, particle, cameraLeft, cameraUp, event.getPartialTick().getGameTimeDeltaTicks());
                  renderedParticles++;
                  if (emitter.isTrailEnabled()) {
                     drawTrail(poseStack, consumer, particle.previousPosition, particle.position,
                        particle.color, particle.size, cameraLeft);
                  }
               }
            }
         }
      }
      for (VFXBeam beam : BEAMS) {
         if (!withinRenderDistance(beam, camera)) continue;
         drawBeam(event, poseStack, source.getBuffer(beamType), beam);
      }
      for (VFXRingShockwave ring : RINGS) {
         if (distanceToSqr(ring.center, camera) > VFXPerformanceBudget.MAX_RENDER_DISTANCE_SQR) continue;
         drawRingShockwave(poseStack, source.getBuffer(ringType), ring);
      }
      source.endBatch(translucent);
      source.endBatch(additive);
      source.endBatch(beamType);
      source.endBatch(ringType);
      renderBatches = 4 + USED_CUSTOM_RENDER_TYPES.size();
      for (RenderType customType : USED_CUSTOM_RENDER_TYPES) {
         source.endBatch(customType);
      }
      } finally {
         USED_CUSTOM_RENDER_TYPES.clear();
         // The event pose stack is owned by LevelRenderer. Undo only the
         // single push made above, including when a custom shader/batch fails.
         poseStack.popPose();
         restoreRenderState();
      }
      renderNanos = System.nanoTime() - renderStart;
   }

   /** Restore the vanilla level defaults before GameRenderer renders first-person hands. */
   private static void restoreRenderState() {
      RenderSystem.enableDepthTest();
      RenderSystem.depthFunc(515); // GL_LEQUAL
      RenderSystem.depthMask(true);
      RenderSystem.colorMask(true, true, true, true);
      RenderSystem.disableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.enableCull();
      RenderSystem.resetTextureMatrix();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }

   private static boolean particleVisible(VFXParticle particle, Vec3 camera) {
      double dx = particle.position.x - camera.x;
      double dy = particle.position.y - camera.y;
      double dz = particle.position.z - camera.z;
      double distanceSqr = dx * dx + dy * dy + dz * dz;
      if (distanceSqr > VFXPerformanceBudget.MAX_RENDER_DISTANCE_SQR) return false;
      // Avoid rasterizing sub-pixel particles at long range.
      return particle.size * particle.size * 64.0 >= distanceSqr * 0.000025;
   }

   public static String debugStats() {
      return String.format(java.util.Locale.ROOT,
         "quality=%s pressure=%s fps=%.1f particles=%d culled=%d emitters=%d beams=%d rings=%d batches=%d update=%.2fms render=%.2fms",
         Config.vfxQuality, QUALITY.pressure(), smoothedFps, renderedParticles, culledParticles,
         EMITTERS.size(), BEAMS.size(), RINGS.size(), renderBatches, updateNanos / 1_000_000.0, renderNanos / 1_000_000.0);
   }

   public static VFXPerformanceBudget.Pressure pressure() { return QUALITY.pressure(); }

   public static boolean hasActiveEffects() {
      return !(EMITTERS.isEmpty() && BEAMS.isEmpty() && RINGS.isEmpty());
   }

   public static boolean hasBloomEffects() {
      for (VFXEmitter emitter : EMITTERS) {
         if (emitter.material().bloom() > 0.001F || emitter.blendMode() == VFXBlendMode.ADDITIVE) return true;
      }
      return false;
   }

   private static boolean withinRenderDistance(VFXBeam beam, Vec3 camera) {
      return distanceToSqr(beam.start, camera) <= VFXPerformanceBudget.MAX_RENDER_DISTANCE_SQR
         || distanceToSqr(beam.end, camera) <= VFXPerformanceBudget.MAX_RENDER_DISTANCE_SQR;
   }

   private static double distanceToSqr(Vector3f point, Vec3 other) {
      double dx = point.x - other.x;
      double dy = point.y - other.y;
      double dz = point.z - other.z;
      return dx * dx + dy * dy + dz * dz;
   }

   private static void drawBillboard(PoseStack poseStack, VertexConsumer consumer, VFXParticle particle,
                                     Vector3f cameraLeft, Vector3f cameraUp, float partialTick) {
      float size = particle.size * particle.billboardScale * (1.0F - particle.lifeProgress() * 0.35F);
      float cx = particle.renderX(partialTick), cy = particle.renderY(partialTick), cz = particle.renderZ(partialTick);
      int color = particle.color;
      float a = ((color >>> 24) & 255) / 255.0F * (1.0F - particle.lifeProgress());
      float r = ((color >>> 16) & 255) / 255.0F;
      float g = ((color >>> 8) & 255) / 255.0F;
      float b = (color & 255) / 255.0F;
      billboardVertex(poseStack, consumer, cx, cy, cz, cameraLeft, cameraUp, size, -1.0F, -1.0F, r, g, b, a, 0.0F, 0.0F);
      billboardVertex(poseStack, consumer, cx, cy, cz, cameraLeft, cameraUp, size, -1.0F, 1.0F, r, g, b, a, 0.0F, 1.0F);
      billboardVertex(poseStack, consumer, cx, cy, cz, cameraLeft, cameraUp, size, 1.0F, 1.0F, r, g, b, a, 1.0F, 1.0F);
      billboardVertex(poseStack, consumer, cx, cy, cz, cameraLeft, cameraUp, size, 1.0F, -1.0F, r, g, b, a, 1.0F, 0.0F);
   }

   private static RenderType renderTypeFor(ResourceLocation texture, VFXEmitter emitter, boolean additive, RenderType translucent, RenderType additiveDefault) {
      if (emitter != null && VFXMaterialShaders.isRegistered(emitter.material().shader())) {
         ResourceLocation materialTexture = emitter.material().texture() != null ? emitter.material().texture() : (texture != null ? texture : PARTICLE_TEXTURE);
         return rememberCustomRenderType(VFXMaterialRenderTypes.get(emitter.material().shader(), materialTexture, additive));
      }
      if (texture == null || texture.equals(PARTICLE_TEXTURE)) {
         return additive ? additiveDefault : translucent;
      }
      if (additive) {
         return rememberCustomRenderType(CUSTOM_ADDITIVE_TEXTURES.computeIfAbsent(texture, key -> RenderType.entityTranslucentEmissive(key)));
      }
      return rememberCustomRenderType(CUSTOM_TRANSLUCENT_TEXTURES.computeIfAbsent(texture, key -> NeoForgeRenderTypes.getUnlitTranslucent(key, false)));
   }

   private static RenderType rememberCustomRenderType(RenderType renderType) {
      if (!USED_CUSTOM_RENDER_TYPES.contains(renderType)) {
         USED_CUSTOM_RENDER_TYPES.add(renderType);
      }
      return renderType;
   }

   private static void billboardVertex(
      PoseStack poseStack, VertexConsumer consumer, float cx, float cy, float cz,
      Vector3f left, Vector3f up, float size, float lx, float uy,
      float r, float g, float b, float a, float u, float v
   ) {
      float x = cx + (left.x * lx + up.x * uy) * size;
      float y = cy + (left.y * lx + up.y * uy) * size;
      float z = cz + (left.z * lx + up.z * uy) * size;
      consumer.addVertex(poseStack.last(), x, y, z)
         .setColor(r, g, b, a)
         .setUv(u, v)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(15728880)
         .setNormal(poseStack.last(), 0.0F, 1.0F, 0.0F);
   }

   private static void drawTrail(PoseStack poseStack, VertexConsumer consumer, Vector3f start, Vector3f end,
                                 int color, float width, Vector3f cameraLeft) {
      float a = ((color >>> 24) & 255) / 255.0F * 0.7F;
      float r = ((color >>> 16) & 255) / 255.0F;
      float g = ((color >>> 8) & 255) / 255.0F;
      float b = (color & 255) / 255.0F;
      float halfWidth = width * 0.5F;
      ribbonQuad(poseStack, consumer, start, end, -cameraLeft.x * halfWidth,
         -cameraLeft.y * halfWidth, -cameraLeft.z * halfWidth, r, g, b, a);
   }

   private static void drawDecal(PoseStack poseStack, VertexConsumer consumer, VFXParticle particle) {
      float size = particle.size * particle.billboardScale;
      Vector3f center = particle.position;
      int color = particle.color;
      float a = ((color >>> 24) & 255) / 255.0F * (1.0F - particle.lifeProgress());
      float r = ((color >>> 16) & 255) / 255.0F;
      float g = ((color >>> 8) & 255) / 255.0F;
      float b = (color & 255) / 255.0F;
      vertexAt(poseStack, consumer, center.x - size, center.y, center.z - size, r, g, b, a, 0.0F, 0.0F);
      vertexAt(poseStack, consumer, center.x - size, center.y, center.z + size, r, g, b, a, 0.0F, 1.0F);
      vertexAt(poseStack, consumer, center.x + size, center.y, center.z + size, r, g, b, a, 1.0F, 1.0F);
      vertexAt(poseStack, consumer, center.x + size, center.y, center.z - size, r, g, b, a, 1.0F, 0.0F);
   }

   private static void drawBeam(RenderLevelStageEvent event, PoseStack poseStack, VertexConsumer consumer, VFXBeam beam) {
      Vector3f dir = new Vector3f(beam.end).sub(beam.start);
      if (dir.lengthSquared() < 1.0E-6F) {
         return;
      }
      Vector3f right = dir.normalize().cross(new Vector3f(0.0F, 1.0F, 0.0F));
      if (right.lengthSquared() < 1.0E-6F) {
         right = dir.normalize().cross(new Vector3f(1.0F, 0.0F, 0.0F));
      }
      right.normalize();
      float alpha = beam.alpha();
      int color = beam.color;
      float r = ((color >>> 16) & 255) / 255.0F;
      float g = ((color >>> 8) & 255) / 255.0F;
      float b = (color & 255) / 255.0F;
      float halfWidth = beam.widthStart * 0.5F;
      ribbonQuad(poseStack, consumer, beam.start, beam.end, right.x * halfWidth,
         right.y * halfWidth, right.z * halfWidth, r, g, b, alpha);
   }

   private static void drawRingShockwave(PoseStack poseStack, VertexConsumer consumer, VFXRingShockwave ring) {
      float radius = ring.radius();
      int color = ring.color;
      float a = ((color >>> 24) & 255) / 255.0F * ring.alpha();
      float r = ((color >>> 16) & 255) / 255.0F;
      float g = ((color >>> 8) & 255) / 255.0F;
      float b = (color & 255) / 255.0F;
      Vector3f c = ring.center;
      vertexAt(poseStack, consumer, c.x - radius, c.y, c.z - radius, r, g, b, a, 0.0F, 0.0F);
      vertexAt(poseStack, consumer, c.x - radius, c.y, c.z + radius, r, g, b, a, 0.0F, 1.0F);
      vertexAt(poseStack, consumer, c.x + radius, c.y, c.z + radius, r, g, b, a, 1.0F, 1.0F);
      vertexAt(poseStack, consumer, c.x + radius, c.y, c.z - radius, r, g, b, a, 1.0F, 0.0F);
   }

   private static void ribbonQuad(PoseStack poseStack, VertexConsumer consumer, Vector3f start, Vector3f end,
                                  float rightX, float rightY, float rightZ, float r, float g, float b, float a) {
      vertexAt(poseStack, consumer, start.x - rightX, start.y - rightY, start.z - rightZ, r, g, b, a, 0.0F, 0.0F);
      vertexAt(poseStack, consumer, start.x + rightX, start.y + rightY, start.z + rightZ, r, g, b, a, 1.0F, 0.0F);
      vertexAt(poseStack, consumer, end.x + rightX, end.y + rightY, end.z + rightZ, r, g, b, a, 1.0F, 1.0F);
      vertexAt(poseStack, consumer, end.x - rightX, end.y - rightY, end.z - rightZ, r, g, b, a, 0.0F, 1.0F);
   }

   private static void vertexAt(PoseStack poseStack, VertexConsumer consumer, float x, float y, float z,
                                float r, float g, float b, float a, float u, float v) {
      consumer.addVertex(poseStack.last(), x, y, z)
         .setColor(r, g, b, a)
         .setUv(u, v)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(15728880)
         .setNormal(poseStack.last(), 0.0F, 1.0F, 0.0F);
   }

}

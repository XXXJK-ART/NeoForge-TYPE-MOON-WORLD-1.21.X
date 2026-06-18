package net.xxxjk.TYPE_MOON_WORLD.vfx.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXBlendMode;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXEmitter;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXParticle;
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
   private static final List<VFXEmitter> EMITTERS = new ArrayList<>();
   private static final List<VFXBeam> BEAMS = new ArrayList<>();
   private static final List<VFXRingShockwave> RINGS = new ArrayList<>();
   private static final List<TrailPoint> TRAILS = new ArrayList<>();
   private static long clientTick;

   private VFXRenderManager() {
   }

   public static void addEmitter(VFXEmitter emitter) {
      EMITTERS.add(emitter);
   }

   public static void addBeam(VFXBeam beam) {
      BEAMS.add(beam);
   }

   public static void addRingShockwave(VFXRingShockwave ring) {
      RINGS.add(ring);
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
      TRAILS.clear();
      VFXEnvironmentManager.clear();
   }

   @SubscribeEvent
   public static void onClientTick(ClientTickEvent.Post event) {
      clientTick++;
      float delta = 1.0F / 20.0F;
      Iterator<VFXEmitter> iterator = EMITTERS.iterator();
      List<VFXEmitter> spawnedByHooks = new ArrayList<>();
      while (iterator.hasNext()) {
         VFXEmitter emitter = iterator.next();
         if (!emitter.tick(delta)) {
            emitter.releaseAll();
            iterator.remove();
         } else {
            for (String subEffect : emitter.drainTriggeredSubEffects()) {
               var definition = net.xxxjk.TYPE_MOON_WORLD.vfx.data.EffectLibrary.INSTANCE.get(subEffect);
               if (definition != null) {
                  spawnedByHooks.addAll(definition.createEmitters(emitter.origin().x, emitter.origin().y, emitter.origin().z, clientTick));
               }
            }
            for (VFXVanillaParticleSpawn spawn : emitter.drainVanillaParticleSpawns()) {
               Minecraft minecraft = Minecraft.getInstance();
               if (minecraft.level != null) {
                  minecraft.level.addParticle(
                     spawn.options(),
                     spawn.position().x,
                     spawn.position().y,
                     spawn.position().z,
                     spawn.velocity().x,
                     spawn.velocity().y,
                     spawn.velocity().z
                  );
               }
            }
         }
      }
      EMITTERS.addAll(spawnedByHooks);
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
   }

   @SubscribeEvent
   public static void onRenderLevelStage(RenderLevelStageEvent event) {
      if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || EMITTERS.isEmpty()) {
         return;
      }
      Minecraft mc = Minecraft.getInstance();
      if (mc.level == null || mc.player == null) {
         return;
      }
      PoseStack poseStack = event.getPoseStack();
      Vec3 camera = event.getCamera().getPosition();
      poseStack.pushPose();
      poseStack.translate(-camera.x, -camera.y, -camera.z);
      BufferSource source = mc.renderBuffers().bufferSource();
      RenderType translucent = NeoForgeRenderTypes.getUnlitTranslucent(PARTICLE_TEXTURE, false);
      RenderType additive = RenderType.entityTranslucentEmissive(PARTICLE_TEXTURE);
      RenderType beamType = RenderType.entityTranslucentEmissive(BEAM_TEXTURE);
      RenderType ringType = RenderType.entityTranslucentEmissive(RING_TEXTURE);
      for (VFXEmitter emitter : EMITTERS) {
         VertexConsumer consumer = source.getBuffer(emitter.blendMode() == VFXBlendMode.ADDITIVE ? additive : translucent);
         for (VFXParticle particle : emitter.particles()) {
            drawBillboard(event, poseStack, consumer, particle);
            if (emitter.isTrailEnabled()) {
               TRAILS.add(new TrailPoint(particle.previousPosition, particle.position, particle.color, particle.size));
            }
         }
      }
      for (TrailPoint trail : TRAILS) {
         drawTrail(event, poseStack, source.getBuffer(additive), trail);
      }
      TRAILS.clear();
      for (VFXBeam beam : BEAMS) {
         drawBeam(event, poseStack, source.getBuffer(beamType), beam);
      }
      for (VFXRingShockwave ring : RINGS) {
         drawRingShockwave(poseStack, source.getBuffer(ringType), ring);
      }
      source.endBatch(translucent);
      source.endBatch(additive);
      source.endBatch(beamType);
      source.endBatch(ringType);
      poseStack.popPose();
   }

   private static void drawBillboard(RenderLevelStageEvent event, PoseStack poseStack, VertexConsumer consumer, VFXParticle particle) {
      float size = particle.size * (1.0F - particle.lifeProgress() * 0.35F);
      Vector3f left = new Vector3f(-size, 0.0F, 0.0F).rotate(event.getCamera().rotation());
      Vector3f up = new Vector3f(0.0F, size, 0.0F).rotate(event.getCamera().rotation());
      Vector3f center = particle.position;
      int color = particle.color;
      float a = ((color >>> 24) & 255) / 255.0F * (1.0F - particle.lifeProgress());
      float r = ((color >>> 16) & 255) / 255.0F;
      float g = ((color >>> 8) & 255) / 255.0F;
      float b = (color & 255) / 255.0F;
      vertex(poseStack, consumer, center, left, up, -1.0F, -1.0F, r, g, b, a, 0.0F, 0.0F);
      vertex(poseStack, consumer, center, left, up, -1.0F, 1.0F, r, g, b, a, 0.0F, 1.0F);
      vertex(poseStack, consumer, center, left, up, 1.0F, 1.0F, r, g, b, a, 1.0F, 1.0F);
      vertex(poseStack, consumer, center, left, up, 1.0F, -1.0F, r, g, b, a, 1.0F, 0.0F);
   }

   private static void vertex(
      PoseStack poseStack,
      VertexConsumer consumer,
      Vector3f center,
      Vector3f left,
      Vector3f up,
      float lx,
      float uy,
      float r,
      float g,
      float b,
      float a,
      float u,
      float v
   ) {
      float x = center.x + left.x * lx + up.x * uy;
      float y = center.y + left.y * lx + up.y * uy;
      float z = center.z + left.z * lx + up.z * uy;
      consumer.addVertex(poseStack.last(), x, y, z)
         .setColor(r, g, b, a)
         .setUv(u, v)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(15728880)
         .setNormal(poseStack.last(), 0.0F, 1.0F, 0.0F);
   }

   private static void drawTrail(RenderLevelStageEvent event, PoseStack poseStack, VertexConsumer consumer, TrailPoint trail) {
      Vector3f cameraRight = new Vector3f(1.0F, 0.0F, 0.0F).rotate(event.getCamera().rotation()).normalize().mul(trail.width * 0.5F);
      int color = trail.color;
      float a = ((color >>> 24) & 255) / 255.0F * 0.7F;
      float r = ((color >>> 16) & 255) / 255.0F;
      float g = ((color >>> 8) & 255) / 255.0F;
      float b = (color & 255) / 255.0F;
      Vector3f start = trail.start;
      Vector3f end = trail.end;
      ribbonQuad(poseStack, consumer, start, end, cameraRight, r, g, b, a);
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
      Vector3f wide = new Vector3f(right).mul(beam.widthStart * 0.5F);
      ribbonQuad(poseStack, consumer, beam.start, beam.end, wide, r, g, b, alpha);
   }

   private static void drawRingShockwave(PoseStack poseStack, VertexConsumer consumer, VFXRingShockwave ring) {
      float radius = ring.radius();
      int color = ring.color;
      float a = ((color >>> 24) & 255) / 255.0F * ring.alpha();
      float r = ((color >>> 16) & 255) / 255.0F;
      float g = ((color >>> 8) & 255) / 255.0F;
      float b = (color & 255) / 255.0F;
      Vector3f c = ring.center;
      vertexAt(poseStack, consumer, new Vector3f(c.x - radius, c.y, c.z - radius), r, g, b, a, 0.0F, 0.0F);
      vertexAt(poseStack, consumer, new Vector3f(c.x - radius, c.y, c.z + radius), r, g, b, a, 0.0F, 1.0F);
      vertexAt(poseStack, consumer, new Vector3f(c.x + radius, c.y, c.z + radius), r, g, b, a, 1.0F, 1.0F);
      vertexAt(poseStack, consumer, new Vector3f(c.x + radius, c.y, c.z - radius), r, g, b, a, 1.0F, 0.0F);
   }

   private static void ribbonQuad(PoseStack poseStack, VertexConsumer consumer, Vector3f start, Vector3f end, Vector3f right, float r, float g, float b, float a) {
      vertexAt(poseStack, consumer, new Vector3f(start).sub(right), r, g, b, a, 0.0F, 0.0F);
      vertexAt(poseStack, consumer, new Vector3f(start).add(right), r, g, b, a, 1.0F, 0.0F);
      vertexAt(poseStack, consumer, new Vector3f(end).add(right), r, g, b, a, 1.0F, 1.0F);
      vertexAt(poseStack, consumer, new Vector3f(end).sub(right), r, g, b, a, 0.0F, 1.0F);
   }

   private static void vertexAt(PoseStack poseStack, VertexConsumer consumer, Vector3f position, float r, float g, float b, float a, float u, float v) {
      consumer.addVertex(poseStack.last(), position.x, position.y, position.z)
         .setColor(r, g, b, a)
         .setUv(u, v)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(15728880)
         .setNormal(poseStack.last(), 0.0F, 1.0F, 0.0F);
   }

   private static final class TrailPoint {
      private final Vector3f start;
      private final Vector3f end;
      private final int color;
      private final float width;

      private TrailPoint(Vector3f start, Vector3f end, int color, float width) {
         this.start = new Vector3f(start);
         this.end = new Vector3f(end);
         this.color = color;
         this.width = width;
      }
   }
}

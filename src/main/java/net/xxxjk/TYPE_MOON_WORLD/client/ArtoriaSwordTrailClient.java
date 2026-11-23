package net.xxxjk.TYPE_MOON_WORLD.client;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.servant.sasaki.SasakiKojiroAction;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.DiarmuidTrailRenderType;

/** Artoria's sword wake follows the actual Excalibur tip instead of Sasaki's authored arc. */
@EventBusSubscriber(modid = TYPE_MOON_WORLD.MOD_ID, value = Dist.CLIENT)
public final class ArtoriaSwordTrailClient {
   private static final int MAX_POINTS = 40;
   private static final float TAIL_LIFE = 6.0F;
   private static final Map<UUID, Swing> SWINGS = new HashMap<>();
   private static net.minecraft.client.multiplayer.ClientLevel activeLevel;

   private ArtoriaSwordTrailClient() { }

   public static boolean isArtoria(LivingEntity owner) {
      return owner instanceof net.xxxjk.TYPE_MOON_WORLD.servant.entity.ArtoriaPendragonEntity
         || owner != null && owner.getMainHandItem().is(ModItems.EXCALIBUR.get());
   }

   public static boolean isOkita(LivingEntity owner) {
      return owner instanceof net.xxxjk.TYPE_MOON_WORLD.servant.entity.OkitaSoujiSaberEntity
         || owner != null && owner.getMainHandItem().is(ModItems.KIKU_ICHIMONJI_NORIMUNE.get());
   }

   public static void begin(LivingEntity owner, SasakiKojiroAction action) {
      if (owner == null || action == null) return;
      ensureLevel();
      if (strikeTicks(action).length == 0) {
         SWINGS.remove(owner.getUUID());
         return;
      }
      SWINGS.put(owner.getUUID(), new Swing(owner, action, owner.level().getGameTime()));
   }

   private static void ensureLevel() {
      var level = Minecraft.getInstance().level;
      if (activeLevel != level) {
         SWINGS.clear();
         activeLevel = level;
      }
   }

   private static int[] strikeTicks(SasakiKojiroAction action) {
      return switch (action) {
         case VERTICAL_SLASH -> new int[]{5};
         case DOWNWARD_SLASH, UPWARD_SLASH -> new int[]{2};
         case HORIZONTAL_SLASH -> new int[]{3};
         case THRUST, OKITA_THRUST -> new int[]{5};
         case OKITA_MUMYOUDAN_ZUKI -> net.xxxjk.TYPE_MOON_WORLD.servant.entity.OkitaSoujiSaberTechniqueService.mumyoudanStrikeTicks();
         case SPINNING_SLASH -> new int[]{4, 11};
         case CROUCH_CUTBACK -> new int[]{3, 11};
         default -> new int[0];
      };
   }

   private static int activeStroke(SasakiKojiroAction action, float age) {
      int[] ticks = strikeTicks(action);
      for (int i = 0; i < ticks.length; i++) {
         float lead = action == SasakiKojiroAction.OKITA_MUMYOUDAN_ZUKI ? 1.5F : 0.0F;
         float tail = action == SasakiKojiroAction.OKITA_MUMYOUDAN_ZUKI ? 0.5F : 3.5F;
         if (age >= ticks[i] - lead && age <= ticks[i] + tail) return i;
      }
      return -1;
   }

   @SubscribeEvent
   public static void render(RenderLevelStageEvent event) {
      if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
      ensureLevel();
      if (activeLevel == null) return;
      long now = activeLevel.getGameTime();
      SWINGS.values().removeIf(swing -> !swing.owner.isAlive() || swing.owner.level() != activeLevel
         || now - swing.started >= swing.action.durationTicks());

      PoseStack poses = event.getPoseStack();
      MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
      Vec3 camera = event.getCamera().getPosition();
      float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
      for (Swing swing : SWINGS.values()) {
         float age = now - swing.started + partial;
         int stroke = activeStroke(swing.action, age);
         if (stroke >= 0 && stroke != swing.activeStroke) {
            swing.points.clear();
            swing.activeStroke = stroke;
         }
         if (stroke < 0 || !(swing.owner.getMainHandItem().is(ModItems.EXCALIBUR.get()) || isOkita(swing.owner))) continue;
         float sampleFrom = Math.max(swing.lastSampleAge + 0.05F, age - 1.0F);
         for (float sample = sampleFrom; sample < age; sample += 0.10F) sample(swing, sample, partial);
         sample(swing, age, partial);
         while (!swing.points.isEmpty() && age - swing.points.peekFirst().age > TAIL_LIFE)
            swing.points.removeFirst();
         if (swing.points.size() >= 2) renderRibbon(poses, buffers, swing, camera, age);
         swing.lastSampleAge = age;
      }
   }

   private static void sample(Swing swing, float age, float partial) {
      Vec3 localTip = SasakiKojiroActionAnimationClient.bladeTip(swing.owner, swing.action, age);
      if (localTip == null) return;
      float yaw = Mth.rotLerp(partial, swing.owner.yBodyRotO, swing.owner.yBodyRot);
      Vec3 worldTip = swing.owner.getPosition(partial).add(localTip.yRot((180.0F - yaw) * Mth.DEG_TO_RAD));
      Vec3 last = swing.points.peekLast() == null ? null : swing.points.peekLast().position;
      if (last == null || last.distanceToSqr(worldTip) > 0.00001) {
         swing.points.addLast(new Point(worldTip, age));
         while (swing.points.size() > MAX_POINTS) swing.points.removeFirst();
      }
   }

   private static void renderRibbon(PoseStack poses, MultiBufferSource.BufferSource buffers, Swing swing,
                                    Vec3 camera, float age) {
      Mode mode = mode(swing.owner);
      RenderType type = DiarmuidTrailRenderType.trail();
      VertexConsumer vertices = buffers.getBuffer(type);
      List<Point> path = new ArrayList<>(swing.points);
      for (int layer = 0; layer < 8; layer++) {
         Vec3 previousSide = Vec3.ZERO;
         for (int i = 1; i < path.size(); i++) {
            Vec3 a = path.get(i - 1).position.subtract(camera);
            Vec3 b = path.get(i).position.subtract(camera);
            Vec3 tangent = b.subtract(a);
            if (tangent.lengthSqr() < 1.0E-10) continue;
            Vec3 side = tangent.cross(a.scale(-1)).normalize();
            if (side.lengthSqr() < 1.0E-8) side = tangent.cross(new Vec3(0, 1, 0)).normalize();
            if (side.dot(previousSide) < 0) side = side.scale(-1);
            Vec3 before = previousSide.lengthSqr() < 1.0E-8 ? side : previousSide;
            float lifeA = 1.0F - Mth.clamp((age - path.get(i - 1).age) / TAIL_LIFE, 0.0F, 1.0F);
            float lifeB = 1.0F - Mth.clamp((age - path.get(i).age) / TAIL_LIFE, 0.0F, 1.0F);
            double widthA = 0.34 * Math.sqrt(lifeA);
            double widthB = 0.34 * Math.sqrt(lifeB);
            float edgeA = -1.0F + layer * 0.25F;
            float edgeB = edgeA + 0.25F;
            vertex(poses, vertices, a.add(before.scale(widthA * edgeA)), edgeA, lifeA, mode);
            vertex(poses, vertices, b.add(side.scale(widthB * edgeA)), edgeA, lifeB, mode);
            vertex(poses, vertices, b.add(side.scale(widthB * edgeB)), edgeB, lifeB, mode);
            vertex(poses, vertices, a.add(before.scale(widthA * edgeB)), edgeB, lifeA, mode);
            previousSide = side;
         }
      }
      buffers.endBatch(type);
   }

   private static void vertex(PoseStack poses, VertexConsumer vertices, Vec3 point, float edge,
                              float life, Mode mode) {
      float center = 1.0F - Math.abs(edge);
      float hot = (float)Math.pow(Math.max(0.0F, center), 5.0F) * life;
      float r = Mth.lerp(hot, mode.edgeR, mode.coreR);
      float g = Mth.lerp(hot, mode.edgeG, mode.coreG);
      float b = Mth.lerp(hot, mode.edgeB, mode.coreB);
      float alpha = (float)Math.pow(Math.max(0.0F, center), 1.4F) * life * 0.82F;
      vertices.addVertex(poses.last(), (float)point.x, (float)point.y, (float)point.z)
         .setColor(r, g, b, alpha);
   }

   private static Mode mode(LivingEntity owner) {
      if (isOkita(owner)) return Mode.ASAGI;
      ItemStack stack = owner.getMainHandItem();
      CompoundTag data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
      if (data.getBoolean("ArtoriaManaBurstActive")) return Mode.GOLD;
      if (data.getBoolean("ServantCardArtoriaWindVeiled") || data.getBoolean("ArtoriaWindDefenseActive")) return Mode.CYAN;
      return Mode.WHITE;
   }

   private enum Mode {
      WHITE(0.72F, 0.84F, 0.96F, 1.0F, 1.0F, 1.0F),
      CYAN(0.03F, 0.38F, 0.72F, 0.68F, 1.0F, 1.0F),
      ASAGI(0.22F, 0.73F, 0.76F, 0.79F, 0.97F, 0.93F),
      GOLD(1.0F, 0.52F, 0.03F, 1.0F, 1.0F, 0.62F);

      private final float edgeR, edgeG, edgeB, coreR, coreG, coreB;

      Mode(float edgeR, float edgeG, float edgeB, float coreR, float coreG, float coreB) {
         this.edgeR = edgeR;
         this.edgeG = edgeG;
         this.edgeB = edgeB;
         this.coreR = coreR;
         this.coreG = coreG;
         this.coreB = coreB;
      }
   }

   private static final class Swing {
      private final LivingEntity owner;
      private final SasakiKojiroAction action;
      private final long started;
      private final ArrayDeque<Point> points = new ArrayDeque<>();
      private int activeStroke = -1;
      private float lastSampleAge;

      private Swing(LivingEntity owner, SasakiKojiroAction action, long started) {
         this.owner = owner;
         this.action = action;
         this.started = started;
      }
   }

   private record Point(Vec3 position, float age) { }
}

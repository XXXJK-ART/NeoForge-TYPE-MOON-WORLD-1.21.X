package net.xxxjk.TYPE_MOON_WORLD.client;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.servant.sasaki.SasakiKojiroAction;

/** Draws a brief luminous ribbon along the actual held sword tip during each strike. */
@EventBusSubscriber(modid = TYPE_MOON_WORLD.MOD_ID, value = Dist.CLIENT)
public final class SasakiSwordTrailClient {
   private static final int MAX_TRAIL_POINTS = 14;
   private static final float STRIKE_WINDOW_TICKS = 3.5F;
   private static final Map<UUID, Swing> SWINGS = new HashMap<>();
   private static net.minecraft.client.multiplayer.ClientLevel activeLevel;

   private SasakiSwordTrailClient() { }

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
         case CROUCH_CUTBACK -> new int[]{3, 11};
         case THRUST -> new int[]{5};
         case SPINNING_SLASH -> new int[]{4, 11};
         default -> new int[0];
      };
   }

   private static int activeStroke(SasakiKojiroAction action, float age) {
      int[] ticks = strikeTicks(action);
      for (int i = 0; i < ticks.length; i++) {
         if (age >= ticks[i] && age <= ticks[i] + STRIKE_WINDOW_TICKS) return i;
      }
      return -1;
   }

   @SubscribeEvent
   public static void render(RenderLevelStageEvent event) {
      if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
      ensureLevel();
      if (activeLevel == null) return;
      SWINGS.values().removeIf(swing -> !swing.owner.isAlive() || swing.owner.level() != activeLevel
         || activeLevel.getGameTime() - swing.started >= swing.action.durationTicks());

      var poses = event.getPoseStack();
      var buffers = Minecraft.getInstance().renderBuffers().bufferSource();
      Vec3 camera = event.getCamera().getPosition();
      float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
      for (Swing swing : SWINGS.values()) {
         float age = activeLevel.getGameTime() - swing.started + partial;
         int stroke = activeStroke(swing.action, age);
         if (stroke >= 0 && stroke != swing.activeStroke) {
            swing.points.clear();
            swing.activeStroke = stroke;
         }
         if (stroke >= 0 && hasSwordInMainHand(swing.owner)) {
            Vec3 localTip = SasakiKojiroActionAnimationClient.bladeTip(swing.owner, swing.action, age);
            if (localTip != null) {
               float yaw = net.minecraft.util.Mth.rotLerp(partial, swing.owner.yBodyRotO, swing.owner.yBodyRot);
               Vec3 worldTip = swing.owner.getPosition(partial).add(localTip.yRot((180.0F - yaw) * net.minecraft.util.Mth.DEG_TO_RAD));
               swing.addPoint(worldTip, age);
            }
         }
         if (swing.points.size() < 2) continue;
         float ageSinceLastSample = age - swing.lastSampleAge;
         if (ageSinceLastSample >= 6.0F) continue;
         List<Vec3> cameraRelativePath = new ArrayList<>(swing.points.size());
         for (Vec3 point : swing.points) cameraRelativePath.add(point.subtract(camera));
         SasakiTsubameSlashRenderer.renderBladeTrail(poses, buffers, ageSinceLastSample,
            cameraRelativePath, Vec3.ZERO);
      }
   }

   private static boolean hasSwordInMainHand(LivingEntity owner) {
      var stack = owner.getMainHandItem();
      return !stack.isEmpty() && (stack.getItem() instanceof SwordItem || stack.is(ItemTags.SWORDS));
   }

   private static final class Swing {
      private final LivingEntity owner;
      private final SasakiKojiroAction action;
      private final long started;
      private final ArrayDeque<Vec3> points = new ArrayDeque<>();
      private int activeStroke = -1;
      private float lastSampleAge;

      private Swing(LivingEntity owner, SasakiKojiroAction action, long started) {
         this.owner = owner;
         this.action = action;
         this.started = started;
      }

      private void addPoint(Vec3 point, float age) {
         Vec3 last = points.peekLast();
         if (last == null || last.distanceToSqr(point) > 0.00001) {
            points.addLast(point);
            while (points.size() > MAX_TRAIL_POINTS) points.removeFirst();
         }
         lastSampleAge = age;
      }
   }

}

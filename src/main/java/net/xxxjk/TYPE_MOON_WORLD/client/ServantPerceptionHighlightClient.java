package net.xxxjk.TYPE_MOON_WORLD.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.xxxjk.TYPE_MOON_WORLD.servant.perception.ServantPerceptionRules;

@EventBusSubscriber(value = Dist.CLIENT)
public final class ServantPerceptionHighlightClient {
   private record Highlight(int color, int started, int until) { }
   private static final Map<Integer, Highlight> HIGHLIGHTS = new HashMap<>();
   private static ClientLevel trackedLevel;
   private static int clientTick;
   private ServantPerceptionHighlightClient() { }

   public static void apply(List<net.xxxjk.TYPE_MOON_WORLD.network.ServantPerceptionMessage.Target> targets) {
      if (targets == null || targets.isEmpty()) {
         HIGHLIGHTS.clear();
         return;
      }
      int until = clientTick + ServantPerceptionRules.BRIGHT_TICKS + ServantPerceptionRules.DARK_TICKS;
      for (var target : targets) if (target != null) {
         Highlight old = HIGHLIGHTS.get(target.entityId());
         int started = old != null && old.color() == target.color() ? old.started() : clientTick;
         HIGHLIGHTS.put(target.entityId(), new Highlight(target.color(), started, until));
      }
   }

   @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
      clientTick++;
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.level != trackedLevel) { HIGHLIGHTS.clear(); trackedLevel = minecraft.level; }
      HIGHLIGHTS.entrySet().removeIf(entry -> entry.getValue().until() <= clientTick);
   }

   @SubscribeEvent public static void render(RenderLevelStageEvent event) {
      if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || HIGHLIGHTS.isEmpty()) return;
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.level == null) return;
      OutlineBufferSource outlines = minecraft.renderBuffers().outlineBufferSource();
      PoseStack poseStack = event.getPoseStack();
      Vec3 camera = event.getCamera().getPosition();
      float partial = event.getPartialTick().getGameTimeDeltaTicks();
      boolean rendered = false;
      for (var entry : HIGHLIGHTS.entrySet()) {
         Highlight highlight = entry.getValue();
         int age = Math.floorMod(clientTick - highlight.started(), ServantPerceptionRules.BRIGHT_TICKS + ServantPerceptionRules.DARK_TICKS);
         if (age >= ServantPerceptionRules.BRIGHT_TICKS) continue;
         Entity entity = minecraft.level.getEntity(entry.getKey());
         if (!(entity instanceof LivingEntity living) || !living.isAlive()) continue;
         int[] rgb = color(highlight.color());
         outlines.setColor(rgb[0], rgb[1], rgb[2], 255);
         double x = Mth.lerp((double)partial, entity.xOld, entity.getX()) - camera.x;
         double y = Mth.lerp((double)partial, entity.yOld, entity.getY()) - camera.y;
         double z = Mth.lerp((double)partial, entity.zOld, entity.getZ()) - camera.z;
         boolean glowing = entity.hasGlowingTag();
         entity.setGlowingTag(true);
         poseStack.pushPose();
         try {
            minecraft.getEntityRenderDispatcher().render(entity, x, y, z,
               Mth.lerp(partial, entity.yRotO, entity.getYRot()), partial, poseStack, outlines,
               minecraft.getEntityRenderDispatcher().getPackedLightCoords(entity, partial));
            rendered = true;
         } finally { entity.setGlowingTag(glowing); poseStack.popPose(); }
      }
      if (rendered) event.getLevelRenderer().requestOutlineEffect();
   }

   private static int[] color(int color) {
      var relation = ServantPerceptionRules.RelationColor.values()[Math.max(0, Math.min(3, color))];
      return new int[]{relation.red(), relation.green(), relation.blue()};
   }
}

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

@EventBusSubscriber(value = Dist.CLIENT)
public final class EnkiduDetectionHighlightClient {
   private static final Map<Integer, Integer> HIGHLIGHT_UNTIL = new HashMap<>();
   private static ClientLevel trackedLevel;
   private static int clientTick;

   private EnkiduDetectionHighlightClient() {
   }

   public static void apply(List<Integer> entityIds, int ticks) {
      int until = clientTick + Math.max(1, ticks);
      for (Integer id : entityIds) {
         if (id != null) {
            HIGHLIGHT_UNTIL.put(id, until);
         }
      }
   }

   @SubscribeEvent
   public static void onClientTick(ClientTickEvent.Post event) {
      clientTick++;
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.level != trackedLevel) {
         HIGHLIGHT_UNTIL.clear();
         trackedLevel = minecraft.level;
      }
      if (HIGHLIGHT_UNTIL.isEmpty() || minecraft.level == null) {
         return;
      }
      HIGHLIGHT_UNTIL.entrySet().removeIf(entry -> entry.getValue() <= clientTick);
   }

   @SubscribeEvent
   public static void onRenderLevel(RenderLevelStageEvent event) {
      if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || HIGHLIGHT_UNTIL.isEmpty()) {
         return;
      }
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.level == null || minecraft.player == null) {
         return;
      }
      OutlineBufferSource outlines = minecraft.renderBuffers().outlineBufferSource();
      PoseStack poseStack = event.getPoseStack();
      Vec3 camera = event.getCamera().getPosition();
      float partialTick = event.getPartialTick().getGameTimeDeltaTicks();
      boolean renderedAny = false;

      for (Integer id : HIGHLIGHT_UNTIL.keySet()) {
         Entity entity = minecraft.level.getEntity(id);
         if (!(entity instanceof LivingEntity living) || !living.isAlive() || entity == minecraft.player) {
            continue;
         }
         outlines.setColor(255, 202, 56, 255);
         double x = Mth.lerp((double)partialTick, entity.xOld, entity.getX()) - camera.x;
         double y = Mth.lerp((double)partialTick, entity.yOld, entity.getY()) - camera.y;
         double z = Mth.lerp((double)partialTick, entity.zOld, entity.getZ()) - camera.z;
         float yaw = Mth.lerp(partialTick, entity.yRotO, entity.getYRot());
         boolean originallyGlowing = entity.hasGlowingTag();
         entity.setGlowingTag(true);
         try {
            minecraft.getEntityRenderDispatcher().render(
               entity,
               x,
               y,
               z,
               yaw,
               partialTick,
               poseStack,
               outlines,
               minecraft.getEntityRenderDispatcher().getPackedLightCoords(entity, partialTick)
            );
            renderedAny = true;
         } finally {
            entity.setGlowingTag(originallyGlowing);
         }
      }
      if (renderedAny) {
         event.getLevelRenderer().requestOutlineEffect();
      }
   }
}

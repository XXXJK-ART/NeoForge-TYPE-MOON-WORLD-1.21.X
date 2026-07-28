package net.xxxjk.TYPE_MOON_WORLD.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.event.entity.living.EffectParticleModificationEvent;
import net.neoforged.neoforge.common.util.TriState;

@EventBusSubscriber(value = Dist.CLIENT)
public final class ShadowHassanClientEvents {
   private static HitResult savedHudHitResult;
   private static Entity savedHudCrosshairTarget;
   private static boolean hudTargetMasked;

   private ShadowHassanClientEvents() {
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void hideConcealedEntity(RenderLivingEvent.Pre<?, ?> event) {
      if (ServantCardConcealmentClient.isPerfectlyConcealed(event.getEntity())) {
         event.setCanceled(true);
      }
   }

   @SubscribeEvent(priority = EventPriority.LOWEST)
   public static void hideConcealedName(RenderNameTagEvent event) {
      if (ServantCardConcealmentClient.isPerfectlyConcealed(event.getEntity())) {
         event.setCanRender(TriState.FALSE);
      }
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void hideConcealedEffectParticles(EffectParticleModificationEvent event) {
      if (ServantCardConcealmentClient.isPerfectlyConcealed(event.getEntity())) event.setVisible(false);
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void hideConcealedHudTarget(RenderGuiEvent.Pre event) {
      restoreHudTarget();
      Minecraft minecraft = Minecraft.getInstance();
      Entity concealed = concealedCrosshairTarget(minecraft);
      if (concealed == null) return;

      savedHudHitResult = minecraft.hitResult;
      savedHudCrosshairTarget = minecraft.crosshairPickEntity;
      hudTargetMasked = true;
      HitResult hit = minecraft.hitResult;
      net.minecraft.world.phys.Vec3 location = hit != null ? hit.getLocation() : concealed.position();
      Direction direction = minecraft.getCameraEntity() != null
         ? Direction.getNearest(minecraft.getCameraEntity().getLookAngle()) : Direction.UP;
      minecraft.hitResult = BlockHitResult.miss(location, direction, BlockPos.containing(location));
      minecraft.crosshairPickEntity = null;
   }

   @SubscribeEvent(priority = EventPriority.LOWEST)
   public static void restoreConcealedHudTarget(RenderGuiEvent.Post event) {
      restoreHudTarget();
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void restoreConcealedHudTargetBeforeTick(ClientTickEvent.Pre event) {
      restoreHudTarget();
   }

   private static Entity concealedCrosshairTarget(Minecraft minecraft) {
      Entity target = minecraft.hitResult instanceof EntityHitResult entityHit
         ? entityHit.getEntity() : minecraft.crosshairPickEntity;
      return ServantCardConcealmentClient.isPerfectlyConcealed(target) ? target : null;
   }

   private static void restoreHudTarget() {
      if (!hudTargetMasked) return;
      Minecraft minecraft = Minecraft.getInstance();
      minecraft.hitResult = savedHudHitResult;
      minecraft.crosshairPickEntity = savedHudCrosshairTarget;
      savedHudHitResult = null;
      savedHudCrosshairTarget = null;
      hudTargetMasked = false;
   }
}

package net.xxxjk.TYPE_MOON_WORLD.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.network.OkitaMumyoudanImpactMessage;

@EventBusSubscriber(modid = TYPE_MOON_WORLD.MOD_ID, value = Dist.CLIENT)
public final class OkitaMumyoudanImpactClient {
   private static ClientLevel impactLevel;
   private static long startedAt;
   private static float strength;

   private OkitaMumyoudanImpactClient() { }

   public static void receive(OkitaMumyoudanImpactMessage message) {
      var mc = Minecraft.getInstance();
      if (mc.level == null) return;
      if (mc.level.getEntity(message.targetId()) instanceof LivingEntity target) target.animateHurt(0.0F);
      var camera = mc.getCameraEntity();
      if (camera == null || camera.getId() != message.attackerId() && camera.getId() != message.targetId()) return;
      impactLevel = mc.level;
      startedAt = impactLevel.getGameTime();
      strength = message.strike() == 2 ? 0.65F : 0.35F;
   }

   @SubscribeEvent
   public static void camera(ViewportEvent.ComputeCameraAngles event) {
      var mc = Minecraft.getInstance();
      if (mc.level != impactLevel || mc.level == null) { strength = 0; impactLevel = null; return; }
      float age = (float)(mc.level.getGameTime() - startedAt + event.getPartialTick());
      if (strength <= 0 || age < 0 || age >= 3.0F) return;
      float envelope = 1.0F - age / 3.0F;
      float pulse = (float)Math.cos(age * Math.PI * 1.3) * envelope * strength;
      // Camera-only recoil: no input rotation, time pause, or target displacement.
      event.setPitch(event.getPitch() + pulse);
      event.setRoll(event.getRoll() + pulse * 0.45F);
   }
}

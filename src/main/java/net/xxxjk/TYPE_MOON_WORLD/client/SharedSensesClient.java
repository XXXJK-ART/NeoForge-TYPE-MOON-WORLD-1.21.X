package net.xxxjk.TYPE_MOON_WORLD.client;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(value = Dist.CLIENT)
public final class SharedSensesClient {
   private static int targetId = -1;
   private static boolean active;
   private SharedSensesClient() { }
   public static void apply(boolean enabled, int entityId) { active = enabled; targetId = enabled ? entityId : -1; update(); }
   @SubscribeEvent public static void tick(ClientTickEvent.Post event) { if (active) update(); }
   private static void update() {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.player == null || minecraft.level == null) return;
      if (!active) { if (minecraft.getCameraEntity() != minecraft.player) minecraft.setCameraEntity(minecraft.player); return; }
      var target = minecraft.level.getEntity(targetId);
      if (target != null && !target.isRemoved() && target.isAlive()) minecraft.setCameraEntity(target);
      else if (minecraft.getCameraEntity() != minecraft.player) minecraft.setCameraEntity(minecraft.player);
   }
}

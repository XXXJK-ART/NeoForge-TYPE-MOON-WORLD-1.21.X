package net.xxxjk.TYPE_MOON_WORLD.vfx.client;

import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.vfx.data.EffectLibrary;
import net.xxxjk.TYPE_MOON_WORLD.vfx.data.VFXEffectDefinition;

@EventBusSubscriber(
   modid = TYPE_MOON_WORLD.MOD_ID,
   value = {Dist.CLIENT}
)
public final class VFXClientLoop {
   private static String activeEffectId;
   private static int ticksUntilNext;
   private static boolean fixedOrigin;
   private static double originX;
   private static double originY;
   private static double originZ;

   private VFXClientLoop() {
   }

   public static boolean start(String effectId) {
      VFXEffectDefinition definition = EffectLibrary.INSTANCE.get(effectId);
      if (definition == null) {
         return false;
      }
      activeEffectId = effectId;
      fixedOrigin = false;
      spawn(definition);
      ticksUntilNext = intervalTicks(definition);
      return true;
   }

   public static boolean start(String effectId, double x, double y, double z) {
      VFXEffectDefinition definition = EffectLibrary.INSTANCE.get(effectId);
      if (definition == null) {
         return false;
      }
      activeEffectId = effectId;
      fixedOrigin = true;
      originX = x;
      originY = y;
      originZ = z;
      spawn(definition);
      ticksUntilNext = intervalTicks(definition);
      return true;
   }

   public static boolean stop() {
      boolean wasRunning = activeEffectId != null;
      activeEffectId = null;
      ticksUntilNext = 0;
      fixedOrigin = false;
      return wasRunning;
   }

   @SubscribeEvent
   public static void onClientTick(ClientTickEvent.Post event) {
      if (activeEffectId == null) {
         return;
      }
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.level == null || minecraft.player == null) {
         return;
      }
      if (ticksUntilNext > 0) {
         ticksUntilNext--;
         return;
      }
      VFXEffectDefinition definition = EffectLibrary.INSTANCE.get(activeEffectId);
      if (definition == null) {
         stop();
         return;
      }
      spawn(definition);
      ticksUntilNext = intervalTicks(definition);
   }

   private static void spawn(VFXEffectDefinition definition) {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.level == null || minecraft.player == null || activeEffectId == null) {
         return;
      }
      long seed = minecraft.level.getGameTime() ^ System.nanoTime();
      double x = fixedOrigin ? originX : minecraft.player.getX();
      double y = fixedOrigin ? originY : minecraft.player.getY();
      double z = fixedOrigin ? originZ : minecraft.player.getZ();
      VFXClientRuntime.spawn(activeEffectId, x, y, z, Optional.empty(), seed);
   }

   private static int intervalTicks(VFXEffectDefinition definition) {
      return Math.max(1, Mth.ceil((definition.duration() + 0.5F) * 20.0F));
   }
}

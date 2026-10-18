package com.example.typemoonaddon.event;

import net.minecraft.server.level.ServerPlayer;
import com.example.typemoonaddon.airflow_blade.AirflowBladeService;
import com.example.typemoonaddon.detection.DetectionService;
import com.example.typemoonaddon.magic.EntityDisplacementService;
import com.example.typemoonaddon.magic.SummoningMagicIntegration;
import com.example.typemoonaddon.magic.WormMagicIntegration;
import com.example.typemoonaddon.imaginary_space.ImaginarySpaceService;
import com.example.typemoonaddon.orias.OriasService;
import com.example.typemoonaddon.andrephius.AndrephiusService;
import com.example.typemoonaddon.andrasias.AndrasiasService;
import com.example.typemoonaddon.storage.StorageService;
import com.example.typemoonaddon.storm.StormService;
import net.xxxjk.TYPE_MOON_WORLD.event.ServerPlayerTickHooks;

/** Addon's server-player work, invoked once by the core coordinator. */
public final class AddonPlayerTickServices {
   private static boolean registered;

   private AddonPlayerTickServices() {
   }

   public static synchronized void register() {
      if (!registered) {
         ServerPlayerTickHooks.register(AddonPlayerTickServices::tick);
         registered = true;
      }
   }

   public static void tick(ServerPlayer player) {
      // These services already contain their own active-state/cooldown checks.
      StormService.tick(player);
      StorageService.tick(player);
      DetectionService.tick(player);
      EntityDisplacementService.tick(player);
      ImaginarySpaceService.tickPlayer(player);
      SummoningMagicIntegration.tick(player);
      WormMagicIntegration.tickCollapse(player);
      AirflowBladeService.tickCharge(player);
      AndrephiusService.tickCharge(player);
      AndrasiasService.tickCharge(player);
      OriasService.tickCharge(player);
      ImaginaryDisplacementEvents.tick(player);
      WormEvents.tick(player);
      WormBodyEvents.tick(player);
      // Knowledge normalization is a one-time login/data-migration task;
      // keeping it off the simulation path avoids a per-player branch every
      // tick and preserves the PlayerEvent handlers in that service.
   }
}

package net.xxxjk.TYPE_MOON_WORLD.event;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.entity.GilgameshGateWeaponProjectileEntity;

/** Prevents short-lived Gate of Babylon projectiles from being persisted by chunk unloads. */
@EventBusSubscriber(modid = TYPE_MOON_WORLD.MOD_ID)
public final class GilgameshGateLifecycleEvents {
   private GilgameshGateLifecycleEvents() {
   }

   @SubscribeEvent
   public static void onChunkUnload(ChunkEvent.Unload event) {
      if (!(event.getLevel() instanceof ServerLevel level)) {
         return;
      }
      int minX = event.getChunk().getPos().getMinBlockX();
      int minZ = event.getChunk().getPos().getMinBlockZ();
      AABB bounds = new AABB(minX, level.getMinBuildHeight(), minZ,
         minX + 16.0D, level.getMaxBuildHeight(), minZ + 16.0D);
      for (GilgameshGateWeaponProjectileEntity projectile : level.getEntitiesOfClass(
         GilgameshGateWeaponProjectileEntity.class, bounds,
         entity -> entity.chunkPosition().equals(event.getChunk().getPos()))) {
         projectile.discard();
      }
   }
}

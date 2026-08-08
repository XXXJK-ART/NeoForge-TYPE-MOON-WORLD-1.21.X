package net.xxxjk.TYPE_MOON_WORLD.magic.unlimited_blade_works;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.block.entity.UBWWeaponBlockEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.UbwControlledSwordEntity;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

public final class UbwSwordControlService {
   public static final String MAGIC_ID = "ubw_sword_control";
   public static final double SCAN_RADIUS = 32.0D;
   public static final int MAX_SWORDS = 64;
   private static final double RELEASE_QUERY_RADIUS = 80.0D;

   private UbwSwordControlService() {
   }

   public static boolean execute(ServerPlayer player) {
      if (player == null || !player.isAlive() || player.isRemoved() || player.isSpectator()) {
         return false;
      }
      List<UbwControlledSwordEntity> waiting = waitingSwords(player);
      if (!waiting.isEmpty()) {
         release(player, waiting);
         player.displayClientMessage(Component.translatable(
            "message.typemoonworld.magic.ubw_sword_control.released", waiting.size()), true);
         return true;
      }
      int spawned = collect(player);
      if (spawned <= 0) {
         player.displayClientMessage(Component.translatable(
            "message.typemoonworld.magic.ubw_sword_control.no_weapons"), true);
         return false;
      }
      player.displayClientMessage(Component.translatable(
         "message.typemoonworld.magic.ubw_sword_control.raised", spawned), true);
      return true;
   }

   private static int collect(ServerPlayer player) {
      ServerLevel level = player.serverLevel();
      Vec3 center = player.position();
      int cap = swordLimit(player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES)
         .proficiency_unlimited_blade_works);
      List<UBWWeaponBlockEntity> candidates = loadedWeaponBlocks(level, center);
      candidates.sort(Comparator.comparingDouble(block -> Vec3.atCenterOf(block.getBlockPos()).distanceToSqr(center)));
      int spawned = 0;
      for (UBWWeaponBlockEntity blockEntity : candidates) {
         if (spawned >= cap) {
            break;
         }
         ItemStack weapon = blockEntity.getStoredItem();
         BlockPos pos = blockEntity.getBlockPos();
         if (weapon.isEmpty() || !level.isLoaded(pos)) {
            continue;
         }
         UbwControlledSwordEntity sword = new UbwControlledSwordEntity(
            level, player, weapon, Vec3.atCenterOf(pos));
         if (level.addFreshEntity(sword)) {
            level.removeBlock(pos, false);
            spawned++;
         }
      }
      return spawned;
   }

   private static List<UBWWeaponBlockEntity> loadedWeaponBlocks(ServerLevel level, Vec3 center) {
      List<UBWWeaponBlockEntity> result = new ArrayList<>();
      int minChunkX = ((int)Math.floor(center.x - SCAN_RADIUS)) >> 4;
      int maxChunkX = ((int)Math.floor(center.x + SCAN_RADIUS)) >> 4;
      int minChunkZ = ((int)Math.floor(center.z - SCAN_RADIUS)) >> 4;
      int maxChunkZ = ((int)Math.floor(center.z + SCAN_RADIUS)) >> 4;
      double radiusSqr = SCAN_RADIUS * SCAN_RADIUS;
      for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
         for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
            if (!level.hasChunk(chunkX, chunkZ)) {
               continue;
            }
            LevelChunk chunk = level.getChunk(chunkX, chunkZ);
            for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
               if (blockEntity instanceof UBWWeaponBlockEntity weaponBlock
                  && Vec3.atCenterOf(weaponBlock.getBlockPos()).distanceToSqr(center) <= radiusSqr) {
                  result.add(weaponBlock);
               }
            }
         }
      }
      return result;
   }

   private static List<UbwControlledSwordEntity> waitingSwords(ServerPlayer player) {
      UUID ownerId = player.getUUID();
      AABB bounds = player.getBoundingBox().inflate(RELEASE_QUERY_RADIUS);
      return player.serverLevel().getEntitiesOfClass(
         UbwControlledSwordEntity.class, bounds, sword -> sword.isWaitingFor(ownerId));
   }

   private static void release(ServerPlayer player, List<UbwControlledSwordEntity> swords) {
      Vec3 playerPos = player.position();
      Vec3 look = player.getLookAngle().normalize();
      Vec3 horizontalLook = new Vec3(look.x, 0.0D, look.z);
      if (horizontalLook.lengthSqr() < 1.0E-6D) {
         horizontalLook = new Vec3(0.0D, 0.0D, 1.0D);
      } else {
         horizontalLook = horizontalLook.normalize();
      }
      for (UbwControlledSwordEntity sword : swords) {
         Vec3 horizontalOffset = new Vec3(
            sword.getX() - playerPos.x, 0.0D, sword.getZ() - playerPos.z);
         double distance = horizontalOffset.length();
         boolean behind = distance > 1.0E-6D && horizontalOffset.normalize().dot(horizontalLook) < 0.0D;
         if (behind && distance > 6.0D) {
            Vec3 first = new Vec3(sword.getX(), sword.getY() + distance, sword.getZ());
            Vec3 second = new Vec3(playerPos.x, first.y, playerPos.z);
            sword.release(first, second, look);
         } else {
            sword.release(null, null, look);
         }
      }
   }

   public static int swordLimit(double proficiency) {
      double normalized = Math.max(0.0D, Math.min(1.0D, (proficiency - 10.0D) / 90.0D));
      return Math.min(MAX_SWORDS, 8 + (int)Math.floor(normalized * 56.0D));
   }
}

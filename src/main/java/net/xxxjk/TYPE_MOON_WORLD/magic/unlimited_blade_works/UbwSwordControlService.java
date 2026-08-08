package net.xxxjk.TYPE_MOON_WORLD.magic.unlimited_blade_works;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.block.entity.SwordBarrelBlockEntity;
import net.xxxjk.TYPE_MOON_WORLD.block.entity.UBWWeaponBlockEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.UbwControlledSwordEntity;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;

public final class UbwSwordControlService {
   public static final String MAGIC_ID = "ubw_sword_control";
   public static final double SCAN_RADIUS = 32.0D;
   public static final int MAX_SWORDS = 64;
   private static final double AIM_RANGE = 64.0D;
   private static final double RELEASE_QUERY_RADIUS = 80.0D;

   private UbwSwordControlService() {
   }

   public static boolean execute(ServerPlayer player) {
      if (player == null || !player.isAlive() || player.isRemoved() || player.isSpectator()) {
         return false;
      }
      List<UbwControlledSwordEntity> waiting = waitingSwords(player);
      if (!waiting.isEmpty()) {
         AimTarget target = findAimTarget(player);
         if (target == null) {
            player.displayClientMessage(Component.translatable(
               "message.typemoonworld.magic.ubw_sword_control.no_target"), true);
            return false;
         }
         release(player, waiting, target);
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
      List<SwordSource> candidates = loadedSwordSources(level, center);
      candidates.sort(Comparator.comparingDouble(source -> source.position.distanceToSqr(center)));
      int spawned = 0;
      for (SwordSource source : candidates) {
         if (spawned >= cap) {
            break;
         }
         BlockPos pos = source.blockEntity.getBlockPos();
         if (source.weapon.isEmpty() || !level.isLoaded(pos)
            || level.getBlockEntity(pos) != source.blockEntity) {
            continue;
         }
         UbwControlledSwordEntity sword = new UbwControlledSwordEntity(
            level, player, source.weapon, source.position);
         if (level.addFreshEntity(sword)) {
            level.removeBlock(pos, false);
            spawned++;
         }
      }
      return spawned;
   }

   private static List<SwordSource> loadedSwordSources(ServerLevel level, Vec3 center) {
      List<SwordSource> result = new ArrayList<>();
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
               Vec3 position = Vec3.atCenterOf(blockEntity.getBlockPos());
               if (position.distanceToSqr(center) > radiusSqr) {
                  continue;
               }
               ItemStack weapon = ItemStack.EMPTY;
               if (blockEntity instanceof UBWWeaponBlockEntity weaponBlock) {
                  weapon = weaponBlock.getStoredItem();
               } else if (blockEntity instanceof SwordBarrelBlockEntity layeredSword) {
                  weapon = layeredSword.getStoredItem();
               }
               if (!weapon.isEmpty()) {
                  result.add(new SwordSource(blockEntity, weapon.copyWithCount(1), position));
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

   private static void release(ServerPlayer player, List<UbwControlledSwordEntity> swords, AimTarget target) {
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
            sword.release(first, second, target.position, target.entityId);
         } else {
            sword.release(null, null, target.position, target.entityId);
         }
      }
   }

   private static AimTarget findAimTarget(ServerPlayer player) {
      ServerLevel level = player.serverLevel();
      Vec3 start = player.getEyePosition();
      Vec3 end = start.add(player.getLookAngle().scale(AIM_RANGE));
      BlockHitResult blockHit = level.clip(new ClipContext(
         start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
      Vec3 clippedEnd = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();
      AABB query = new AABB(start, clippedEnd).inflate(1.0D);
      Entity selected = null;
      double selectedDistance = Double.MAX_VALUE;
      for (Entity candidate : level.getEntities(player, query,
         entity -> entity.isAlive() && entity.isPickable() && !entity.isSpectator()
            && !EntityUtils.isImmunePlayerTarget(entity))) {
         java.util.Optional<Vec3> hit = candidate.getBoundingBox().inflate(0.3D).clip(start, clippedEnd);
         if (hit.isPresent()) {
            double distance = start.distanceToSqr(hit.get());
            if (distance < selectedDistance) {
               selected = candidate;
               selectedDistance = distance;
            }
         }
      }
      if (selected != null) {
         Vec3 center = selected.position().add(0.0D, selected.getBbHeight() * 0.5D, 0.0D);
         return new AimTarget(center, selected.getUUID());
      }
      return blockHit.getType() == HitResult.Type.BLOCK
         ? new AimTarget(blockHit.getLocation(), null)
         : null;
   }

   public static int swordLimit(double proficiency) {
      double normalized = Math.max(0.0D, Math.min(1.0D, (proficiency - 10.0D) / 90.0D));
      return Math.min(MAX_SWORDS, 8 + (int)Math.floor(normalized * 56.0D));
   }

   private record SwordSource(BlockEntity blockEntity, ItemStack weapon, Vec3 position) {
   }

   private record AimTarget(Vec3 position, UUID entityId) {
   }
}

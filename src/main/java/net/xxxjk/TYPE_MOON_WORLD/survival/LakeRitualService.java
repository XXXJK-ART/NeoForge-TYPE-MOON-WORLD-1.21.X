package net.xxxjk.TYPE_MOON_WORLD.survival;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** Handles lake transformations directly in any connected source-water lake. */
public final class LakeRitualService {
   private static final String UNTIL = "TypeMoonLakeRitualUntil";
   private static final String SWORD = "TypeMoonLakeRitualSword";
   private static final String OFFERING = "TypeMoonLakeRitualOffering";
   private static final String ANCHOR = "TypeMoonLakeRitualAnchor";
   private LakeRitualService() {}

   @SubscribeEvent
   public static void tick(EntityTickEvent.Post event) {
      if (!(event.getEntity() instanceof ItemEntity entity) || !(entity.level() instanceof ServerLevel level)) return;
      CompoundTag data = entity.getPersistentData();
      if (data.contains(UNTIL)) {
         tickRising(entity, level, data);
         return;
      }
      if (entity.tickCount % 5 != 0 || !touchesSourceWater(level, entity)
         || !LakeRitualRules.isLake(level, BlockPos.containing(entity.position()))) return;
      // Water currents can separate the two offerings before the five-tick
      // poll sees them. Search the whole practical six-source lake footprint,
      // rather than requiring the entities to overlap.
      List<ItemEntity> nearby = level.getEntitiesOfClass(ItemEntity.class, entity.getBoundingBox().inflate(8.0), other ->
         other != entity && other.isAlive() && touchesSourceWater(level, other)
            && LakeRitualRules.isLake(level, BlockPos.containing(other.position()))
            && !other.getPersistentData().contains(UNTIL));
      for (ItemEntity other : nearby) {
         ItemStack result = LakeRitualRules.result(entity.getItem(), other.getItem());
         ItemEntity swordEntity = entity;
         ItemEntity offeringEntity = other;
         if (result.isEmpty()) {
            result = LakeRitualRules.result(other.getItem(), entity.getItem());
            swordEntity = other;
            offeringEntity = entity;
         }
         if (result.isEmpty()) continue;
         startRitual(swordEntity, offeringEntity, result, level);
         return;
      }
   }

   private static void startRitual(ItemEntity swordEntity, ItemEntity offeringEntity, ItemStack result, ServerLevel level) {
      ItemStack sword = swordEntity.getItem().copyWithCount(1);
      ItemStack offering = offeringEntity.getItem().copyWithCount(1);
      swordEntity.getItem().shrink(1);
      ItemStack remainingSwords = swordEntity.getItem().copy();
      offeringEntity.getItem().shrink(1);
      if (offeringEntity.getItem().isEmpty()) offeringEntity.discard();
      if (!remainingSwords.isEmpty()) {
         ItemEntity remainder = new ItemEntity(level, swordEntity.getX(), swordEntity.getY(), swordEntity.getZ(), remainingSwords);
         remainder.setPickUpDelay(10);
         level.addFreshEntity(remainder);
      }
      swordEntity.setItem(result.copy());
      swordEntity.setNoGravity(true);
      swordEntity.setDeltaMovement(0.0, 0.035, 0.0);
      CompoundTag data = swordEntity.getPersistentData();
      data.putLong(UNTIL, level.getGameTime() + LakeRitualRules.DURATION);
      data.put(SWORD, sword.save(level.registryAccess()));
      data.put(OFFERING, offering.save(level.registryAccess()));
      // The item intentionally rises out of the water during the ritual. Keep
      // validating the original lake instead of the moving item position.
      data.putLong(ANCHOR, BlockPos.containing(swordEntity.position()).asLong());
   }

   private static void tickRising(ItemEntity entity, ServerLevel level, CompoundTag data) {
      long anchor = data.getLong(ANCHOR);
      if (!data.contains(ANCHOR) || !LakeRitualRules.isLake(level, BlockPos.of(anchor))) {
         cancel(entity, level, data);
         return;
      }
      entity.setNoGravity(true);
      entity.setDeltaMovement(0.0, 0.035, 0.0);
      if (level.getGameTime() < data.getLong(UNTIL)) return;
      data.remove(UNTIL);
      data.remove(SWORD);
      data.remove(OFFERING);
      data.remove(ANCHOR);
      entity.setNoGravity(false);
      entity.setDeltaMovement(0.0, 0.0, 0.0);
   }

   private static void cancel(ItemEntity entity, ServerLevel level, CompoundTag data) {
      ItemStack sword = ItemStack.parseOptional(level.registryAccess(), data.getCompound(SWORD));
      ItemStack offering = ItemStack.parseOptional(level.registryAccess(), data.getCompound(OFFERING));
      entity.setItem(sword);
      entity.setNoGravity(false);
      entity.setDeltaMovement(0.0, 0.1, 0.0);
      data.remove(UNTIL);
      data.remove(SWORD);
      data.remove(OFFERING);
      data.remove(ANCHOR);
      if (!offering.isEmpty()) entity.level().addFreshEntity(new ItemEntity(level, entity.getX(), entity.getY(), entity.getZ(), offering));
   }

   /**
    * ItemEntity#isInWater is a cached flag updated before movement. During the
    * same tick an item can be visibly in a source block while that flag is
    * still false, so inspect the entity bounds and fluid state directly.
    */
   private static boolean touchesSourceWater(ServerLevel level, ItemEntity entity) {
      // Include the small gap that vanilla's bobbing item can have above the
      // water surface; an item resting on a source block is still an offering
      // in that lake.
      AABB box = entity.getBoundingBox().inflate(0.1D);
      int minX = Mth.floor(box.minX);
      int maxX = Mth.floor(box.maxX);
      int minY = Mth.floor(box.minY);
      int maxY = Mth.floor(box.maxY);
      int minZ = Mth.floor(box.minZ);
      int maxZ = Mth.floor(box.maxZ);
      for (int x = minX; x <= maxX; x++) {
         for (int y = minY; y <= maxY; y++) {
            for (int z = minZ; z <= maxZ; z++) {
               BlockPos pos = new BlockPos(x, y, z);
               if (!level.hasChunkAt(pos)) continue;
               var fluid = level.getFluidState(pos);
               if (fluid.is(FluidTags.WATER) && fluid.isSource()
                  && pos.getY() + fluid.getHeight(level, pos) > box.minY
                  && pos.getY() < box.maxY) {
                  return true;
               }
            }
         }
      }
      return false;
   }
}

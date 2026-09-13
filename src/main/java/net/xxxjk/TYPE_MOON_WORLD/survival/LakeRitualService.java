package net.xxxjk.TYPE_MOON_WORLD.survival;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** Handles lake transformations directly in any connected source-water lake. */
@EventBusSubscriber(modid = "typemoonworld")
public final class LakeRitualService {
   private static final String UNTIL = "TypeMoonLakeRitualUntil";
   private static final String SWORD = "TypeMoonLakeRitualSword";
   private static final String OFFERING = "TypeMoonLakeRitualOffering";
   private LakeRitualService() {}

   @SubscribeEvent
   public static void tick(EntityTickEvent.Post event) {
      if (!(event.getEntity() instanceof ItemEntity entity) || !(entity.level() instanceof ServerLevel level)) return;
      CompoundTag data = entity.getPersistentData();
      if (data.contains(UNTIL)) {
         tickRising(entity, level, data);
         return;
      }
      if (entity.tickCount % 5 != 0 || !entity.isInWater() || !LakeRitualRules.isLake(level, BlockPos.containing(entity.position()))) return;
      List<ItemEntity> nearby = level.getEntitiesOfClass(ItemEntity.class, entity.getBoundingBox().inflate(1.5), other ->
         other != entity && other.isAlive() && other.isInWater() && !other.getPersistentData().contains(UNTIL));
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
      offeringEntity.getItem().shrink(1);
      if (offeringEntity.getItem().isEmpty()) offeringEntity.discard();
      swordEntity.setItem(result.copy());
      swordEntity.setNoGravity(true);
      swordEntity.setDeltaMovement(0.0, 0.035, 0.0);
      CompoundTag data = swordEntity.getPersistentData();
      data.putLong(UNTIL, level.getGameTime() + LakeRitualRules.DURATION);
      data.put(SWORD, sword.save(level.registryAccess()));
      data.put(OFFERING, offering.save(level.registryAccess()));
   }

   private static void tickRising(ItemEntity entity, ServerLevel level, CompoundTag data) {
      if (!LakeRitualRules.isLake(level, BlockPos.containing(entity.position()))) {
         cancel(entity, level, data);
         return;
      }
      entity.setNoGravity(true);
      entity.setDeltaMovement(0.0, 0.035, 0.0);
      if (level.getGameTime() < data.getLong(UNTIL)) return;
      data.remove(UNTIL);
      data.remove(SWORD);
      data.remove(OFFERING);
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
      if (!offering.isEmpty()) entity.level().addFreshEntity(new ItemEntity(level, entity.getX(), entity.getY(), entity.getZ(), offering));
   }
}

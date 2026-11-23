package net.xxxjk.TYPE_MOON_WORLD.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** Temporary planted Excalibur display while Artoria maintains Invisible Air defense. */
public final class ArtoriaWindDefenseSwordEntity extends Entity {
   public ArtoriaWindDefenseSwordEntity(EntityType<?> type, Level level) {
      super(type, level);
      this.noPhysics = true;
      this.noCulling = true;
   }

   @Override
   public void tick() {
      super.tick();
      if (this.tickCount > 110) this.discard();
   }

   @Override
   protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) { }

   @Override
   protected void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) { }

   @Override
   protected void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) { }

   @Override
   public boolean shouldBeSaved() {
      return false;
   }
}

package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;

public final class ShadowHassanDeathShadowEntity extends PathfinderMob {
   private UUID pursuitUuid;

   public ShadowHassanDeathShadowEntity(EntityType<? extends ShadowHassanDeathShadowEntity> type, Level level) {
      super(type, level);
      this.noPhysics = true;
      this.noCulling = true;
   }

   public void setPursuitUuid(UUID pursuitUuid) {
      this.pursuitUuid = pursuitUuid;
   }

   @Override
   public void tick() {
      super.tick();
      this.setNoGravity(true);
      this.noPhysics = true;
      if (this.level() instanceof ServerLevel level && this.tickCount % 2 == 0) {
         level.sendParticles(ParticleTypes.SQUID_INK, this.getX(), this.getY() + 0.9, this.getZ(),
            5, 0.34, 0.82, 0.34, 0.012);
         level.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY() + 0.8, this.getZ(),
            2, 0.25, 0.72, 0.25, 0.008);
      }
   }

   @Override
   public boolean hurt(DamageSource source, float amount) {
      if (source.is(DamageTypes.GENERIC_KILL) || source.is(DamageTypes.FELL_OUT_OF_WORLD)) {
         this.removePursuit();
         this.discard();
         return true;
      }
      return false;
   }

   @Override
   public void kill() {
      this.removePursuit();
      super.kill();
   }

   private void removePursuit() {
      if (this.level() instanceof ServerLevel level && this.pursuitUuid != null) {
         net.xxxjk.TYPE_MOON_WORLD.servant.shadowhassan.ShadowHassanPursuitData.get(level.getServer()).removePursuit(this.pursuitUuid);
      }
   }

   @Override
   public void readAdditionalSaveData(CompoundTag tag) {
      this.pursuitUuid = tag.hasUUID("Pursuit") ? tag.getUUID("Pursuit") : null;
   }

   @Override
   public void addAdditionalSaveData(CompoundTag tag) {
      if (this.pursuitUuid != null) tag.putUUID("Pursuit", this.pursuitUuid);
   }
}

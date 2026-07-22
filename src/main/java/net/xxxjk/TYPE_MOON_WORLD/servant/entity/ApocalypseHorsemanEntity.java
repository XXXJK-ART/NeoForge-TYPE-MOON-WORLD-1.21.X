package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.xxxjk.TYPE_MOON_WORLD.servant.palerider.OwnedPaleRiderMob;

public final class ApocalypseHorsemanEntity extends OwnedPaleRiderMob {
   private static final EntityDataAccessor<Integer> CALAMITY = SynchedEntityData.defineId(ApocalypseHorsemanEntity.class, EntityDataSerializers.INT);

   public ApocalypseHorsemanEntity(EntityType<? extends ApocalypseHorsemanEntity> type, Level level) {
      super(type, level);
      this.setNoAi(true);
   }

   public static AttributeSupplier.Builder createAttributes() {
      return PathfinderMob.createMobAttributes()
         .add(Attributes.MAX_HEALTH, 400.0)
         .add(Attributes.MOVEMENT_SPEED, 0.36)
         .add(Attributes.ATTACK_DAMAGE, 5.0)
         .add(Attributes.ARMOR, 12.0)
         .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(CALAMITY, Calamity.SWORD.ordinal());
   }

   public void setCalamity(Calamity calamity) {
      this.entityData.set(CALAMITY, calamity.ordinal());
   }

   public Calamity getCalamity() {
      return Calamity.values()[Math.max(0, Math.min(Calamity.values().length - 1, this.entityData.get(CALAMITY)))];
   }

   @Override
   public void tick() {
      super.tick();
      if (!this.level().isClientSide()) {
         PaleRiderEntity owner = this.getPaleRiderOwner();
         if (owner == null || !owner.isCalamityActive()) {
            this.discard();
            return;
         }
         owner.spawnHorsemanParticles(this);
      }
   }

   @Override
   public void die(DamageSource source) {
      PaleRiderEntity owner = this.getPaleRiderOwner();
      if (owner != null) owner.disableCalamity(this.getCalamity());
      super.die(source);
   }

   @Override
   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      tag.putInt("Calamity", this.getCalamity().ordinal());
   }

   @Override
   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      this.entityData.set(CALAMITY, Math.max(0, Math.min(Calamity.values().length - 1, tag.getInt("Calamity"))));
   }

   public enum Calamity {
      SWORD,
      FAMINE,
      BEAST
   }
}

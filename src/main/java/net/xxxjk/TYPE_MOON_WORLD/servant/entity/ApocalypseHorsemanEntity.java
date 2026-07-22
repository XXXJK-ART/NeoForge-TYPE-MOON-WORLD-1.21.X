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
import net.minecraft.network.chat.Component;
import net.xxxjk.TYPE_MOON_WORLD.servant.palerider.OwnedPaleRiderMob;

public final class ApocalypseHorsemanEntity extends OwnedPaleRiderMob {
   private static final EntityDataAccessor<Integer> CALAMITY = SynchedEntityData.defineId(ApocalypseHorsemanEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Boolean> PALE_RIDER_PROXY = SynchedEntityData.defineId(ApocalypseHorsemanEntity.class, EntityDataSerializers.BOOLEAN);

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
      builder.define(PALE_RIDER_PROXY, false);
   }

   public void setCalamity(Calamity calamity) {
      this.entityData.set(CALAMITY, calamity.ordinal());
      this.updateHorsemanName();
   }

   public Calamity getCalamity() {
      return Calamity.values()[Math.max(0, Math.min(Calamity.values().length - 1, this.entityData.get(CALAMITY)))];
   }

   public void setPaleRiderProxy(boolean value) {
      this.entityData.set(PALE_RIDER_PROXY, value);
      this.updateHorsemanName();
   }

   public boolean isPaleRiderProxy() {
      return this.entityData.get(PALE_RIDER_PROXY);
   }

   @Override
   public void tick() {
      super.tick();
      if (!this.level().isClientSide()) {
         PaleRiderEntity owner = this.getPaleRiderOwner();
         boolean expired = owner == null || (this.isPaleRiderProxy() ? !owner.hasAnyDomain() : !owner.isCalamityActive());
         if (expired) {
            this.discard();
            return;
         }
         this.updateHorsemanName();
         owner.spawnHorsemanParticles(this);
      }
   }

   @Override
   public boolean isInvulnerableTo(DamageSource source) {
      return true;
   }

   @Override
   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      tag.putInt("Calamity", this.getCalamity().ordinal());
      tag.putBoolean("PaleRiderProxy", this.isPaleRiderProxy());
   }

   @Override
   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      this.entityData.set(CALAMITY, Math.max(0, Math.min(Calamity.values().length - 1, tag.getInt("Calamity"))));
      this.entityData.set(PALE_RIDER_PROXY, tag.getBoolean("PaleRiderProxy"));
      this.updateHorsemanName();
   }

   private void updateHorsemanName() {
      String key = this.isPaleRiderProxy() ? "entity.typemoonworld.horseman.death" : switch (this.getCalamity()) {
         case SWORD -> "entity.typemoonworld.horseman.conquest";
         case FAMINE -> "entity.typemoonworld.horseman.war";
         case BEAST -> "entity.typemoonworld.horseman.famine";
      };
      this.setCustomName(Component.translatable(key));
      this.setCustomNameVisible(true);
   }

   public enum Calamity {
      SWORD,
      FAMINE,
      BEAST
   }
}

package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.server.level.ServerLevel;
import net.xxxjk.TYPE_MOON_WORLD.servant.palerider.OwnedPaleRiderMob;
import net.xxxjk.TYPE_MOON_WORLD.servant.palerider.PaleRiderInfectionService;
import org.joml.Vector3f;

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
         net.minecraft.world.entity.LivingEntity owner = this.getPaleRiderLivingOwner();
         boolean active = owner instanceof PaleRiderEntity rider ? (this.isPaleRiderProxy() ? rider.hasAnyDomain() : rider.isCalamityActive())
            : owner instanceof net.minecraft.server.level.ServerPlayer player && PaleRiderInfectionService.isPaleRiderCardPlayer(player)
               && (this.isPaleRiderProxy()
                  ? player.getPersistentData().getBoolean("PaleRiderCardCalamityActive") || player.getPersistentData().getBoolean("PaleRiderCardUnderworldActive")
                  : player.getPersistentData().getBoolean("PaleRiderCardCalamityActive"));
         boolean expired = owner == null || !active;
         if (expired) {
            this.discard();
            return;
         }
         this.updateHorsemanName();
         if (owner instanceof PaleRiderEntity rider) rider.spawnHorsemanParticles(this);
         else if (this.level() instanceof ServerLevel serverLevel) this.spawnBodyParticles(serverLevel);
      }
   }

   private void spawnBodyParticles(ServerLevel level) {
      Vector3f color = this.isPaleRiderProxy() ? new Vector3f(0.12F, 0.5F, 1.0F) : switch (this.getCalamity()) {
         case SWORD -> new Vector3f(0.85F, 0.88F, 0.92F);
         case FAMINE -> new Vector3f(0.45F, 0.025F, 0.02F);
         case BEAST -> new Vector3f(0.035F, 0.035F, 0.035F);
      };
      level.sendParticles(new DustParticleOptions(color, 1.5F), this.getX(), this.getY() + 0.9, this.getZ(), 10, 0.3, 0.9, 0.3, 0.015);
      level.sendParticles(ParticleTypes.ASH, this.getX(), this.getY() + 1.0, this.getZ(), 4, 0.25, 0.8, 0.25, 0.01);
   }

   @Override
   public boolean isInvulnerableTo(DamageSource source) {
      return true;
   }

   @Override
   public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
      return !this.isPaleRiderProxy() && super.doHurtTarget(target);
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

package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.servant.palerider.PaleRiderInfectionService;

public final class PaleRiderCrowEntity extends Parrot {
   private static final String TAG_OWNER = "PaleRiderOwner";
   private UUID ownerUuid;
   private long nextAttackTick;
   private boolean domainSpawned;

   public PaleRiderCrowEntity(EntityType<? extends Parrot> type, Level level) {
      super(type, level);
      this.setPersistenceRequired();
      this.setSilent(true);
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Parrot.createAttributes()
         .add(Attributes.MAX_HEALTH, 10.0)
         .add(Attributes.FLYING_SPEED, 0.48)
         .add(Attributes.MOVEMENT_SPEED, 0.28)
         .add(Attributes.FOLLOW_RANGE, 64.0)
         .add(Attributes.ATTACK_DAMAGE, 2.0);
   }

   @Override
   protected void registerGoals() {
      this.goalSelector.addGoal(0, new FloatGoal(this));
   }

   @Override
   protected void customServerAiStep() {
      super.customServerAiStep();
      PaleRiderEntity owner = this.getPaleRiderOwner();
      if (owner == null || !owner.isAlive()) {
         this.discard();
         return;
      }
      LivingEntity target = owner.findPaleRiderEnemy(64.0);
      if (target == null) {
         this.setTarget(null);
         Vec3 perch = this.gatheringPosition(owner);
         if (this.distanceToSqr(perch) > 2.25) {
            this.getNavigation().moveTo(perch.x, perch.y, perch.z, 1.0);
         }
         return;
      }
      this.setTarget(target);
      this.getLookControl().setLookAt(target, 30.0F, 30.0F);
      this.getNavigation().moveTo(target.getX(), target.getY() + target.getBbHeight() * 0.65, target.getZ(), 1.2);
      double reach = this.getBbWidth() + target.getBbWidth() + 0.8;
      long now = this.level().getGameTime();
      if (this.distanceToSqr(target) <= reach * reach && now >= this.nextAttackTick) {
         this.nextAttackTick = now + 30L;
         target.hurt(this.damageSources().mobAttack(this), (float)this.getAttributeValue(Attributes.ATTACK_DAMAGE));
         PaleRiderInfectionService.infect(target, owner, 1);
      }
   }

   private Vec3 gatheringPosition(PaleRiderEntity owner) {
      int hash = this.getUUID().hashCode() & Integer.MAX_VALUE;
      double angle = Math.toRadians(hash % 360);
      double radius = 3.0 + (hash / 360 % 4) * 0.9;
      double height = 2.5 + (hash / 1440 % 3) * 0.8;
      return owner.position().add(Math.cos(angle) * radius, height, Math.sin(angle) * radius);
   }

   public void setPaleRiderOwner(PaleRiderEntity owner) {
      this.ownerUuid = owner.getUUID();
   }

   public UUID getPaleRiderOwnerUuid() {
      return this.ownerUuid;
   }

   public PaleRiderEntity getPaleRiderOwner() {
      if (this.ownerUuid == null || !(this.level() instanceof ServerLevel level)) return null;
      return level.getEntity(this.ownerUuid) instanceof PaleRiderEntity rider ? rider : null;
   }

   public void setDomainSpawned(boolean domainSpawned) {
      this.domainSpawned = domainSpawned;
   }

   public boolean isDomainSpawned() {
      return this.domainSpawned;
   }

   @Override
   public boolean isAlliedTo(Entity other) {
      PaleRiderEntity owner = this.getPaleRiderOwner();
      return super.isAlliedTo(other) || owner != null && (other == owner || owner.isAlliedTo(other));
   }

   @Override
   public boolean removeWhenFarAway(double distance) {
      return false;
   }

   @Override
   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      if (this.ownerUuid != null) tag.putUUID(TAG_OWNER, this.ownerUuid);
      tag.putBoolean("DomainSpawned", this.domainSpawned);
   }

   @Override
   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      this.ownerUuid = tag.hasUUID(TAG_OWNER) ? tag.getUUID(TAG_OWNER) : null;
      this.domainSpawned = tag.getBoolean("DomainSpawned");
      this.setSilent(true);
   }
}

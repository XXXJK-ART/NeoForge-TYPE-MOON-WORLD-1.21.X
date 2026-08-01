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
import net.xxxjk.TYPE_MOON_WORLD.servant.palerider.PaleRiderEntityIndex;

public final class PaleRiderCrowEntity extends Parrot {
   private static final String TAG_OWNER = "PaleRiderOwner";
   private UUID ownerUuid;
   private long nextAttackTick;
   private long nextTargetScanTick;
   private long nextNavigationTick;
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
      LivingEntity owner = this.getPaleRiderLivingOwner();
      if (owner == null || !owner.isAlive()) {
         this.discard();
         return;
      }
      if (PaleRiderInfectionService.isStationaryAnchor(this)) {
         PaleRiderInfectionService.holdStationaryAnchor(this);
         return;
      }
      super.customServerAiStep();
      if (owner instanceof net.minecraft.server.level.ServerPlayer player
         && PaleRiderInfectionService.isPaleRiderCardPlayer(player)) {
         long now = this.level().getGameTime();
         int command = player.getPersistentData().getInt("PaleRiderCardCommand");
         if (command != 2) {
            if (this.getTarget() != null) this.setTarget(null);
            if (command == 1 && !this.getNavigation().isDone()) this.getNavigation().stop();
            else if (command == 3 && this.canRefreshNavigation(now, 20)) {
               this.getNavigation().moveTo(player.getX(), player.getY() + 2.5, player.getZ(), 1.0);
            }
            return;
         }
      }
      LivingEntity target = findEnemy(owner, 64.0);
      if (target == null) {
         if (this.getTarget() != null) this.setTarget(null);
         Vec3 perch = this.gatheringPosition(owner);
         if (this.distanceToSqr(perch) > 2.25 && this.canRefreshNavigation(this.level().getGameTime(), 20)) {
            this.getNavigation().moveTo(perch.x, perch.y, perch.z, 1.0);
         } else if (this.distanceToSqr(perch) <= 2.25 && !this.getNavigation().isDone()) {
            this.getNavigation().stop();
         }
         return;
      }
      if (this.getTarget() != target) this.setTarget(target);
      this.getLookControl().setLookAt(target, 30.0F, 30.0F);
      long now = this.level().getGameTime();
      if (this.canRefreshNavigation(now, 10)) {
         this.getNavigation().moveTo(target.getX(), target.getY() + target.getBbHeight() * 0.65, target.getZ(), 1.2);
      }
      double reach = this.getBbWidth() + target.getBbWidth() + 0.8;
      if (this.distanceToSqr(target) <= reach * reach && now >= this.nextAttackTick) {
         this.nextAttackTick = now + 30L;
         target.hurt(this.damageSources().mobAttack(this), (float)this.getAttributeValue(Attributes.ATTACK_DAMAGE));
         PaleRiderInfectionService.infect(target, owner, 1);
      }
   }

   private boolean canRefreshNavigation(long now, int interval) {
      if (now < this.nextNavigationTick) return false;
      this.nextNavigationTick = now + interval + Math.floorMod(this.getId(), 5);
      return true;
   }

   private Vec3 gatheringPosition(LivingEntity owner) {
      int hash = this.getUUID().hashCode() & Integer.MAX_VALUE;
      double angle = Math.toRadians(hash % 360);
      double radius = 3.0 + (hash / 360 % 4) * 0.9;
      double height = 2.5 + (hash / 1440 % 3) * 0.8;
      return owner.position().add(Math.cos(angle) * radius, height, Math.sin(angle) * radius);
   }

   public void setPaleRiderOwner(LivingEntity owner) {
      UUID previous = this.ownerUuid;
      UUID next = owner == null ? null : owner.getUUID();
      if (previous != null && !previous.equals(next)) {
         PaleRiderEntityIndex.unregisterOwned(previous, this);
      }
      this.ownerUuid = next;
      if (this.ownerUuid != null) {
         PaleRiderEntityIndex.registerOwned(this.ownerUuid, this);
      }
   }

   public UUID getPaleRiderOwnerUuid() {
      return this.ownerUuid;
   }

   public PaleRiderEntity getPaleRiderOwner() {
      if (this.ownerUuid == null || !(this.level() instanceof ServerLevel level)) return null;
      return level.getEntity(this.ownerUuid) instanceof PaleRiderEntity rider ? rider : null;
   }

   public LivingEntity getPaleRiderLivingOwner() {
      if (this.ownerUuid == null || !(this.level() instanceof ServerLevel level)) return null;
      return level.getEntity(this.ownerUuid) instanceof LivingEntity owner ? owner : null;
   }

   public void setDomainSpawned(boolean domainSpawned) {
      this.domainSpawned = domainSpawned;
   }

   public boolean isDomainSpawned() {
      return this.domainSpawned;
   }

   @Override
   public boolean isAlliedTo(Entity other) {
      LivingEntity owner = this.getPaleRiderLivingOwner();
      return super.isAlliedTo(other) || owner != null && (other == owner || owner.isAlliedTo(other) || other.isAlliedTo(owner));
   }

   private LivingEntity findEnemy(LivingEntity owner, double radius) {
      if (owner instanceof PaleRiderEntity rider) return rider.findPaleRiderEnemy(radius);
      if (this.getTarget() != null && this.getTarget().isAlive()
         && !PaleRiderInfectionService.arePaleRiderAllies(owner, this.getTarget())
         && !net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils.isImmunePlayerTarget(this.getTarget())) return this.getTarget();
      long now = this.level().getGameTime();
      if (now < this.nextTargetScanTick) return null;
      this.nextTargetScanTick = now + 40L + Math.floorMod(this.getId(), 16);
      return this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(radius),
         target -> target != this && target != owner && target.isAlive()
            && !PaleRiderInfectionService.arePaleRiderAllies(owner, target)
            && !net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils.isImmunePlayerTarget(target))
         .stream().min((left, right) -> Double.compare(left.distanceToSqr(this), right.distanceToSqr(this))).orElse(null);
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
      if (this.ownerUuid != null) {
         PaleRiderEntityIndex.registerOwned(this.ownerUuid, this);
      }
   }

   @Override
   public void remove(net.minecraft.world.entity.Entity.RemovalReason reason) {
      if (this.ownerUuid != null) {
         PaleRiderEntityIndex.unregisterOwned(this.ownerUuid, this);
      }
      super.remove(reason);
   }
}

package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.mixin.LivingEntityInputAccessor;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardPaleRiderSkills;
import net.xxxjk.TYPE_MOON_WORLD.servant.palerider.OwnedPaleRiderMob;
import net.xxxjk.TYPE_MOON_WORLD.servant.palerider.PaleRiderInfectionService;
import org.joml.Vector3f;

public final class ApocalypseHorseEntity extends OwnedPaleRiderMob {
   private static final String STATIC_X_TAG = "PaleRiderStaticMountX";
   private static final String STATIC_Z_TAG = "PaleRiderStaticMountZ";
   private long nextNavigationTick;

   public ApocalypseHorseEntity(EntityType<? extends ApocalypseHorseEntity> type, Level level) {
      super(type, level);
      this.setNoAi(true);
   }

   public static AttributeSupplier.Builder createAttributes() {
      return PathfinderMob.createMobAttributes()
         .add(Attributes.MAX_HEALTH, 1000.0)
         .add(Attributes.MOVEMENT_SPEED, 0.4)
         .add(Attributes.ARMOR, 8.0)
         .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
   }

   @Override
   public boolean isInvulnerableTo(DamageSource source) {
      return true;
   }

   @Override
   public void tick() {
      super.tick();
      if (!(this.level() instanceof ServerLevel level)) return;
      LivingEntity owner = this.getPaleRiderLivingOwner();
      boolean active = owner instanceof PaleRiderEntity rider ? rider.hasAnyDomain()
         : owner instanceof net.minecraft.server.level.ServerPlayer player && PaleRiderInfectionService.isPaleRiderCardPlayer(player)
            && (player.getPersistentData().getBoolean("PaleRiderCardCalamityActive") || player.getPersistentData().getBoolean("PaleRiderCardUnderworldActive"));
      if (owner == null || !active) {
         this.discard();
         return;
      }
      if (owner instanceof ServerPlayer player && PaleRiderInfectionService.isPaleRiderCardPlayer(player)) {
         this.tickCardFormation(player);
      } else if (!this.getPassengers().isEmpty()) {
         LivingEntity target = owner instanceof PaleRiderEntity rider ? rider.findPaleRiderEnemy(64.0) :
            level.getEntitiesOfClass(LivingEntity.class, owner.getBoundingBox().inflate(64.0), e -> e != owner && e.isAlive()
               && !PaleRiderInfectionService.arePaleRiderAllies(owner, e)).stream().findFirst().orElse(null);
         if (target != null) {
            this.setNoAi(false);
            Vec3 destination = formationDestination(owner, target, this.getFirstPassenger());
            if (this.canRefreshNavigation(10)) {
               this.getNavigation().moveTo(destination.x, destination.y, destination.z, 1.15);
            }
         }
      }
      if (this.tickCount % 8 == Math.floorMod(this.getId(), 8)) {
         level.sendParticles(new DustParticleOptions(new Vector3f(0.15F, 0.55F, 1.0F), 1.2F),
            this.getX(), this.getY() + 0.2, this.getZ(), 4, 0.55, 0.15, 0.55, 0.02);
         level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, this.getX(), this.getY() + 0.2, this.getZ(), 2, 0.45, 0.08, 0.45, 0.01);
      }
   }

   private void tickCardFormation(ServerPlayer player) {
      if (this.getFirstPassenger() == player) {
         this.setNoAi(true);
         this.getNavigation().stop();
         return;
      }
      if (!(this.getFirstPassenger() instanceof ApocalypseHorsemanEntity horseman)) return;
      if (horseman.isPaleRiderProxy()) {
         this.setNoAi(true);
         this.getNavigation().stop();
         if (!this.getPersistentData().contains(STATIC_X_TAG)) {
            this.getPersistentData().putDouble(STATIC_X_TAG, this.getX());
            this.getPersistentData().putDouble(STATIC_Z_TAG, this.getZ());
         }
         double anchorX = this.getPersistentData().getDouble(STATIC_X_TAG);
         double anchorZ = this.getPersistentData().getDouble(STATIC_Z_TAG);
         if (this.distanceToSqr(anchorX, this.getY(), anchorZ) > 1.0E-4) this.teleportTo(anchorX, this.getY(), anchorZ);
         this.setDeltaMovement(0.0, this.getDeltaMovement().y, 0.0);
         this.hasImpulse = true;
         return;
      }
      LivingEntity leader = ServantCardPaleRiderSkills.getDomainFormationLeader(player);
      if (leader == null || !leader.isAlive()) {
         this.setNoAi(true);
         this.getNavigation().stop();
         this.setDeltaMovement(Vec3.ZERO);
         return;
      }
      Vec3 destination = cardFormationDestination(leader, horseman);
      if (this.distanceToSqr(destination) > 24.0 * 24.0) {
         this.teleportTo(destination.x, destination.y, destination.z);
         this.getNavigation().stop();
      } else if (this.distanceToSqr(destination) > 2.0 * 2.0) {
         this.setNoAi(false);
         if (this.canRefreshNavigation(10)) {
            this.getNavigation().moveTo(destination.x, destination.y, destination.z, 1.15);
         }
      } else {
         this.getNavigation().stop();
         this.setDeltaMovement(this.getDeltaMovement().multiply(0.35, 1.0, 0.35));
      }
   }

   private boolean canRefreshNavigation(int interval) {
      long now = this.level().getGameTime();
      if (now < this.nextNavigationTick) return false;
      this.nextNavigationTick = now + interval + Math.floorMod(this.getId(), 5);
      return true;
   }

   @Override
   public LivingEntity getControllingPassenger() {
      return this.getFirstPassenger() instanceof Player player ? player : super.getControllingPassenger();
   }

   @Override
   protected Vec3 getRiddenInput(Player player, Vec3 travelVector) {
      float strafe = player.xxa * 0.5F;
      float forward = player.zza;
      if (forward < 0.0F) forward *= 0.25F;
      return new Vec3(strafe, 0.0, forward);
   }

   @Override
   protected float getRiddenSpeed(Player player) {
      return (float)this.getAttributeValue(Attributes.MOVEMENT_SPEED);
   }

   @Override
   protected void tickRidden(Player player, Vec3 travelVector) {
      super.tickRidden(player, travelVector);
      this.setYRot(player.getYRot());
      this.setXRot(player.getXRot() * 0.5F);
      this.yRotO = this.yBodyRot = this.yHeadRot = this.getYRot();
      if (this.isControlledByLocalInstance() && this.onGround()
         && ((LivingEntityInputAccessor)player).typemoonworld$isJumping()) {
         this.jumpFromGround();
      }
   }

   private static Vec3 cardFormationDestination(LivingEntity leader, ApocalypseHorsemanEntity horseman) {
      double angle = Math.toRadians(leader.getYRot());
      Vec3 forward = new Vec3(-Math.sin(angle), 0.0, Math.cos(angle));
      Vec3 side = new Vec3(-forward.z, 0.0, forward.x);
      double sideOffset = switch (horseman.getCalamity()) {
         case SWORD -> -4.2;
         case FAMINE -> 0.0;
         case BEAST -> 4.2;
      };
      double rearOffset = horseman.getCalamity() == ApocalypseHorsemanEntity.Calamity.FAMINE ? 4.6 : 2.8;
      return leader.position().add(side.scale(sideOffset)).subtract(forward.scale(rearOffset));
   }

   private static Vec3 formationDestination(LivingEntity owner, LivingEntity target, net.minecraft.world.entity.Entity passenger) {
      if (!(passenger instanceof ApocalypseHorsemanEntity horseman)) return target.position();
      Vec3 forward = target.position().subtract(owner.position()).multiply(1.0, 0.0, 1.0);
      if (forward.lengthSqr() < 1.0E-4) forward = new Vec3(0.0, 0.0, 1.0);
      forward = forward.normalize();
      Vec3 side = new Vec3(-forward.z, 0.0, forward.x);
      if (horseman.isPaleRiderProxy()) return target.position().subtract(forward.scale(1.8));
      double sideOffset = switch (horseman.getCalamity()) {
         case SWORD -> -4.2;
         case FAMINE -> 0.0;
         case BEAST -> 4.2;
      };
      double rearOffset = horseman.getCalamity() == ApocalypseHorsemanEntity.Calamity.FAMINE ? 4.6 : 2.8;
      return target.position().add(side.scale(sideOffset)).subtract(forward.scale(rearOffset));
   }
}

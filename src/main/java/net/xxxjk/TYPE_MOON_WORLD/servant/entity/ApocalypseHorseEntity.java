package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.servant.palerider.OwnedPaleRiderMob;
import org.joml.Vector3f;

public final class ApocalypseHorseEntity extends OwnedPaleRiderMob {
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
      PaleRiderEntity owner = this.getPaleRiderOwner();
      if (owner == null || !owner.hasAnyDomain()) {
         this.discard();
         return;
      }
      if (!this.getPassengers().isEmpty()) {
         LivingEntity target = owner.findPaleRiderEnemy(64.0);
         if (target != null) {
            this.setNoAi(false);
            Vec3 destination = formationDestination(owner, target, this.getFirstPassenger());
            this.getNavigation().moveTo(destination.x, destination.y, destination.z, 1.15);
         }
      }
      level.sendParticles(new DustParticleOptions(new Vector3f(0.15F, 0.55F, 1.0F), 1.2F),
         this.getX(), this.getY() + 0.2, this.getZ(), 8, 0.55, 0.15, 0.55, 0.02);
      level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, this.getX(), this.getY() + 0.2, this.getZ(), 4, 0.45, 0.08, 0.45, 0.01);
   }

   private static Vec3 formationDestination(PaleRiderEntity owner, LivingEntity target, net.minecraft.world.entity.Entity passenger) {
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

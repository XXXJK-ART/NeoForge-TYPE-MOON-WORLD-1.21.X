package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
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
            this.getNavigation().moveTo(target, 1.15);
         }
      }
      level.sendParticles(new DustParticleOptions(new Vector3f(0.15F, 0.55F, 1.0F), 1.2F),
         this.getX(), this.getY() + 0.2, this.getZ(), 8, 0.55, 0.15, 0.55, 0.02);
      level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, this.getX(), this.getY() + 0.2, this.getZ(), 4, 0.45, 0.08, 0.45, 0.01);
   }
}

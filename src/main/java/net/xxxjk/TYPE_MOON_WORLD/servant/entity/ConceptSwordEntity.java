package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import java.util.UUID;
import java.util.LinkedList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;

public final class ConceptSwordEntity extends ThrowableItemProjectile {
   public final List<net.minecraft.world.phys.Vec3> tracePos = new LinkedList<>();
   private UUID ownerUuid;
   private float damage = 30.0F;

   public ConceptSwordEntity(EntityType<? extends ConceptSwordEntity> type, Level level) {
      super(type, level);
   }

   public ConceptSwordEntity(Level level, LivingEntity owner, LivingEntity target, float damage) {
      this(net.xxxjk.TYPE_MOON_WORLD.init.ModEntities.CONCEPT_SWORD.get(), level);
      this.ownerUuid = owner.getUUID();
      this.damage = damage;
      this.setOwner(owner);
      this.setPos(owner.getX(), owner.getY() + 2.5, owner.getZ());
      this.shoot(target.getX() - this.getX(), target.getY() + target.getBbHeight() * 0.5 - this.getY(), target.getZ() - this.getZ(), 1.6F, 1.5F);
   }

   @Override
   protected Item getDefaultItem() {
      return Items.NETHERITE_SWORD;
   }

   @Override
   protected void onHitEntity(EntityHitResult result) {
      super.onHitEntity(result);
      Entity owner = this.getOwner();
      if (result.getEntity() instanceof LivingEntity living && owner instanceof LivingEntity livingOwner
         && !net.xxxjk.TYPE_MOON_WORLD.servant.palerider.PaleRiderInfectionService.arePaleRiderAllies(living, livingOwner)) {
         living.hurt(livingOwner.damageSources().source(net.xxxjk.TYPE_MOON_WORLD.servant.palerider.PaleRiderDamageTypes.CONCEPT_SWORD, this, livingOwner), this.damage);
      }
      this.discard();
   }

   @Override
   public void tick() {
      super.tick();
      if (this.level().isClientSide()) {
         net.xxxjk.TYPE_MOON_WORLD.client.renderer.ProjectileVisualEffectHelper.captureTrace(this.tracePos, this, 64);
      } else if (this.level() instanceof ServerLevel level) {
         level.sendParticles(new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(0.95F, 0.02F, 0.025F), 1.25F),
            this.getX(), this.getY(), this.getZ(), 4, 0.08, 0.08, 0.08, 0.01);
         level.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL, this.getX(), this.getY(), this.getZ(), 1, 0.04, 0.04, 0.04, 0.01);
      }
      if (this.tickCount > 60) this.discard();
   }

   @Override
   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      if (this.ownerUuid != null) tag.putUUID("PaleRiderOwner", this.ownerUuid);
      tag.putFloat("Damage", this.damage);
   }

   @Override
   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      if (tag.hasUUID("PaleRiderOwner")) this.ownerUuid = tag.getUUID("PaleRiderOwner");
      this.damage = tag.getFloat("Damage");
      if (this.ownerUuid != null && this.level() instanceof ServerLevel level) {
         Entity owner = level.getEntity(this.ownerUuid);
         if (owner != null) this.setOwner(owner);
      }
   }
}

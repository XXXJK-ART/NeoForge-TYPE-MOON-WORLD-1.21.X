package net.xxxjk.TYPE_MOON_WORLD.entity;

import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;

/** Dedicated two-stage projectile used only by Unlimited Blade Works sword control. */
public class UbwControlledSwordEntity extends ThrowableItemProjectile {
   public static final int STATE_RISING = 0;
   public static final int STATE_WAITING = 1;
   public static final int STATE_REPOSITION_UP = 2;
   public static final int STATE_REPOSITION_IN = 3;
   public static final int STATE_ATTACKING = 4;
   public static final int MAX_LIFETIME = 20 * 45;
   private static final double POSITION_SPEED = 1.25D;
   private static final double ATTACK_SPEED = 2.6D;

   private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(
      UbwControlledSwordEntity.class, EntityDataSerializers.INT);
   private Vec3 waypoint = Vec3.ZERO;
   private Vec3 attackDirection = Vec3.ZERO;
   private UUID controllerId;

   public UbwControlledSwordEntity(EntityType<? extends ThrowableItemProjectile> type, Level level) {
      super(type, level);
   }

   public UbwControlledSwordEntity(Level level, LivingEntity owner, ItemStack weapon, Vec3 position) {
      super(ModEntities.UBW_CONTROLLED_SWORD.get(), owner, level);
      this.controllerId = owner.getUUID();
      this.setItem(weapon.copyWithCount(1));
      this.setPos(position);
      this.setNoGravity(true);
      this.waypoint = position.add(0.0D, 10.0D, 0.0D);
      this.entityData.set(STATE, STATE_RISING);
   }

   @Override
   protected Item getDefaultItem() {
      return Items.IRON_SWORD;
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(STATE, STATE_RISING);
   }

   public int controlState() {
      return this.entityData.get(STATE);
   }

   public boolean isWaitingFor(UUID ownerId) {
      return ownerId != null && ownerId.equals(this.controllerId)
         && (controlState() == STATE_RISING || controlState() == STATE_WAITING);
   }

   public void release(Vec3 firstWaypoint, Vec3 secondWaypoint, Vec3 direction) {
      if (direction == null || direction.lengthSqr() < 1.0E-6D) {
         return;
      }
      this.attackDirection = direction.normalize();
      if (firstWaypoint != null && secondWaypoint != null) {
         this.waypoint = firstWaypoint;
         this.entityData.set(STATE, STATE_REPOSITION_UP);
         this.getPersistentData().putDouble("ControlSecondX", secondWaypoint.x);
         this.getPersistentData().putDouble("ControlSecondY", secondWaypoint.y);
         this.getPersistentData().putDouble("ControlSecondZ", secondWaypoint.z);
      } else {
         beginAttack();
      }
   }

   @Override
   public void tick() {
      if (!this.level().isClientSide) {
         Entity owner = getOwner();
         if (this.tickCount >= MAX_LIFETIME || owner == null || !owner.isAlive() || owner.isRemoved()
            || this.controllerId == null || !this.controllerId.equals(owner.getUUID())) {
            this.discard();
            return;
         }

         int state = controlState();
         if (state == STATE_RISING || state == STATE_REPOSITION_UP || state == STATE_REPOSITION_IN) {
            moveTowardWaypoint(state);
         } else if (state == STATE_WAITING) {
            this.setDeltaMovement(Vec3.ZERO);
         }
      }

      updateRotation();
      super.tick();
   }

   private void moveTowardWaypoint(int state) {
      Vec3 offset = this.waypoint.subtract(this.position());
      if (offset.lengthSqr() <= POSITION_SPEED * POSITION_SPEED) {
         this.setPos(this.waypoint);
         this.setDeltaMovement(Vec3.ZERO);
         if (state == STATE_RISING) {
            this.entityData.set(STATE, STATE_WAITING);
         } else if (state == STATE_REPOSITION_UP) {
            this.waypoint = new Vec3(
               this.getPersistentData().getDouble("ControlSecondX"),
               this.getPersistentData().getDouble("ControlSecondY"),
               this.getPersistentData().getDouble("ControlSecondZ"));
            this.entityData.set(STATE, STATE_REPOSITION_IN);
         } else {
            beginAttack();
         }
      } else {
         this.setDeltaMovement(offset.normalize().scale(POSITION_SPEED));
      }
   }

   private void beginAttack() {
      this.entityData.set(STATE, STATE_ATTACKING);
      this.setNoGravity(true);
      this.setDeltaMovement(this.attackDirection.normalize().scale(ATTACK_SPEED));
   }

   protected void updateRotation() {
      Vec3 motion = this.getDeltaMovement();
      if (motion.lengthSqr() < 1.0E-6D) {
         return;
      }
      double horizontal = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
      this.setXRot((float)Math.toDegrees(Math.atan2(motion.y, horizontal)));
      this.setYRot((float)Math.toDegrees(Math.atan2(motion.x, motion.z)));
      this.xRotO = this.getXRot();
      this.yRotO = this.getYRot();
   }

   @Override
   protected void onHit(HitResult result) {
      if (this.level().isClientSide || controlState() != STATE_ATTACKING) {
         return;
      }
      if (result instanceof EntityHitResult entityHit) {
         Entity target = entityHit.getEntity();
         if (target == getOwner() || EntityUtils.isImmunePlayerTarget(target)) {
            return;
         }
         target.invulnerableTime = 0;
         target.hurt(this.damageSources().thrown(this, getOwner()), weaponDamage());
         target.invulnerableTime = 0;
      }
      this.discard();
   }

   private float weaponDamage() {
      ItemAttributeModifiers modifiers = this.getItem().getOrDefault(
         DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
      return (float)Math.max(4.0D, modifiers.compute(1.0D, EquipmentSlot.MAINHAND));
   }

   @Override
   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      tag.putInt("ControlState", controlState());
      putVec(tag, "Waypoint", this.waypoint);
      putVec(tag, "Attack", this.attackDirection);
      if (this.controllerId != null) {
         tag.putUUID("Controller", this.controllerId);
      }
   }

   @Override
   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      this.entityData.set(STATE, tag.getInt("ControlState"));
      this.waypoint = getVec(tag, "Waypoint");
      this.attackDirection = getVec(tag, "Attack");
      this.controllerId = tag.hasUUID("Controller") ? tag.getUUID("Controller") : null;
      this.setNoGravity(true);
   }

   private static void putVec(CompoundTag tag, String key, Vec3 value) {
      tag.putDouble(key + "X", value.x);
      tag.putDouble(key + "Y", value.y);
      tag.putDouble(key + "Z", value.z);
   }

   private static Vec3 getVec(CompoundTag tag, String key) {
      return new Vec3(tag.getDouble(key + "X"), tag.getDouble(key + "Y"), tag.getDouble(key + "Z"));
   }
}
